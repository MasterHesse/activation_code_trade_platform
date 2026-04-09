package com.actrade.activationrunner.application;

import java.nio.file.Path;

/**
 * 容器执行上下文
 *
 * <p>在容器执行开始前创建，包含执行所需的所有路径和配置信息。
 * 该记录在执行过程中保持不变，确保执行的环境一致性。</p>
 *
 * @param containerName 容器名称 (唯一标识)
 * @param imageName 镜像名称
 * @param timeoutSeconds 超时时间 (秒)
 * @param inputDir 输入目录
 * @param outputDir 输出目录
 * @param logsDir 日志目录
 * @param tempDir 临时目录
 * @param stdoutPath 标准输出文件路径
 * @param stderrPath 标准错误文件路径
 * @param resultJsonPath 结果 JSON 文件路径
 * @param memoryUsageMbPeak 峰值内存使用 (MB)
 * @param cpuUsagePercent CPU 使用率
 * @param diskUsageBytes 磁盘使用字节数
 * @param gvisorSandboxEnabled 是否启用 gVisor 沙箱
 * @param gvisorSandboxType gVisor 沙箱类型
 * @param gvisorTraceEnabled gVisor 系统调用跟踪是否启用
 * @param executionCommand 原始执行命令
 */
public record ContainerExecutionContext(
        String containerName,
        String imageName,
        int timeoutSeconds,
        Path inputDir,
        Path outputDir,
        Path logsDir,
        Path tempDir,
        Path stdoutPath,
        Path stderrPath,
        Path resultJsonPath,
        Long memoryUsageMbPeak,
        Double cpuUsagePercent,
        Long diskUsageBytes,
        boolean gvisorSandboxEnabled,
        String gvisorSandboxType,
        boolean gvisorTraceEnabled,
        String executionCommand
) {

    /**
     * 创建执行上下文构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 执行上下文构建器
     */
    public static class Builder {
        private String containerName;
        private String imageName;
        private int timeoutSeconds;
        private Path inputDir;
        private Path outputDir;
        private Path logsDir;
        private Path tempDir;
        private Path stdoutPath;
        private Path stderrPath;
        private Path resultJsonPath;
        private Long memoryUsageMbPeak;
        private Double cpuUsagePercent;
        private Long diskUsageBytes;
        private boolean gvisorSandboxEnabled;
        private String gvisorSandboxType;
        private boolean gvisorTraceEnabled;
        private String executionCommand;

        public Builder containerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Builder imageName(String imageName) {
            this.imageName = imageName;
            return this;
        }

        public Builder timeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
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

        public Builder tempDir(Path tempDir) {
            this.tempDir = tempDir;
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

        public Builder executionCommand(String executionCommand) {
            this.executionCommand = executionCommand;
            return this;
        }

        public ContainerExecutionContext build() {
            return new ContainerExecutionContext(
                    containerName, imageName, timeoutSeconds,
                    inputDir, outputDir, logsDir, tempDir,
                    stdoutPath, stderrPath, resultJsonPath,
                    memoryUsageMbPeak, cpuUsagePercent, diskUsageBytes,
                    gvisorSandboxEnabled, gvisorSandboxType, gvisorTraceEnabled,
                    executionCommand
            );
        }
    }
}
