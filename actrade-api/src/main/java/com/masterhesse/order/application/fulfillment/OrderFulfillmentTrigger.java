package com.masterhesse.order.application.fulfillment;

import java.util.UUID;

public interface OrderFulfillmentTrigger {
    void onOrderPaid(UUID orderId);
}