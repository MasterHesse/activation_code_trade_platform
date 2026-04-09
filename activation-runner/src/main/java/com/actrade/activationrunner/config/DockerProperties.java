package com.actrade.activationrunner.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "docker")
public class DockerProperties {

    /**
     * Docker 二进制文件路径
     */
    @NotBlank
    private String binary = "docker";

    /**
     * OCI 运行时名称
     * 推荐使用 runsc (gVisor) 以启用沙箱隔离
     */
    @NotBlank
    private String runtime = "runsc";

    /**
     * 是否禁用容器网络
     * 激活任务默认禁用网络以增强安全性
     */
    @NotNull
    private Boolean networkDisabled = true;

    /**
     * 默认执行超时时间 (秒)
     */
    @NotNull
    private Integer defaultTimeoutSeconds = 300;

    /**
     * 默认内存限制 (MB)
     */
    @NotNull
    private Integer defaultMemoryMb = 256;

    /**
     * 默认 CPU 限制
     */
    @NotNull
    private Double defaultCpuLimit = 1.0d;

    /**
     * 最大进程数限制 (防止 fork 炸弹)
     */
    @NotNull
    private Integer pidsLimit = 128;

    /**
     * 镜像配置
     */
    @Valid
    @NotNull
    private Image image = new Image();

    /**
     * gVisor 沙箱配置
     * 嵌入 gVisor 相关配置
     */
    @Valid
    @NotNull
    private GVisorConfig gvisor = new GVisorConfig();

    @Getter
    @Setter
    public static class Image {

        /**
         * Shell 镜像名称
         * 用于执行激活脚本的基础镜像
         */
        @NotBlank
        private String shell = "activation-runner-shell:latest";

        /**
         * 镜像拉取超时 (秒)
         */
        @NotNull
        private Integer pullTimeoutSeconds = 300;

        /**
         * 镜像预热策略
         * - none: 不预热
         * - always: 总是预热
         * - on-demand: 按需拉取
         */
        @NotBlank
        private String prewarmStrategy = "on-demand";
    }

    /**
     * gVisor 沙箱配置 (嵌入在 docker 配置下)
     */
    @Getter
    @Setter
    public static class GVisorConfig {

        /**
         * 是否启用 gVisor 沙箱
         */
        @NotNull
        private Boolean enabled = true;

        /**
         * gVisor runsc 二进制文件路径
         */
        @NotBlank
        private String runscPath = "/usr/local/bin/runsc";

        /**
         * 沙箱类型
         * - application: 默认沙箱
         * - hostinet: 使用宿主机网络
         */
        @NotBlank
        private String sandboxType = "application";

        /**
         * 文件系统访问模式
         * - overlay: overlayfs (默认)
         * - passthrough: 透传宿主机
         */
        @NotBlank
        private String fileSystemType = "overlay";

        /**
         * 调试模式
         */
        @NotNull
        private Boolean debug = false;

        /**
         * 调试日志路径
         */
        private String debugLogPath = "/var/log/runsc";

        /**
         * 日志格式: text | json
         */
        @NotBlank
        private String logFormat = "text";

        /**
         * 系统调用跟踪 (生产环境不建议开启)
         */
        @NotNull
        private Boolean traceSyscall = false;

        /**
         * 系统调用跟踪输出文件
         */
        private String traceFile = "/var/log/runsc_syscall";

        /**
         * 网络模式
         */
        @NotBlank
        private String networkMode = "sandboxed";

        /**
         * 是否使用 seccomp
         */
        @NotNull
        private Boolean seccompEnabled = true;

        /**
         * seccomp 配置文件路径
         */
        private String seccompProfile;

        /**
         * 是否禁用宿主共享
         */
        @NotNull
        private Boolean disableHostSharing = true;

        /**
         * 心跳间隔 (毫秒)
         */
        @NotNull
        private Integer heartbeatIntervalMs = 10000;

        /**
         * 最大启动时间 (毫秒)
         */
        @NotNull
        private Integer maxStartTimeMs = 30000;

        /**
         * 测试模式
         */
        @NotNull
        private Boolean testModeEnabled = false;

        /**
         * 警告处理策略: log | crash | abort
         */
        @NotBlank
        private String warningAction = "log";
    }
}