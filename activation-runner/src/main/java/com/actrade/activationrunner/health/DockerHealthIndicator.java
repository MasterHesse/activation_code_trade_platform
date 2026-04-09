package com.actrade.activationrunner.health;

import com.actrade.activationrunner.config.DockerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Docker 连接健康检查指示器
 *
 * <p>检查 Docker daemon 是否可用，以及 gVisor runtime 是否正确配置。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DockerHealthIndicator implements HealthIndicator {

    private final DockerProperties dockerProperties;

    @Override
    public Health health() {
        String dockerBinary = dockerProperties.getBinary();
        
        try {
            // 检查 Docker daemon 是否响应
            ProcessBuilder pb = new ProcessBuilder(dockerBinary, "info", "--format", "{{.ServerVersion}}");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return Health.down()
                        .withDetail("error", "Docker command timed out")
                        .build();
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return Health.down()
                        .withDetail("error", "Docker daemon not responding")
                        .withDetail("exitCode", exitCode)
                        .build();
            }
            
            String version = output.toString().trim();
            if (version.isEmpty()) {
                return Health.down()
                        .withDetail("error", "Empty response from Docker")
                        .build();
            }
            
            // 检查 gVisor runtime
            String runtime = dockerProperties.getRuntime();
            if ("runsc".equals(runtime)) {
                return checkGvisorRuntime();
            }
            
            return Health.up()
                    .withDetail("version", version)
                    .withDetail("runtime", runtime)
                    .withDetail("networkDisabled", dockerProperties.getNetworkDisabled())
                    .build();
            
        } catch (Exception e) {
            log.warn("Docker health check failed: {}", e.getMessage());
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withDetail("binary", dockerBinary)
                    .build();
        }
    }
    
    private Health checkGvisorRuntime() {
        try {
            ProcessBuilder pb = new ProcessBuilder(dockerProperties.getBinary(), 
                    "info", "--format", "{{.Runtimes.runsc}}");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }
            
            process.waitFor(5, TimeUnit.SECONDS);
            String gvisorStatus = output.toString().trim();
            
            if (gvisorStatus.contains("path")) {
                return Health.up()
                        .withDetail("version", gvisorStatus.split("path:")[1].split(",")[0].trim())
                        .withDetail("runtime", "runsc")
                        .withDetail("gvisor", "configured")
                        .withDetail("networkDisabled", dockerProperties.getNetworkDisabled())
                        .build();
            } else {
                return Health.up()
                        .withDetail("docker", "available")
                        .withDetail("runtime", "runsc")
                        .withDetail("gvisor", "status unknown")
                        .build();
            }
            
        } catch (Exception e) {
            log.warn("gVisor runtime check failed: {}", e.getMessage());
            return Health.up()
                    .withDetail("docker", "available")
                    .withDetail("runtime", "runsc")
                    .withDetail("gvisor", "check failed: " + e.getMessage())
                    .build();
        }
    }
}
