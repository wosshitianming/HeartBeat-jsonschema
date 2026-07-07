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
 * 提供报表管理 HTTP 接口，负责接收请求并委托应用服务完成用例编排。
 */
@RestController
@RequestMapping("/api/v1/report")
public class ReportController {

    @Resource
    private ReportService reportService;

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托报表管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/datasets")
    @PreAuthorize("@permissionGuard.has('biz:report:list')")
    public Result<List<DynamicRecordResponse>> datasets() {
        return listResponse(reportService.listDatasets());
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托报表管理应用服务完成处理。
     *
     * @param request 报表管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/datasets")
    @PreAuthorize("@permissionGuard.has('biz:report:edit')")
    @OperLog(module = "报表", action = "保存数据集")
    public Result<DynamicRecordResponse> saveDataset(@RequestBody ReportDatasetRequest request) {
        return recordResponse(reportService.saveDataset(request));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托报表管理应用服务完成处理。
     *
     * @return 处理后的业务结果。
     */
    @GetMapping("/templates")
    @PreAuthorize("@permissionGuard.has('biz:report:list')")
    public Result<List<DynamicRecordResponse>> templates() {
        return listResponse(reportService.listTemplates());
    }

    /**
     * 保存业务数据，按当前记录状态选择新增或更新路径，并统一委托报表管理应用服务完成处理。
     *
     * @param request 报表管理请求参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/templates")
    @PreAuthorize("@permissionGuard.has('biz:report:edit')")
    @OperLog(module = "报表", action = "保存模板")
    public Result<DynamicRecordResponse> saveTemplate(@RequestBody ReportTemplateRequest request) {
        return recordResponse(reportService.saveTemplate(request));
    }

    /**
     * 查询业务数据详情，供上层用例继续编排或返回给调用方，并统一委托报表管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 报表管理请求参数。
     * @param limit 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/datasets/{id}/query")
    @PreAuthorize("@permissionGuard.has('biz:report:query')")
    public Result<List<DynamicRecordResponse>> query(@PathVariable String id,
                                                     @RequestBody(required = false) ReportQueryRequest request,
                                                     @RequestParam(required = false) Integer limit) {
        return listResponse(reportService.query(id, params(request), limit));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托报表管理应用服务完成处理。
     *
     * @param id 业务记录标识。
     * @param request 报表管理请求参数。
     * @param limit 业务处理所需参数。
     * @return 处理后的业务结果。
     */
    @PostMapping("/datasets/{id}/export.csv")
    @PreAuthorize("@permissionGuard.has('biz:report:export')")
    @OperLog(module = "报表", action = "导出 CSV")
    public ResponseEntity<byte[]> exportCsv(@PathVariable String id,
                                            @RequestBody(required = false) ReportQueryRequest request,
                                            @RequestParam(required = false) Integer limit) {
        byte[] body = reportService.exportCsv(id, params(request), limit);
        String fileName = "attachment; filename=report-" + id + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, fileName)
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(body);
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托报表管理应用服务完成处理。
     *
     * @param request 报表管理请求参数。
     * @return 处理后的业务结果。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> params(ReportQueryRequest request) {
        Object params = request == null ? null : request.getParams();
        return params instanceof Map ? (Map<String, Object>) params : Collections.emptyMap();
    }

    /**
     * 查询列表数据，保持返回结构稳定并便于前端直接消费，并统一委托报表管理应用服务完成处理。
     *
     * @param records 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    /**
     * 处理当前业务用例，保持调用方不感知内部实现细节，并统一委托报表管理应用服务完成处理。
     *
     * @param record 应用层业务记录。
     * @return 处理后的业务结果。
     */
    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
