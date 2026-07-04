package top.kx.heartbeat.interfaces.tool;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.tool.CodegenService;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.common.response.DynamicRecordResponse;
import top.kx.heartbeat.interfaces.tool.response.CodePreviewResponse;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * Flex 代码生成工具接口控制器。
 *
 * <p>负责表结构导入、生成配置查询、代码预览和代码下载的后台入口。</p>
 */
@RestController
@RequestMapping("/api/v1/tool/gen")
public class ToolGenController {

    /**
     * 代码生成应用服务。
     */
    @Resource
    private CodegenService codegenService;

    /**
     * 查询数据库可导入表列表。
     *
     * @return 数据库表列表响应
     */
    @GetMapping("/tables")
    @PreAuthorize("@permissionGuard.has('tool:gen:list')")
    public Result<List<DynamicRecordResponse>> listDatabaseTables() {
        // 查询数据库表元数据动态记录列表。
        List<Map<String, Object>> tables = codegenService.listDatabaseTables();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(tables);
        // 返回数据库表列表。
        return Result.success(response);
    }

    /**
     * 查询已导入生成配置列表。
     *
     * @return 已导入生成配置列表响应
     */
    @GetMapping("/imported")
    @PreAuthorize("@permissionGuard.has('tool:gen:list')")
    public Result<List<DynamicRecordResponse>> listImportedTables() {
        // 查询已导入表配置动态记录列表。
        List<Map<String, Object>> tables = codegenService.listImportedTables();
        // 将动态记录列表转换为统一响应对象列表。
        List<DynamicRecordResponse> response = DynamicRecordResponse.fromList(tables);
        // 返回已导入生成配置列表。
        return Result.success(response);
    }

    /**
     * 导入数据库表结构。
     *
     * @param tableName 数据库表名称
     * @return 表结构导入结果
     */
    @PostMapping("/tables/import")
    @PreAuthorize("@permissionGuard.has('tool:gen:import')")
    public Result<DynamicRecordResponse> importTable(@RequestParam String tableName) {
        // 导入表结构动态记录。
        Map<String, Object> imported = codegenService.importTable(tableName);
        // 将动态记录转换为统一响应对象。
        DynamicRecordResponse response = DynamicRecordResponse.from(imported);
        // 返回表结构导入结果。
        return Result.success(response);
    }

    /**
     * 预览生成代码。
     *
     * @param id 生成配置标识
     * @return 生成代码预览响应
     */
    @GetMapping("/tables/{id}/preview")
    @PreAuthorize("@permissionGuard.has('tool:gen:list')")
    public Result<CodePreviewResponse> preview(@PathVariable String id) {
        // 生成代码预览文件映射。
        Map<String, String> preview = codegenService.preview(id);
        // 将文件映射转换为规范响应对象。
        CodePreviewResponse response = CodePreviewResponse.from(preview);
        // 返回生成代码预览结果。
        return Result.success(response);
    }

    /**
     * 下载生成代码 ZIP。
     *
     * @param id 生成配置标识
     * @return 生成代码 ZIP 二进制响应
     */
    @GetMapping("/tables/{id}/download")
    @PreAuthorize("@permissionGuard.has('tool:gen:download')")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        // 生成代码 ZIP 字节数组。
        byte[] zipBytes = codegenService.download(id);
        // 组装下载文件名称。
        String fileName = "attachment; filename=heartbeat-codegen-" + id + ".zip";
        // 创建成功响应构建器。
        ResponseEntity.BodyBuilder responseBuilder = ResponseEntity.ok();
        // 写入下载响应头。
        responseBuilder.header(HttpHeaders.CONTENT_DISPOSITION, fileName);
        // 写入二进制响应类型。
        responseBuilder.contentType(MediaType.APPLICATION_OCTET_STREAM);
        // 返回 ZIP 文件内容。
        return responseBuilder.body(zipBytes);
    }
}
