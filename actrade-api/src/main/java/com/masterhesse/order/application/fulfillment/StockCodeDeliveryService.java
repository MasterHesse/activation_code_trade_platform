package com.masterhesse.order.application.fulfillment;

import com.masterhesse.order.domain.Order;

public interface StockCodeDeliveryService {
    void assignAndDeliver(Order order);
}