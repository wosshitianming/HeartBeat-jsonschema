package top.kx.heartbeat.application.monitor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import top.kx.heartbeat.application.common.response.RecordResponse;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.lang.management.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务监控：聚合 JVM、操作系统与磁盘指标（参考 RuoYi monitor/server）。
 */
@Service
public class MonitorService {

    @Resource
    private ObjectProvider<CacheManager> cacheManagerProvider;
    @Resource
    private ObjectProvider<DataSource> dataSourceProvider;

    public RecordResponse serverInfo() {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("timestamp", Instant.now().toString());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("cpu", cpuInfo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("memory", memoryInfo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("jvm", jvmInfo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("disk", diskInfo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("runtime", runtimeInfo());
        // 返回已经完成封装的业务结果。
        return RecordResponse.from(result);
    }

    public RecordResponse cacheInfo() {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> result = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("enabled", cacheManager != null);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("provider", cacheManager == null ? "NONE" : cacheManager.getClass().getName());
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> caches = new ArrayList<>();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (cacheManager != null) {
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (String name : cacheManager.getCacheNames()) {
                // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
                Map<String, Object> cache = new LinkedHashMap<>();
                // 写入对外字段，保持调用方依赖的响应结构稳定。
                cache.put("name", name);
                // 计算当前分支的中间结果，供后续判断或组装使用。
                Object nativeCache = cacheManager.getCache(name) == null
                        // 条件成立时使用前一个分支计算出的业务值。
                        ? null
                        // 条件不成立时使用兜底业务值。
                        : cacheManager.getCache(name).getNativeCache();
                // 写入对外字段，保持调用方依赖的响应结构稳定。
                cache.put("implementation", nativeCache == null
                        // 条件成立时使用前一个分支计算出的业务值。
                        ? ""
                        // 条件不成立时使用兜底业务值。
                        : nativeCache.getClass().getName());
                // 加入当前处理结果，供后续批量返回或继续组装。
                caches.add(cache);
            }
        }
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("caches", caches);
        // 返回已经完成封装的业务结果。
        return RecordResponse.from(result);
    }

    public RecordResponse dataSourceInfo() {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> result = new LinkedHashMap<>();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (dataSource == null) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("implementation", "NONE");
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            result.put("enabled", false);
            // 返回已经完成封装的业务结果。
            return RecordResponse.from(result);
        }
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("enabled", true);
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put("implementation", dataSource.getClass().getName());
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        putMetric(result, "maximumPoolSize", dataSource, "getMaximumPoolSize");
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        putMetric(result, "minimumIdle", dataSource, "getMinimumIdle");
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Object poolBean = invoke(dataSource, "getHikariPoolMXBean");
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (poolBean != null) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            putMetric(result, "activeConnections", poolBean, "getActiveConnections");
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            putMetric(result, "idleConnections", poolBean, "getIdleConnections");
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            putMetric(result, "totalConnections", poolBean, "getTotalConnections");
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            putMetric(result, "threadsAwaitingConnection", poolBean, "getThreadsAwaitingConnection");
        }
        // 返回已经完成封装的业务结果。
        return RecordResponse.from(result);
    }

    private Map<String, Object> cpuInfo() {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> cpu = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        cpu.put("cores", os.getAvailableProcessors());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        cpu.put("loadAverage", round(os.getSystemLoadAverage()));
        // 根据当前业务条件选择对应处理路径。
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            com.sun.management.OperatingSystemMXBean sunOs =
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    (com.sun.management.OperatingSystemMXBean) os;
            // 计算当前分支的中间结果，供后续判断或组装使用。
            double systemCpu = sunOs.getSystemCpuLoad();
            // 计算当前分支的中间结果，供后续判断或组装使用。
            double processCpu = sunOs.getProcessCpuLoad();
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            cpu.put("systemUsage", percent(systemCpu));
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            cpu.put("processUsage", percent(processCpu));
        }
        // 返回已经完成封装的业务结果。
        return cpu;
    }

    private Map<String, Object> memoryInfo() {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> memory = new LinkedHashMap<>();
        // 根据当前业务条件选择对应处理路径。
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            com.sun.management.OperatingSystemMXBean sunOs =
                    // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                    (com.sun.management.OperatingSystemMXBean) os;
            // 计算当前分支的中间结果，供后续判断或组装使用。
            long total = sunOs.getTotalPhysicalMemorySize();
            // 计算当前分支的中间结果，供后续判断或组装使用。
            long free = sunOs.getFreePhysicalMemorySize();
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            memory.put("total", total);
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            memory.put("free", free);
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            memory.put("used", total - free);
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            memory.put("usage", percent(total > 0 ? (double) (total - free) / total : 0));
        }
        // 返回已经完成封装的业务结果。
        return memory;
    }

    private Map<String, Object> jvmInfo() {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Runtime runtime = Runtime.getRuntime();

        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> jvm = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("name", System.getProperty("java.vm.name"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("version", System.getProperty("java.version"));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("heapUsed", heap.getUsed());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("heapMax", heap.getMax());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("heapCommitted", heap.getCommitted());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("heapUsage", percent(heap.getMax() > 0 ? (double) heap.getUsed() / heap.getMax() : 0));
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("nonHeapUsed", nonHeap.getUsed());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("nonHeapMax", nonHeap.getMax());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("runtimeTotal", runtime.totalMemory());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("runtimeFree", runtime.freeMemory());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("runtimeMax", runtime.maxMemory());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("gc", gcInfo());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        jvm.put("threads", threadInfo());
        // 返回已经完成封装的业务结果。
        return jvm;
    }

    private List<Map<String, Object>> gcInfo() {
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> collectors = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
            Map<String, Object> item = new LinkedHashMap<>();
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("name", gc.getName());
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("count", gc.getCollectionCount());
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            item.put("timeMs", gc.getCollectionTime());
            // 加入当前处理结果，供后续批量返回或继续组装。
            collectors.add(item);
        }
        // 返回已经完成封装的业务结果。
        return collectors;
    }

    private Map<String, Object> threadInfo() {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> info = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        info.put("live", threads.getThreadCount());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        info.put("daemon", threads.getDaemonThreadCount());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        info.put("peak", threads.getPeakThreadCount());
        // 返回已经完成封装的业务结果。
        return info;
    }

    private List<Map<String, Object>> diskInfo() {
        // 创建结果集合，承接后续逐项组装的数据。
        List<Map<String, Object>> disks = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (File root : File.listRoots()) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            long total = root.getTotalSpace();
            // 计算当前分支的中间结果，供后续判断或组装使用。
            long free = root.getFreeSpace();
            // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
            Map<String, Object> disk = new LinkedHashMap<>();
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            disk.put("path", root.getAbsolutePath());
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            disk.put("total", total);
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            disk.put("free", free);
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            disk.put("used", total - free);
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            disk.put("usage", percent(total > 0 ? (double) (total - free) / total : 0));
            // 加入当前处理结果，供后续批量返回或继续组装。
            disks.add(disk);
        }
        // 返回已经完成封装的业务结果。
        return disks;
    }

    private Map<String, Object> runtimeInfo() {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, Object> info = new LinkedHashMap<>();
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        info.put("name", runtime.getVmName());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        info.put("startTime", Instant.ofEpochMilli(runtime.getStartTime()).toString());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        info.put("uptimeMs", runtime.getUptime());
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        info.put("inputArguments", runtime.getInputArguments());
        // 返回已经完成封装的业务结果。
        return info;
    }

    private double percent(double ratio) {
        if (ratio < 0) {
            return 0;
        }
        return round(ratio * 100);
    }

    private double round(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0;
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private void putMetric(Map<String, Object> target, String key, Object source, String method) {
        Object value = invoke(source, method);
        if (value != null) {
            target.put(key, value);
        }
    }

    private Object invoke(Object source, String methodName) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return source.getClass().getMethod(methodName).invoke(source);
        } catch (ReflectiveOperationException ignored) {
            // 返回已经完成封装的业务结果。
            return null;
        }
    }
}
