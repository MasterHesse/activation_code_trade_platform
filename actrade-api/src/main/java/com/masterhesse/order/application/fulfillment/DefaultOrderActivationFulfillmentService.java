package com.masterhesse.order.application.fulfillment;

import com.masterhesse.order.domain.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DefaultOrderActivationFulfillmentService implements OrderActivationFulfillmentService {

    @Override
    public void generateAndDeliver(Order order) {
        log.info("Activation fulfillment start. orderId={}, orderNo={}",
                order.getOrderId(), order.getOrderNo());

        throw new UnsupportedOperationException("激活工具履约逻辑暂未实现");
    }
}