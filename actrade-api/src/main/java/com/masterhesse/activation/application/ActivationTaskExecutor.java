package com.masterhesse.activation.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.masterhesse.activation.domain.entity.ActivationToolVersion;
import com.masterhesse.activation.domain.entity.FileAsset;
import com.masterhesse.activation.domain.enums.ActivationTaskArtifactType;
import com.masterhesse.activation.persistence.ActivationToolVersionRepository;
import com.masterhesse.activation.persistence.FileAssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class ActivationTaskExecutor {

    private static final Logger log = LoggerFactory.getLogger(ActivationTaskExecutor.class);

    private final ActivationToolVersionRepository activationToolVersionRepository;
    private final FileAssetRepository fileAssetRepository;
    private final ObjectMapper objectMapper;
    private final ActivationExecutionProperties executionProperties;

    public ActivationTaskExecutor(ActivationToolVersionRepository activationToolVersionRepository,
                                  FileAssetRepository fileAssetRepository,
                                  ObjectMapper objectMapper,
                                  ActivationExecutionProperties executionProperties) {
        this.activationToolVersionRepository = activationToolVersionRepository;
        this.fileAssetRepository = fileAssetRepository;
        this.objectMapper = objectMapper;
        this.executionProperties = executionProperties;
    }

    public ActivationTaskExecutionResult execute(Long taskId,
                                                 String taskNo,
                                                 Long attemptId,
                                                 UUID activationToolVersionId,
                                                 String payloadJson) {
        try {
            if (activationToolVersionId == null) {
                return ActivationTaskExecutionResult.failed(
                        "TOOL_VERSION_ID_MISSING",
                        "activationToolVersionId is null"
                );
            }

            ActivationToolVersion toolVersion = activationToolVersionRepository.findById(activationToolVersionId)
                    .orElse(null);
            if (toolVersion == null) {
                return ActivationTaskExecutionResult.failed(
                        "TOOL_VERSION_NOT_FOUND",
                        "ActivationToolVersion not found, id=" + activationToolVersionId
                );
            }

            FileAsset packageAsset = resolvePackageAsset(toolVersion);
            if (packageAsset == null) {
                return ActivationTaskExecutionResult.failed(
                        "PACKAGE_FILE_ASSET_NOT_FOUND",
                        "FileAsset not found for toolVersionId=" + activationToolVersionId
                );
            }

            ActivationToolManifest manifest = buildManifest(toolVersion);

            Path workspace = createWorkspace(taskNo, attemptId);
            Path inputDir = Files.createDirectories(workspace.resolve("input"));
            Path outputDir = Files.createDirectories(workspace.resolve("output"));
            Path logDir = Files.createDirectories(workspace.resolve("logs"));
            Path packageDir = Files.createDirectories(workspace.resolve("package"));

            Path payloadFile = inputDir.resolve("payload.json");
            Files.writeString(
                    payloadFile,
                    normalizePayloadJson(payloadJson),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );

            Path packageSourcePath = resolvePackageSourcePath(packageAsset);
            ToolPackageLayout packageLayout = materializePackage(packageSourcePath, packageAsset, packageDir);

            Map<String, String> placeholders = buildPlaceholders(
                    taskId,
                    taskNo,
                    attemptId,
                    workspace,
                    packageLayout.getPackageRoot(),
                    packageLayout.getPackageFile(),
                    inputDir,
                    outputDir,
                    payloadFile
            );

            Path workingDirectory = resolveWorkingDirectory(manifest, packageLayout.getPackageRoot(), placeholders);
            Files.createDirectories(workingDirectory);

            List<String> command = buildCommand(manifest, placeholders);
            if (command.isEmpty()) {
                return ActivationTaskExecutionResult.failed(
                        "ENTRY_COMMAND_EMPTY",
                        "Manifest entryCommand is empty"
                );
            }

            log.info(
                    "Prepared activation execution. taskId={}, taskNo={}, attemptId={}, toolVersionId={}, workspace={}, command={}",
                    taskId,
                    taskNo,
                    attemptId,
                    activationToolVersionId,
                    workspace,
                    command
            );

            return doExecuteProcess(
                    taskId,
                    taskNo,
                    attemptId,
                    manifest,
                    command,
                    workingDirectory,
                    placeholders,
                    outputDir,
                    logDir
            );
        } catch (Exception e) {
            log.error(
                    "Unexpected exception while executing activation task. taskId={}, taskNo={}, attemptId={}, toolVersionId={}",
                    taskId,
                    taskNo,
                    attemptId,
                    activationToolVersionId,
                    e
            );
            return ActivationTaskExecutionResult.failed(
                    "EXECUTION_EXCEPTION",
                    safeMessage(e)
            );
        }
    }

    private FileAsset resolvePackageAsset(ActivationToolVersion toolVersion) {
        if (toolVersion.getFile() != null) {
            return toolVersion.getFile();
        }

        UUID fileId = toolVersion.getFileId();
        if (fileId == null) {
            throw new IllegalArgumentException("ActivationToolVersion.fileId is null");
        }

        return fileAssetRepository.findById(fileId)
                .orElseThrow(() -> new IllegalArgumentException("FileAsset not found, id=" + fileId));
    }

    private ActivationToolManifest buildManifest(ActivationToolVersion toolVersion) {
        ActivationToolManifest manifest;

        Map<String, Object> manifestContent = toolVersion.getManifestContent();
        if (manifestContent == null || manifestContent.isEmpty()) {
            manifest = new ActivationToolManifest();
        } else {
            manifest = objectMapper.convertValue(manifestContent, ActivationToolManifest.class);
        }

        if (manifest.getEntryCommand() == null) {
            manifest.setEntryCommand(new ArrayList<>());
        }
        if (manifest.getArguments() == null) {
            manifest.setArguments(new ArrayList<>());
        }
        if (manifest.getEnv() == null) {
            manifest.setEnv(new HashMap<>());
        }
        if (manifest.getSuccessExitCodes() == null) {
            manifest.setSuccessExitCodes(new ArrayList<>());
        }

        if (manifest.getTimeoutSeconds() == null && toolVersion.getTimeoutSeconds() != null) {
            manifest.setTimeoutSeconds(toolVersion.getTimeoutSeconds());
        }

        if (manifest.getScanOutputDir() == null) {
            manifest.setScanOutputDir(Boolean.TRUE);
        }

        if (!StringUtils.hasText(manifest.getWorkingDirectory())) {
            manifest.setWorkingDirectory("");
        }

        if (manifest.getEntryCommand().isEmpty()) {
            List<String> fallbackEntryCommand = buildFallbackEntryCommand(toolVersion);
            manifest.setEntryCommand(fallbackEntryCommand);
        }

        if (manifest.getSuccessExitCodes().isEmpty()) {
            manifest.setSuccessExitCodes(Collections.singletonList(0));
        }

        if (manifest.getEntryCommand() == null || manifest.getEntryCommand().isEmpty()) {
            throw new IllegalArgumentException(
                    "No executable command found in manifestContent / execCommand / entrypoint"
            );
        }

        return manifest;
    }

    private List<String> buildFallbackEntryCommand(ActivationToolVersion toolVersion) {
        if (StringUtils.hasText(toolVersion.getExecCommand())) {
            return buildShellCommand(toolVersion.getExecCommand());
        }

        if (StringUtils.hasText(toolVersion.getEntrypoint())) {
            return Collections.singletonList(toolVersion.getEntrypoint());
        }

        return Collections.emptyList();
    }

    private List<String> buildShellCommand(String execCommand) {
        if (isWindows()) {
            return List.of("cmd", "/c", execCommand);
        }
        return List.of("bash", "-lc", execCommand);
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT)
                .contains("win");
    }

    private ActivationTaskExecutionResult doExecuteProcess(Long taskId,
                                                           String taskNo,
                                                           Long attemptId,
                                                           ActivationToolManifest manifest,
                                                           List<String> command,
                                                           Path workingDirectory,
                                                           Map<String, String> placeholders,
                                                           Path outputDir,
                                                           Path logDir) {
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Process process = null;
        List<ActivationTaskExecutionArtifact> artifacts = new ArrayList<>();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workingDirectory.toFile());

            Map<String, String> env = processBuilder.environment();
            if (manifest.getEnv() != null) {
                for (Map.Entry<String, String> entry : manifest.getEnv().entrySet()) {
                    env.put(entry.getKey(), applyPlaceholders(entry.getValue(), placeholders));
                }
            }

            process = processBuilder.start();

            CompletableFuture<String> stdoutFuture = readStreamAsync(process.getInputStream(), executorService);
            CompletableFuture<String> stderrFuture = readStreamAsync(process.getErrorStream(), executorService);

            int timeoutSeconds = resolveTimeoutSeconds(manifest);
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(5, TimeUnit.SECONDS);
                }
            }

            String stdout = getFutureValue(stdoutFuture);
            String stderr = getFutureValue(stderrFuture);

            artifacts.add(writeTextArtifact(
                    logDir.resolve("stdout.log"),
                    stdout,
                    ActivationTaskArtifactType.STDOUT
            ));
            artifacts.add(writeTextArtifact(
                    logDir.resolve("stderr.log"),
                    stderr,
                    ActivationTaskArtifactType.STDERR
            ));

            if (manifest.getScanOutputDir() == null || manifest.getScanOutputDir()) {
                artifacts.addAll(scanOutputArtifacts(outputDir));
            }

            if (!finished) {
                return ActivationTaskExecutionResult.failed(
                        "EXECUTION_TIMEOUT",
                        "Process timed out after " + timeoutSeconds + " seconds",
                        null,
                        artifacts
                );
            }

            int exitCode = process.exitValue();
            Set<Integer> successExitCodes = resolveSuccessExitCodes(manifest);

            if (!successExitCodes.contains(exitCode)) {
                return ActivationTaskExecutionResult.failed(
                        "NON_ZERO_EXIT",
                        "Process exited with code " + exitCode,
                        exitCode,
                        artifacts
                );
            }

            return ActivationTaskExecutionResult.success(
                    "Process execution completed successfully",
                    exitCode,
                    artifacts
            );
        } catch (Exception e) {
            log.error(
                    "Process execution failed. taskId={}, taskNo={}, attemptId={}",
                    taskId,
                    taskNo,
                    attemptId,
                    e
            );
            return ActivationTaskExecutionResult.failed(
                    "PROCESS_EXECUTION_FAILED",
                    safeMessage(e),
                    null,
                    artifacts
            );
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            executorService.shutdownNow();
        }
    }

    private Path createWorkspace(String taskNo, Long attemptId) throws IOException {
        Path root = Paths.get(executionProperties.getWorkspaceRoot());
        Files.createDirectories(root);

        String safeTaskNo = sanitizeFileName(taskNo);
        Path workspace = root.resolve(safeTaskNo + "-attempt-" + attemptId);
        Files.createDirectories(workspace);
        return workspace;
    }

    private String normalizePayloadJson(String payloadJson) {
        if (!StringUtils.hasText(payloadJson)) {
            return "{}";
        }
        return payloadJson;
    }

    private Path resolvePackageSourcePath(FileAsset packageAsset) {
        List<Path> candidates = new ArrayList<>();

        String objectKey = packageAsset.getObjectKey();
        String bucketName = packageAsset.getBucketName();
        String storedFilename = packageAsset.getStoredFilename();

        if (StringUtils.hasText(objectKey)) {
            tryAddAbsolutePathCandidate(candidates, objectKey);
        }

        if (StringUtils.hasText(executionProperties.getAssetRoot())) {
            Path assetRoot = Paths.get(executionProperties.getAssetRoot());

            if (StringUtils.hasText(bucketName) && StringUtils.hasText(objectKey)) {
                candidates.add(assetRoot.resolve(bucketName).resolve(objectKey).normalize());
            }
            if (StringUtils.hasText(objectKey)) {
                candidates.add(assetRoot.resolve(objectKey).normalize());
            }
            if (StringUtils.hasText(bucketName) && StringUtils.hasText(storedFilename)) {
                candidates.add(assetRoot.resolve(bucketName).resolve(storedFilename).normalize());
            }
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }

        throw new IllegalArgumentException(
                "Cannot resolve package source path from FileAsset. " +
                        "fileId=" + packageAsset.getFileId() +
                        ", storageProvider=" + packageAsset.getStorageProvider() +
                        ", bucketName=" + packageAsset.getBucketName() +
                        ", objectKey=" + packageAsset.getObjectKey() +
                        ", storedFilename=" + packageAsset.getStoredFilename()
        );
    }

    private void tryAddAbsolutePathCandidate(List<Path> candidates, String rawPath) {
        try {
            Path path = Paths.get(rawPath);
            if (path.isAbsolute()) {
                candidates.add(path.normalize());
            }
        } catch (InvalidPathException ignored) {
            // ignore invalid raw path
        }
    }

    private ToolPackageLayout materializePackage(Path sourcePath,
                                                 FileAsset packageAsset,
                                                 Path packageDir) throws IOException {
        if (Files.isDirectory(sourcePath)) {
            copyDirectory(sourcePath, packageDir);
            markCommonScriptsExecutable(packageDir);
            return new ToolPackageLayout(packageDir, null);
        }

        String fileName = StringUtils.hasText(packageAsset.getOriginalFilename())
                ? packageAsset.getOriginalFilename()
                : sourcePath.getFileName().toString();

        Path copiedFile = packageDir.resolve(fileName);
        Files.copy(sourcePath, copiedFile, StandardCopyOption.REPLACE_EXISTING);
        copiedFile.toFile().setExecutable(true, false);

        if (fileName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            Path unzipDir = packageDir.resolve("unzipped");
            Files.createDirectories(unzipDir);
            unzip(copiedFile, unzipDir);
            markCommonScriptsExecutable(unzipDir);
            return new ToolPackageLayout(unzipDir, copiedFile);
        }

        return new ToolPackageLayout(packageDir, copiedFile);
    }

    private void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        Files.walkFileTree(sourceDir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(dir);
                Path target = targetDir.resolve(relative.toString());
                Files.createDirectories(target);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = sourceDir.relativize(file);
                Path target = targetDir.resolve(relative.toString());
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void unzip(Path zipFile, Path targetDir) throws IOException {
        try (InputStream inputStream = Files.newInputStream(zipFile);
             ZipInputStream zipInputStream = new ZipInputStream(inputStream)) {

            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path targetPath = targetDir.resolve(entry.getName()).normalize();
                if (!targetPath.startsWith(targetDir)) {
                    throw new IOException("Illegal zip entry: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    if (targetPath.getParent() != null) {
                        Files.createDirectories(targetPath.getParent());
                    }
                    Files.copy(zipInputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zipInputStream.closeEntry();
            }
        }
    }

    private void markCommonScriptsExecutable(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> {
                        String lowerName = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        if (lowerName.endsWith(".sh")
                                || lowerName.endsWith(".bin")
                                || lowerName.endsWith(".run")
                                || lowerName.endsWith(".command")) {
                            path.toFile().setExecutable(true, false);
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to mark scripts executable. root={}", root, e);
        }
    }

    private Map<String, String> buildPlaceholders(Long taskId,
                                                  String taskNo,
                                                  Long attemptId,
                                                  Path workspace,
                                                  Path packageRoot,
                                                  Path packageFile,
                                                  Path inputDir,
                                                  Path outputDir,
                                                  Path payloadFile) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("{taskId}", String.valueOf(taskId));
        placeholders.put("{taskNo}", taskNo == null ? "" : taskNo);
        placeholders.put("{attemptId}", String.valueOf(attemptId));
        placeholders.put("{workspace}", workspace.toString());
        placeholders.put("{packageRoot}", packageRoot.toString());
        placeholders.put("{packageFile}", packageFile == null ? "" : packageFile.toString());
        placeholders.put("{inputDir}", inputDir.toString());
        placeholders.put("{outputDir}", outputDir.toString());
        placeholders.put("{payloadFile}", payloadFile.toString());
        return placeholders;
    }

    private Path resolveWorkingDirectory(ActivationToolManifest manifest,
                                         Path packageRoot,
                                         Map<String, String> placeholders) {
        if (!StringUtils.hasText(manifest.getWorkingDirectory())) {
            return packageRoot;
        }

        String resolved = applyPlaceholders(manifest.getWorkingDirectory(), placeholders);
        Path resolvedPath = Paths.get(resolved);
        if (resolvedPath.isAbsolute()) {
            return resolvedPath.normalize();
        }
        return packageRoot.resolve(resolvedPath).normalize();
    }

    private List<String> buildCommand(ActivationToolManifest manifest, Map<String, String> placeholders) {
        List<String> command = new ArrayList<>();

        for (String item : manifest.getEntryCommand()) {
            if (StringUtils.hasText(item)) {
                command.add(applyPlaceholders(item, placeholders));
            }
        }

        if (manifest.getArguments() != null) {
            for (String arg : manifest.getArguments()) {
                if (arg != null) {
                    command.add(applyPlaceholders(arg, placeholders));
                }
            }
        }

        return command;
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        if (text == null) {
            return null;
        }

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private int resolveTimeoutSeconds(ActivationToolManifest manifest) {
        if (manifest.getTimeoutSeconds() != null && manifest.getTimeoutSeconds() > 0) {
            return manifest.getTimeoutSeconds();
        }
        return executionProperties.getDefaultTimeoutSeconds();
    }

    private Set<Integer> resolveSuccessExitCodes(ActivationToolManifest manifest) {
        if (manifest.getSuccessExitCodes() == null || manifest.getSuccessExitCodes().isEmpty()) {
            return Collections.singleton(0);
        }
        return new HashSet<>(manifest.getSuccessExitCodes());
    }

    private CompletableFuture<String> readStreamAsync(InputStream inputStream, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try (InputStream is = inputStream) {
                return new String(is.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }, executor);
    }

    private String getFutureValue(CompletableFuture<String> future) {
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "Failed to read process stream: " + safeMessage(e);
        }
    }

    private ActivationTaskExecutionArtifact writeTextArtifact(Path targetFile,
                                                              String content,
                                                              ActivationTaskArtifactType artifactType) throws IOException {
        Files.createDirectories(targetFile.getParent());

        String finalContent = content == null ? "" : content;
        Files.writeString(
                targetFile,
                finalContent,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );

        return new ActivationTaskExecutionArtifact(
                artifactType,
                targetFile.getFileName().toString(),
                targetFile.toAbsolutePath().toString(),
                "text/plain",
                Files.size(targetFile)
        );
    }

    private List<ActivationTaskExecutionArtifact> scanOutputArtifacts(Path outputDir) throws IOException {
        List<ActivationTaskExecutionArtifact> artifacts = new ArrayList<>();
        if (!Files.exists(outputDir)) {
            return artifacts;
        }

        try (Stream<Path> stream = Files.walk(outputDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(file -> {
                        try {
                            String mediaType = Files.probeContentType(file);
                            if (!StringUtils.hasText(mediaType)) {
                                mediaType = "application/octet-stream";
                            }

                            String relativeName = outputDir.relativize(file).toString();
                            artifacts.add(new ActivationTaskExecutionArtifact(
                                    ActivationTaskArtifactType.OUTPUT_FILE,
                                    relativeName,
                                    file.toAbsolutePath().toString(),
                                    mediaType,
                                    Files.size(file)
                            ));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        return artifacts;
    }

    private String sanitizeFileName(String text) {
        if (!StringUtils.hasText(text)) {
            return "activation-task";
        }
        return text.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String safeMessage(Throwable throwable) {
        if (throwable == null) {
            return "unknown";
        }
        if (StringUtils.hasText(throwable.getMessage())) {
            return throwable.getMessage();
        }
        return throwable.getClass().getSimpleName();
    }

    private static class ToolPackageLayout {

        private final Path packageRoot;
        private final Path packageFile;

        public ToolPackageLayout(Path packageRoot, Path packageFile) {
            this.packageRoot = packageRoot;
            this.packageFile = packageFile;
        }

        public Path getPackageRoot() {
            return packageRoot;
        }

        public Path getPackageFile() {
            return packageFile;
        }
    }
}