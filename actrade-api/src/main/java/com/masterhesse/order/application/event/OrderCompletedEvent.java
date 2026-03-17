package com.masterhesse.order.application.event;

import java.util.UUID;

public record OrderCompletedEvent(UUID orderId) {
}