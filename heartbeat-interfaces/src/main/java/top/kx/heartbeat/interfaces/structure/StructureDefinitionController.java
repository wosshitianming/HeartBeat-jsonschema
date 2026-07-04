package top.kx.heartbeat.interfaces.structure;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import top.kx.heartbeat.application.structure.StructureApplicationService;
import top.kx.heartbeat.application.structure.dto.StructureDefinitionDTO;
import top.kx.heartbeat.application.structure.dto.StructurePreviewDTO;
import top.kx.heartbeat.application.structure.dto.StructureVersionDiffDTO;
import top.kx.heartbeat.application.structure.dto.ValidationResultDTO;
import top.kx.heartbeat.domain.common.audit.OperLog;
import top.kx.heartbeat.interfaces.common.Result;
import top.kx.heartbeat.interfaces.structure.request.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * 结构定义接口控制器。
 *
 * <p>负责结构预览、定义创建、版本管理、草稿管理、差异比较、发布和数据校验入口。</p>
 */
@RestController
@RequestMapping("/api/v1/structure-definitions")
public class StructureDefinitionController {

    /**
     * 结构定义应用服务。
     */
    @Resource
    private StructureApplicationService structureApplicationService;

    /**
     * 预览结构推断结果。
     *
     * @param request 结构预览请求对象
     * @return 结构预览结果
     */
    @PostMapping("/preview")
    @PreAuthorize("@permissionGuard.has('structure:definition:list')")
    public Result<StructurePreviewDTO> preview(@Valid @RequestBody PreviewStructureRequest request) {
        // 调用应用服务预览结构。
        StructurePreviewDTO preview = structureApplicationService.preview(
                request.getSamples(),
                request.getValidationMode(),
                request.getUiOverrides()
        );
        // 返回结构预览结果。
        return Result.success(preview);
    }

    /**
     * 创建结构定义。
     *
     * @param request 结构定义创建请求对象
     * @return 结构定义创建结果
     */
    @PostMapping
    @PreAuthorize("@permissionGuard.has('structure:definition:edit')")
    public Result<StructureDefinitionDTO> create(
            @Valid @RequestBody CreateStructureDefinitionRequest request) {
        // 调用应用服务创建结构定义。
        StructureDefinitionDTO definition = structureApplicationService.create(
                request.getName(),
                request.getDescription(),
                request.getSamples(),
                request.getValidationMode(),
                request.getUiOverrides(),
                request.isActivate()
        );
        // 返回结构定义创建结果。
        return Result.success(definition);
    }

    /**
     * 查询结构定义列表。
     *
     * @return 结构定义列表
     */
    @GetMapping
    @PreAuthorize("@permissionGuard.has('structure:definition:list')")
    public Result<List<StructureDefinitionDTO>> list() {
        // 查询结构定义列表。
        List<StructureDefinitionDTO> definitions = structureApplicationService.list();
        // 返回结构定义列表。
        return Result.success(definitions);
    }

    /**
     * 查询结构定义详情。
     *
     * @param id 结构定义标识
     * @return 结构定义详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("@permissionGuard.has('structure:definition:list')")
    public Result<StructureDefinitionDTO> get(@PathVariable String id) {
        // 查询结构定义详情。
        StructureDefinitionDTO definition = structureApplicationService.get(id);
        // 返回结构定义详情。
        return Result.success(definition);
    }

    /**
     * 创建结构定义版本。
     *
     * @param id 结构定义标识
     * @param request 结构版本创建请求对象
     * @return 结构定义版本创建结果
     */
    @PostMapping("/{id}/versions")
    @PreAuthorize("@permissionGuard.has('structure:definition:edit')")
    public Result<StructureDefinitionDTO> createVersion(
            @PathVariable String id,
            @Valid @RequestBody CreateStructureVersionRequest request) {
        // 调用应用服务创建结构版本。
        StructureDefinitionDTO definition = structureApplicationService.createVersion(
                id,
                request.getSamples(),
                request.getValidationMode(),
                request.getUiOverrides()
        );
        // 返回结构版本创建结果。
        return Result.success(definition);
    }

    /**
     * 保存结构定义草稿。
     *
     * @param id 结构定义标识
     * @param request 结构草稿保存请求对象
     * @return 结构草稿保存结果
     */
    @PostMapping("/{id}/draft")
    @PreAuthorize("@permissionGuard.has('structure:definition:edit')")
    public Result<StructureDefinitionDTO> saveDraft(
            @PathVariable String id,
            @Valid @RequestBody SaveStructureDraftRequest request) {
        // 调用应用服务保存结构草稿。
        StructureDefinitionDTO definition = structureApplicationService.saveDraft(
                id,
                request.getSamples(),
                request.getValidationMode(),
                request.getUiOverrides()
        );
        // 返回结构草稿保存结果。
        return Result.success(definition);
    }

    /**
     * 复制指定版本到草稿。
     *
     * @param id 结构定义标识
     * @param versionNo 结构版本号
     * @return 草稿复制结果
     */
    @PostMapping("/{id}/draft/from-version/{versionNo}")
    @PreAuthorize("@permissionGuard.has('structure:definition:edit')")
    public Result<StructureDefinitionDTO> copyVersionToDraft(
            @PathVariable String id,
            @PathVariable Integer versionNo) {
        // 调用应用服务复制版本到草稿。
        StructureDefinitionDTO definition = structureApplicationService.copyVersionToDraft(id, versionNo);
        // 返回草稿复制结果。
        return Result.success(definition);
    }

    /**
     * 从草稿创建结构版本。
     *
     * @param id 结构定义标识
     * @return 结构版本创建结果
     */
    @PostMapping("/{id}/versions/from-draft")
    @PreAuthorize("@permissionGuard.has('structure:definition:edit')")
    public Result<StructureDefinitionDTO> createVersionFromDraft(@PathVariable String id) {
        // 调用应用服务从草稿创建版本。
        StructureDefinitionDTO definition = structureApplicationService.createVersionFromDraft(id);
        // 返回结构版本创建结果。
        return Result.success(definition);
    }

    /**
     * 比较结构版本差异。
     *
     * @param id 结构定义标识
     * @param fromVersionNo 起始版本号
     * @param toVersionNo 目标版本号
     * @param toDraft 是否比较到草稿
     * @return 结构版本差异结果
     */
    @GetMapping("/{id}/diff")
    @PreAuthorize("@permissionGuard.has('structure:definition:list')")
    public Result<StructureVersionDiffDTO> diff(
            @PathVariable String id,
            @RequestParam(value = "fromVersionNo", required = false) Integer fromVersionNo,
            @RequestParam(value = "toVersionNo", required = false) Integer toVersionNo,
            @RequestParam(value = "toDraft", defaultValue = "false") boolean toDraft) {
        // 调用应用服务比较结构差异。
        StructureVersionDiffDTO diff = structureApplicationService.diff(id, fromVersionNo, toVersionNo, toDraft);
        // 返回结构差异结果。
        return Result.success(diff);
    }

    /**
     * 发布并激活结构版本。
     *
     * @param id 结构定义标识
     * @param request 结构版本激活请求对象
     * @return 结构版本激活结果
     */
    @PutMapping("/{id}/active-version")
    @PreAuthorize("@permissionGuard.has('structure:definition:publish')")
    @OperLog(module = "结构配置", action = "发布结构版本")
    public Result<StructureDefinitionDTO> activate(
            @PathVariable String id,
            @Valid @RequestBody ActivateStructureVersionRequest request) {
        // 调用应用服务激活结构版本。
        StructureDefinitionDTO definition = structureApplicationService.activate(id, request.getVersionNo());
        // 返回结构版本激活结果。
        return Result.success(definition);
    }

    /**
     * 校验结构数据。
     *
     * @param id 结构定义标识
     * @param request 结构数据校验请求对象
     * @return 结构数据校验结果
     */
    @PostMapping("/{id}/validate")
    @PreAuthorize("@permissionGuard.has('structure:definition:list')")
    public Result<ValidationResultDTO> validate(
            @PathVariable String id,
            @Valid @RequestBody ValidateStructureRequest request) {
        // 调用应用服务校验结构数据。
        ValidationResultDTO result = structureApplicationService.validate(
                id,
                request.getVersionNo(),
                request.getPayload(),
                request.getValidationMode()
        );
        // 返回结构数据校验结果。
        return Result.success(result);
    }
}
