package com.actrade.activationrunner.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "runner")
public class RunnerProperties {

    @NotBlank
    private String instanceId = "activation-runner";

    @NotNull
    private Path workspaceRoot = Path.of("./.runner-workspace");

    @NotBlank
    private String executionMode = "docker";

    @NotNull
    private Integer taskTimeoutSeconds = 300;

    @Valid
    @NotNull
    private Cleanup cleanup = new Cleanup();

    @Getter
    @Setter
    public static class Cleanup {

        @NotNull
        private Boolean enabled = true;

        @NotNull
        private Integer expireHours = 24;
    }
}