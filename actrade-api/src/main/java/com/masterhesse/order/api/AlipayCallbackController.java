package com.masterhesse.order.api;

import com.masterhesse.order.application.OrderPaymentService;
import com.masterhesse.order.domain.Order;
import com.masterhesse.order.persistence.OrderRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/payments/alipay")
public class AlipayCallbackController {

    private final OrderPaymentService orderPaymentService;
    private final OrderRepository orderRepository;

    public AlipayCallbackController(OrderPaymentService orderPaymentService, OrderRepository orderRepository) {
        this.orderPaymentService = orderPaymentService;
        this.orderRepository = orderRepository;
    }

    /**
     * 测试用：模拟支付成功回调（跳过签名验证）
     * 仅用于开发/测试环境
     */
    @PostMapping("/test/simulate-success")
    public String simulatePaymentSuccess(@RequestParam String paymentRequestNo,
                                         @RequestParam(defaultValue = "ALIPAY_TEST_123456") String tradeNo,
                                         @RequestParam(defaultValue = "9.99") String totalAmount) {
        log.info("Test simulate payment success. paymentRequestNo={}, tradeNo={}, totalAmount={}",
                paymentRequestNo, tradeNo, totalAmount);
        return orderPaymentService.simulatePaymentSuccess(paymentRequestNo, tradeNo, totalAmount);
    }

    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        log.info("Received alipay notify. params={}", params);
        return orderPaymentService.handleAlipayNotify(params);
    }

    /**
     * 支付宝同步回调（return_url）
     * 用户完成支付后从支付宝页面跳转回来
     * 注意：同步回调只做展示，实际支付状态以异步通知(notify)为准
     */
    @GetMapping(value = "/return", produces = MediaType.TEXT_HTML_VALUE)
    public String alipayReturn(@RequestParam Map<String, String> params) {
        String outTradeNo = escapeHtml(params.getOrDefault("out_trade_no", ""));
        String tradeNo = escapeHtml(params.getOrDefault("trade_no", ""));
        String totalAmount = escapeHtml(params.getOrDefault("total_amount", ""));
        String tradeStatus = escapeHtml(params.getOrDefault("trade_status", ""));

        // 同步回调时尝试处理支付状态（与异步通知逻辑一致）
        // 某些情况下同步回调可能先于异步通知到达，此时需要更新状态
        if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
            try {
                Map<String, String> notifyParams = new HashMap<>(params);
                notifyParams.put("trade_status", tradeStatus);
                orderPaymentService.handleAlipayNotify(notifyParams);
                log.info("Alipay return handled successfully. outTradeNo={}, tradeStatus={}", outTradeNo, tradeStatus);
            } catch (Exception e) {
                log.error("Failed to handle alipay return. outTradeNo={}", outTradeNo, e);
            }
        }

        // 根据 paymentRequestNo 查询订单 ID 用于跳转
        String orderId = "";
        if (outTradeNo != null && !outTradeNo.isEmpty()) {
            Optional<Order> orderOpt = orderRepository.findByPaymentRequestNo(outTradeNo);
            if (orderOpt.isPresent()) {
                orderId = orderOpt.get().getOrderId().toString();
            }
        }

        // 构建跳转目标 URL
        String orderPageUrl = orderId.isEmpty() ? "/orders" : "/orders/" + orderId;

        boolean isSuccess = "TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus);

        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>支付宝支付返回</title>
                    <style>
                        body {
                            font-family: Arial, sans-serif;
                            max-width: 720px;
                            margin: 40px auto;
                            padding: 0 16px;
                            color: #222;
                            line-height: 1.6;
                        }
                        .card {
                            border: 1px solid #ddd;
                            border-radius: 8px;
                            padding: 20px;
                            background: #fafafa;
                        }
                        h2 { margin-top: 0; }
                        .hint { color: #666; }
                        code {
                            background: #f0f0f0;
                            padding: 2px 6px;
                            border-radius: 4px;
                        }
                        .success { color: #67c23a; }
                        .redirect-info {
                            margin-top: 20px;
                            padding: 12px;
                            background: #f4f4f5;
                            border-radius: 4px;
                        }
                        .btn {
                            display: inline-block;
                            padding: 8px 16px;
                            background: #409eff;
                            color: white;
                            text-decoration: none;
                            border-radius: 4px;
                            margin-top: 10px;
                        }
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2 class="%s">支付宝页面已返回商户系统</h2>
                        <p><strong>商户支付单号：</strong><code>%s</code></p>
                        <p><strong>支付宝交易号：</strong><code>%s</code></p>
                        <p><strong>支付金额：</strong><code>%s</code></p>
                        <p><strong>页面返回状态：</strong><code>%s</code></p>
                        <p class="hint">注意：最终支付结果以后端异步通知(notify)为准。</p>
                        <div class="redirect-info">
                            <p>页面将在 <span id="countdown">3</span> 秒后自动跳转...</p>
                            <a href="%s" class="btn" id="orderBtn">立即跳转查看订单</a>
                        </div>
                    </div>
                    <script>
                        var seconds = 3;
                        var countdown = setInterval(function() {
                            seconds--;
                            var el = document.getElementById('countdown');
                            if (el) el.textContent = seconds;
                            if (seconds <= 0) {
                                clearInterval(countdown);
                                // 如果有 opener 窗口，关闭当前窗口并刷新 opener
                                if (window.opener) {
                                    window.opener.location.href = '%s';
                                    window.close();
                                } else {
                                    window.location.href = '%s';
                                }
                            }
                        }, 1000);
                    </script>
                </body>
                </html>
                """.formatted(
                    isSuccess ? "success" : "",
                    outTradeNo, tradeNo, totalAmount, tradeStatus,
                    orderPageUrl, orderPageUrl, orderPageUrl);
    }

    private Map<String, String> extractParams(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values == null || values.length == 0) {
                params.put(key, null);
            } else {
                params.put(key, String.join(",", values));
            }
        });
        return params;
    }

    private String escapeHtml(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}