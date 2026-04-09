package com.actrade.activationrunner.application;

import com.actrade.activationrunner.config.DockerProperties;
import com.actrade.activationrunner.config.GVisorProperties;
import com.actrade.activationrunner.client.dto.ToolVersionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.*;

/**
 * Docker 命令构建器
 *
 * <p>负责构建各种 Docker CLI 命令，包括：
 * <ul>
 *   <li>镜像拉取 (docker pull)</li>
 *   <li>容器创建 (docker create)</li>
 *   <li>容器启动 (docker start)</li>
 *   <li>容器运行 (docker run)</li>
 *   <li>容器停止 (docker stop)</li>
 *   <li>容器删除 (docker rm)</li>
 *   <li>镜像检查 (docker inspect)</li>
 *   <li>系统信息 (docker info)</li>
 * </ul>
 * </p>
 *
 * <p>与 gVisor runsc 集成的关键点：
 * <ul>
 *   <li>使用 --runtime 参数指定 runsc 作为 OCI 运行时</li>
 *   <li>通过 runsc 分发配置传递 gVisor 特定参数</li>
 *   <li>支持网络隔离、文件系统类型等 gVisor 特性</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DockerCommandBuilder {

    private final DockerProperties dockerProperties;
    private final GVisorProperties gVisorProperties;

    /**
     * 构建镜像拉取命令
     */
    public List<String> buildPullCommand(String image) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("pull");
        cmd.add(image);

        log.debug("Built pull command for image: {}", image);
        return cmd;
    }

    /**
     * 构建镜像存在性检查命令
     */
    public List<String> buildImageExistsCommand(String image) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("image");
        cmd.add("inspect");
        cmd.add(image);

        return cmd;
    }

    /**
     * 构建容器创建命令 (docker create)
     *
     * <p>此方法构建完整的容器创建命令，集成 gVisor 运行时配置。
     * 容器将以 gVisor 沙箱模式运行，实现强隔离。</p>
     *
     * @param containerName 容器名称 (唯一标识)
     * @param image 镜像名称
     * @param workDir 容器内工作目录
     * @param command 容器启动命令 (可为空)
     * @param entrypoint 入口点 (可覆盖镜像默认)
     * @param volumeMounts 卷挂载映射
     * @param envVars 环境变量
     * @param memoryMb 内存限制 (MB)
     * @param cpuLimit CPU 限制
     * @param pidsLimit 最大进程数
     * @param networkDisabled 是否禁用网络
     * @param labelMap 标签映射
     * @return 命令列表
     */
    public List<String> buildCreateCommand(
            String containerName,
            String image,
            Path workDir,
            String[] command,
            String[] entrypoint,
            Map<String, String> volumeMounts,
            Map<String, String> envVars,
            Integer memoryMb,
            Double cpuLimit,
            Integer pidsLimit,
            boolean networkDisabled,
            Map<String, String> labelMap
    ) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("create");

        // 容器名称
        cmd.add("--name");
        cmd.add(containerName);

        // gVisor 运行时
        cmd.add("--runtime");
        cmd.add(dockerProperties.getRuntime());

        // 交互式模式
        cmd.add("-i");

        // 工作目录
        if (workDir != null) {
            cmd.add("-w");
            cmd.add(workDir.toString());
        }

        // 内存限制
        int effectiveMemoryMb = memoryMb != null ? memoryMb : dockerProperties.getDefaultMemoryMb();
        cmd.add("-m");
        cmd.add(effectiveMemoryMb + "m");

        // CPU 限制
        double effectiveCpu = cpuLimit != null ? cpuLimit : dockerProperties.getDefaultCpuLimit();
        cmd.add("--cpus");
        cmd.add(String.valueOf(effectiveCpu));

        // PID 限制 (防止 fork 炸弹)
        int effectivePidsLimit = pidsLimit != null ? pidsLimit : dockerProperties.getPidsLimit();
        cmd.add("--pids-limit");
        cmd.add(String.valueOf(effectivePidsLimit));

        // 网络配置
        if (networkDisabled || dockerProperties.getNetworkDisabled()) {
            cmd.add("--network");
            cmd.add("none");
        }

        // 环境变量
        if (envVars != null) {
            for (Map.Entry<String, String> env : envVars.entrySet()) {
                cmd.add("-e");
                cmd.add(env.getKey() + "=" + env.getValue());
            }
        }

        // 卷挂载
        if (volumeMounts != null) {
            for (Map.Entry<String, String> mount : volumeMounts.entrySet()) {
                cmd.add("-v");
                cmd.add(mount.getKey() + ":" + mount.getValue());
            }
        }

        // 标签
        if (labelMap != null) {
            for (Map.Entry<String, String> label : labelMap.entrySet()) {
                cmd.add("--label");
                cmd.add(label.getKey() + "=" + label.getValue());
            }
        }

        // 入口点
        if (entrypoint != null && entrypoint.length > 0) {
            cmd.add("--entrypoint");
            cmd.add(String.join(" ", entrypoint));
        }

        // gVisor runsc 分发配置
        buildGVisorFlags(cmd);

        // 镜像
        cmd.add(image);

        // 命令
        if (command != null && command.length > 0) {
            cmd.addAll(Arrays.asList(command));
        }

        log.debug("Built create command for container: {}, image: {}", containerName, image);
        return cmd;
    }

    /**
     * 构建容器运行命令 (docker run)
     *
     * <p>docker run = docker create + docker start
     * 适用于一次性执行场景</p>
     */
    public List<String> buildRunCommand(
            String containerName,
            String image,
            Path workDir,
            String[] command,
            String[] entrypoint,
            Map<String, String> volumeMounts,
            Map<String, String> envVars,
            Integer memoryMb,
            Double cpuLimit,
            Integer pidsLimit,
            boolean networkDisabled,
            Map<String, String> labelMap,
            long timeoutSeconds
    ) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("run");

        // 容器名称
        cmd.add("--name");
        cmd.add(containerName);

        // gVisor 运行时
        cmd.add("--runtime");
        cmd.add(dockerProperties.getRuntime());

        // 交互式模式
        cmd.add("-i");

        // 超时设置 (在 stop 命令中使用)
        // 注意: timeout 参数需要外部处理

        // 工作目录
        if (workDir != null) {
            cmd.add("-w");
            cmd.add(workDir.toString());
        }

        // 资源限制
        int effectiveMemoryMb = memoryMb != null ? memoryMb : dockerProperties.getDefaultMemoryMb();
        cmd.add("-m");
        cmd.add(effectiveMemoryMb + "m");

        double effectiveCpu = cpuLimit != null ? cpuLimit : dockerProperties.getDefaultCpuLimit();
        cmd.add("--cpus");
        cmd.add(String.valueOf(effectiveCpu));

        int effectivePidsLimit = pidsLimit != null ? pidsLimit : dockerProperties.getPidsLimit();
        cmd.add("--pids-limit");
        cmd.add(String.valueOf(effectivePidsLimit));

        // 网络配置
        if (networkDisabled || dockerProperties.getNetworkDisabled()) {
            cmd.add("--network");
            cmd.add("none");
        }

        // 环境变量
        if (envVars != null) {
            for (Map.Entry<String, String> env : envVars.entrySet()) {
                cmd.add("-e");
                cmd.add(env.getKey() + "=" + env.getValue());
            }
        }

        // 卷挂载
        if (volumeMounts != null) {
            for (Map.Entry<String, String> mount : volumeMounts.entrySet()) {
                cmd.add("-v");
                cmd.add(mount.getKey() + ":" + mount.getValue());
            }
        }

        // 标签
        if (labelMap != null) {
            for (Map.Entry<String, String> label : labelMap.entrySet()) {
                cmd.add("--label");
                cmd.add(label.getKey() + "=" + label.getValue());
            }
        }

        // 入口点
        if (entrypoint != null && entrypoint.length > 0) {
            cmd.add("--entrypoint");
            cmd.add(String.join(" ", entrypoint));
        }

        // gVisor runsc 分发配置
        buildGVisorFlags(cmd);

        // 镜像
        cmd.add(image);

        // 命令
        if (command != null && command.length > 0) {
            cmd.addAll(Arrays.asList(command));
        }

        log.debug("Built run command for container: {}, image: {}", containerName, image);
        return cmd;
    }

    /**
     * 构建容器启动命令 (docker start)
     */
    public List<String> buildStartCommand(String containerName, boolean attach) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("start");

        if (attach) {
            cmd.add("-a");
        } else {
            cmd.add("-a"); // 附加输出以便捕获日志
        }

        cmd.add(containerName);

        log.debug("Built start command for container: {}", containerName);
        return cmd;
    }

    /**
     * 构建容器停止命令 (docker stop)
     */
    public List<String> buildStopCommand(String containerName, int timeoutSeconds) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("stop");
        cmd.add("-t");
        cmd.add(String.valueOf(timeoutSeconds));
        cmd.add(containerName);

        log.debug("Built stop command for container: {}, timeout: {}s", containerName, timeoutSeconds);
        return cmd;
    }

    /**
     * 构建容器删除命令 (docker rm)
     */
    public List<String> buildRmCommand(String containerName, boolean force) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("rm");

        if (force) {
            cmd.add("-f");
        }

        cmd.add(containerName);

        log.debug("Built rm command for container: {}, force: {}", containerName, force);
        return cmd;
    }

    /**
     * 构建容器状态检查命令 (docker inspect)
     */
    public List<String> buildInspectCommand(String containerName) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("inspect");
        cmd.add(containerName);

        return cmd;
    }

    /**
     * 构建容器日志查看命令 (docker logs)
     */
    public List<String> buildLogsCommand(String containerName, boolean follow, int tail) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("logs");

        if (follow) {
            cmd.add("-f");
        }

        if (tail > 0) {
            cmd.add("--tail");
            cmd.add(String.valueOf(tail));
        }

        cmd.add(containerName);

        return cmd;
    }

    /**
     * 构建容器执行命令 (docker exec)
     */
    public List<String> buildExecCommand(String containerName, String[] command, boolean detach) {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("exec");
        cmd.add("-i");

        if (detach) {
            cmd.add("-d");
        }

        cmd.add(containerName);
        cmd.addAll(Arrays.asList(command));

        return cmd;
    }

    /**
     * 构建 Docker 系统信息命令 (docker info)
     */
    public List<String> buildInfoCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("info");
        cmd.add("--format");
        cmd.add("{{json .}}");

        return cmd;
    }

    /**
     * 构建运行时检查命令 (验证 runsc 是否可用)
     */
    public List<String> buildRuntimeCheckCommand() {
        List<String> cmd = new ArrayList<>();
        cmd.add(dockerProperties.getBinary());
        cmd.add("info");
        cmd.add("--format");
        cmd.add("{{.Runtimes}}");

        return cmd;
    }

    /**
     * 构建 gVisor 分发标志
     *
     * <p>这些参数通过 Docker 传递给 runsc 运行时。
     * gVisor 使用 runsc 分发来接收自定义配置。</p>
     */
    private void buildGVisorFlags(List<String> cmd) {
        if (!gVisorProperties.getEnabled()) {
            return;
        }

        // 添加 runsc 特定的分发标志
        // 注意: 这些参数通过环境变量传递
        // RUNSC_FLAG_* 环境变量会被 runsc 识别

        // 调试模式
        if (gVisorProperties.getDebug()) {
            cmd.add("--env");
            cmd.add("RUNSC_FLAG_debug=true");
        }

        // 日志格式
        if (!gVisorProperties.getLogFormat().isEmpty()) {
            cmd.add("--env");
            cmd.add("RUNSC_FLAG_log_format=" + gVisorProperties.getLogFormat());
        }

        // 调试日志路径
        if (gVisorProperties.getDebug() && !gVisorProperties.getDebugLogPath().isEmpty()) {
            cmd.add("--env");
            cmd.add("RUNSC_FLAG_debug_log=" + gVisorProperties.getDebugLogPath());
        }

        // 系统调用跟踪
        if (gVisorProperties.getTraceSyscall()) {
            cmd.add("--env");
            cmd.add("RUNSC_FLAG_trace=true");
            if (!gVisorProperties.getTraceFile().isEmpty()) {
                cmd.add("--env");
                cmd.add("RUNSC_FLAG_trace_file=" + gVisorProperties.getTraceFile());
            }
        }

        // 文件系统类型
        if (!gVisorProperties.getFileSystemType().isEmpty()) {
            cmd.add("--env");
            cmd.add("RUNSC_FLAG_file_access=overlay");
        }

        // 网络模式
        if (gVisorProperties.getNetworkDisabled()) {
            cmd.add("--env");
            cmd.add("RUNSC_FLAG_net=host");
        } else {
            cmd.add("--env");
            cmd.add("RUNSC_FLAG_net=" + gVisorProperties.getNetworkMode());
        }

        // 心跳间隔
        cmd.add("--env");
        cmd.add("RUNSC_FLAG_heartbeat=" + gVisorProperties.getHeartbeatIntervalMs());

        // 测试模式
        if (gVisorProperties.getTestModeEnabled()) {
            cmd.add("--env");
            cmd.add("RUNSC_FLAG_test=true");
        }

        // cgo 追踪
        if (gVisorProperties.getCgoTracingEnabled()) {
            cmd.add("--env");
            cmd.add("RUNSC_FLAG_cgo_tracing=true");
        }
    }

    /**
     * 从 ToolVersionDto 构建容器执行命令
     */
    public List<String> buildExecutionCommand(
            String containerName,
            String image,
            Path inputDir,
            ToolVersionDto toolVersion,
            Map<String, String> runtimeEnvVars
    ) {
        // 解析入口点
        String[] entrypoint = null;
        if (toolVersion.entrypoint() != null && !toolVersion.entrypoint().isBlank()) {
            entrypoint = toolVersion.entrypoint().split("\\s+");
        }

        // 解析执行命令
        String[] command = null;
        if (toolVersion.execCommand() != null && !toolVersion.execCommand().isBlank()) {
            command = toolVersion.execCommand().split("\\s+");
        }

        // 构建环境变量
        Map<String, String> envVars = new HashMap<>();
        envVars.put("TASK_INPUT_DIR", "/workspace/input");
        envVars.put("TASK_OUTPUT_DIR", "/workspace/output");
        envVars.put("TASK_LOGS_DIR", "/workspace/logs");
        envVars.put("RUNTIME_TYPE", toolVersion.runtimeType() != null ? toolVersion.runtimeType() : "unknown");

        if (runtimeEnvVars != null) {
            envVars.putAll(runtimeEnvVars);
        }

        // 构建卷挂载
        Map<String, String> volumeMounts = new HashMap<>();
        volumeMounts.put(inputDir.toString(), "/workspace/input:ro");

        // 资源限制
        Integer memoryMb = toolVersion.maxMemoryMb();
        Double cpuLimit = null;
        Integer pidsLimit = gVisorProperties.getPidsLimit();
        boolean networkDisabled = gVisorProperties.getNetworkDisabled();

        // 标签
        Map<String, String> labels = new HashMap<>();
        labels.put("actrade.task.runtime", toolVersion.runtimeType() != null ? toolVersion.runtimeType() : "unknown");
        labels.put("actrade.task.os", toolVersion.runtimeOs() != null ? toolVersion.runtimeOs() : "linux");
        labels.put("actrade.task.arch", toolVersion.runtimeArch() != null ? toolVersion.runtimeArch() : "amd64");

        return buildCreateCommand(
                containerName,
                image,
                Path.of("/workspace/input"),
                command,
                entrypoint,
                volumeMounts,
                envVars,
                memoryMb,
                cpuLimit,
                pidsLimit,
                networkDisabled,
                labels
        );
    }

    /**
     * 将命令列表转换为字符串
     */
    public String toCommandString(List<String> command) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < command.size(); i++) {
            String part = command.get(i);
            if (part.contains(" ") || part.contains("\"") || part.contains("'")) {
                // 需要引号保护
                sb.append('"').append(part.replace("\"", "\\\"")).append('"');
            } else {
                sb.append(part);
            }
            if (i < command.size() - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
