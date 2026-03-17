package com.masterhesse.order.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_order_no", columnNames = "order_no")
        },
        indexes = {
                @Index(name = "idx_orders_user_id", columnList = "user_id"),
                @Index(name = "idx_orders_merchant_id", columnList = "merchant_id"),
                @Index(name = "idx_orders_seller_id", columnList = "seller_id"),
                @Index(name = "idx_orders_order_status", columnList = "order_status"),
                @Index(name = "idx_orders_payment_status", columnList = "payment_status"),
                @Index(name = "idx_orders_payment_request_no", columnList = "payment_request_no"),
                @Index(name = "idx_orders_pay_deadline_at", columnList = "pay_deadline_at"),
                @Index(name = "idx_orders_confirm_deadline_at", columnList = "confirm_deadline_at"),
                @Index(name = "idx_orders_created_at", columnList = "created_at")
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "order_no", nullable = false, length = 64)
    private String orderNo;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "merchant_id", nullable = false)
    private UUID merchantId;

    @Column(name = "seller_id")
    private UUID sellerId;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "pay_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal payAmount;

    @Column(name = "seller_income_amount", precision = 18, scale = 2)
    private BigDecimal sellerIncomeAmount;

    @Column(name = "platform_income_amount", precision = 18, scale = 2)
    private BigDecimal platformIncomeAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 32)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 32)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", length = 32)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_type", length = 32)
    private FulfillmentType fulfillmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fulfillment_status", length = 32)
    private FulfillmentStatus fulfillmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "settlement_status", length = 32)
    private SettlementStatus settlementStatus;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "pay_deadline_at")
    private LocalDateTime payDeadlineAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "confirm_deadline_at")
    private LocalDateTime confirmDeadlineAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "payment_request_no", length = 64, unique = true)
    private String paymentRequestNo;

    @Column(name = "channel_trade_no", length = 64)
    private String channelTradeNo;

    @Column(name = "remark", length = 500)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
        if (this.payAmount == null) {
            this.payAmount = BigDecimal.ZERO;
        }
        if (this.sellerIncomeAmount == null) {
            this.sellerIncomeAmount = BigDecimal.ZERO;
        }
        if (this.platformIncomeAmount == null) {
            this.platformIncomeAmount = BigDecimal.ZERO;
        }
        if (this.orderStatus == null) {
            this.orderStatus = OrderStatus.CREATED;
        }
        if (this.paymentStatus == null) {
            this.paymentStatus = PaymentStatus.UNPAID;
        }
        if (this.fulfillmentStatus == null) {
            this.fulfillmentStatus = FulfillmentStatus.PENDING;
        }
        if (this.settlementStatus == null) {
            this.settlementStatus = SettlementStatus.UNSETTLED;
        }
        if (this.payDeadlineAt == null) {
            this.payDeadlineAt = now.plusMinutes(30);
        }
        if (this.sellerId == null && this.merchantId != null) {
            this.sellerId = this.merchantId;
        }
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getOrderId() {
        return orderId;
    }

    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(UUID merchantId) {
        this.merchantId = merchantId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public void setSellerId(UUID sellerId) {
        this.sellerId = sellerId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public BigDecimal getSellerIncomeAmount() {
        return sellerIncomeAmount;
    }

    public void setSellerIncomeAmount(BigDecimal sellerIncomeAmount) {
        this.sellerIncomeAmount = sellerIncomeAmount;
    }

    public BigDecimal getPlatformIncomeAmount() {
        return platformIncomeAmount;
    }

    public void setPlatformIncomeAmount(BigDecimal platformIncomeAmount) {
        this.platformIncomeAmount = platformIncomeAmount;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public FulfillmentType getFulfillmentType() {
        return fulfillmentType;
    }

    public void setFulfillmentType(FulfillmentType fulfillmentType) {
        this.fulfillmentType = fulfillmentType;
    }

    public FulfillmentStatus getFulfillmentStatus() {
        return fulfillmentStatus;
    }

    public void setFulfillmentStatus(FulfillmentStatus fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }

    public SettlementStatus getSettlementStatus() {
        return settlementStatus;
    }

    public void setSettlementStatus(SettlementStatus settlementStatus) {
        this.settlementStatus = settlementStatus;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public LocalDateTime getPayDeadlineAt() {
        return payDeadlineAt;
    }

    public void setPayDeadlineAt(LocalDateTime payDeadlineAt) {
        this.payDeadlineAt = payDeadlineAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public LocalDateTime getConfirmDeadlineAt() {
        return confirmDeadlineAt;
    }

    public void setConfirmDeadlineAt(LocalDateTime confirmDeadlineAt) {
        this.confirmDeadlineAt = confirmDeadlineAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(LocalDateTime confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public String getPaymentRequestNo() {
        return paymentRequestNo;
    }

    public void setPaymentRequestNo(String paymentRequestNo) {
        this.paymentRequestNo = paymentRequestNo;
    }

    public String getChannelTradeNo() {
        return channelTradeNo;
    }

    public void setChannelTradeNo(String channelTradeNo) {
        this.channelTradeNo = channelTradeNo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}