package com.actrade.activationrunner.config;

import com.actrade.activationrunner.application.DockerExecutionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Docker 和 gVisor 配置启用类
 *
 * <p>负责启用和验证 Docker/gVisor 配置属性。</p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties({
        DockerProperties.class,
        GVisorProperties.class
})
@RequiredArgsConstructor
public class DockerConfiguration {

    private final DockerProperties dockerProperties;
    private final GVisorProperties gVisorProperties;
    private final DockerExecutionService dockerExecutionService;

    @PostConstruct
    public void validateConfiguration() {
        log.info("Docker configuration loaded:");
        log.info("  binary: {}", dockerProperties.getBinary());
        log.info("  runtime: {}", dockerProperties.getRuntime());
        log.info("  networkDisabled: {}", dockerProperties.getNetworkDisabled());
        log.info("  defaultTimeoutSeconds: {}", dockerProperties.getDefaultTimeoutSeconds());
        log.info("  defaultMemoryMb: {}", dockerProperties.getDefaultMemoryMb());
        log.info("  defaultCpuLimit: {}", dockerProperties.getDefaultCpuLimit());
        log.info("  pidsLimit: {}", dockerProperties.getPidsLimit());
        log.info("  shellImage: {}", dockerProperties.getImage().getShell());

        log.info("gVisor configuration loaded:");
        log.info("  enabled: {}", gVisorProperties.getEnabled());
        log.info("  runscPath: {}", gVisorProperties.getRunscPath());
        log.info("  runtimeName: {}", gVisorProperties.getRuntimeName());
        log.info("  sandboxType: {}", gVisorProperties.getSandboxType());
        log.info("  fileSystemType: {}", gVisorProperties.getFileSystemType());
        log.info("  debug: {}", gVisorProperties.getDebug());
        log.info("  logFormat: {}", gVisorProperties.getLogFormat());
        log.info("  traceSyscall: {}", gVisorProperties.getTraceSyscall());
        log.info("  networkMode: {}", gVisorProperties.getNetworkMode());
        log.info("  networkDisabled: {}", gVisorProperties.getNetworkDisabled());
        log.info("  disableHostSharing: {}", gVisorProperties.getDisableHostSharing());
        log.info("  seccompEnabled: {}", gVisorProperties.getSeccompEnabled());
        log.info("  heartbeatIntervalMs: {}", gVisorProperties.getHeartbeatIntervalMs());
        log.info("  maxStartTimeMs: {}", gVisorProperties.getMaxStartTimeMs());
        log.info("  warningAction: {}", gVisorProperties.getWarningAction());

        // 检查运行时可用性
        if (gVisorProperties.getEnabled()) {
            try {
                DockerExecutionService.RuntimeCheckResult checkResult =
                        dockerExecutionService.checkRuntime();

                if (!checkResult.isReady()) {
                    log.warn("Docker/gVisor runtime check indicates potential issues: {}", checkResult.status());
                    log.warn("Docker available: {}", checkResult.dockerAvailable());
                    log.warn("gVisor available: {}", checkResult.gvisorAvailable());
                    log.warn("Sandbox enabled: {}", checkResult.sandboxEnabled());

                    if (!checkResult.dockerAvailable()) {
                        log.error("Docker daemon is not available! Container execution will fail.");
                    }

                    if (checkResult.sandboxEnabled() && !checkResult.gvisorAvailable()) {
                        log.error("gVisor runsc is not available! Please install runsc and configure Docker.");
                        log.error("Run: sudo apt install runsc  # or your package manager");
                        log.error("Then add to /etc/docker/daemon.json: {{\"runtimes\": {{\"runsc\": {{\"path\": \"{}\"}}}}}}}",
                                gVisorProperties.getRunscPath());
                    }
                } else {
                    log.info("Docker/gVisor runtime check passed. Status: {}", checkResult.status());
                }
            } catch (Exception e) {
                log.warn("Failed to check Docker/gVisor runtime availability: {}", e.getMessage());
                log.warn("Container execution may fail if Docker or gVisor is not properly configured.");
            }
        } else {
            log.warn("gVisor sandbox is DISABLED! Container execution will use standard runtime.");
            log.warn("This is NOT recommended for production environments.");
        }
    }
}
