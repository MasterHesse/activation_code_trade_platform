package com.masterhesse.app_users.api;

import com.masterhesse.app_users.api.request.CreateUserRequest;
import com.masterhesse.app_users.api.request.UpdateUserRequest;
import com.masterhesse.app_users.api.response.UserResponse;
import com.masterhesse.app_users.application.UserService;
import com.masterhesse.app_users.domain.UserRole;
import com.masterhesse.app_users.domain.UserStatus;
import com.masterhesse.common.api.ApiResponse;
import com.masterhesse.common.api.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.create(request));
    }

    @GetMapping("/{userId}")
    public ApiResponse<UserResponse> getById(@PathVariable UUID userId) {
        return ApiResponse.success(userService.getById(userId));
    }

    @GetMapping
    public ApiResponse<PageResult<UserResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "page 不能小于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 不能小于 1")
            @Max(value = 100, message = "size 不能大于 100") int size
    ) {
        return ApiResponse.success(
                PageResult.from(userService.page(keyword, role, status, page, size))
        );
    }

    @PutMapping("/{userId}")
    public ApiResponse<UserResponse> update(
            @PathVariable UUID userId,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success(userService.update(userId, request));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<Void> delete(@PathVariable UUID userId) {
        userService.delete(userId);
        return ApiResponse.success("删除成功");
    }
}