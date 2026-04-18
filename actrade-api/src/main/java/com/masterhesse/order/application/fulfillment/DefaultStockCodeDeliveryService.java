package com.masterhesse.order.application.fulfillment;

import com.masterhesse.activation.domain.entity.ActivationCodeInventory;
import com.masterhesse.activation.persistence.ActivationCodeInventoryRepository;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderItem;
import com.masterhesse.order.persistence.OrderItemRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class DefaultStockCodeDeliveryService implements StockCodeDeliveryService {

    private final ActivationCodeInventoryRepository codeRepository;
    private final OrderItemRepository orderItemRepository;

    public DefaultStockCodeDeliveryService(
            ActivationCodeInventoryRepository codeRepository,
            OrderItemRepository orderItemRepository) {
        this.codeRepository = codeRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Override
    @Transactional
    public void assignAndDeliver(Order order) {
        log.info("Stock code delivery start. orderId={}, orderNo={}",
                order.getOrderId(), order.getOrderNo());

        List<OrderItem> items = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(order.getOrderId());
        if (items.isEmpty()) {
            throw new IllegalStateException("订单没有商品明细: " + order.getOrderNo());
        }

        List<UUID> lockedCodeIds = new ArrayList<>();

        try {
            // 逐个订单项分配激活码
            for (OrderItem item : items) {
                int requiredQty = item.getQuantity();
                int lockedQty = assignCodesForItem(order, item, requiredQty);
                lockedCodeIds.addAll(extractCodeIds(item.getAssignedCodeIds()));

                log.info("Assigned {} codes for orderItem. orderId={}, productId={}, required={}, locked={}",
                        lockedQty, order.getOrderId(), item.getProductId(), requiredQty, lockedQty);

                if (lockedQty < requiredQty) {
                    log.warn("Insufficient stock codes. orderId={}, productId={}, required={}, available={}",
                            order.getOrderId(), item.getProductId(), requiredQty, lockedQty);
                }
            }

            // 标记为已售
            if (!lockedCodeIds.isEmpty()) {
                codeRepository.markAsSold(lockedCodeIds, LocalDateTime.now());
            }

            log.info("Stock code delivery completed. orderId={}, orderNo={}, totalCodes={}",
                    order.getOrderId(), order.getOrderNo(), lockedCodeIds.size());

        } catch (Exception e) {
            // 失败时释放已锁定的激活码
            if (!lockedCodeIds.isEmpty()) {
                codeRepository.releaseCodes(lockedCodeIds, LocalDateTime.now());
                log.warn("Stock code delivery failed, released {} codes. orderId={}",
                        lockedCodeIds.size(), order.getOrderId());
            }
            throw e;
        }
    }

    private int assignCodesForItem(Order order, OrderItem item, int requiredQty) {
        // 查询可用激活码
        List<ActivationCodeInventory> availableCodes = codeRepository.findAvailableByProductId(
                item.getProductId(),
                LocalDateTime.now(),
                PageRequest.of(0, requiredQty + 10) // 多查询一些以防不足
        );

        if (availableCodes.isEmpty()) {
            log.error("No available codes for product. orderId={}, productId={}",
                    order.getOrderId(), item.getProductId());
            return 0;
        }

        // 锁定激活码
        List<UUID> codeIds = availableCodes.stream()
                .limit(requiredQty)
                .map(ActivationCodeInventory::getCodeId)
                .toList();

        int lockedCount = codeRepository.lockCodes(
                codeIds,
                order.getOrderId(),
                item.getOrderItemId(),
                LocalDateTime.now()
        );

        if (lockedCount > 0) {
            // 更新订单项的激活码 ID
            List<ActivationCodeInventory> lockedCodes = availableCodes.stream()
                    .limit(lockedCount)
                    .toList();

            List<String> codeIdsStr = lockedCodes.stream()
                    .map(c -> c.getCodeId().toString())
                    .toList();

            item.assignCodes(codeIdsStr);
            item.setDeliveryStatus("DELIVERED");
            orderItemRepository.save(item);
        }

        return lockedCount;
    }

    private List<UUID> extractCodeIds(List<String> codeIdsStr) {
        if (codeIdsStr == null || codeIdsStr.isEmpty()) {
            return List.of();
        }
        return codeIdsStr.stream()
                .map(UUID::fromString)
                .toList();
    }
}