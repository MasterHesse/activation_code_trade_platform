package com.masterhesse.order.application.settlement;

import com.masterhesse.order.api.response.PageResponse;
import com.masterhesse.order.api.response.SellerSettlementResponse;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.domain.PaymentStatus;
import com.masterhesse.order.domain.SellerSettlement;
import com.masterhesse.order.domain.SettlementStatus;
import com.masterhesse.order.persistence.OrderRepository;
import com.masterhesse.order.persistence.SellerSettlementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class SellerSettlementService {

    private static final DateTimeFormatter SETTLEMENT_NO_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final SellerSettlementRepository sellerSettlementRepository;
    private final OrderRepository orderRepository;

    public SellerSettlementService(SellerSettlementRepository sellerSettlementRepository,
                                   OrderRepository orderRepository) {
        this.sellerSettlementRepository = sellerSettlementRepository;
        this.orderRepository = orderRepository;
    }

    // =========================
    // 查询类接口
    // =========================

    @Transactional(readOnly = true)
    public SellerSettlementResponse getSettlement(UUID settlementId) {
        SellerSettlement settlement = sellerSettlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "结算单不存在: " + settlementId
                ));
        return SellerSettlementResponse.from(settlement);
    }

    @Transactional(readOnly = true)
    public SellerSettlementResponse getSettlementByOrderId(UUID orderId) {
        SellerSettlement settlement = sellerSettlementRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "订单结算单不存在: " + orderId
                ));
        return SellerSettlementResponse.from(settlement);
    }

    @Transactional(readOnly = true)
    public List<SellerSettlementResponse> listSellerSettlements(UUID sellerId) {
        return sellerSettlementRepository.findBySellerIdOrderByCreatedAtDesc(sellerId)
                .stream()
                .map(SellerSettlementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SellerSettlementResponse> listMerchantSettlements(UUID merchantId) {
        return sellerSettlementRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId)
                .stream()
                .map(SellerSettlementResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerSettlementResponse> pageSellerSettlements(UUID sellerId,
                                                                        SettlementStatus status,
                                                                        int page,
                                                                        int size) {
        Pageable pageable = buildPageable(page, size);

        Page<SellerSettlement> settlementPage = (status == null)
                ? sellerSettlementRepository.findBySellerId(sellerId, pageable)
                : sellerSettlementRepository.findBySellerIdAndSettlementStatus(sellerId, status, pageable);

        return toPageResponse(settlementPage.map(SellerSettlementResponse::from));
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerSettlementResponse> pageMerchantSettlements(UUID merchantId,
                                                                          SettlementStatus status,
                                                                          int page,
                                                                          int size) {
        Pageable pageable = buildPageable(page, size);

        Page<SellerSettlement> settlementPage = (status == null)
                ? sellerSettlementRepository.findByMerchantId(merchantId, pageable)
                : sellerSettlementRepository.findByMerchantIdAndSettlementStatus(merchantId, status, pageable);

        return toPageResponse(settlementPage.map(SellerSettlementResponse::from));
    }

    // =========================
    // 写入类接口
    // =========================

    /**
     * 创建待结算单。
     *
     * 推荐触发时机：
     * 1. 买家确认收货
     * 2. 超时自动确认收货
     * 3. 历史数据补建
     *
     * 幂等：
     * - 如果已存在结算单，直接返回已有结算单
     */
    @Transactional
    public SellerSettlementResponse createPendingSettlement(UUID orderId) {
        SellerSettlement settlement = createPendingSettlementInternal(orderId);
        return SellerSettlementResponse.from(settlement);
    }

    /**
     * 手动结算订单。
     *
     * 幂等：
     * - 如果已经 SETTLED，直接返回
     * - 如果不存在结算单，会先补建 UNSETTLED 再结算
     */
    @Transactional
    public SellerSettlementResponse settleOrder(UUID orderId) {
        SellerSettlement settlement = markSettledInternal(orderId);
        return SellerSettlementResponse.from(settlement);
    }

    // =========================
    // 内部结算逻辑
    // =========================

    private SellerSettlement createPendingSettlementInternal(UUID orderId) {
        Order order = lockOrderOrThrow(orderId);
        return createPendingSettlementInternal(order);
    }

    private SellerSettlement createPendingSettlementInternal(Order order) {
        SellerSettlement existing = sellerSettlementRepository.findByOrderIdForUpdate(order.getOrderId())
                .orElse(null);

        if (existing != null) {
            syncOrderSettlementStatus(order, existing.getSettlementStatus());
            return existing;
        }

        validateOrderCanCreatePendingSettlement(order);

        SellerSettlement settlement = new SellerSettlement();
        settlement.setSettlementNo(generateSettlementNo());
        settlement.setOrderId(order.getOrderId());
        settlement.setSellerId(resolveSellerId(order));
        settlement.setMerchantId(resolveMerchantId(order));
        settlement.setSettlementAmount(resolveSettlementAmount(order));
        settlement.setSettlementStatus(SettlementStatus.UNSETTLED);
        settlement.setSettledAt(null);
        settlement.setRemark("订单待结算");

        SellerSettlement saved = sellerSettlementRepository.saveAndFlush(settlement);
        syncOrderSettlementStatus(order, SettlementStatus.UNSETTLED);

        return saved;
    }

    private SellerSettlement markSettledInternal(UUID orderId) {
        Order order = lockOrderOrThrow(orderId);

        validateOrderCanSettle(order);

        SellerSettlement settlement = sellerSettlementRepository.findByOrderIdForUpdate(orderId)
                .orElseGet(() -> createPendingSettlementInternal(order));

        if (settlement.getSettlementStatus() != SettlementStatus.SETTLED) {
            settlement.setSettlementStatus(SettlementStatus.SETTLED);
            settlement.setSettledAt(LocalDateTime.now());
            settlement.setRemark("订单已结算");
            settlement = sellerSettlementRepository.saveAndFlush(settlement);
        } else if (settlement.getSettledAt() == null) {
            settlement.setSettledAt(LocalDateTime.now());
            settlement = sellerSettlementRepository.saveAndFlush(settlement);
        }

        syncOrderSettlementStatus(order, SettlementStatus.SETTLED);

        return settlement;
    }

    // =========================
    // 规则校验
    // =========================

    private void validateOrderCanCreatePendingSettlement(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "未支付订单不允许创建结算单，orderId=" + order.getOrderId()
            );
        }

        if (isCanceled(order)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已取消订单不允许创建结算单，orderId=" + order.getOrderId()
            );
        }

        if (order.getMerchantId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "订单 merchantId 不能为空，orderId=" + order.getOrderId()
            );
        }

        resolveSellerId(order);
        resolveMerchantId(order);
        resolveSettlementAmount(order);
    }

    private void validateOrderCanSettle(Order order) {
        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "未支付订单不允许结算，orderId=" + order.getOrderId()
            );
        }

        if (isCanceled(order)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "已取消订单不允许结算，orderId=" + order.getOrderId()
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

    // =========================
    // 金额/参与方解析
    // =========================

    /**
     * 正确设计原则：
     * 1. 优先使用 sellerIncomeAmount
     * 2. 如果 sellerIncomeAmount 为 null，允许兼容退化到 payAmount / totalAmount
     * 3. 如果 sellerIncomeAmount 明确为 0.00，但 payAmount > 0，
     *    视为“收益拆分未正确计算”，直接报错，禁止生成错误结算单
     */
    private BigDecimal resolveSettlementAmount(Order order) {
        BigDecimal sellerIncomeAmount = order.getSellerIncomeAmount();
        BigDecimal payAmount = order.getPayAmount();
        BigDecimal totalAmount = order.getTotalAmount();

        if (sellerIncomeAmount != null) {
            if (sellerIncomeAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "sellerIncomeAmount 不能小于 0，orderId=" + order.getOrderId()
                );
            }

            if (sellerIncomeAmount.compareTo(BigDecimal.ZERO) == 0
                    && payAmount != null
                    && payAmount.compareTo(BigDecimal.ZERO) > 0) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "订单实付金额大于 0，但 sellerIncomeAmount = 0.00，说明卖家收益尚未正确计算，禁止生成 0 元结算单。orderId="
                                + order.getOrderId()
                );
            }

            return sellerIncomeAmount;
        }

        if (payAmount != null) {
            if (payAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "payAmount 不能小于 0，orderId=" + order.getOrderId()
                );
            }
            return payAmount;
        }

        if (totalAmount != null) {
            if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "totalAmount 不能小于 0，orderId=" + order.getOrderId()
                );
            }
            return totalAmount;
        }

        return BigDecimal.ZERO;
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

    private UUID resolveMerchantId(Order order) {
        if (order.getMerchantId() != null) {
            return order.getMerchantId();
        }

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "订单 merchantId 不能为空，orderId=" + order.getOrderId()
        );
    }

    // =========================
    // 订单状态同步
    // =========================

    private void syncOrderSettlementStatus(Order order, SettlementStatus targetStatus) {
        if (targetStatus == null) {
            return;
        }

        if (order.getSettlementStatus() != targetStatus) {
            order.setSettlementStatus(targetStatus);
            orderRepository.save(order);
        }
    }

    private Order lockOrderOrThrow(UUID orderId) {
        return orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "订单不存在: " + orderId
                ));
    }

    // =========================
    // 单号生成
    // =========================

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

    // =========================
    // 分页工具
    // =========================

    private Pageable buildPageable(int page, int size) {
        if (page < 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "page 不能小于 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "size 必须在 1 到 100 之间"
            );
        }

        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> pageData) {
        PageResponse<T> response = new PageResponse<>();

        setValue(response, "content", pageData.getContent());
        setValue(response, "page", pageData.getNumber());
        setValue(response, "size", pageData.getSize());
        setValue(response, "totalElements", pageData.getTotalElements());
        setValue(response, "totalPages", pageData.getTotalPages());
        setValue(response, "first", pageData.isFirst());
        setValue(response, "last", pageData.isLast());

        return response;
    }

    private void setValue(Object target, String fieldName, Object value) {
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        Method[] methods = target.getClass().getMethods();
        for (Method method : methods) {
            if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                try {
                    method.invoke(target, value);
                    return;
                } catch (Exception ignored) {
                }
            }
        }

        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (Exception ignored) {
                return;
            }
        }
    }
}