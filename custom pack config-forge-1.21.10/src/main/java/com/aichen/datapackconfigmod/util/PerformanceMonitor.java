package com.aichen.datapackconfigmod.util;

import com.aichen.datapackconfigmod.DataPackConfigMod;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * 性能监控器
 * 参考东方灵梦模组的性能监控机制，帮助识别性能瓶颈
 */
public class PerformanceMonitor {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    // 性能统计
    private static final Map<String, PerformanceStats> statsMap = new HashMap<>();
    
    // 是否启用性能监控
    private static boolean enabled = false;
    
    /**
     * 开始性能监控
     * @param operationName 操作名称
     */
    public static void start(String operationName) {
        if (!enabled) return;
        
        PerformanceStats stats = statsMap.computeIfAbsent(operationName, k -> new PerformanceStats());
        stats.startTime = System.nanoTime();
        stats.running = true;
    }
    
    /**
     * 结束性能监控
     * @param operationName 操作名称
     */
    public static void end(String operationName) {
        if (!enabled) return;
        
        PerformanceStats stats = statsMap.get(operationName);
        if (stats != null && stats.running) {
            long duration = System.nanoTime() - stats.startTime;
            stats.totalTime += duration;
            stats.callCount++;
            stats.running = false;
            
            // 记录性能警告
            if (duration > 100000000) { // 超过100ms
                LOGGER.warn("[DatapackConfigMod] 性能警告: {} 操作耗时 {}ms", operationName, duration / 1000000);
            }
        }
    }
    
    /**
     * 启用性能监控
     */
    public static void enable() {
        enabled = true;
        LOGGER.info("[DatapackConfigMod] 性能监控已启用");
    }
    
    /**
     * 禁用性能监控
     */
    public static void disable() {
        enabled = false;
        LOGGER.info("[DatapackConfigMod] 性能监控已禁用");
    }
    
    /**
     * 打印性能统计报告
     */
    public static void printStats() {
        if (!enabled) return;
        
        LOGGER.info("[DatapackConfigMod] ==== 性能统计报告 ====");
        for (Map.Entry<String, PerformanceStats> entry : statsMap.entrySet()) {
            PerformanceStats stats = entry.getValue();
            if (stats.callCount > 0) {
                long avgTime = stats.totalTime / stats.callCount;
                LOGGER.info("[DatapackConfigMod] {}: 调用次数={}, 总耗时={}ms, 平均耗时={}ms", 
                    entry.getKey(), stats.callCount, stats.totalTime / 1000000, avgTime / 1000000);
            }
        }
        LOGGER.info("[DatapackConfigMod] ======================");
    }
    
    /**
     * 清理性能统计
     */
    public static void clearStats() {
        statsMap.clear();
        LOGGER.info("[DatapackConfigMod] 性能统计已清除");
    }
    
    /**
     * 性能统计数据结构
     */
    private static class PerformanceStats {
        long startTime = 0;
        long totalTime = 0;
        int callCount = 0;
        boolean running = false;
    }
}