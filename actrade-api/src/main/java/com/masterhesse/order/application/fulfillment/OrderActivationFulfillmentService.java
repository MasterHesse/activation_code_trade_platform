package com.masterhesse.order.application.fulfillment;

import com.masterhesse.order.domain.Order;

public interface OrderActivationFulfillmentService {
    void generateAndDeliver(Order order);
}