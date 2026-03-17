package com.masterhesse.order.application.event;

import com.masterhesse.order.application.settlement.OrderSettlementService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderCompletedEventListener {

    private final OrderSettlementService orderSettlementService;

    public OrderCompletedEventListener(OrderSettlementService orderSettlementService) {
        this.orderSettlementService = orderSettlementService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderCompletedEvent event) {
        orderSettlementService.createPendingSettlement(event.orderId());
    }
}