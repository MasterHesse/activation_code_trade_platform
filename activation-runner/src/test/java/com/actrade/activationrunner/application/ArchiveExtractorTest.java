package com.actrade.activationrunner.application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ArchiveExtractor 单元测试
 */
class ArchiveExtractorTest {

    private ArchiveExtractor archiveExtractor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        archiveExtractor = new ArchiveExtractor();
    }

    @Test
    @DisplayName("应正确解压包含多个文件的 ZIP")
    void shouldExtractZipWithMultipleFiles() throws IOException {
        // Given: 创建一个包含多个文件的 ZIP
        Path zipFile = createTestZip("test.zip", "file1.txt", "Hello", "file2.txt", "World", "subdir/file3.txt", "Nested!");

        Path targetDir = tempDir.resolve("extracted");

        // When
        ArchiveExtractor.ExtractResult result = archiveExtractor.extract(zipFile, targetDir);

        // Then
        assertTrue(result.success());
        assertEquals(targetDir, result.extractedDir());
        assertEquals(3, result.fileCount());

        // 验证文件内容
        assertEquals("Hello", Files.readString(targetDir.resolve("file1.txt")));
        assertEquals("World", Files.readString(targetDir.resolve("file2.txt")));
        assertEquals("Nested!", Files.readString(targetDir.resolve("subdir/file3.txt")));
    }

    @Test
    @DisplayName("应正确解压空 ZIP")
    void shouldExtractEmptyZip() throws IOException {
        // Given: 创建一个空 ZIP
        Path zipFile = createTestZip("empty.zip");

        Path targetDir = tempDir.resolve("extracted");

        // When
        ArchiveExtractor.ExtractResult result = archiveExtractor.extract(zipFile, targetDir);

        // Then
        assertTrue(result.success());
        assertEquals(0, result.fileCount());
        assertEquals(0L, result.totalBytes());
    }

    @Test
    @DisplayName("应拒绝路径遍历攻击")
    void shouldRejectZipSlipAttack() throws IOException {
        // Given: 创建一个包含路径遍历文件的 ZIP
        Path zipFile = createZipWithPathTraversal("malicious.zip");

        Path targetDir = tempDir.resolve("extracted");

        // When & Then
        assertThrows(SecurityException.class, () -> archiveExtractor.extract(zipFile, targetDir));
    }

    @Test
    @DisplayName("应拒绝不存在的文件")
    void shouldRejectNonExistentFile() {
        // Given
        Path nonExistent = tempDir.resolve("non-existent.zip");
        Path targetDir = tempDir.resolve("extracted");

        // When & Then
        assertThrows(IOException.class, () -> archiveExtractor.extract(nonExistent, targetDir));
    }

    @Test
    @DisplayName("应正确检测 ZIP 类型")
    void shouldDetectZipType() throws IOException {
        // Given
        Path zipFile = createTestZip("test.zip", "test.txt", "content");
        Path tarFile = tempDir.resolve("test.tar");
        Path unknownFile = tempDir.resolve("test.unknown");
        Files.writeString(tarFile, "not a zip");
        Files.writeString(unknownFile, "unknown format");

        // When
        ArchiveExtractor.ArchiveType zipType = archiveExtractor.detectArchiveType(zipFile);
        ArchiveExtractor.ArchiveType tarType = archiveExtractor.detectArchiveType(tarFile);
        ArchiveExtractor.ArchiveType unknownType = archiveExtractor.detectArchiveType(unknownFile);

        // Then
        assertEquals(ArchiveExtractor.ArchiveType.ZIP, zipType);
        assertEquals(ArchiveExtractor.ArchiveType.TAR, tarType);
        assertEquals(ArchiveExtractor.ArchiveType.UNKNOWN, unknownType);
    }

    @Test
    @DisplayName("应处理文件名中的空格和特殊字符")
    void shouldHandleSpecialCharactersInFilenames() throws IOException {
        // Given
        Path zipFile = createTestZip("special.zip",
                "file with spaces.txt", "content1",
                "file@#$%.txt", "content2");

        Path targetDir = tempDir.resolve("extracted");

        // When
        ArchiveExtractor.ExtractResult result = archiveExtractor.extract(zipFile, targetDir);

        // Then
        assertTrue(result.success());
        assertEquals(2, result.fileCount());
        assertEquals("content1", Files.readString(targetDir.resolve("file with spaces.txt")));
        assertEquals("content2", Files.readString(targetDir.resolve("file@#$%.txt")));
    }

    /**
     * 创建测试用的 ZIP 文件
     */
    private Path createTestZip(String zipName, String... entries) throws IOException {
        Path zipPath = tempDir.resolve(zipName);

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (int i = 0; i < entries.length; i += 2) {
                String entryName = entries[i];
                String content = entries[i + 1];

                ZipEntry entry = new ZipEntry(entryName);
                zos.putNextEntry(entry);
                zos.write(content.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }

        return zipPath;
    }

    /**
     * 创建包含路径遍历攻击的 ZIP 文件
     */
    private Path createZipWithPathTraversal(String zipName) throws IOException {
        Path zipPath = tempDir.resolve(zipName);

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipPath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // 恶意条目：尝试路径遍历
            ZipEntry entry = new ZipEntry("../../../malicious.txt");
            zos.putNextEntry(entry);
            zos.write("malicious content".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        return zipPath;
    }
}
