package com.actrade.activationrunner.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 压缩包解压工具
 * 支持 ZIP 格式
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveExtractor {

    private static final int BUFFER_SIZE = 8192;

    /**
     * 解压 ZIP 文件到目标目录
     *
     * @param archiveFile 压缩包文件
     * @param targetDir   目标目录
     * @return 解压后的目录路径
     */
    public ExtractResult extract(Path archiveFile, Path targetDir) throws IOException {
        if (archiveFile == null || Files.notExists(archiveFile)) {
            throw new IOException("Archive file does not exist: " + archiveFile);
        }

        if (Files.exists(targetDir) && !Files.isDirectory(targetDir)) {
            throw new IOException("Target path exists but is not a directory: " + targetDir);
        }

        Files.createDirectories(targetDir);

        String archiveName = archiveFile.getFileName().toString().toLowerCase();
        if (!archiveName.endsWith(".zip")) {
            return ExtractResult.failure("Unsupported archive format: " + archiveName + ". Only ZIP is supported.");
        }

        long totalBytesExtracted = 0;
        int fileCount = 0;

        try (InputStream fis = Files.newInputStream(archiveFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = resolveSafePath(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                    log.trace("Extracted directory: {}", entry.getName());
                } else {
                    // 确保父目录存在
                    if (entryPath.getParent() != null) {
                        Files.createDirectories(entryPath.getParent());
                    }

                    // 提取文件
                    long fileSize = extractFile(zis, entryPath);
                    totalBytesExtracted += fileSize;
                    fileCount++;
                    log.trace("Extracted file: {} ({} bytes)", entry.getName(), fileSize);
                }

                zis.closeEntry();
            }
        }

        log.info("Archive extracted successfully. archive={}, targetDir={}, files={}, bytes={}",
                archiveFile.getFileName(), targetDir.getFileName(), fileCount, totalBytesExtracted);

        return ExtractResult.success(targetDir, fileCount, totalBytesExtracted);
    }

    /**
     * 解压 ZIP 输入流到目标目录（不落盘压缩包）
     *
     * @param archiveStream ZIP 输入流
     * @param targetDir     目标目录
     * @param fileSize      压缩包大小（用于日志）
     * @return 解压结果
     */
    public ExtractResult extract(InputStream archiveStream, Path targetDir, long fileSize) throws IOException {
        if (Files.exists(targetDir) && !Files.isDirectory(targetDir)) {
            throw new IOException("Target path exists but is not a directory: " + targetDir);
        }

        Files.createDirectories(targetDir);

        long totalBytesExtracted = 0;
        int fileCount = 0;
        long bytesRead = 0;

        try (BufferedInputStream bis = new BufferedInputStream(archiveStream);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path entryPath = resolveSafePath(targetDir, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    if (entryPath.getParent() != null) {
                        Files.createDirectories(entryPath.getParent());
                    }

                    long fileSize2 = extractFile(zis, entryPath);
                    totalBytesExtracted += fileSize2;
                    fileCount++;
                }

                zis.closeEntry();

                // 进度日志
                if (fileSize > 0) {
                    bytesRead += entry.getCompressedSize();
                    if (bytesRead > 0) {
                        int progress = (int) Math.min((bytesRead * 100) / fileSize, 100);
                        if (progress % 25 == 0) {
                            log.debug("Extracting archive: {}% complete", progress);
                        }
                    }
                }
            }
        }

        log.info("Archive extracted from stream. files={}, bytes={}", fileCount, totalBytesExtracted);

        return ExtractResult.success(targetDir, fileCount, totalBytesExtracted);
    }

    /**
     * 检测压缩包类型
     *
     * @param file 压缩包文件
     * @return 压缩包类型
     */
    public ArchiveType detectArchiveType(Path file) throws IOException {
        if (file == null || Files.notExists(file)) {
            return ArchiveType.UNKNOWN;
        }

        String name = file.getFileName().toString().toLowerCase();

        if (name.endsWith(".zip")) {
            return ArchiveType.ZIP;
        } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
            return ArchiveType.TAR_GZ;
        } else if (name.endsWith(".tar")) {
            return ArchiveType.TAR;
        } else if (name.endsWith(".gz") || name.endsWith(".gzip")) {
            return ArchiveType.GZIP;
        }

        // 通过文件头检测
        try (InputStream is = Files.newInputStream(file);
             BufferedInputStream bis = new BufferedInputStream(is, 4)) {
            byte[] header = bis.readNBytes(4);

            if (header.length >= 4) {
                // PK (ZIP)
                if (header[0] == 0x50 && header[1] == 0x4B) {
                    return ArchiveType.ZIP;
                }
            }
        }

        return ArchiveType.UNKNOWN;
    }

    private long extractFile(ZipInputStream zis, Path targetPath) throws IOException {
        long fileSize = 0;

        try (BufferedOutputStream bos = new BufferedOutputStream(
                Files.newOutputStream(targetPath, StandardOpenOption.CREATE_NEW))) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = zis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                fileSize += bytesRead;
            }
        }

        return fileSize;
    }

    /**
     * 防止 ZIP Slip 攻击 - 确保解压路径不会超出目标目录
     */
    private Path resolveSafePath(Path targetDir, String entryName) {
        // 清理路径中的 .. 和绝对路径
        String normalized = entryName
                .replace("\\", "/")
                .replaceAll("^/+", "")
                .replaceAll("/+", "/");

        Path resolved = targetDir.resolve(normalized).normalize();

        // 确保解析后的路径在目标目录内
        if (!resolved.startsWith(targetDir)) {
            throw new SecurityException(
                    "Archive contains unsafe path that escapes target directory: " + entryName
            );
        }

        return resolved;
    }

    /**
     * 压缩包类型枚举
     */
    public enum ArchiveType {
        ZIP,
        TAR_GZ,
        TAR,
        GZIP,
        UNKNOWN
    }

    /**
     * 解压结果
     */
    public record ExtractResult(
            boolean success,
            Path extractedDir,
            Integer fileCount,
            Long totalBytes,
            String errorMessage
    ) {

        public static ExtractResult success(Path extractedDir, int fileCount, long totalBytes) {
            return new ExtractResult(true, extractedDir, fileCount, totalBytes, null);
        }

        public static ExtractResult failure(String errorMessage) {
            return new ExtractResult(false, null, null, null, errorMessage);
        }
    }
}
