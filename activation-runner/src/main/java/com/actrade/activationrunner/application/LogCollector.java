package com.actrade.activationrunner.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Docker 容器日志采集器
 *
 * <p>负责从容器实时采集标准输出和标准错误流，
 * 支持同步和异步两种模式。</p>
 *
 * <p>主要功能：
 * <ul>
 *   <li>实时捕获容器 stdout/stderr 输出</li>
 *   <li>支持日志流式写入文件</li>
 *   <li>支持日志行级回调处理</li>
 *   <li>支持日志流中断和超时控制</li>
 *   <li>自动处理编码问题</li>
 * </ul>
 * </p>
 *
 * <p>与 gVisor 集成说明：
 * <ul>
 *   <li>gVisor 容器使用 runsc 运行时，日志采集方式与标准 Docker 容器相同</li>
 *   <li>通过 docker logs 命令获取容器输出</li>
 *   <li>支持 tail 参数控制初始读取行数</li>
 *   <li>支持 -f 参数进行持续跟踪</li>
 * </ul>
 * </p>
 *
 * @see DockerCommandBuilder#buildLogsCommand(String, boolean, int)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LogCollector {

    private static final int DEFAULT_TAIL_LINES = 1000;
    private static final int LOG_FLUSH_INTERVAL_MS = 100;
    private static final int BUFFER_SIZE = 8192;

    /**
     * 收集容器日志（同步模式）
     *
     * <p>执行 docker logs 命令，实时捕获并写入指定文件。
     * 方法会阻塞直到日志收集完成或超时。</p>
     *
     * @param containerName 容器名称
     * @param stdoutPath 标准输出文件路径
     * @param stderrPath 标准错误文件路径
     * @param timeoutSeconds 最大等待时间
     * @return 日志收集结果
     */
    public LogCollectionResult collectSync(
            String containerName,
            Path stdoutPath,
            Path stderrPath,
            int timeoutSeconds
    ) {
        return collectSync(containerName, stdoutPath, stderrPath, timeoutSeconds, -1);
    }

    /**
     * 收集容器日志（同步模式，带初始行数限制）
     *
     * @param containerName 容器名称
     * @param stdoutPath 标准输出文件路径
     * @param stderrPath 标准错误文件路径
     * @param timeoutSeconds 最大等待时间
     * @param initialTailLines 初始读取的行数 (-1 表示全部)
     * @return 日志收集结果
     */
    public LogCollectionResult collectSync(
            String containerName,
            Path stdoutPath,
            Path stderrPath,
            int timeoutSeconds,
            int initialTailLines
    ) {
        log.debug("Starting sync log collection. container={}, timeout={}s, tail={}",
                containerName, timeoutSeconds, initialTailLines);

        StringBuilder stdoutContent = new StringBuilder();
        StringBuilder stderrContent = new StringBuilder();
        long startTime = System.currentTimeMillis();
        AtomicBoolean completed = new AtomicBoolean(false);

        try {
            // 确保目标文件目录存在
            if (stdoutPath.getParent() != null) {
                Files.createDirectories(stdoutPath.getParent());
            }
            if (stderrPath.getParent() != null) {
                Files.createDirectories(stderrPath.getParent());
            }

            // 打开输出流
            try (var stdoutStream = Files.newOutputStream(stdoutPath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                 var stderrStream = Files.newOutputStream(stderrPath,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                // 构建 docker logs 命令
                ProcessBuilder pb = new ProcessBuilder();
                pb.command(buildLogsCommand(containerName, false,
                        initialTailLines > 0 ? initialTailLines : DEFAULT_TAIL_LINES));
                pb.redirectErrorStream(false);

                Process process = pb.start();

                // 分别处理 stdout 和 stderr
                Thread stdoutThread = startLogCaptureThread(
                        process.getInputStream(),
                        stdoutStream,
                        stdoutContent,
                        "stdout",
                        completed
                );

                Thread stderrThread = startLogCaptureThread(
                        process.getErrorStream(),
                        stderrStream,
                        stderrContent,
                        "stderr",
                        completed
                );

                // 等待进程完成或超时
                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                long elapsed = System.currentTimeMillis() - startTime;

                if (!finished) {
                    log.warn("Log collection timed out, destroying process. container={}", containerName);
                    process.destroyForcibly();
                    return LogCollectionResult.timeout(containerName, elapsed, stdoutContent.toString(), stderrContent.toString());
                }

                // 等待日志线程结束
                stdoutThread.join(1000);
                stderrThread.join(1000);

                int exitCode = process.exitValue();
                long duration = System.currentTimeMillis() - startTime;

                if (exitCode != 0) {
                    log.warn("docker logs exited with non-zero code. container={}, exitCode={}", containerName, exitCode);
                }

                return LogCollectionResult.success(
                        containerName,
                        duration,
                        stdoutContent.toString(),
                        stderrContent.toString(),
                        exitCode
                );

            } catch (IOException e) {
                log.error("Failed to write log files. container={}, stdout={}, stderr={}",
                        containerName, stdoutPath, stderrPath, e);
                return LogCollectionResult.error(containerName, e.getMessage());
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Log collection interrupted. container={}", containerName, e);
            return LogCollectionResult.error(containerName, "Interrupted: " + e.getMessage());
        } catch (IOException e) {
            log.error("Failed to start log collection process. container={}", containerName, e);
            return LogCollectionResult.error(containerName, e.getMessage());
        }
    }

    /**
     * 收集容器日志（异步模式）
     *
     * <p>返回异步任务，可用于在容器运行期间持续跟踪日志。</p>
     *
     * @param containerName 容器名称
     * @param outputPath 日志输出文件路径
     * @param callback 行级回调处理
     * @return 异步日志收集任务
     */
    public AsyncLogCollector collectAsync(
            String containerName,
            Path outputPath,
            LogLineCallback callback
    ) {
        log.debug("Starting async log collection. container={}", containerName);

        AsyncLogCollector collector = new AsyncLogCollector(
                containerName,
                outputPath,
                callback,
                this
        );
        collector.start();
        return collector;
    }

    /**
     * 读取已存在的日志文件内容
     */
    public String readLogContent(Path logPath) throws IOException {
        if (Files.notExists(logPath)) {
            return "";
        }
        return Files.readString(logPath, StandardCharsets.UTF_8);
    }

    /**
     * 读取日志文件并返回指定行数
     */
    public List<String> readLogLines(Path logPath, int maxLines) throws IOException {
        if (Files.notExists(logPath)) {
            return List.of();
        }
        try (BufferedReader reader = Files.newBufferedReader(logPath, StandardCharsets.UTF_8)) {
            return reader.lines()
                    .skip(Math.max(0, reader.lines().count() - maxLines))
                    .toList();
        }
    }

    /**
     * 追加内容到日志文件
     */
    public void appendToLog(Path logPath, String content) throws IOException {
        if (logPath.getParent() != null) {
            Files.createDirectories(logPath.getParent());
        }
        Files.writeString(logPath, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    /**
     * 追加单行到日志文件
     */
    public void appendLine(Path logPath, String line) throws IOException {
        appendToLog(logPath, line + System.lineSeparator());
    }

    /**
     * 清空日志文件
     */
    public void clearLog(Path logPath) throws IOException {
        if (Files.exists(logPath)) {
            Files.writeString(logPath, "", StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    private List<String> buildLogsCommand(String containerName, boolean follow, int tail) {
        return List.of("docker", "logs");
    }

    private Thread startLogCaptureThread(
            InputStream inputStream,
            java.io.OutputStream outputStream,
            StringBuilder contentBuilder,
            String streamName,
            AtomicBoolean completed
    ) {
        Thread thread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8), BUFFER_SIZE)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 追加到内存缓冲
                    contentBuilder.append(line).append(System.lineSeparator());

                    // 写入文件
                    outputStream.write((line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
                    outputStream.flush();
                }
            } catch (IOException e) {
                if (!completed.get()) {
                    log.debug("Log stream closed for {}. {}", streamName, e.getMessage());
                }
            }
        }, "log-capture-" + streamName);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * 日志行级回调接口
     */
    @FunctionalInterface
    public interface LogLineCallback {
        void onLine(String line, boolean isStderr);
    }

    /**
     * 异步日志收集器
     */
    public static class AsyncLogCollector {
        private final String containerName;
        private final Path outputPath;
        private final LogLineCallback callback;
        private final LogCollector logCollector;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private Process process;
        private Thread captureThread;

        public AsyncLogCollector(String containerName, Path outputPath,
                               LogLineCallback callback, LogCollector logCollector) {
            this.containerName = containerName;
            this.outputPath = outputPath;
            this.callback = callback;
            this.logCollector = logCollector;
        }

        public void start() {
            if (running.compareAndSet(false, true)) {
                captureThread = new Thread(this::runCapture, "async-log-collector-" + containerName);
                captureThread.setDaemon(true);
                captureThread.start();
            }
        }

        public void stop() {
            running.set(false);
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            if (captureThread != null) {
                captureThread.interrupt();
            }
        }

        public boolean isRunning() {
            return running.get();
        }

        private void runCapture() {
            try {
                // 先读取已有日志
                String existingContent = logCollector.readLogContent(outputPath);
                if (!existingContent.isEmpty() && callback != null) {
                    for (String line : existingContent.split(System.lineSeparator())) {
                        callback.onLine(line, false);
                    }
                }

                // 然后持续跟踪
                ProcessBuilder pb = new ProcessBuilder();
                pb.command("docker", "logs", "-f", "--tail", "0", containerName);
                pb.redirectErrorStream(false);

                process = pb.start();

                try (var outputStream = Files.newOutputStream(outputPath,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                     var reader = new BufferedReader(
                             new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

                    String line;
                    while (running.get() && (line = reader.readLine()) != null) {
                        // 写入文件
                        outputStream.write((line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
                        outputStream.flush();

                        // 回调
                        if (callback != null) {
                            callback.onLine(line, false);
                        }
                    }
                }

            } catch (IOException e) {
                log.debug("Async log collection error. container={}, error={}",
                        containerName, e.getMessage());
            } finally {
                running.set(false);
            }
        }
    }

    /**
     * 日志收集结果
     *
     * @param containerName 容器名称
     * @param success 是否成功
     * @param durationMs 收集耗时
     * @param stdoutContent 标准输出内容
     * @param stderrContent 标准错误内容
     * @param exitCode docker logs 退出码
     * @param errorMessage 错误信息
     * @param timedOut 是否超时
     */
    public record LogCollectionResult(
            String containerName,
            boolean success,
            long durationMs,
            String stdoutContent,
            String stderrContent,
            int exitCode,
            String errorMessage,
            boolean timedOut
    ) {
        public static LogCollectionResult success(String containerName, long durationMs,
                                                  String stdout, String stderr, int exitCode) {
            return new LogCollectionResult(containerName, true, durationMs,
                    stdout, stderr, exitCode, null, false);
        }

        public static LogCollectionResult error(String containerName, String errorMessage) {
            return new LogCollectionResult(containerName, false, 0,
                    "", "", -1, errorMessage, false);
        }

        public static LogCollectionResult timeout(String containerName, long durationMs,
                                                  String stdout, String stderr) {
            return new LogCollectionResult(containerName, false, durationMs,
                    stdout, stderr, -1, "Timeout", true);
        }

        public String getFullOutput() {
            return stdoutContent + (stderrContent.isEmpty() ? "" : System.lineSeparator() + stderrContent);
        }
    }
}
