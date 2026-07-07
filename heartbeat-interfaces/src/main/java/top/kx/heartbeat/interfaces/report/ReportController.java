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

@RestController
@RequestMapping("/api/v1/report")
public class ReportController {

    @Resource
    private ReportService reportService;

    @GetMapping("/datasets")
    @PreAuthorize("@permissionGuard.has('biz:report:list')")
    public Result<List<DynamicRecordResponse>> datasets() {
        return listResponse(reportService.listDatasets());
    }

    @PostMapping("/datasets")
    @PreAuthorize("@permissionGuard.has('biz:report:edit')")
    @OperLog(module = "报表", action = "保存数据集")
    public Result<DynamicRecordResponse> saveDataset(@RequestBody ReportDatasetRequest request) {
        return recordResponse(reportService.saveDataset(request));
    }

    @GetMapping("/templates")
    @PreAuthorize("@permissionGuard.has('biz:report:list')")
    public Result<List<DynamicRecordResponse>> templates() {
        return listResponse(reportService.listTemplates());
    }

    @PostMapping("/templates")
    @PreAuthorize("@permissionGuard.has('biz:report:edit')")
    @OperLog(module = "报表", action = "保存模板")
    public Result<DynamicRecordResponse> saveTemplate(@RequestBody ReportTemplateRequest request) {
        return recordResponse(reportService.saveTemplate(request));
    }

    @PostMapping("/datasets/{id}/query")
    @PreAuthorize("@permissionGuard.has('biz:report:query')")
    public Result<List<DynamicRecordResponse>> query(@PathVariable String id,
                                                     @RequestBody(required = false) ReportQueryRequest request,
                                                     @RequestParam(required = false) Integer limit) {
        return listResponse(reportService.query(id, params(request), limit));
    }

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> params(ReportQueryRequest request) {
        Object params = request == null ? null : request.getParams();
        return params instanceof Map ? (Map<String, Object>) params : Collections.emptyMap();
    }

    private Result<List<DynamicRecordResponse>> listResponse(List<RecordResponse> records) {
        return Result.success(DynamicRecordResponse.fromRecordList(records));
    }

    private Result<DynamicRecordResponse> recordResponse(RecordResponse record) {
        return Result.success(DynamicRecordResponse.from(record));
    }
}
