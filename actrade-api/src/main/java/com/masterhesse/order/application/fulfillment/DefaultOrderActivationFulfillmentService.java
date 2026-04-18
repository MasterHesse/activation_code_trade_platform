package com.masterhesse.order.application.fulfillment;

import com.masterhesse.activation.application.ActivationTaskService;
import com.masterhesse.activation.api.request.CreateActivationTaskRequest;
import com.masterhesse.activation.api.response.ActivationTaskResponse;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderItem;
import com.masterhesse.order.persistence.OrderItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DefaultOrderActivationFulfillmentService implements OrderActivationFulfillmentService {

    private final ActivationTaskService activationTaskService;
    private final OrderItemRepository orderItemRepository;

    public DefaultOrderActivationFulfillmentService(
            ActivationTaskService activationTaskService,
            OrderItemRepository orderItemRepository) {
        this.activationTaskService = activationTaskService;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public void generateAndDeliver(Order order) {
        log.info("Activation fulfillment start. orderId={}, orderNo={}",
                order.getOrderId(), order.getOrderNo());

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(order.getOrderId());
        if (items.isEmpty()) {
            throw new IllegalStateException("订单没有商品明细: " + order.getOrderNo());
        }

        for (OrderItem item : items) {
            if (item.getActivationToolId() == null) {
                log.warn("Order item has no activation tool configured. orderId={}, orderItemId={}, productId={}",
                        order.getOrderId(), item.getOrderItemId(), item.getProductId());
                item.setDeliveryStatus("NO_TOOL_CONFIGURED");
                orderItemRepository.save(item);
                continue;
            }

            try {
                // 更新发货状态为处理中
                item.setDeliveryStatus("PROCESSING");
                orderItemRepository.save(item);

                // 创建激活任务
                CreateActivationTaskRequest request = new CreateActivationTaskRequest();
                request.setOrderId((long) order.getOrderId().hashCode()); // 临时方案：使用 hashCode
                request.setOrderItemId((long) item.getOrderItemId().hashCode());
                request.setMerchantId((long) order.getMerchantId().hashCode());
                request.setActivationToolId(item.getActivationToolId());
                request.setActivationToolVersionId(item.getActivationToolVersionId());
                request.setMaxAttempts(3);
                request.setPayloadJson("{}"); // 默认空 JSON

                ActivationTaskResponse taskResponse = activationTaskService.createTask(request);

                // 更新订单项的任务 ID
                item.setActivationTaskId(taskResponse.getId());
                item.setDeliveryStatus("DELIVERED");
                orderItemRepository.save(item);

                log.info("Activation task created. orderId={}, orderItemId={}, taskId={}, taskNo={}",
                        order.getOrderId(), item.getOrderItemId(), taskResponse.getId(), taskResponse.getTaskNo());

            } catch (Exception e) {
                item.setDeliveryStatus("FAILED");
                orderItemRepository.save(item);

                log.error("Failed to create activation task. orderId={}, orderItemId={}, productId={}",
                        order.getOrderId(), item.getOrderItemId(), item.getProductId(), e);
            }
        }

        log.info("Activation fulfillment completed. orderId={}, orderNo={}",
                order.getOrderId(), order.getOrderNo());
    }
}