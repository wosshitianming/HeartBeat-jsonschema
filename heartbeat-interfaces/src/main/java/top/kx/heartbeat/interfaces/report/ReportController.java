// 注释：声明当前文件所属的包路径。
package top.kx.heartbeat.interfaces.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.common.response.RecordResponse;
import top.kx.heartbeat.application.report.ReportService;
import top.kx.heartbeat.application.report.request.ReportDatasetRequest;
import top.kx.heartbeat.application.report.request.ReportQueryRequest;
import top.kx.heartbeat.application.report.request.ReportTemplateRequest;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 注释：当前类用于承载对应业务逻辑。
 */
// 注释：声明当前元素使用的注解配置。
@RestController
// 注释：声明当前元素使用的注解配置。
@RequestMapping("/api/v1/report")
public class ReportController {

    // 注释：声明当前元素使用的注解配置。
    @Resource
    // 注释：声明当前成员或方法。
    private ReportService reportService;

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/datasets")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:report:list')")
    public Result<List<DynamicRecordResponse>> datasets() {
        // 注释：返回当前处理结果。
        return listResponse(reportService.listDatasets());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/datasets")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:report:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "报表", action = "保存数据集")
    public Result<DynamicRecordResponse> saveDataset(@RequestBody ReportDatasetRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(reportService.saveDataset(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @GetMapping("/templates")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:report:list')")
    public Result<List<DynamicRecordResponse>> templates() {
        // 注释：返回当前处理结果。
        return listResponse(reportService.listTemplates());
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/templates")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:report:edit')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "报表", action = "保存模板")
    public Result<DynamicRecordResponse> saveTemplate(@RequestBody ReportTemplateRequest request) {
        // 注释：返回当前处理结果。
        return recordResponse(reportService.saveTemplate(request));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/datasets/{id}/query")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:report:query')")
    public Result<List<DynamicRecordResponse>> query(@PathVariable String id,
                                                     // 注释：声明当前元素使用的注解配置。
                                                     @RequestBody(required = false) ReportQueryRequest request,
                                                     // 注释：声明当前元素使用的注解配置。
                                                     @RequestParam(required = false) Integer limit) {
        // 注释：返回当前处理结果。
        return listResponse(reportService.query(id, params(request), limit));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @PostMapping("/datasets/{id}/export.csv")
    // 注释：声明当前元素使用的注解配置。
    @PreAuthorize("@permissionGuard.has('biz:report:export')")
    // 注释：声明当前元素使用的注解配置。
    @OperLog(module = "报表", action = "导出 CSV")
    public ResponseEntity<byte[]> exportCsv(@PathVariable String id,
                                            // 注释：声明当前元素使用的注解配置。
                                            @RequestBody(required = false) ReportQueryRequest request,
                                            // 注释：声明当前元素使用的注解配置。
                                            @RequestParam(required = false) Integer limit) {
        // 注释：设置或计算当前变量值。
        byte[] body = reportService.exportCsv(id, params(request), limit);
        // 注释：设置或计算当前变量值。
        String fileName = "attachment; filename=report-" + id + ".csv";
        // 注释：返回当前处理结果。
        return ResponseEntity.ok()
                // 注释：继续当前链式调用。
                .header(HttpHeaders.CONTENT_DISPOSITION, fileName)
                // 注释：继续当前链式调用。
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                // 注释：继续当前链式调用。
                .body(body);
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    // 注释：声明当前元素使用的注解配置。
    @SuppressWarnings("unchecked")
    private Map<String, Object> params(ReportQueryRequest request) {
        // 注释：设置或计算当前变量值。
        Object params = request == null ? null : request.getParams();
        // 注释：返回当前处理结果。
        return params instanceof Map ? (Map<String, Object>) params : Collections.emptyMap();
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        // 注释：返回当前处理结果。
        return Result.success(DynamicRecordResponse.fromRecordList(records));
        // 注释：结束当前代码块。
    }

    /**
     * 注释：当前方法用于执行对应业务处理。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        // 注释：返回当前处理结果。
        return Result.success(DynamicRecordResponse.from(record));
        // 注释：结束当前代码块。
    }
// 注释：结束当前代码块。
}
