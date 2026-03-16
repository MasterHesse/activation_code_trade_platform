package com.masterhesse.order.api;

import com.masterhesse.order.application.OrderPaymentService;
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

@Slf4j
@RestController
@RequestMapping("/api/payments/alipay")
public class AlipayCallbackController {

    private final OrderPaymentService orderPaymentService;

    public AlipayCallbackController(OrderPaymentService orderPaymentService) {
        this.orderPaymentService = orderPaymentService;
    }

    @PostMapping("/notify")
    public String notify(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        log.info("Received alipay notify. params={}", params);
        return orderPaymentService.handleAlipayNotify(params);
    }

    @GetMapping(value = "/return", produces = MediaType.TEXT_HTML_VALUE)
    public String alipayReturn(@RequestParam Map<String, String> params) {
        String outTradeNo = escapeHtml(params.getOrDefault("out_trade_no", ""));
        String tradeNo = escapeHtml(params.getOrDefault("trade_no", ""));
        String totalAmount = escapeHtml(params.getOrDefault("total_amount", ""));
        String tradeStatus = escapeHtml(params.getOrDefault("trade_status", ""));

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
                    </style>
                </head>
                <body>
                    <div class="card">
                        <h2>支付宝页面已返回商户系统</h2>
                        <p><strong>商户支付单号：</strong><code>%s</code></p>
                        <p><strong>支付宝交易号：</strong><code>%s</code></p>
                        <p><strong>支付金额：</strong><code>%s</code></p>
                        <p><strong>页面返回状态：</strong><code>%s</code></p>
                        <p class="hint">注意：最终支付结果以后端异步通知 notify 为准。</p>
                    </div>
                </body>
                </html>
                """.formatted(outTradeNo, tradeNo, totalAmount, tradeStatus);
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