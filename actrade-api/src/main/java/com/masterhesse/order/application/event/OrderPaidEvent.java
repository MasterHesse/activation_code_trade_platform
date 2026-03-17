package com.masterhesse.order.application.event;

import java.util.UUID;

public record OrderPaidEvent(UUID orderId) {
}