package com.actrade.activationrunner.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Runner 执行指标收集器
 *
 * <p>使用 Micrometer 收集和暴露 Runner 运行指标，供 Prometheus 抓取。</p>
 */
@Slf4j
@Component
public class RunnerMetrics {

    private final MeterRegistry meterRegistry;
    
    // 计数器
    private final Counter tasksReceivedCounter;
    private final Counter tasksClaimedCounter;
    private final Counter tasksCompletedCounter;
    private final Counter tasksFailedCounter;
    private final Counter tasksRetriedCounter;
    
    // 定时器
    private final Timer taskExecutionTimer;
    private final Timer workspaceCreationTimer;
    private final Timer packageDownloadTimer;
    private final Timer artifactUploadTimer;
    
    // 实时指标
    private final AtomicLong activeTasks = new AtomicLong(0);
    private final AtomicLong totalTasksExecuted = new AtomicLong(0);
    private final AtomicLong totalTasksFailed = new AtomicLong(0);
    
    // 错误类型统计
    private final ConcurrentHashMap<String, Counter> errorCounters = new ConcurrentHashMap<>();

    public RunnerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 初始化计数器
        this.tasksReceivedCounter = Counter.builder("runner.tasks.received")
                .description("Total number of tasks received from message queue")
                .tag("type", "total")
                .register(meterRegistry);
        
        this.tasksClaimedCounter = Counter.builder("runner.tasks.claimed")
                .description("Total number of tasks successfully claimed")
                .tag("type", "success")
                .register(meterRegistry);
        
        this.tasksCompletedCounter = Counter.builder("runner.tasks.completed")
                .description("Total number of tasks completed successfully")
                .tag("type", "success")
                .register(meterRegistry);
        
        this.tasksFailedCounter = Counter.builder("runner.tasks.failed")
                .description("Total number of tasks failed")
                .tag("type", "failed")
                .register(meterRegistry);
        
        this.tasksRetriedCounter = Counter.builder("runner.tasks.retried")
                .description("Total number of tasks retried")
                .tag("type", "retry")
                .register(meterRegistry);
        
        // 初始化定时器
        this.taskExecutionTimer = Timer.builder("runner.task.execution.duration")
                .description("Time taken to execute a task")
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);
        
        this.workspaceCreationTimer = Timer.builder("runner.workspace.creation.duration")
                .description("Time taken to create workspace")
                .publishPercentiles(0.5, 0.9, 0.95)
                .register(meterRegistry);
        
        this.packageDownloadTimer = Timer.builder("runner.package.download.duration")
                .description("Time taken to download package from MinIO")
                .publishPercentiles(0.5, 0.9, 0.95)
                .register(meterRegistry);
        
        this.artifactUploadTimer = Timer.builder("runner.artifact.upload.duration")
                .description("Time taken to upload artifacts to MinIO")
                .publishPercentiles(0.5, 0.9, 0.95)
                .register(meterRegistry);
        
        // 注册 Gauge
        Gauge.builder("runner.tasks.active", activeTasks, AtomicLong::get)
                .description("Number of currently executing tasks")
                .register(meterRegistry);
        
        Gauge.builder("runner.tasks.total", totalTasksExecuted, AtomicLong::get)
                .description("Total number of tasks executed")
                .register(meterRegistry);
        
        Gauge.builder("runner.tasks.failed.total", totalTasksFailed, AtomicLong::get)
                .description("Total number of tasks failed")
                .register(meterRegistry);
    }

    /**
     * 记录任务接收
     */
    public void recordTaskReceived() {
        tasksReceivedCounter.increment();
    }

    /**
     * 记录任务领取成功
     */
    public void recordTaskClaimed() {
        tasksClaimedCounter.increment();
    }

    /**
     * 记录任务开始执行
     */
    public void recordTaskStarted() {
        activeTasks.incrementAndGet();
    }

    /**
     * 记录任务执行完成
     */
    public void recordTaskCompleted(Duration executionTime) {
        activeTasks.decrementAndGet();
        totalTasksExecuted.incrementAndGet();
        tasksCompletedCounter.increment();
        taskExecutionTimer.record(executionTime);
    }

    /**
     * 记录任务执行失败
     */
    public void recordTaskFailed(String errorType, Duration executionTime) {
        activeTasks.decrementAndGet();
        totalTasksFailed.incrementAndGet();
        tasksFailedCounter.increment();
        taskExecutionTimer.record(executionTime);
        
        // 记录错误类型
        getErrorCounter(errorType).increment();
    }

    /**
     * 记录任务重试
     */
    public void recordTaskRetried() {
        tasksRetriedCounter.increment();
    }

    /**
     * 记录工作空间创建时间
     */
    public void recordWorkspaceCreation(Duration duration) {
        workspaceCreationTimer.record(duration);
    }

    /**
     * 记录包下载时间
     */
    public void recordPackageDownload(Duration duration) {
        packageDownloadTimer.record(duration);
    }

    /**
     * 记录产物上传时间
     */
    public void recordArtifactUpload(Duration duration) {
        artifactUploadTimer.record(duration);
    }

    /**
     * 记录 Docker 容器执行时间
     */
    public void recordContainerExecution(Duration duration, String status) {
        Timer.builder("runner.container.execution.duration")
                .description("Time taken to execute container")
                .tag("status", status)
                .publishPercentiles(0.5, 0.9, 0.95, 0.99)
                .register(meterRegistry)
                .record(duration);
    }

    private Counter getErrorCounter(String errorType) {
        return errorCounters.computeIfAbsent(errorType, type ->
                Counter.builder("runner.tasks.errors")
                        .description("Number of task errors by type")
                        .tag("error_type", type)
                        .register(meterRegistry)
        );
    }

    /**
     * 获取当前活跃任务数
     */
    public long getActiveTasksCount() {
        return activeTasks.get();
    }
}
