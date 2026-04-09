package com.actrade.activationrunner.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * gVisor 沙箱运行时配置属性
 *
 * <p>gVisor (Google Sandbox) 是一个用户态内核，提供强隔离的容器运行时。
 * 通过 --runtime runsc 与 Docker 集成，为容器提供额外的安全层。</p>
 *
 * <p>主要安全特性：
 * <ul>
 *   <li>用户态内核 (Sentry) 拦截所有系统调用</li>
 *   <li>内存隔离的地址空间布局随机化 (ASLR)</li>
 *   <li>文件系统隔离 (Warden)</li>
 *   <li>网络隔离选项</li>
 * </ul>
 * </p>
 *
 * @see <a href="https://gvisor.dev/docs/">gVisor Documentation</a>
 */
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "docker.gvisor")
public class GVisorProperties {

    public GVisorProperties() {
        // 从 docker.gvisor 前缀加载配置
    }

    /**
     * 是否启用 gVisor 沙箱
     * 默认: true
     */
    @NotNull
    private Boolean enabled = true;

    /**
     * gVisor runsc 二进制文件路径
     * 默认: /usr/local/bin/runsc
     */
    @NotBlank
    private String runscPath = "/usr/local/bin/runsc";

    /**
     * gVisor 运行时名称 (Docker OCI runtime name)
     * 默认: runsc
     */
    @NotBlank
    private String runtimeName = "runsc";

    /**
     * 沙箱类型
     * - application: 默认沙箱，适用于普通应用
     * - hostinet: 使用宿主机网络栈但保留其他隔离
     * - none: 禁用网络隔离的最小沙箱
     */
    @NotBlank
    private String sandboxType = "application";

    /**
     * 文件系统挂载类型
     * - overlay: 使用 overlayfs (默认)
     * - passthrough: 透传宿主机文件系统
     * - none: 无持久化文件系统
     */
    @NotBlank
    private String fileSystemType = "overlay";

    /**
     * 容器根文件系统路径
     * 默认: /var/lib/docker/virtualization/docker
     */
    @NotBlank
    private String rootPath = "/var/lib/docker/virtualization/docker";

    /**
     * debug 模式开关
     * 启用后会输出详细的 gVisor 调试日志
     */
    @NotNull
    private Boolean debug = false;

    /**
     * debug-log 路径
     * gVisor 调试日志输出文件
     */
    private String debugLogPath = "/var/log/runsc";

    /**
     * 日志格式
     * - text: 文本格式 (默认)
     * - json: JSON 格式
     */
    @NotBlank
    private String logFormat = "text";

    /**
     * 系统调用跟踪
     * 启用后会记录所有系统调用 (生产环境不建议开启)
     */
    @NotNull
    private Boolean traceSyscall = false;

    /**
     * 系统调用跟踪文件
     */
    private String traceFile = "/var/log/runsc_syscall";

    /**
     * 网络模式
     * - sandboxed: 完整隔离的网络栈 (默认)
     * - hostinet: 使用宿主机网络栈
     * - none: 禁用网络
     */
    @NotBlank
    private String networkMode = "sandboxed";

    /**
     * 是否禁用网络
     * 设置为 true 时容器无法访问网络
     */
    @NotNull
    private Boolean networkDisabled = true;

    /**
     * 是否禁用主机共享
     * 设为 true 时阻止容器访问宿主机
     */
    @NotNull
    private Boolean disableHostSharing = true;

    /**
     * 是否启用 seccomp
     * 使用 gVisor 内置的 seccomp 过滤器
     */
    @NotNull
    private Boolean seccompEnabled = true;

    /**
     * seccomp 配置文件路径
     * 如果为空则使用 gVisor 默认配置
     */
    private String seccompProfile;

    /**
     * 容器 cgroup 路径
     * 用于资源限制
     */
    private String cgroupPath;

    /**
     * 是否使用用户命名空间
     * 提供额外的用户 ID 隔离
     */
    @NotNull
    private Boolean userNamespaceEnabled = false;

    /**
     * 最大进程数 (PIDs limit)
     * 限制容器内可用进程数，防止 fork 炸弹
     */
    @NotNull
    private Integer pidsLimit = 128;

    /**
     * 内存限制 (MB)
     * 沙箱内存限制
     */
    private Integer memoryLimitMb;

    /**
     * CPU 限制
     * CPU 核心数或权重
     */
    private Double cpuLimit;

    /**
     * io 限制 (可选)
     * 格式: device:IOBytes
     * 例如: /dev/sda:1048576
     */
    private String ioLimit;

    /**
     * 测试模式
     * 启用后会运行一些自检
     */
    @NotNull
    private Boolean testModeEnabled = false;

    /**
     * 心跳间隔 (毫秒)
     * gVisor 心跳检测间隔
     */
    @NotNull
    private Integer heartbeatIntervalMs = 10000;

    /**
     * 最大启动时间 (毫秒)
     * 容器启动最大等待时间
     */
    @NotNull
    private Integer maxStartTimeMs = 30000;

    /**
     * 是否启用 cgo 追踪
     * 用于调试 native 代码
     */
    @NotNull
    private Boolean cgoTracingEnabled = false;

    /**
     * 警告处理策略
     * - log: 仅记录日志
     * - crash: 触发崩溃
     * - abort: 终止进程
     */
    @NotBlank
    private String warningAction = "log";
}
