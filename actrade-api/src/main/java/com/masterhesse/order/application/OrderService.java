package com.masterhesse.order.application;

import com.masterhesse.order.api.request.PayOrderRequest;
import com.masterhesse.order.api.request.SubmitOrderItemRequest;
import com.masterhesse.order.api.request.SubmitOrderRequest;
import com.masterhesse.order.api.response.OrderResponse;
import com.masterhesse.order.api.response.OrderSummaryResponse;
import com.masterhesse.order.api.response.PageResponse;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.OrderItem;
import com.masterhesse.order.domain.OrderStatus;
import com.masterhesse.order.domain.PaymentMethod;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.persistence.OrderItemRepository;
import com.masterhesse.order.persistence.OrderRepository;
import com.masterhesse.product.domain.DeliveryMode;
import com.masterhesse.product.domain.Product;
import com.masterhesse.product.persistence.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final DateTimeFormatter ORDER_NO_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse submitOrder(SubmitOrderRequest request) {
        List<UUID> productIds = request.getItems().stream()
                .map(SubmitOrderItemRequest::getProductId)
                .distinct()
                .toList();

        List<Product> products = productRepository.findAllById(productIds);

        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(this::readProductId, Function.identity()));

        List<UUID> missingProductIds = productIds.stream()
                .filter(productId -> !productMap.containsKey(productId))
                .toList();

        if (!missingProductIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "以下商品不存在: " + missingProductIds
            );
        }

        Set<UUID> merchantIds = request.getItems().stream()
                .map(item -> productMap.get(item.getProductId()))
                .map(this::readMerchantId)
                .collect(Collectors.toSet());

        if (merchantIds.size() != 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "一个订单只能提交同一商家的商品"
            );
        }

        UUID merchantId = merchantIds.iterator().next();

        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(request.getUserId());
        order.setMerchantId(merchantId);
        order.setOrderStatus(OrderStatus.CREATED);
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setRemark(normalizeRemark(request.getRemark()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (SubmitOrderItemRequest itemRequest : request.getItems()) {
            Product product = productMap.get(itemRequest.getProductId());

            BigDecimal unitPrice = readProductPrice(product);
            Integer quantity = itemRequest.getQuantity();
            BigDecimal subtotalAmount = unitPrice.multiply(BigDecimal.valueOf(quantity.longValue()));

            OrderItem orderItem = new OrderItem();
            orderItem.setProductId(readProductId(product));
            orderItem.setProductName(readProductName(product));
            orderItem.setDeliveryMode(readDeliveryMode(product));
            orderItem.setUnitPrice(unitPrice);
            orderItem.setQuantity(quantity);
            orderItem.setSubtotalAmount(subtotalAmount);

            orderItems.add(orderItem);
            totalAmount = totalAmount.add(subtotalAmount);
        }

        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        for (OrderItem orderItem : orderItems) {
            orderItem.setOrderId(savedOrder.getOrderId());
        }

        List<OrderItem> savedItems = orderItemRepository.saveAll(orderItems);

        return OrderResponse.from(savedOrder, savedItems);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getOrderStatus() == OrderStatus.CANCELED) {
            return buildOrderResponse(order);
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已支付订单暂不支持取消"
            );
        }

        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "当前订单状态不允许取消: " + order.getOrderStatus()
            );
        }

        order.setOrderStatus(OrderStatus.CANCELED);

        Order savedOrder = orderRepository.save(order);
        return buildOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse payOrder(UUID orderId, PayOrderRequest request) {
        Order order = findOrderOrThrow(orderId);

        if (order.getOrderStatus() == OrderStatus.CANCELED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已取消订单不允许支付"
            );
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return buildOrderResponse(order);
        }

        if (order.getOrderStatus() != OrderStatus.CREATED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "当前订单状态不允许支付: " + order.getOrderStatus()
            );
        }

        PaymentMethod paymentMethod = request.getPaymentMethod();
        handlePaymentByMethod(order, paymentMethod);

        Order savedOrder = orderRepository.save(order);
        return buildOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse deliverOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getOrderStatus() == OrderStatus.DELIVERING) {
            return buildOrderResponse(order);
        }

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "未支付订单不允许发货"
            );
        }

        if (order.getOrderStatus() != OrderStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "当前订单状态不允许发货: " + order.getOrderStatus()
            );
        }

        order.setOrderStatus(OrderStatus.DELIVERING);

        Order savedOrder = orderRepository.save(order);
        return buildOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse markDeliveryFailed(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getOrderStatus() == OrderStatus.DELIVERY_FAILED) {
            return buildOrderResponse(order);
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "当前订单状态不允许标记配送失败: " + order.getOrderStatus()
            );
        }

        order.setOrderStatus(OrderStatus.DELIVERY_FAILED);

        Order savedOrder = orderRepository.save(order);
        return buildOrderResponse(savedOrder);
    }

    @Transactional
    public OrderResponse confirmReceipt(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        if (order.getOrderStatus() == OrderStatus.COMPLETED) {
            return buildOrderResponse(order);
        }

        if (order.getOrderStatus() != OrderStatus.DELIVERING) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "当前订单状态不允许确认收货: " + order.getOrderStatus()
            );
        }

        order.setOrderStatus(OrderStatus.COMPLETED);

        Order savedOrder = orderRepository.save(order);
        return buildOrderResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = findOrderOrThrow(orderId);
        return buildOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listUserOrders(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> listMerchantOrders(UUID merchantId) {
        return orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId).stream()
                .map(OrderSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> pageUserOrders(UUID userId,
                                                             OrderStatus status,
                                                             int page,
                                                             int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> orderPage;
        if (status == null) {
            orderPage = orderRepository.findByUserId(userId, pageRequest);
        } else {
            orderPage = orderRepository.findByUserIdAndOrderStatus(userId, status, pageRequest);
        }

        return PageResponse.from(orderPage, OrderSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> pageMerchantOrders(UUID merchantId,
                                                                 OrderStatus status,
                                                                 int page,
                                                                 int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Order> orderPage;
        if (status == null) {
            orderPage = orderRepository.findByMerchantId(merchantId, pageRequest);
        } else {
            orderPage = orderRepository.findByMerchantIdAndOrderStatus(merchantId, status, pageRequest);
        }

        return PageResponse.from(orderPage, OrderSummaryResponse::from);
    }

    private void handlePaymentByMethod(Order order, PaymentMethod paymentMethod) {
        switch (paymentMethod) {
            case MOCK, MANUAL -> markPaid(order, paymentMethod);
            default -> throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "暂不支持的支付方式: " + paymentMethod
            );
        }
    }

    private void markPaid(Order order, PaymentMethod paymentMethod) {
        order.setPaymentMethod(paymentMethod);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
    }

    private OrderResponse buildOrderResponse(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderIdOrderByCreatedAtAsc(order.getOrderId());
        return OrderResponse.from(order, orderItems);
    }

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "订单不存在: " + orderId
                ));
    }

    private String generateOrderNo() {
        for (int i = 0; i < 10; i++) {
            String candidate = "ORD"
                    + LocalDateTime.now().format(ORDER_NO_TIME_FORMATTER)
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

            if (!orderRepository.existsByOrderNo(candidate)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "订单号生成失败，请稍后重试"
        );
    }

    private String normalizeRemark(String remark) {
        if (remark == null) {
            return null;
        }

        String value = remark.trim();
        if (value.isEmpty()) {
            return null;
        }

        if (value.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "remark 不能超过 500 个字符"
            );
        }

        return value;
    }

    /**
     * 如果你的 Product 实体字段名不同，优先改下面这几个 readXXX 方法即可。
     */

    private UUID readProductId(Product product) {
        UUID productId = product.getProductId();
        if (productId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "商品 productId 不能为空"
            );
        }
        return productId;
    }

    private UUID readMerchantId(Product product) {
        UUID merchantId = product.getMerchantId();
        if (merchantId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "商品 merchantId 不能为空，productId=" + readProductId(product)
            );
        }
        return merchantId;
    }

    private String readProductName(Product product) {
        String productName = product.getName();
        if (productName == null || productName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "商品名称不能为空，productId=" + readProductId(product)
            );
        }
        return productName;
    }

    private BigDecimal readProductPrice(Product product) {
        BigDecimal price = product.getPrice();
        if (price == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "商品价格不能为空，productId=" + readProductId(product)
            );
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "商品价格不能小于 0，productId=" + readProductId(product)
            );
        }
        return price;
    }

    private DeliveryMode readDeliveryMode(Product product) {
        DeliveryMode deliveryMode = product.getDeliveryMode();
        if (deliveryMode == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "商品 deliveryMode 不能为空，productId=" + readProductId(product)
            );
        }
        return deliveryMode;
    }
}