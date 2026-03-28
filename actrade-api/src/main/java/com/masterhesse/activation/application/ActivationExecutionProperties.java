package com.masterhesse.activation.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "activation.execution")
public class ActivationExecutionProperties {

    /**
     * 工作目录根路径
     */
    private String workspaceRoot = System.getProperty("java.io.tmpdir") + "/activation-workspaces";

    /**
     * 文件资产根目录
     */
    private String assetRoot = System.getProperty("java.io.tmpdir") + "/activation-assets";

    /**
     * 默认超时时间（秒）
     */
    private Integer defaultTimeoutSeconds = 300;

    /**
     * 是否启用失败自动重试
     */
    private Boolean retryEnabled = true;

    /**
     * 最大尝试次数（包含首次执行）
     */
    private Integer maxAttempts = 3;

    /**
     * 每次失败后的延迟重试秒数
     * 例如 [30, 120, 600]
     * 表示：
     * - 第1次失败后，30秒后重试
     * - 第2次失败后，120秒后重试
     * - 第3次失败后，600秒后重试
     */
    private List<Long> retryDelaysSeconds = List.of(30L, 120L, 600L);

    public String getWorkspaceRoot() {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot) {
        this.workspaceRoot = workspaceRoot;
    }

    public String getAssetRoot() {
        return assetRoot;
    }

    public void setAssetRoot(String assetRoot) {
        this.assetRoot = assetRoot;
    }

    public Integer getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    public void setDefaultTimeoutSeconds(Integer defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    public Boolean getRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(Boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public List<Long> getRetryDelaysSeconds() {
        return retryDelaysSeconds;
    }

    public void setRetryDelaysSeconds(List<Long> retryDelaysSeconds) {
        this.retryDelaysSeconds = retryDelaysSeconds;
    }
}