package com.actrade.activationrunner.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 校验工具
 */
@Slf4j
@Component
public class ChecksumValidator {

    private static final int BUFFER_SIZE = 8192;

    /**
     * 校验文件的 SHA-256 校验和
     *
     * @param file           待校验文件
     * @param expectedSha256 期望的 SHA-256 值（十六进制字符串）
     * @return 校验结果
     */
    public ValidationResult validate(Path file, String expectedSha256) {
        if (file == null || Files.notExists(file)) {
            return ValidationResult.failure("File does not exist: " + file);
        }

        if (expectedSha256 == null || expectedSha256.isBlank()) {
            return ValidationResult.failure("Expected SHA-256 checksum is empty");
        }

        String normalizedExpected = expectedSha256.toLowerCase().trim();

        try {
            String actualChecksum = computeSha256(file);

            if (normalizedExpected.equals(actualChecksum)) {
                log.debug("Checksum validation passed. file={}, checksum={}", file.getFileName(), actualChecksum);
                return ValidationResult.success();
            } else {
                log.warn(
                        "Checksum mismatch. file={}, expected={}, actual={}",
                        file.getFileName(),
                        normalizedExpected,
                        actualChecksum
                );
                return ValidationResult.failure(
                        "Checksum mismatch: expected " + normalizedExpected + ", got " + actualChecksum
                );
            }
        } catch (IOException e) {
            log.error("Failed to compute checksum for file: {}", file, e);
            return ValidationResult.failure("IO error while computing checksum: " + e.getMessage());
        }
    }

    /**
     * 计算文件的 SHA-256 校验和
     *
     * @param file 文件路径
     * @return SHA-256 十六进制字符串
     * @throws IOException 如果读取文件失败
     */
    public String computeSha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }

            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 算法在所有 Java 实现中都可用，这里不会发生
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 从输入流计算 SHA-256（不落盘）
     *
     * @param inputStream 输入流
     * @param fileSize    文件大小（用于日志）
     * @return SHA-256 十六进制字符串
     * @throws IOException 如果读取失败
     */
    public String computeSha256(InputStream inputStream, long fileSize) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            long totalRead = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
                totalRead += bytesRead;

                if (fileSize > 0) {
                    int progress = (int) ((totalRead * 100) / fileSize);
                    if (progress % 20 == 0) {
                        log.trace("Computing checksum: {}% ({}/{} bytes)", progress, totalRead, fileSize);
                    }
                }
            }

            byte[] hashBytes = digest.digest();
            return bytesToHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * 校验结果
     */
    public record ValidationResult(boolean valid, String errorMessage) {

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}
