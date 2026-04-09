package com.actrade.activationrunner.application;

import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * 容器执行结果记录
 *
 * <p>记录一次容器执行的全部信息，包括：
 * <ul>
 *   <li>执行状态和退出码</li>
 *   <li>时间统计</li>
 *   <li>输出文件路径</li>
 *   <li>性能指标</li>
 *   <li>gVisor 沙箱相关信息</li>
 * </ul>
 * </p>
 *
 * <p>该记录是不可变的，用于在执行完成后传递给编排器。</p>
 *
 * @param success 执行是否成功
 * @param exitCode 容器退出码
 * @param errorCode 错误代码 (失败时)
 * @param errorMessage 错误信息 (失败时)
 * @param containerId 容器 ID
 * @param containerName 容器名称
 * @param imageName 使用的镜像名称
 * @param startedAt 执行开始时间
 * @param finishedAt 执行结束时间
 * @param durationMs 执行耗时 (毫秒)
 * @param timedOut 是否超时
 * @param timeoutSeconds 设定的超时时间
 * @param stdoutPath 标准输出文件路径
 * @param stderrPath 标准错误文件路径
 * @param resultJsonPath 结果 JSON 文件路径
 * @param inputDir 输入目录路径
 * @param outputDir 输出目录路径
 * @param logsDir 日志目录路径
 * @param exitReason 退出原因 (正常退出/信号终止/超时)
 * @param signalNumber 终止信号编号 (如被信号杀死)
 * @param memoryUsageMbPeak 峰值内存使用 (MB)
 * @param cpuUsagePercent CPU 使用率
 * @param diskUsageBytes 磁盘使用字节数
 * @param containerState 容器最终状态
 * @param gvisorSandboxEnabled 是否启用了 gVisor 沙箱
 * @param gvisorSandboxType gVisor 沙箱类型
 * @param gvisorTraceEnabled gVisor 系统调用跟踪是否启用
 * @param diagnostics 诊断信息映射
 * @param executionCommand 执行命令 (用于调试)
 */
public record ContainerExecutionResult(
        // 执行状态
        boolean success,
        int exitCode,
        String errorCode,
        String errorMessage,

        // 容器信息
        String containerId,
        String containerName,
        String imageName,

        // 时间信息
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        long durationMs,
        boolean timedOut,
        int timeoutSeconds,

        // 输出文件
        Path stdoutPath,
        Path stderrPath,
        Path resultJsonPath,
        Path inputDir,
        Path outputDir,
        Path logsDir,

        // 退出详情
        String exitReason,
        Integer signalNumber,

        // 性能指标
        Long memoryUsageMbPeak,
        Double cpuUsagePercent,
        Long diskUsageBytes,

        // 容器状态
        String containerState,

        // gVisor 信息
        boolean gvisorSandboxEnabled,
        String gvisorSandboxType,
        boolean gvisorTraceEnabled,

        // 诊断信息
        Map<String, String> diagnostics,

        // 原始命令 (调试用)
        String executionCommand
) {

    /**
     * 创建一个成功的执行结果
     */
    public static ContainerExecutionResult success(
            String containerId,
            String containerName,
            String imageName,
            int exitCode,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            ContainerExecutionContext context
    ) {
        return new ContainerExecutionResult(
                true,
                exitCode,
                null,
                null,
                containerId,
                containerName,
                imageName,
                startedAt,
                finishedAt,
                Duration.between(startedAt, finishedAt).toMillis(),
                false,
                context.timeoutSeconds(),
                context.stdoutPath(),
                context.stderrPath(),
                context.resultJsonPath(),
                context.inputDir(),
                context.outputDir(),
                context.logsDir(),
                "NormalExit",
                null,
                context.memoryUsageMbPeak(),
                context.cpuUsagePercent(),
                context.diskUsageBytes(),
                "exited",
                true,
                context.gvisorSandboxType(),
                context.gvisorTraceEnabled(),
                Map.of(),
                context.executionCommand()
        );
    }

    /**
     * 创建一个失败的执行结果
     */
    public static ContainerExecutionResult failure(
            String containerId,
            String containerName,
            String imageName,
            String errorCode,
            String errorMessage,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            ContainerExecutionContext context
    ) {
        return new ContainerExecutionResult(
                false,
                -1,
                errorCode,
                errorMessage,
                containerId,
                containerName,
                imageName,
                startedAt,
                finishedAt,
                Duration.between(startedAt, finishedAt).toMillis(),
                false,
                context.timeoutSeconds(),
                context.stdoutPath(),
                context.stderrPath(),
                context.resultJsonPath(),
                context.inputDir(),
                context.outputDir(),
                context.logsDir(),
                "Error",
                null,
                context.memoryUsageMbPeak(),
                context.cpuUsagePercent(),
                context.diskUsageBytes(),
                "failed",
                true,
                context.gvisorSandboxType(),
                context.gvisorTraceEnabled(),
                Map.of("errorCode", errorCode, "errorMessage", errorMessage),
                context.executionCommand()
        );
    }

    /**
     * 创建一个超时导致的失败执行结果
     */
    public static ContainerExecutionResult timeout(
            String containerId,
            String containerName,
            String imageName,
            int timeoutSeconds,
            OffsetDateTime startedAt,
            ContainerExecutionContext context
    ) {
        OffsetDateTime finishedAt = OffsetDateTime.now();
        return new ContainerExecutionResult(
                false,
                -1,
                "EXECUTION_TIMEOUT",
                "Container execution timed out after " + timeoutSeconds + " seconds",
                containerId,
                containerName,
                imageName,
                startedAt,
                finishedAt,
                Duration.between(startedAt, finishedAt).toMillis(),
                true,
                timeoutSeconds,
                context.stdoutPath(),
                context.stderrPath(),
                context.resultJsonPath(),
                context.inputDir(),
                context.outputDir(),
                context.logsDir(),
                "Timeout",
                null,
                context.memoryUsageMbPeak(),
                context.cpuUsagePercent(),
                context.diskUsageBytes(),
                "timeout",
                true,
                context.gvisorSandboxType(),
                context.gvisorTraceEnabled(),
                Map.of("timeoutSeconds", String.valueOf(timeoutSeconds)),
                context.executionCommand()
        );
    }

    /**
     * 创建一个异常导致的失败执行结果
     */
    public static ContainerExecutionResult exception(
            String containerName,
            String imageName,
            String errorCode,
            String errorMessage,
            OffsetDateTime startedAt,
            ContainerExecutionContext context
    ) {
        OffsetDateTime finishedAt = OffsetDateTime.now();
        return new ContainerExecutionResult(
                false,
                -1,
                errorCode,
                errorMessage,
                null,
                containerName,
                imageName,
                startedAt,
                finishedAt,
                Duration.between(startedAt, finishedAt).toMillis(),
                false,
                context.timeoutSeconds(),
                context.stdoutPath(),
                context.stderrPath(),
                context.resultJsonPath(),
                context.inputDir(),
                context.outputDir(),
                context.logsDir(),
                "Exception",
                null,
                null,
                null,
                null,
                "error",
                true,
                context.gvisorSandboxType(),
                context.gvisorTraceEnabled(),
                Map.of("errorCode", errorCode, "errorMessage", errorMessage),
                context.executionCommand()
        );
    }

    /**
     * 创建 builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 结果构建器
     */
    public static class Builder {
        private boolean success;
        private int exitCode;
        private String errorCode;
        private String errorMessage;
        private String containerId;
        private String containerName;
        private String imageName;
        private OffsetDateTime startedAt;
        private OffsetDateTime finishedAt;
        private long durationMs;
        private boolean timedOut;
        private int timeoutSeconds;
        private Path stdoutPath;
        private Path stderrPath;
        private Path resultJsonPath;
        private Path inputDir;
        private Path outputDir;
        private Path logsDir;
        private String exitReason;
        private Integer signalNumber;
        private Long memoryUsageMbPeak;
        private Double cpuUsagePercent;
        private Long diskUsageBytes;
        private String containerState;
        private boolean gvisorSandboxEnabled;
        private String gvisorSandboxType;
        private boolean gvisorTraceEnabled;
        private Map<String, String> diagnostics;
        private String executionCommand;

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder exitCode(int exitCode) {
            this.exitCode = exitCode;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder containerId(String containerId) {
            this.containerId = containerId;
            return this;
        }

        public Builder containerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Builder imageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        public Builder startedAt(OffsetDateTime startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder finishedAt(OffsetDateTime finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public Builder durationMs(long durationMs) {
            this.durationMs = durationMs;
            return this;
        }

        public Builder timedOut(boolean timedOut) {
            this.timedOut = timedOut;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
            return this;
        }

        public Builder stdoutPath(Path stdoutPath) {
            this.stdoutPath = stdoutPath;
            return this;
        }

        public Builder stderrPath(Path stderrPath) {
            this.stderrPath = stderrPath;
            return this;
        }

        public Builder resultJsonPath(Path resultJsonPath) {
            this.resultJsonPath = resultJsonPath;
            return this;
        }

        public Builder inputDir(Path inputDir) {
            this.inputDir = inputDir;
            return this;
        }

        public Builder outputDir(Path outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public Builder logsDir(Path logsDir) {
            this.logsDir = logsDir;
            return this;
        }

        public Builder exitReason(String exitReason) {
            this.exitReason = exitReason;
            return this;
        }

        public Builder signalNumber(Integer signalNumber) {
            this.signalNumber = signalNumber;
            return this;
        }

        public Builder memoryUsageMbPeak(Long memoryUsageMbPeak) {
            this.memoryUsageMbPeak = memoryUsageMbPeak;
            return this;
        }

        public Builder cpuUsagePercent(Double cpuUsagePercent) {
            this.cpuUsagePercent = cpuUsagePercent;
            return this;
        }

        public Builder diskUsageBytes(Long diskUsageBytes) {
            this.diskUsageBytes = diskUsageBytes;
            return this;
        }

        public Builder containerState(String containerState) {
            this.containerState = containerState;
            return this;
        }

        public Builder gvisorSandboxEnabled(boolean gvisorSandboxEnabled) {
            this.gvisorSandboxEnabled = gvisorSandboxEnabled;
            return this;
        }

        public Builder gvisorSandboxType(String gvisorSandboxType) {
            this.gvisorSandboxType = gvisorSandboxType;
            return this;
        }

        public Builder gvisorTraceEnabled(boolean gvisorTraceEnabled) {
            this.gvisorTraceEnabled = gvisorTraceEnabled;
            return this;
        }

        public Builder diagnostics(Map<String, String> diagnostics) {
            this.diagnostics = diagnostics;
            return this;
        }

        public Builder executionCommand(String executionCommand) {
            this.executionCommand = executionCommand;
            return this;
        }

        public ContainerExecutionResult build() {
            return new ContainerExecutionResult(
                    success, exitCode, errorCode, errorMessage,
                    containerId, containerName, imageName,
                    startedAt, finishedAt, durationMs, timedOut, timeoutSeconds,
                    stdoutPath, stderrPath, resultJsonPath,
                    inputDir, outputDir, logsDir,
                    exitReason, signalNumber,
                    memoryUsageMbPeak, cpuUsagePercent, diskUsageBytes,
                    containerState,
                    gvisorSandboxEnabled, gvisorSandboxType, gvisorTraceEnabled,
                    diagnostics,
                    executionCommand
            );
        }
    }
}
