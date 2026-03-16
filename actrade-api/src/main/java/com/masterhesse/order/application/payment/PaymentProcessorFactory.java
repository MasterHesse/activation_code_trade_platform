package com.masterhesse.order.application.payment;

import com.masterhesse.common.exception.BusinessException;
import com.masterhesse.order.domain.PaymentMethod;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentProcessorFactory {

    private final Map<PaymentMethod, PaymentProcessor> processorMap = new EnumMap<>(PaymentMethod.class);

    public PaymentProcessorFactory(List<PaymentProcessor> processors) {
        for (PaymentProcessor processor : processors) {
            PaymentProcessor old = processorMap.put(processor.supportMethod(), processor);
            if (old != null) {
                throw new IllegalStateException("支付处理器重复注册: " + processor.supportMethod());
            }
        }
    }

    public PaymentProcessor getProcessor(PaymentMethod paymentMethod) {
        PaymentProcessor processor = processorMap.get(paymentMethod);
        if (processor == null) {
            throw new BusinessException("暂不支持的支付方式: " + paymentMethod);
        }
        return processor;
    }
}