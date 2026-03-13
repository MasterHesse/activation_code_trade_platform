package com.masterhesse.web;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.masterhesse.common.api.ApiResponse;
import com.masterhesse.common.exception.BusinessException;
import com.masterhesse.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFound(EntityNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse("invalid"),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "参数校验失败", errors));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleBindException(BindException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> Optional.ofNullable(fieldError.getDefaultMessage()).orElse("invalid"),
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "参数绑定失败", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleConstraintViolation(
            ConstraintViolationException ex) {

        Map<String, String> errors = ex.getConstraintViolations()
                .stream()
                .collect(Collectors.toMap(
                        violation -> extractLeafField(violation.getPropertyPath().toString()),
                        ConstraintViolation::getMessage,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "参数校验失败", errors));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex) {

        Map<String, String> errors = new LinkedHashMap<>();
        errors.put(ex.getParameterName(), "缺少必要请求参数");

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "缺少必要请求参数", errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex) {

        Map<String, String> errors = new LinkedHashMap<>();
        String message = "参数类型错误";
        if (ex.getRequiredType() != null) {
            message = "参数类型错误，应为: " + ex.getRequiredType().getSimpleName();
        }
        errors.put(ex.getName(), message);

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, "参数类型错误", errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        String msg = "请求体格式错误";

        Throwable root = ex.getMostSpecificCause();
        if (root instanceof InvalidFormatException invalidFormatException) {
            String field = invalidFormatException.getPath()
                    .stream()
                    .map(ref -> ref.getFieldName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining("."));

            if (!StringUtils.hasText(field)) {
                field = "unknown";
            }

            if (invalidFormatException.getTargetType() != null
                    && invalidFormatException.getTargetType().isEnum()) {

                Object[] enumConstants = invalidFormatException.getTargetType().getEnumConstants();
                String optionalValues = enumConstants == null
                        ? ""
                        : Arrays.stream(enumConstants)
                        .map(String::valueOf)
                        .collect(Collectors.joining(", "));

                msg = "字段 " + field + " 的值非法，可选值: " + optionalValues;
            } else {
                msg = "字段 " + field + " 的值类型不正确";
            }
        }

        return ResponseEntity.badRequest()
                .body(ApiResponse.error(400, msg));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ApiResponse.error(405, "不支持的请求方法: " + ex.getMethod()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex) {

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ApiResponse.error(415, "不支持的 Content-Type，请使用 application/json"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex) {

        log.warn("Data integrity violation", ex);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(409, "数据冲突，可能是唯一字段重复或关联数据不存在"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, "请求地址不存在: " + ex.getResourcePath()));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(NoHandlerFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, "请求地址不存在: " + ex.getRequestURL()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(500, "服务器内部错误"));
    }

    private String extractLeafField(String propertyPath) {
        if (!StringUtils.hasText(propertyPath)) {
            return propertyPath;
        }
        int index = propertyPath.lastIndexOf('.');
        return index >= 0 ? propertyPath.substring(index + 1) : propertyPath;
    }
}