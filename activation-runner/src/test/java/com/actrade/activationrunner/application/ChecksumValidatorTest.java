package com.actrade.activationrunner.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ChecksumValidator 单元测试
 */
class ChecksumValidatorTest {

    private ChecksumValidator checksumValidator;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        checksumValidator = new ChecksumValidator();
    }

    @Test
    @DisplayName("应正确计算文件 SHA-256")
    void shouldComputeSha256() throws IOException {
        // Given: 一个已知内容的文件
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Hello, World!");

        // 期望的 SHA-256: "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f"
        String expectedSha256 = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";

        // When
        String actualSha256 = checksumValidator.computeSha256(testFile);

        // Then
        assertEquals(expectedSha256, actualSha256);
    }

    @Test
    @DisplayName("应正确校验匹配的校验和")
    void shouldValidateMatchingChecksum() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");
        String correctChecksum = checksumValidator.computeSha256(testFile);

        // When
        ChecksumValidator.ValidationResult result = checksumValidator.validate(testFile, correctChecksum);

        // Then
        assertTrue(result.valid());
        assertNull(result.errorMessage());
    }

    @Test
    @DisplayName("应拒绝不匹配的校验和")
    void shouldRejectMismatchedChecksum() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test content");
        String wrongChecksum = "0000000000000000000000000000000000000000000000000000000000000000";

        // When
        ChecksumValidator.ValidationResult result = checksumValidator.validate(testFile, wrongChecksum);

        // Then
        assertFalse(result.valid());
        assertNotNull(result.errorMessage());
        assertTrue(result.errorMessage().contains("Checksum mismatch"));
    }

    @Test
    @DisplayName("应处理空文件")
    void shouldHandleEmptyFile() throws IOException {
        // Given: 空文件
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);

        // 期望的 SHA-256: 空文件的 SHA-256
        // "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        String expectedSha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        // When
        String actualSha256 = checksumValidator.computeSha256(emptyFile);

        // Then
        assertEquals(expectedSha256, actualSha256);
    }

    @Test
    @DisplayName("应拒绝不存在的文件")
    void shouldRejectNonExistentFile() {
        // Given
        Path nonExistentFile = tempDir.resolve("non-existent.txt");

        // When
        ChecksumValidator.ValidationResult result = checksumValidator.validate(nonExistentFile, "somechecksum");

        // Then
        assertFalse(result.valid());
        assertTrue(result.errorMessage().contains("does not exist"));
    }

    @Test
    @DisplayName("应拒绝空校验和")
    void shouldRejectEmptyChecksum() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "content");

        // When
        ChecksumValidator.ValidationResult result = checksumValidator.validate(testFile, "");

        // Then
        assertFalse(result.valid());
        assertTrue(result.errorMessage().contains("empty"));
    }

    @Test
    @DisplayName("应忽略大小写差异")
    void shouldIgnoreCaseDifference() throws IOException {
        // Given
        Path testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "Test");
        String lowerCaseChecksum = checksumValidator.computeSha256(testFile);
        String upperCaseChecksum = lowerCaseChecksum.toUpperCase();

        // When
        ChecksumValidator.ValidationResult lowerResult = checksumValidator.validate(testFile, lowerCaseChecksum);
        ChecksumValidator.ValidationResult upperResult = checksumValidator.validate(testFile, upperCaseChecksum);

        // Then
        assertTrue(lowerResult.valid());
        assertTrue(upperResult.valid());
    }
}
