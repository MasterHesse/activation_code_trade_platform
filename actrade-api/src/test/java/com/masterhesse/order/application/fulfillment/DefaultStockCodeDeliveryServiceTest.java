package com.masterhesse.order.application.fulfillment;

import com.masterhesse.activation.domain.entity.ActivationCodeInventory;
import com.masterhesse.activation.domain.enums.ActivationCodeStatus;
import com.masterhesse.activation.persistence.ActivationCodeInventoryRepository;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderItem;
import com.masterhesse.order.domain.OrderStatus;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.persistence.OrderItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("库存码发货服务单元测试")
class DefaultStockCodeDeliveryServiceTest {

    @Mock
    private ActivationCodeInventoryRepository codeRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private DefaultStockCodeDeliveryService deliveryService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private UUID orderId;
    private UUID productId;
    private UUID merchantId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        merchantId = UUID.randomUUID();

        testOrder = new Order();
        testOrder.setOrderId(orderId);
        testOrder.setOrderNo("ORD-20260409-001");
        testOrder.setUserId(UUID.randomUUID());
        testOrder.setMerchantId(merchantId);
        testOrder.setTotalAmount(BigDecimal.valueOf(100));
        testOrder.setPayAmount(BigDecimal.valueOf(100));
        testOrder.setOrderStatus(OrderStatus.PAID);
        testOrder.setPaymentStatus(PaymentStatus.PAID);

        testOrderItem = new OrderItem();
        testOrderItem.setOrderItemId(UUID.randomUUID());
        testOrderItem.setOrderId(orderId);
        testOrderItem.setProductId(productId);
        testOrderItem.setProductName("测试商品");
        testOrderItem.setQuantity(2);
        testOrderItem.setUnitPrice(BigDecimal.valueOf(50));
        testOrderItem.setSubtotalAmount(BigDecimal.valueOf(100));
        testOrderItem.setDeliveryStatus("PENDING");
    }

    @Test
    @DisplayName("成功分配库存码")
    void assignAndDeliver_Success() {
        // Given
        List<ActivationCodeInventory> availableCodes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ActivationCodeInventory code = createAvailableCode(productId, merchantId);
            availableCodes.add(code);
        }

        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of(testOrderItem));
        when(codeRepository.findAvailableByProductId(eq(productId), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(availableCodes);
        when(codeRepository.lockCodes(anyList(), eq(orderId), any(UUID.class), any(LocalDateTime.class)))
                .thenReturn(2);
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(codeRepository.markAsSold(anyList(), any(LocalDateTime.class)))
                .thenReturn(2);

        // When
        assertDoesNotThrow(() -> deliveryService.assignAndDeliver(testOrder));

        // Then
        verify(codeRepository).lockCodes(anyList(), eq(orderId), eq(testOrderItem.getOrderItemId()), any(LocalDateTime.class));
        verify(codeRepository).markAsSold(anyList(), any(LocalDateTime.class));
        verify(orderItemRepository, atLeastOnce()).save(any(OrderItem.class));

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, atLeastOnce()).save(captor.capture());
        assertEquals("DELIVERED", captor.getValue().getDeliveryStatus());
    }

    @Test
    @DisplayName("库存不足时部分分配")
    void assignAndDeliver_InsufficientStock() {
        // Given
        List<ActivationCodeInventory> availableCodes = new ArrayList<>();
        ActivationCodeInventory code = createAvailableCode(productId, merchantId);
        availableCodes.add(code);

        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of(testOrderItem));
        when(codeRepository.findAvailableByProductId(eq(productId), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(availableCodes);
        when(codeRepository.lockCodes(anyList(), eq(orderId), any(UUID.class), any(LocalDateTime.class)))
                .thenReturn(1); // 只锁定1个，但需要2个
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(codeRepository.markAsSold(anyList(), any(LocalDateTime.class)))
                .thenReturn(1);

        // When
        assertDoesNotThrow(() -> deliveryService.assignAndDeliver(testOrder));

        // Then
        verify(orderItemRepository).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("无可用库存码时继续执行（记录警告）")
    void assignAndDeliver_NoAvailableCodes() {
        // Given
        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of(testOrderItem));
        when(codeRepository.findAvailableByProductId(eq(productId), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(new ArrayList<>());

        // When & Then - 不会抛出异常，但会记录警告
        assertDoesNotThrow(() -> deliveryService.assignAndDeliver(testOrder));
        verify(orderItemRepository, never()).save(any(OrderItem.class));
    }

    @Test
    @DisplayName("订单无商品明细时抛出异常")
    void assignAndDeliver_NoOrderItems() {
        // Given
        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(new ArrayList<>());

        // When & Then
        assertThrows(IllegalStateException.class, () -> deliveryService.assignAndDeliver(testOrder));
    }

    @Test
    @DisplayName("保存订单项失败时不释放码")
    void assignAndDeliver_SaveFailsNoRelease() {
        // Given
        List<ActivationCodeInventory> availableCodes = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ActivationCodeInventory code = createAvailableCode(productId, merchantId);
            availableCodes.add(code);
        }

        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of(testOrderItem));
        when(codeRepository.findAvailableByProductId(eq(productId), any(LocalDateTime.class), any(PageRequest.class)))
                .thenReturn(availableCodes);
        when(codeRepository.lockCodes(anyList(), eq(orderId), any(UUID.class), any(LocalDateTime.class)))
                .thenReturn(2);
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenThrow(new RuntimeException("数据库错误"));

        // When & Then
        assertThrows(RuntimeException.class, () -> deliveryService.assignAndDeliver(testOrder));

        // 验证没有释放码（因为 markAsSold 失败后才释放）
        verify(codeRepository, never()).releaseCodes(anyList(), any(LocalDateTime.class));
    }

    private ActivationCodeInventory createAvailableCode(UUID productId, UUID merchantId) {
        ActivationCodeInventory code = ActivationCodeInventory.create(
                productId,
                merchantId,
                "BATCH-001",
                "encrypted_value_" + UUID.randomUUID(),
                "****-****-****",
                "hash_" + UUID.randomUUID(),
                LocalDateTime.now().plusYears(1),
                "Test code"
        );
        // 使用反射设置 ID（因为实体已保存但未设置 ID）
        try {
            java.lang.reflect.Field idField = ActivationCodeInventory.class.getDeclaredField("codeId");
            idField.setAccessible(true);
            idField.set(code, UUID.randomUUID());
        } catch (Exception e) {
            // ignore
        }
        return code;
    }
}
