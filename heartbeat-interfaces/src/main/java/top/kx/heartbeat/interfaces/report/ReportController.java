package top.kx.heartbeat.interfaces.report;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.report.ReportService;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.request.DynamicRecordRequest;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 报表管理接口控制器。
 *
 * <p>负责报表数据集、模板、查询和导出的后台管理入口。</p>
 */
@RestController
@RequestMapping("/api/v1/report")
public class ReportController {

    /**
     * 报表应用服务。
     */
    @Resource
    private ReportService reportService;

    /**
     * 查询报表数据集列表。
     *
     * @return 报表数据集列表响应
     */
    @GetMapping("/datasets")
    @PreAuthorize("@permissionGuard.has('biz:report:list')")
    public Result<List<DynamicRecordResponse>> datasets() {
        // 查询报表数据集动态记录列表。
        List<Map<String, Object>> datasets = reportService.listDatasets();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(datasets);
        // 返回报表数据集列表。
        return Result.success(response);
    }

    /**
     * 保存报表数据集。
     *
     * @param request 报表数据集保存参数
     * @return 报表数据集保存结果
     */
    @PostMapping("/datasets")
    @PreAuthorize("@permissionGuard.has('biz:report:edit')")
    @OperLog(module = "报表", action = "保存数据集")
    public Result<DynamicRecordResponse> saveDataset(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 保存报表数据集动态记录。
        Map<String, Object> dataset = reportService.saveDataset(payload);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(dataset);
        // 返回报表数据集保存结果。
        return Result.success(response);
    }

    /**
     * 查询报表模板列表。
     *
     * @return 报表模板列表响应
     */
    @GetMapping("/templates")
    @PreAuthorize("@permissionGuard.has('biz:report:list')")
    public Result<List<DynamicRecordResponse>> templates() {
        // 查询报表模板动态记录列表。
        List<Map<String, Object>> templates = reportService.listTemplates();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(templates);
        // 返回报表模板列表。
        return Result.success(response);
    }

    /**
     * 保存报表模板。
     *
     * @param request 报表模板保存参数
     * @return 报表模板保存结果
     */
    @PostMapping("/templates")
    @PreAuthorize("@permissionGuard.has('biz:report:edit')")
    @OperLog(module = "报表", action = "保存模板")
    public Result<DynamicRecordResponse> saveTemplate(@RequestBody DynamicRecordRequest request) {
        // 转换动态请求对象为业务字段映射。
        Map<String, Object> payload = request.toMap();
        // 保存报表模板动态记录。
        Map<String, Object> template = reportService.saveTemplate(payload);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(template);
        // 返回报表模板保存结果。
        return Result.success(response);
    }

    /**
     * 查询报表数据集结果。
     *
     * @param id 数据集标识
     * @param params 查询参数
     * @param limit 查询条数限制
     * @return 报表查询结果列表响应
     */
    @PostMapping("/datasets/{id}/query")
    @PreAuthorize("@permissionGuard.has('biz:report:query')")
    public Result<List<DynamicRecordResponse>> query(@PathVariable String id,
                                                     @RequestBody(required = false) DynamicRecordRequest params,
                                                     @RequestParam(required = false) Integer limit) {
        // 兜底空查询参数。
        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params.toMap();
        // 查询报表数据集结果动态记录列表。
        List<Map<String, Object>> rows = reportService.query(id, safeParams, limit);
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(rows);
        // 返回报表查询结果。
        return Result.success(response);
    }

    /**
     * 导出报表数据集 CSV。
     *
     * @param id 数据集标识
     * @param params 查询参数
     * @param limit 查询条数限制
     * @return CSV 文件二进制响应
     */
    @PostMapping("/datasets/{id}/export.csv")
    @PreAuthorize("@permissionGuard.has('biz:report:export')")
    @OperLog(module = "报表", action = "导出 CSV")
    public ResponseEntity<byte[]> exportCsv(@PathVariable String id,
                                            @RequestBody(required = false) DynamicRecordRequest params,
                                            @RequestParam(required = false) Integer limit) {
        // 兜底空查询参数。
        Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params.toMap();
        // 导出 CSV 文件字节数组。
        byte[] body = reportService.exportCsv(id, safeParams, limit);
        // 组装下载文件名称。
        String fileName = "attachment; filename=report-" + id + ".csv";
        // 创建成功响应构建器。
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        // 写入下载响应头。
        responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, fileName);
        // 写入 CSV 响应类型。
        responseBuilder.contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"));
        // 返回 CSV 文件内容。
        return responseBuilder.body(body);
    }
}
