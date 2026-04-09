package com.actrade.activationrunner.application;

import com.actrade.activationrunner.config.DockerProperties;
import com.actrade.activationrunner.config.GVisorProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DockerCommandBuilder 单元测试
 *
 * <p>测试 Docker 命令构建器的各种命令生成功能，
 * 包括镜像拉取、容器创建、启动、停止等。</p>
 *
 * <p>同时验证 gVisor 沙箱配置的集成。</p>
 */
class DockerCommandBuilderTest {

    private DockerCommandBuilder commandBuilder;
    private DockerProperties dockerProperties;
    private GVisorProperties gVisorProperties;

    @BeforeEach
    void setUp() {
        dockerProperties = new DockerProperties();
        dockerProperties.setBinary("docker");
        dockerProperties.setRuntime("runsc");
        dockerProperties.setNetworkDisabled(true);
        dockerProperties.setDefaultMemoryMb(256);
        dockerProperties.setDefaultCpuLimit(1.0);
        dockerProperties.setPidsLimit(128);

        DockerProperties.Image image = new DockerProperties.Image();
        image.setShell("test-shell:latest");
        dockerProperties.setImage(image);

        gVisorProperties = new GVisorProperties();
        gVisorProperties.setEnabled(true);
        gVisorProperties.setRunscPath("/usr/bin/runsc");
        gVisorProperties.setRuntimeName("runsc");
        gVisorProperties.setSandboxType("application");
        gVisorProperties.setFileSystemType("overlay");
        gVisorProperties.setDebug(false);
        gVisorProperties.setLogFormat("text");
        gVisorProperties.setTraceSyscall(false);
        gVisorProperties.setNetworkDisabled(true);
        gVisorProperties.setNetworkMode("sandboxed");
        gVisorProperties.setHeartbeatIntervalMs(10000);
        gVisorProperties.setTestModeEnabled(false);
        gVisorProperties.setCgoTracingEnabled(false);
        gVisorProperties.setDebugLogPath("/var/log/runsc");
        gVisorProperties.setTraceFile("/var/log/runsc_syscall");

        commandBuilder = new DockerCommandBuilder(dockerProperties, gVisorProperties);
    }

    @Nested
    @DisplayName("镜像命令测试")
    class ImageCommandTests {

        @Test
        @DisplayName("构建镜像拉取命令")
        void testBuildPullCommand() {
            List<String> command = commandBuilder.buildPullCommand("test-image:latest");

            assertNotNull(command);
            assertTrue(command.size() >= 3);
            assertEquals("docker", command.get(0));
            assertEquals("pull", command.get(1));
            assertEquals("test-image:latest", command.get(2));
        }

        @Test
        @DisplayName("构建镜像存在性检查命令")
        void testBuildImageExistsCommand() {
            List<String> command = commandBuilder.buildImageExistsCommand("test-image:latest");

            assertNotNull(command);
            assertEquals("docker", command.get(0));
            assertEquals("image", command.get(1));
            assertEquals("inspect", command.get(2));
            assertEquals("test-image:latest", command.get(3));
        }
    }

    @Nested
    @DisplayName("容器创建命令测试")
    class ContainerCreateTests {

        @Test
        @DisplayName("构建基本容器创建命令")
        void testBuildBasicCreateCommand() {
            List<String> command = commandBuilder.buildCreateCommand(
                    "test-container",
                    "test-image:latest",
                    null,
                    new String[]{"echo", "hello"},
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null
            );

            assertNotNull(command);
            assertTrue(command.size() > 5);

            // 检查基本元素
            assertEquals("docker", command.get(0));
            assertEquals("create", command.get(1));
            assertEquals("--name", command.get(2));
            assertEquals("test-container", command.get(3));
            assertEquals("--runtime", command.get(4));
            assertEquals("runsc", command.get(5));
        }

        @Test
        @DisplayName("验证 gVisor 运行时配置")
        void testGvisorRuntimeConfiguration() {
            List<String> command = commandBuilder.buildCreateCommand(
                    "gvisor-container",
                    "test-image:latest",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null
            );

            // 检查 --runtime 参数
            int runtimeIndex = command.indexOf("--runtime");
            assertTrue(runtimeIndex > 0);
            assertEquals("runsc", command.get(runtimeIndex + 1));

            // 检查网络禁用
            int networkIndex = command.indexOf("--network");
            assertTrue(networkIndex > 0);
            assertEquals("none", command.get(networkIndex + 1));
        }

        @Test
        @DisplayName("验证资源限制配置")
        void testResourceLimits() {
            List<String> command = commandBuilder.buildCreateCommand(
                    "resource-container",
                    "test-image:latest",
                    null,
                    null,
                    null,
                    null,
                    null,
                    512,
                    2.0,
                    256,
                    true,
                    null
            );

            // 检查内存限制
            int memIndex = command.indexOf("-m");
            assertTrue(memIndex > 0);
            assertEquals("512m", command.get(memIndex + 1));

            // 检查 CPU 限制
            int cpuIndex = command.indexOf("--cpus");
            assertTrue(cpuIndex > 0);
            assertEquals("2.0", command.get(cpuIndex + 1));

            // 检查 PID 限制
            int pidsIndex = command.indexOf("--pids-limit");
            assertTrue(pidsIndex > 0);
            assertEquals("256", command.get(pidsIndex + 1));
        }

        @Test
        @DisplayName("验证环境变量配置")
        void testEnvironmentVariables() {
            Map<String, String> envVars = Map.of(
                    "KEY1", "value1",
                    "KEY2", "value2"
            );

            List<String> command = commandBuilder.buildCreateCommand(
                    "env-container",
                    "test-image:latest",
                    null,
                    null,
                    null,
                    null,
                    envVars,
                    null,
                    null,
                    null,
                    true,
                    null
            );

            // 检查环境变量
            int envIndex1 = command.indexOf("-e");
            assertTrue(envIndex1 > 0);
            assertEquals("KEY1=value1", command.get(envIndex1 + 1));

            int envIndex2 = command.indexOf("KEY2=value2");
            assertTrue(envIndex2 > 0);
        }

        @Test
        @DisplayName("验证卷挂载配置")
        void testVolumeMounts() {
            Map<String, String> mounts = Map.of(
                    "/host/path:/container/path",
                    "/data:/mnt/data"
            );

            List<String> command = commandBuilder.buildCreateCommand(
                    "volume-container",
                    "test-image:latest",
                    null,
                    null,
                    null,
                    mounts,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null
            );

            // 检查卷挂载
            int volIndex1 = command.indexOf("-v");
            assertTrue(volIndex1 > 0);
            assertEquals("/host/path:/container/path", command.get(volIndex1 + 1));
        }

        @Test
        @DisplayName("验证标签配置")
        void testLabels() {
            Map<String, String> labels = Map.of(
                    "app", "test",
                    "env", "production"
            );

            List<String> command = commandBuilder.buildCreateCommand(
                    "label-container",
                    "test-image:latest",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    labels
            );

            // 检查标签
            int labelIndex = command.indexOf("--label");
            assertTrue(labelIndex > 0);
            assertEquals("app=test", command.get(labelIndex + 1));
        }

        @Test
        @DisplayName("验证入口点配置")
        void testEntrypoint() {
            List<String> command = commandBuilder.buildCreateCommand(
                    "entrypoint-container",
                    "test-image:latest",
                    null,
                    null,
                    new String[]{"/bin/sh", "-c"},
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null
            );

            // 检查入口点
            int entrypointIndex = command.indexOf("--entrypoint");
            assertTrue(entrypointIndex > 0);
            assertEquals("/bin/sh -c", command.get(entrypointIndex + 1));
        }
    }

    @Nested
    @DisplayName("容器生命周期命令测试")
    class ContainerLifecycleTests {

        @Test
        @DisplayName("构建容器启动命令")
        void testBuildStartCommand() {
            List<String> command = commandBuilder.buildStartCommand("test-container", true);

            assertNotNull(command);
            assertEquals("docker", command.get(0));
            assertEquals("start", command.get(1));
            assertEquals("-a", command.get(2));
            assertEquals("test-container", command.get(3));
        }

        @Test
        @DisplayName("构建容器停止命令")
        void testBuildStopCommand() {
            List<String> command = commandBuilder.buildStopCommand("test-container", 30);

            assertNotNull(command);
            assertEquals("docker", command.get(0));
            assertEquals("stop", command.get(1));
            assertEquals("-t", command.get(2));
            assertEquals("30", command.get(3));
            assertEquals("test-container", command.get(4));
        }

        @Test
        @DisplayName("构建容器删除命令")
        void testBuildRmCommand() {
            List<String> command = commandBuilder.buildRmCommand("test-container", true);

            assertNotNull(command);
            assertEquals("docker", command.get(0));
            assertEquals("rm", command.get(1));
            assertEquals("-f", command.get(2));
            assertEquals("test-container", command.get(3));
        }

        @Test
        @DisplayName("构建容器检查命令")
        void testBuildInspectCommand() {
            List<String> command = commandBuilder.buildInspectCommand("test-container");

            assertNotNull(command);
            assertEquals("docker", command.get(0));
            assertEquals("inspect", command.get(1));
            assertEquals("test-container", command.get(2));
        }

        @Test
        @DisplayName("构建容器日志命令")
        void testBuildLogsCommand() {
            List<String> command = commandBuilder.buildLogsCommand("test-container", true, 100);

            assertNotNull(command);
            assertEquals("docker", command.get(0));
            assertEquals("logs", command.get(1));
            assertEquals("-f", command.get(2));
            assertEquals("--tail", command.get(3));
            assertEquals("100", command.get(4));
            assertEquals("test-container", command.get(5));
        }
    }

    @Nested
    @DisplayName("系统命令测试")
    class SystemCommandTests {

        @Test
        @DisplayName("构建 Docker info 命令")
        void testBuildInfoCommand() {
            List<String> command = commandBuilder.buildInfoCommand();

            assertNotNull(command);
            assertEquals("docker", command.get(0));
            assertEquals("info", command.get(1));
        }

        @Test
        @DisplayName("构建运行时检查命令")
        void testBuildRuntimeCheckCommand() {
            List<String> command = commandBuilder.buildRuntimeCheckCommand();

            assertNotNull(command);
            assertEquals("docker", command.get(0));
            assertEquals("info", command.get(1));
        }
    }

    @Nested
    @DisplayName("命令字符串转换测试")
    class CommandStringConversionTests {

        @Test
        @DisplayName("简单命令转换")
        void testSimpleCommandConversion() {
            List<String> command = List.of("docker", "ps");
            String result = commandBuilder.toCommandString(command);

            assertEquals("docker ps", result);
        }

        @Test
        @DisplayName("带空格参数的命令转换")
        void testCommandWithSpaces() {
            List<String> command = List.of("docker", "run", "-it", "my image");
            String result = commandBuilder.toCommandString(command);

            assertEquals("docker run -it \"my image\"", result);
        }

        @Test
        @DisplayName("带引号的命令转换")
        void testCommandWithQuotes() {
            List<String> command = List.of("docker", "run", "test\"image");
            String result = commandBuilder.toCommandString(command);

            assertTrue(result.contains("\\\""));
        }
    }

    @Nested
    @DisplayName("gVisor 配置测试")
    class GVisorConfigTests {

        @Test
        @DisplayName("验证 gVisor 调试模式")
        void testGvisorDebugMode() {
            gVisorProperties.setDebug(true);

            List<String> command = commandBuilder.buildCreateCommand(
                    "debug-container",
                    "test-image:latest",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null
            );

            // 检查调试环境变量
            boolean hasDebugFlag = command.stream().anyMatch(c -> c.contains("RUNSC_FLAG_debug=true"));
            assertTrue(hasDebugFlag);
        }

        @Test
        @DisplayName("验证 gVisor 系统调用跟踪")
        void testGvisorSyscallTrace() {
            gVisorProperties.setTraceSyscall(true);

            List<String> command = commandBuilder.buildCreateCommand(
                    "trace-container",
                    "test-image:latest",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null
            );

            // 检查跟踪环境变量
            boolean hasTraceFlag = command.stream().anyMatch(c -> c.contains("RUNSC_FLAG_trace=true"));
            assertTrue(hasTraceFlag);
        }

        @Test
        @DisplayName("gVisor 禁用时不应添加环境变量")
        void testGvisorDisabledNoEnvVars() {
            gVisorProperties.setEnabled(false);

            List<String> command = commandBuilder.buildCreateCommand(
                    "no-gvisor-container",
                    "test-image:latest",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    true,
                    null
            );

            // 检查没有 RUNSC_FLAG
            boolean hasRunscFlag = command.stream().anyMatch(c -> c.contains("RUNSC_FLAG"));
            assertFalse(hasRunscFlag);
        }

        @Test
        @DisplayName("验证 gVisor 网络模式")
        void testGvisorNetworkMode() {
            gVisorProperties.setNetworkDisabled(false);
            gVisorProperties.setNetworkMode("hostinet");

            List<String> command = commandBuilder.buildCreateCommand(
                    "network-container",
                    "test-image:latest",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,  // networkDisabled = false
                    null
            );

            // 检查网络模式配置
            boolean hasNetworkMode = command.stream().anyMatch(c -> c.contains("RUNSC_FLAG_net=hostinet"));
            assertTrue(hasNetworkMode);
        }
    }
}
