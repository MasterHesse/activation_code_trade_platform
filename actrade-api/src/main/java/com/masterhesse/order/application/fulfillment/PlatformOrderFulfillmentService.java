package com.masterhesse.order.application.fulfillment;

import com.masterhesse.order.domain.FulfillmentStatus;
import com.masterhesse.order.domain.FulfillmentType;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderStatus;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.persistence.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class PlatformOrderFulfillmentService implements OrderFulfillmentTrigger {

    private final OrderRepository orderRepository;
    private final OrderActivationFulfillmentService orderActivationFulfillmentService;
    private final StockCodeDeliveryService stockCodeDeliveryService;

    public PlatformOrderFulfillmentService(OrderRepository orderRepository,
                                           OrderActivationFulfillmentService orderActivationFulfillmentService,
                                           StockCodeDeliveryService stockCodeDeliveryService) {
        this.orderRepository = orderRepository;
        this.orderActivationFulfillmentService = orderActivationFulfillmentService;
        this.stockCodeDeliveryService = stockCodeDeliveryService;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderPaid(UUID orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null) {
            return;
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            return;
        }

        if (order.getFulfillmentStatus() == FulfillmentStatus.SUCCESS) {
            return;
        }

        order.setFulfillmentStatus(FulfillmentStatus.PROCESSING);
        orderRepository.saveAndFlush(order);

        try {
            if (order.getFulfillmentType() == FulfillmentType.ACTIVATION_TOOL) {
                orderActivationFulfillmentService.generateAndDeliver(order);
            } else {
                stockCodeDeliveryService.assignAndDeliver(order);
            }

            order.setFulfillmentStatus(FulfillmentStatus.SUCCESS);
            order.setOrderStatus(OrderStatus.DELIVERING);
            order.setDeliveredAt(LocalDateTime.now());
            order.setConfirmDeadlineAt(LocalDateTime.now().plusHours(24));

            orderRepository.save(order);

            log.info("Order fulfillment success. orderId={}, orderNo={}", order.getOrderId(), order.getOrderNo());
        } catch (Exception ex) {
            order.setFulfillmentStatus(FulfillmentStatus.FAILED);
            order.setOrderStatus(OrderStatus.DELIVERY_FAILED);
            orderRepository.save(order);

            log.error("Order fulfillment failed. orderId={}, orderNo={}", order.getOrderId(), order.getOrderNo(), ex);
        }
    }
}