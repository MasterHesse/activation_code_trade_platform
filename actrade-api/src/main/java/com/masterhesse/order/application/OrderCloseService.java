package com.masterhesse.order.application;

import com.masterhesse.order.application.event.OrderPaidEvent;
import com.masterhesse.order.application.payment.PaymentCloseResult;
import com.masterhesse.order.application.payment.PaymentProcessor;
import com.masterhesse.order.application.payment.PaymentProcessorFactory;
import com.masterhesse.order.application.payment.PaymentQueryResult;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderStatus;
import com.masterhesse.order.domain.PaymentMethod;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.persistence.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderCloseService {

    private final OrderRepository orderRepository;
    private final PaymentProcessorFactory paymentProcessorFactory;
    private final ApplicationEventPublisher eventPublisher;

    public OrderCloseService(OrderRepository orderRepository,
                             PaymentProcessorFactory paymentProcessorFactory,
                             ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.paymentProcessorFactory = paymentProcessorFactory;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 扫描并关闭超时未支付订单
     */
    @Transactional
    public int closeExpiredOrders() {
        List<UUID> orderIds = orderRepository.findExpiredUnpaidOrderIds(LocalDateTime.now());
        int changedCount = 0;

        for (UUID orderId : orderIds) {
            try {
                if (closeExpiredOrder(orderId)) {
                    changedCount++;
                }
            } catch (Exception ex) {
                log.error("Close expired order failed, orderId={}", orderId, ex);
            }
        }

        return changedCount;
    }

    /**
     * 关闭单个超时未支付订单
     */
    @Transactional
    public boolean closeExpiredOrder(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return false;
        }

        if (order.getOrderStatus() != OrderStatus.CREATED) {
            return false;
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (order.getPayDeadlineAt() == null || order.getPayDeadlineAt().isAfter(now)) {
            return false;
        }

        boolean wasPaid = order.getPaymentStatus() == PaymentStatus.PAID;
        boolean changed = false;

        PaymentMethod paymentMethod = order.getPaymentMethod();
        if (paymentMethod != null && StringUtils.hasText(order.getPaymentRequestNo())) {
            PaymentProcessor processor = paymentProcessorFactory.getProcessor(paymentMethod);

            PaymentQueryResult queryResult = processor.query(order);
            applyQueryResult(order, queryResult, now);

            if (order.getPaymentStatus() == PaymentStatus.PAID) {
                changed = true;
            } else if (order.getPaymentStatus() == PaymentStatus.CLOSED) {
                changed = true;
            } else {
                PaymentCloseResult closeResult = processor.close(order);

                if (closeResult.isPaid()) {
                    markPaid(order, now);
                } else {
                    markClosed(order, now);
                }
                changed = true;
            }
        } else {
            markClosed(order, now);
            changed = true;
        }

        if (!changed) {
            return false;
        }

        Order savedOrder = orderRepository.saveAndFlush(order);

        if (!wasPaid && savedOrder.getPaymentStatus() == PaymentStatus.PAID) {
            eventPublisher.publishEvent(new OrderPaidEvent(savedOrder.getOrderId()));
        }

        log.info("Expired order processed. orderId={}, orderNo={}, orderStatus={}, paymentStatus={}",
                savedOrder.getOrderId(),
                savedOrder.getOrderNo(),
                savedOrder.getOrderStatus(),
                savedOrder.getPaymentStatus());

        return true;
    }

    private void applyQueryResult(Order order, PaymentQueryResult queryResult, LocalDateTime now) {
        if (StringUtils.hasText(queryResult.paymentRequestNo())) {
            order.setPaymentRequestNo(queryResult.paymentRequestNo());
        }
        if (StringUtils.hasText(queryResult.channelTradeNo())) {
            order.setChannelTradeNo(queryResult.channelTradeNo());
        }

        switch (queryResult.paymentStatus()) {
            case PAYING -> order.setPaymentStatus(PaymentStatus.PAYING);

            case PAID -> markPaid(order, now);

            case CLOSED -> markClosed(order, now);

            case FAILED -> order.setPaymentStatus(PaymentStatus.FAILED);

            case REFUNDED -> order.setPaymentStatus(PaymentStatus.REFUNDED);

            case UNPAID -> order.setPaymentStatus(PaymentStatus.UNPAID);
        }
    }

    private void markPaid(Order order, LocalDateTime now) {
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.PAID);

        if (order.getPaidAt() == null) {
            order.setPaidAt(now);
        }
    }

    private void markClosed(Order order, LocalDateTime now) {
        order.setPaymentStatus(PaymentStatus.CLOSED);

        if (order.getOrderStatus() == OrderStatus.CREATED) {
            order.setOrderStatus(OrderStatus.CANCELED);
        }

        if (order.getClosedAt() == null) {
            order.setClosedAt(now);
        }
    }
}