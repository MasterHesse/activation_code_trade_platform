package com.masterhesse.order.api.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class SubmitOrderRequest {

    @NotNull(message = "userId 不能为空")
    private UUID userId;

    @Valid
    @NotEmpty(message = "items 不能为空")
    private List<SubmitOrderItemRequest> items;

    private String remark;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public List<SubmitOrderItemRequest> getItems() {
        return items;
    }

    public void setItems(List<SubmitOrderItemRequest> items) {
        this.items = items;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}