package com.masterhesse.order.application;

import com.masterhesse.common.exception.BusinessException;
import com.masterhesse.common.exception.ResourceNotFoundException;
import com.masterhesse.order.application.event.OrderCompletedEvent;
import com.masterhesse.order.domain.FulfillmentStatus;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderStatus;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.persistence.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OrderReceiptService {

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderReceiptService(OrderRepository orderRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 用户手动确认收货
     */
    @Transactional
    public void confirmReceipt(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("订单不存在: " + orderId));

        // 幂等：已完成则直接返回
        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "未支付订单不允许确认收货");
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERING) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "当前订单状态不允许确认收货: " + order.getOrderStatus()
            );
        }

        LocalDateTime now = LocalDateTime.now();
        completeOrder(order, now, false);
    }

    /**
     * 扫描超时未确认收货订单并自动确认
     */
    @Transactional
    public int autoConfirmTimeoutOrders() {
        List<UUID> orderIds = orderRepository.findTimeoutConfirmOrderIds(LocalDateTime.now());
        int changedCount = 0;

        for (UUID orderId : orderIds) {
            try {
                if (autoConfirmTimeoutOrder(orderId)) {
                    changedCount++;
                }
            } catch (Exception ex) {
                log.error("Auto confirm receipt failed, orderId={}", orderId, ex);
            }
        }

        return changedCount;
    }

    /**
     * 自动确认单个超时订单
     */
    @Transactional
    public boolean autoConfirmTimeoutOrder(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return false;
        }

        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            return false;
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERING) {
            return false;
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (order.getConfirmDeadlineAt() == null || order.getConfirmDeadlineAt().isAfter(now)) {
            return false;
        }

        completeOrder(order, now, true);
        return true;
    }

    private void completeOrder(Order order, LocalDateTime confirmTime, boolean autoConfirmed) {
        order.setOrderStatus(OrderStatus.COMPLETED);

        if (order.getConfirmedAt() == null) {
            order.setConfirmedAt(confirmTime);
        }

        order.setConfirmDeadlineAt(null);

        if (order.getFulfillmentStatus() == null) {
            order.setFulfillmentStatus(FulfillmentStatus.SUCCESS);
        }

        Order savedOrder = orderRepository.saveAndFlush(order);

        eventPublisher.publishEvent(new OrderCompletedEvent(savedOrder.getOrderId()));

        log.info(
                "Order receipt confirmed. orderId={}, orderNo={}, autoConfirmed={}",
                savedOrder.getOrderId(),
                savedOrder.getOrderNo(),
                autoConfirmed
        );
    }
}