package com.masterhesse.order.application.fulfillment;

import com.masterhesse.order.domain.Order;
import org.springframework.stereotype.Service;

@Service
public class DefaultStockCodeDeliveryService implements StockCodeDeliveryService {
    @Override
    public void assignAndDeliver(Order order) {
        throw new UnsupportedOperationException("库存码发货逻辑暂未实现");
    }
}