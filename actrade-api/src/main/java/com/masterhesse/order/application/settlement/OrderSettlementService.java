package com.masterhesse.order.application.settlement;

import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.domain.SellerSettlement;
import com.masterhesse.order.domain.SettlementStatus;
import com.masterhesse.order.persistence.OrderRepository;
import com.masterhesse.order.persistence.SellerSettlementRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class OrderSettlementService {

    private static final DateTimeFormatter SETTLEMENT_NO_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OrderRepository orderRepository;
    private final SellerSettlementRepository sellerSettlementRepository;

    public OrderSettlementService(OrderRepository orderRepository,
                                  SellerSettlementRepository sellerSettlementRepository) {
        this.orderRepository = orderRepository;
        this.sellerSettlementRepository = sellerSettlementRepository;
    }

    /**
     * 创建待结算记录
     *
     * 适用场景：
     * 1. 用户确认收货后自动创建
     * 2. 历史数据修复，手动补建
     *
     * 幂等：
     * - 如果当前订单已经存在结算单，则直接返回已有结算单
     */
    @Transactional
    public SellerSettlement createPendingSettlement(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        SellerSettlement existing = sellerSettlementRepository.findByOrderId(orderId).orElse(null);
        if (existing != null) {
            syncOrderSettlementStatus(order, existing.getSettlementStatus());
            return existing;
        }

        validateOrderCanCreateSettlement(order);

        SellerSettlement settlement = new SellerSettlement();
        settlement.setSettlementNo(generateSettlementNo());
        settlement.setOrderId(order.getOrderId());
        settlement.setSellerId(resolveSellerId(order));
        settlement.setMerchantId(order.getMerchantId());
        settlement.setSettlementAmount(resolveSettlementAmount(order));
        settlement.setSettlementStatus(SettlementStatus.UNSETTLED);
        settlement.setSettledAt(null);
        settlement.setRemark("订单待结算");

        SellerSettlement savedSettlement = sellerSettlementRepository.save(settlement);

        if (order.getSettlementStatus() != SettlementStatus.UNSETTLED) {
            order.setSettlementStatus(SettlementStatus.UNSETTLED);
            orderRepository.save(order);
        }

        return savedSettlement;
    }

    /**
     * 标记订单已结算
     *
     * 幂等：
     * - 如果已经是 SETTLED，则直接返回
     * - 如果订单没有结算单，会自动补建一条后再结算
     */
    @Transactional
    public SellerSettlement markSettled(UUID orderId) {
        Order order = findOrderOrThrow(orderId);

        validateOrderCanSettle(order);

        SellerSettlement settlement = sellerSettlementRepository.findByOrderId(orderId)
                .orElseGet(() -> createPendingSettlement(orderId));

        if (settlement.getSettlementStatus() != SettlementStatus.SETTLED) {
            settlement.setSettlementStatus(SettlementStatus.SETTLED);
            settlement.setSettledAt(LocalDateTime.now());
            settlement.setRemark("订单已结算");
            settlement = sellerSettlementRepository.save(settlement);
        } else if (settlement.getSettledAt() == null) {
            settlement.setSettledAt(LocalDateTime.now());
            settlement = sellerSettlementRepository.save(settlement);
        }

        if (order.getSettlementStatus() != SettlementStatus.SETTLED) {
            order.setSettlementStatus(SettlementStatus.SETTLED);
            orderRepository.save(order);
        }

        return settlement;
    }

    @Transactional(readOnly = true)
    public SellerSettlement getByOrderIdOrThrow(UUID orderId) {
        return sellerSettlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "订单结算单不存在: " + orderId
                ));
    }

    private void validateOrderCanCreateSettlement(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "未支付订单不允许创建结算单"
            );
        }

        if (isCanceled(order)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已取消订单不允许创建结算单"
            );
        }

        if (order.getMerchantId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "订单 merchantId 不能为空，orderId=" + order.getOrderId()
            );
        }
    }

    private void validateOrderCanSettle(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "未支付订单不允许结算"
            );
        }

        if (isCanceled(order)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已取消订单不允许结算"
            );
        }
    }

    private boolean isCanceled(Order order) {
        if (order.getOrderStatus() == null) {
            return false;
        }

        String statusName = order.getOrderStatus().name();
        return "CANCELED".equalsIgnoreCase(statusName)
                || "CANCELLED".equalsIgnoreCase(statusName);
    }

    /**
     * 结算金额优先取 sellerIncomeAmount；
     * 如果没有，再退化为 payAmount；
     * 再没有，退化为 totalAmount。
     */
    private BigDecimal resolveSettlementAmount(Order order) {
        BigDecimal amount = order.getSellerIncomeAmount();

        if (amount == null) {
            amount = order.getPayAmount();
        }
        if (amount == null) {
            amount = order.getTotalAmount();
        }
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "结算金额不能小于 0，orderId=" + order.getOrderId()
            );
        }

        return amount;
    }

    private UUID resolveSellerId(Order order) {
        if (order.getSellerId() != null) {
            return order.getSellerId();
        }

        if (order.getMerchantId() != null) {
            return order.getMerchantId();
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "订单 sellerId / merchantId 不能为空，orderId=" + order.getOrderId()
        );
    }

    private void syncOrderSettlementStatus(Order order, SettlementStatus settlementStatus) {
        if (settlementStatus == null) {
            return;
        }

        if (order.getSettlementStatus() != settlementStatus) {
            order.setSettlementStatus(settlementStatus);
            orderRepository.save(order);
        }
    }

    private Order findOrderOrThrow(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "订单不存在: " + orderId
                ));
    }

    private String generateSettlementNo() {
        for (int i = 0; i < 10; i++) {
            String candidate = "SET"
                    + LocalDateTime.now().format(SETTLEMENT_NO_TIME_FORMATTER)
                    + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));

            if (!sellerSettlementRepository.existsBySettlementNo(candidate)) {
                return candidate;
            }
        }

        throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "结算单号生成失败，请稍后重试"
        );
    }
}