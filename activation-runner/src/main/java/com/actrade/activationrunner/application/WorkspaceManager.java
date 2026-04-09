package com.actrade.activationrunner.application;

import com.actrade.activationrunner.config.RunnerProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class WorkspaceManager {

    private final RunnerProperties runnerProperties;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(runnerProperties.getWorkspaceRoot());
        log.info("Workspace root initialized: {}", runnerProperties.getWorkspaceRoot().toAbsolutePath());
    }

    public WorkspaceContext createWorkspace(String taskNo, Integer attemptNo) throws IOException {
        String safeTaskNo = sanitizeSegment(taskNo == null ? "unknown-task" : taskNo);
        String safeAttemptNo = attemptNo == null ? "0" : String.valueOf(attemptNo);

        String dirName = safeTaskNo
                + "__attempt-" + safeAttemptNo
                + "__" + UUID.randomUUID().toString().replace("-", "");

        Path rootDir = runnerProperties.getWorkspaceRoot()
                .resolve(dirName)
                .toAbsolutePath()
                .normalize();

        Path inputDir = rootDir.resolve("input");
        Path outputDir = rootDir.resolve("output");
        Path logsDir = rootDir.resolve("logs");
        Path tempDir = rootDir.resolve("temp");

        Files.createDirectories(rootDir);
        Files.createDirectories(inputDir);
        Files.createDirectories(outputDir);
        Files.createDirectories(logsDir);
        Files.createDirectories(tempDir);

        WorkspaceContext context = new WorkspaceContext(
                rootDir,
                inputDir,
                outputDir,
                logsDir,
                tempDir
        );

        log.info(
                "Workspace created. taskNo={}, attemptNo={}, rootDir={}",
                taskNo,
                attemptNo,
                context.rootDir()
        );

        return context;
    }

    public void writeUtf8(Path file, String content) throws IOException {
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, content == null ? "" : content, StandardCharsets.UTF_8);
    }

    public void cleanup(WorkspaceContext context) throws IOException {
        if (context == null || context.rootDir() == null || Files.notExists(context.rootDir())) {
            return;
        }

        deleteRecursively(context.rootDir());
        log.info("Workspace deleted: {}", context.rootDir());
    }

    public void cleanupIfEnabled(WorkspaceContext context) {
        if (!Boolean.TRUE.equals(runnerProperties.getCleanup().getEnabled())) {
            log.info(
                    "Workspace cleanup disabled, keep workspace. rootDir={}",
                    context == null ? null : context.rootDir()
            );
            return;
        }

        try {
            cleanup(context);
        } catch (Exception ex) {
            log.warn(
                    "Workspace cleanup failed. rootDir={}",
                    context == null ? null : context.rootDir(),
                    ex
            );
        }
    }

    public int cleanupExpiredWorkspaces() {
        if (!Boolean.TRUE.equals(runnerProperties.getCleanup().getEnabled())) {
            return 0;
        }

        Path root = runnerProperties.getWorkspaceRoot();
        if (Files.notExists(root)) {
            return 0;
        }

        Instant expireBefore = Instant.now()
                .minus(runnerProperties.getCleanup().getExpireHours(), ChronoUnit.HOURS);

        List<Path> expiredDirs;
        try (Stream<Path> stream = Files.list(root)) {
            expiredDirs = stream
                    .filter(Files::isDirectory)
                    .filter(path -> isExpired(path, expireBefore))
                    .toList();
        } catch (IOException ex) {
            log.warn("Failed to scan workspace root for cleanup. root={}", root, ex);
            return 0;
        }

        int cleaned = 0;
        for (Path dir : expiredDirs) {
            try {
                deleteRecursively(dir);
                cleaned++;
                log.info("Expired workspace cleaned: {}", dir);
            } catch (Exception ex) {
                log.warn("Failed to clean expired workspace: {}", dir, ex);
            }
        }

        return cleaned;
    }

    private boolean isExpired(Path path, Instant expireBefore) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(expireBefore);
        } catch (IOException ex) {
            log.debug("Failed to inspect workspace dir mtime: {}", path, ex);
            return false;
        }
    }

    private void deleteRecursively(Path root) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> paths = stream
                    .sorted(Comparator.reverseOrder())
                    .toList();

            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    private String sanitizeSegment(String raw) {
        return raw
                .replaceAll("[\\\\/:*?\"<>|\\s]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+", "")
                .replaceAll("_+$", "");
    }

    public record WorkspaceContext(
            Path rootDir,
            Path inputDir,
            Path outputDir,
            Path logsDir,
            Path tempDir
    ) {
        public Path stdoutLog() {
            return logsDir.resolve("stdout.log");
        }

        public Path stderrLog() {
            return logsDir.resolve("stderr.log");
        }

        public Path resultJson() {
            return outputDir.resolve("result.json");
        }
    }
}