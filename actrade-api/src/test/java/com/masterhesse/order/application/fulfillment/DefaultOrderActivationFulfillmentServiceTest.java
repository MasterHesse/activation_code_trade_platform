package com.masterhesse.order.application.fulfillment;

import com.masterhesse.activation.application.ActivationTaskService;
import com.masterhesse.activation.api.response.ActivationTaskResponse;
import com.masterhesse.activation.domain.enums.ActivationTaskStatus;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("激活任务发货服务单元测试")
class DefaultOrderActivationFulfillmentServiceTest {

    @Mock
    private ActivationTaskService activationTaskService;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private DefaultOrderActivationFulfillmentService fulfillmentService;

    private Order testOrder;
    private OrderItem testOrderItem;
    private UUID orderId;
    private UUID productId;
    private UUID merchantId;
    private UUID toolId;
    private UUID toolVersionId;

    @BeforeEach
    void setUp() {
        orderId = UUID.randomUUID();
        productId = UUID.randomUUID();
        merchantId = UUID.randomUUID();
        toolId = UUID.randomUUID();
        toolVersionId = UUID.randomUUID();

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
        testOrderItem.setQuantity(1);
        testOrderItem.setUnitPrice(BigDecimal.valueOf(100));
        testOrderItem.setSubtotalAmount(BigDecimal.valueOf(100));
        testOrderItem.setActivationToolId(toolId);
        testOrderItem.setActivationToolVersionId(toolVersionId);
        testOrderItem.setDeliveryStatus("PENDING");
    }

    @Test
    @DisplayName("成功创建激活任务")
    void generateAndDeliver_Success() {
        // Given
        ActivationTaskResponse taskResponse = new ActivationTaskResponse();
        taskResponse.setId(1L);
        taskResponse.setTaskNo("AT-20260409-001");
        taskResponse.setStatus(ActivationTaskStatus.PENDING);

        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of(testOrderItem));
        when(activationTaskService.createTask(any())).thenReturn(taskResponse);
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> fulfillmentService.generateAndDeliver(testOrder));

        // Then
        verify(activationTaskService).createTask(any());
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, times(2)).save(captor.capture());

        List<OrderItem> savedItems = captor.getAllValues();
        // 第二次保存应该是 DELIVERED 状态
        assertEquals("DELIVERED", savedItems.get(1).getDeliveryStatus());
        assertEquals(1L, savedItems.get(1).getActivationTaskId());
    }

    @Test
    @DisplayName("订单无商品明细时抛出异常")
    void generateAndDeliver_NoOrderItems() {
        // Given
        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(new ArrayList<>());

        // When & Then
        assertThrows(IllegalStateException.class,
                () -> fulfillmentService.generateAndDeliver(testOrder));
    }

    @Test
    @DisplayName("订单项无激活工具配置时标记失败")
    void generateAndDeliver_NoToolConfigured() {
        // Given
        testOrderItem.setActivationToolId(null);
        testOrderItem.setActivationToolVersionId(null);

        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of(testOrderItem));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> fulfillmentService.generateAndDeliver(testOrder));

        // Then
        verify(activationTaskService, never()).createTask(any());
        verify(orderItemRepository, times(1)).save(any(OrderItem.class));

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository).save(captor.capture());
        assertEquals("NO_TOOL_CONFIGURED", captor.getValue().getDeliveryStatus());
    }

    @Test
    @DisplayName("创建激活任务失败时标记失败状态")
    void generateAndDeliver_TaskCreationFails() {
        // Given
        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of(testOrderItem));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(activationTaskService.createTask(any()))
                .thenThrow(new RuntimeException("创建任务失败"));

        // When
        assertDoesNotThrow(() -> fulfillmentService.generateAndDeliver(testOrder));

        // Then
        verify(activationTaskService).createTask(any());
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));

        ArgumentCaptor<OrderItem> captor = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderItemRepository, atLeastOnce()).save(captor.capture());
        // 最后一个保存应该是 FAILED 状态
        assertEquals("FAILED", captor.getValue().getDeliveryStatus());
    }

    @Test
    @DisplayName("处理多个订单项")
    void generateAndDeliver_MultipleItems() {
        // Given
        OrderItem item2 = new OrderItem();
        item2.setOrderItemId(UUID.randomUUID());
        item2.setOrderId(orderId);
        item2.setProductId(UUID.randomUUID());
        item2.setProductName("测试商品2");
        item2.setQuantity(1);
        item2.setActivationToolId(toolId);
        item2.setActivationToolVersionId(toolVersionId);
        item2.setDeliveryStatus("PENDING");

        ActivationTaskResponse taskResponse1 = new ActivationTaskResponse();
        taskResponse1.setId(1L);
        taskResponse1.setTaskNo("AT-20260409-001");

        ActivationTaskResponse taskResponse2 = new ActivationTaskResponse();
        taskResponse2.setId(2L);
        taskResponse2.setTaskNo("AT-20260409-002");

        when(orderItemRepository.findByOrderIdOrderByCreatedAtAsc(orderId))
                .thenReturn(List.of(testOrderItem, item2));
        when(activationTaskService.createTask(any()))
                .thenReturn(taskResponse1)
                .thenReturn(taskResponse2);
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        assertDoesNotThrow(() -> fulfillmentService.generateAndDeliver(testOrder));

        // Then
        verify(activationTaskService, times(2)).createTask(any());
        verify(orderItemRepository, atLeast(2)).save(any(OrderItem.class));
    }
}
