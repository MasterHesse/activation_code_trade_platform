package com.masterhesse.order.application.event;

import com.masterhesse.order.application.fulfillment.OrderFulfillmentTrigger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderPaidEventListener {

    private final OrderFulfillmentTrigger orderFulfillmentTrigger;

    public OrderPaidEventListener(OrderFulfillmentTrigger orderFulfillmentTrigger) {
        this.orderFulfillmentTrigger = orderFulfillmentTrigger;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OrderPaidEvent event) {
        orderFulfillmentTrigger.onOrderPaid(event.orderId());
    }
}