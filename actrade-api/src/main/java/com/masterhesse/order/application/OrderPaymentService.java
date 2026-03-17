package com.masterhesse.order.application;

import com.masterhesse.common.exception.BusinessException;
import com.masterhesse.common.exception.ResourceNotFoundException;
import com.masterhesse.order.api.request.PayOrderRequest;
import com.masterhesse.order.api.request.PaymentInitiateRequest;
import com.masterhesse.order.api.response.OrderResponse;
import com.masterhesse.order.api.response.PaymentInitiateResponse;
import com.masterhesse.order.application.event.OrderPaidEvent;
import com.masterhesse.order.application.payment.PaymentExecuteResult;
import com.masterhesse.order.application.payment.PaymentProcessor;
import com.masterhesse.order.application.payment.PaymentProcessorFactory;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderItem;
import com.masterhesse.order.domain.OrderStatus;
import com.masterhesse.order.domain.PaymentMethod;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.persistence.OrderItemRepository;
import com.masterhesse.order.persistence.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class OrderPaymentService {

    private static final DateTimeFormatter PAYMENT_NO_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentProcessorFactory paymentProcessorFactory;
    private final ApplicationEventPublisher eventPublisher;

    public OrderPaymentService(OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               PaymentProcessorFactory paymentProcessorFactory,
                               ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentProcessorFactory = paymentProcessorFactory;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 兼容旧接口：只允许同步支付方式
     */
    @Transactional
    public OrderResponse payOrder(UUID orderId, PayOrderRequest request) {
        if (request.getPaymentMethod() == PaymentMethod.ALIPAY) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "ALIPAY 请调用 /api/orders/{orderId}/payment-initiate"
            );
        }

        PaymentInitiateResponse response =
                initiatePayment(orderId, PaymentInitiateRequest.of(request.getPaymentMethod()));

        if (!response.isPaid()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "当前支付方式未同步支付成功");
        }

        Order order = findOrderOrThrow(orderId);
        return buildOrderResponse(order);
    }

    /**
     * 新统一支付发起接口
     * 并发安全策略：
     * 1. 先对订单行加 PESSIMISTIC_WRITE 锁
     * 2. Order 上再加 @Version 兜底
     * 3. payment_request_no 唯一索引
     */
    @Transactional
    public PaymentInitiateResponse initiatePayment(UUID orderId, PaymentInitiateRequest request) {
        Order order = findOrderForUpdateOrThrow(orderId);

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单已支付，不允许重复发起支付");
        }

        validateCanPay(order, request.getPaymentMethod());

        boolean reuseCurrentPayingAttempt =
                order.getPaymentStatus() == PaymentStatus.PAYING
                        && order.getPaymentMethod() == request.getPaymentMethod()
                        && StringUtils.hasText(order.getPaymentRequestNo());

        if (order.getPaymentStatus() == PaymentStatus.PAYING && !reuseCurrentPayingAttempt) {
            throw new BusinessException(HttpStatus.CONFLICT, "订单已有支付中的单据，请勿切换支付方式重复发起");
        }

        if (!reuseCurrentPayingAttempt) {
            order.setPaymentMethod(request.getPaymentMethod());
            order.setPaymentRequestNo(generatePaymentRequestNo());
            order.setChannelTradeNo(null);
        }

        PaymentProcessor processor = paymentProcessorFactory.getProcessor(request.getPaymentMethod());
        PaymentExecuteResult result = processor.initiate(order, request);

        boolean paidJustNow = applyPaymentResult(order, result);
        Order savedOrder = orderRepository.saveAndFlush(order);

        if (paidJustNow) {
            eventPublisher.publishEvent(new OrderPaidEvent(savedOrder.getOrderId()));
        }

        log.info("Payment initiated. orderId={}, orderNo={}, paymentMethod={}, paymentRequestNo={}, paymentStatus={}",
                savedOrder.getOrderId(),
                savedOrder.getOrderNo(),
                savedOrder.getPaymentMethod(),
                savedOrder.getPaymentRequestNo(),
                savedOrder.getPaymentStatus());

        return PaymentInitiateResponse.from(savedOrder, result.getMessage(), result.getChannelData());
    }

    /**
     * 支付宝异步通知
     * 第三方回调不包装 ApiResponse
     */
    @Transactional
    public String handleAlipayNotify(Map<String, String> params) {
        try {
            String outTradeNo = params.get("out_trade_no");
            if (!StringUtils.hasText(outTradeNo)) {
                log.warn("Alipay notify missing out_trade_no, params={}", params);
                return "failure";
            }

            Order order = orderRepository.findByPaymentRequestNoForUpdate(outTradeNo).orElse(null);
            if (order == null) {
                log.warn("Alipay notify order not found, outTradeNo={}, params={}", outTradeNo, params);
                return "failure";
            }

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                log.info("Alipay notify ignored because order already paid. orderNo={}, paymentRequestNo={}",
                        order.getOrderNo(), order.getPaymentRequestNo());
                return "success";
            }

            if (order.getOrderStatus() == OrderStatus.CANCELED) {
                log.warn("Alipay notify ignored because order already canceled. orderNo={}, paymentRequestNo={}",
                        order.getOrderNo(), order.getPaymentRequestNo());
                return "success";
            }

            order.setPaymentMethod(PaymentMethod.ALIPAY);

            PaymentProcessor processor = paymentProcessorFactory.getProcessor(PaymentMethod.ALIPAY);
            PaymentExecuteResult result = processor.handleCallback(order, params);

            boolean paidJustNow = applyPaymentResult(order, result);
            Order savedOrder = orderRepository.saveAndFlush(order);

            if (paidJustNow) {
                eventPublisher.publishEvent(new OrderPaidEvent(savedOrder.getOrderId()));
            }

            log.info("Alipay notify handled. orderNo={}, paymentRequestNo={}, paymentStatus={}, channelTradeNo={}",
                    savedOrder.getOrderNo(),
                    savedOrder.getPaymentRequestNo(),
                    savedOrder.getPaymentStatus(),
                    savedOrder.getChannelTradeNo());

            return "success";
        } catch (Exception ex) {
            log.error("Handle alipay notify failed, params={}", params, ex);
            return "failure";
        }
    }

    private void validateCanPay(Order order, PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "paymentMethod 不能为空");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已取消订单不允许支付");
        }

        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "当前订单状态不允许支付: " + order.getOrderStatus());
        }

        if (order.getPaymentStatus() == PaymentStatus.CLOSED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已关闭订单不允许支付");
        }

        if (order.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "已退款订单不允许重新支付");
        }

        if (order.getPayDeadlineAt() != null && !order.getPayDeadlineAt().isAfter(LocalDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "订单已超出支付时效");
        }

        if (order.getPayAmount() == null || order.getPayAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "订单支付金额必须大于 0");
        }
    }

    private boolean applyPaymentResult(Order order, PaymentExecuteResult result) {
        if (StringUtils.hasText(result.getPaymentRequestNo())) {
            order.setPaymentRequestNo(result.getPaymentRequestNo());
        }
        if (StringUtils.hasText(result.getChannelTradeNo())) {
            order.setChannelTradeNo(result.getChannelTradeNo());
        }

        boolean paidJustNow =
                order.getPaymentStatus() != PaymentStatus.PAID
                        && result.getPaymentStatus() == PaymentStatus.PAID;

        switch (result.getPaymentStatus()) {
            case PAYING -> order.setPaymentStatus(PaymentStatus.PAYING);

            case PAID -> {
                order.setPaymentStatus(PaymentStatus.PAID);
                order.setOrderStatus(OrderStatus.PAID);
                if (order.getPaidAt() == null) {
                    order.setPaidAt(LocalDateTime.now());
                }
            }

            case FAILED -> order.setPaymentStatus(PaymentStatus.FAILED);

            case CLOSED -> {
                order.setPaymentStatus(PaymentStatus.CLOSED);
                if (order.getOrderStatus() == OrderStatus.CREATED) {
                    order.setOrderStatus(OrderStatus.CANCELED);
                }
                if (order.getClosedAt() == null) {
                    order.setClosedAt(LocalDateTime.now());
                }
            }

            case REFUNDED -> order.setPaymentStatus(PaymentStatus.REFUNDED);

            case UNPAID -> order.setPaymentStatus(PaymentStatus.UNPAID);
        }

        return paidJustNow;
    }

    private String generatePaymentRequestNo() {
        for (int i = 0; i < 10; i++) {
            String candidate = "PAY"
                    + LocalDateTime.now().format(PAYMENT_NO_TIME_FORMATTER)
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

            if (!orderRepository.existsByPaymentRequestNo(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "支付单号生成失败，请稍后重试");
    }

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));
    }

    private Order findOrderForUpdateOrThrow(UUID orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));
    }

    private OrderResponse buildOrderResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(order.getOrderId());
        return OrderResponse.from(order, items);
    }
}