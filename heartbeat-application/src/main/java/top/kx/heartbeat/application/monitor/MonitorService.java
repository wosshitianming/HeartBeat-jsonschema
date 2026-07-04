package top.kx.heartbeat.application.monitor;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

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

    public Map<String, Object> serverInfo() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("timestamp", Instant.now().toString());
        result.put("cpu", cpuInfo());
        result.put("memory", memoryInfo());
        result.put("jvm", jvmInfo());
        result.put("disk", diskInfo());
        result.put("runtime", runtimeInfo());
        return result;
    }

    public Map<String, Object> cacheInfo() {
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enabled", cacheManager != null);
        result.put("provider", cacheManager == null ? "NONE" : cacheManager.getClass().getName());
        List<Map<String, Object>> caches = new ArrayList<>();
        if (cacheManager != null) {
            for (String name : cacheManager.getCacheNames()) {
                Map<String, Object> cache = new LinkedHashMap<>();
                cache.put("name", name);
                Object nativeCache = cacheManager.getCache(name) == null
                        ? null
                        : cacheManager.getCache(name).getNativeCache();
                cache.put("implementation", nativeCache == null
                        ? ""
                        : nativeCache.getClass().getName());
                caches.add(cache);
            }
        }
        result.put("caches", caches);
        return result;
    }

    public Map<String, Object> dataSourceInfo() {
        DataSource dataSource = dataSourceProvider.getIfAvailable();
        Map<String, Object> result = new LinkedHashMap<>();
        if (dataSource == null) {
            result.put("implementation", "NONE");
            result.put("enabled", false);
            return result;
        }
        result.put("enabled", true);
        result.put("implementation", dataSource.getClass().getName());
        putMetric(result, "maximumPoolSize", dataSource, "getMaximumPoolSize");
        putMetric(result, "minimumIdle", dataSource, "getMinimumIdle");
        Object poolBean = invoke(dataSource, "getHikariPoolMXBean");
        if (poolBean != null) {
            putMetric(result, "activeConnections", poolBean, "getActiveConnections");
            putMetric(result, "idleConnections", poolBean, "getIdleConnections");
            putMetric(result, "totalConnections", poolBean, "getTotalConnections");
            putMetric(result, "threadsAwaitingConnection", poolBean, "getThreadsAwaitingConnection");
        }
        return result;
    }

    private Map<String, Object> cpuInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> cpu = new LinkedHashMap<>();
        cpu.put("cores", os.getAvailableProcessors());
        cpu.put("loadAverage", round(os.getSystemLoadAverage()));
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOs =
                    (com.sun.management.OperatingSystemMXBean) os;
            double systemCpu = sunOs.getSystemCpuLoad();
            double processCpu = sunOs.getProcessCpuLoad();
            cpu.put("systemUsage", percent(systemCpu));
            cpu.put("processUsage", percent(processCpu));
        }
        return cpu;
    }

    private Map<String, Object> memoryInfo() {
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        Map<String, Object> memory = new LinkedHashMap<>();
        if (os instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunOs =
                    (com.sun.management.OperatingSystemMXBean) os;
            long total = sunOs.getTotalPhysicalMemorySize();
            long free = sunOs.getFreePhysicalMemorySize();
            memory.put("total", total);
            memory.put("free", free);
            memory.put("used", total - free);
            memory.put("usage", percent(total > 0 ? (double) (total - free) / total : 0));
        }
        return memory;
    }

    private Map<String, Object> jvmInfo() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        Runtime runtime = Runtime.getRuntime();

        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("name", System.getProperty("java.vm.name"));
        jvm.put("version", System.getProperty("java.version"));
        jvm.put("heapUsed", heap.getUsed());
        jvm.put("heapMax", heap.getMax());
        jvm.put("heapCommitted", heap.getCommitted());
        jvm.put("heapUsage", percent(heap.getMax() > 0 ? (double) heap.getUsed() / heap.getMax() : 0));
        jvm.put("nonHeapUsed", nonHeap.getUsed());
        jvm.put("nonHeapMax", nonHeap.getMax());
        jvm.put("runtimeTotal", runtime.totalMemory());
        jvm.put("runtimeFree", runtime.freeMemory());
        jvm.put("runtimeMax", runtime.maxMemory());
        jvm.put("gc", gcInfo());
        jvm.put("threads", threadInfo());
        return jvm;
    }

    private List<Map<String, Object>> gcInfo() {
        List<Map<String, Object>> collectors = new ArrayList<>();
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", gc.getName());
            item.put("count", gc.getCollectionCount());
            item.put("timeMs", gc.getCollectionTime());
            collectors.add(item);
        }
        return collectors;
    }

    private Map<String, Object> threadInfo() {
        ThreadMXBean threads = ManagementFactory.getThreadMXBean();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("live", threads.getThreadCount());
        info.put("daemon", threads.getDaemonThreadCount());
        info.put("peak", threads.getPeakThreadCount());
        return info;
    }

    private List<Map<String, Object>> diskInfo() {
        List<Map<String, Object>> disks = new ArrayList<>();
        for (File root : File.listRoots()) {
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            Map<String, Object> disk = new LinkedHashMap<>();
            disk.put("path", root.getAbsolutePath());
            disk.put("total", total);
            disk.put("free", free);
            disk.put("used", total - free);
            disk.put("usage", percent(total > 0 ? (double) (total - free) / total : 0));
            disks.add(disk);
        }
        return disks;
    }

    private Map<String, Object> runtimeInfo() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("name", runtime.getVmName());
        info.put("startTime", Instant.ofEpochMilli(runtime.getStartTime()).toString());
        info.put("uptimeMs", runtime.getUptime());
        info.put("inputArguments", runtime.getInputArguments());
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
        try {
            return source.getClass().getMethod(methodName).invoke(source);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
