package top.kx.heartbeat.application.structure;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.kx.heartbeat.application.structure.artifact.ArtifactGenerator;
import top.kx.heartbeat.application.structure.artifact.ArtifactGeneratorRegistry;
import top.kx.heartbeat.application.structure.dto.*;
import top.kx.heartbeat.application.structure.mapper.StructureDtoMapper;
import top.kx.heartbeat.application.structure.validation.StructurePayloadValidator;
import top.kx.heartbeat.domain.auth.CurrentUserProvider;
import top.kx.heartbeat.domain.common.exception.DomainException;
import top.kx.heartbeat.domain.structure.StructureErrorCode;
import top.kx.heartbeat.domain.structure.model.StructureDefinition;
import top.kx.heartbeat.domain.structure.model.StructureDraft;
import top.kx.heartbeat.domain.structure.model.StructureNode;
import top.kx.heartbeat.domain.structure.model.StructureVersion;
import top.kx.heartbeat.domain.structure.repository.StructureDefinitionRepository;
import top.kx.heartbeat.domain.structure.repository.StructurePublishAuditRepository;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

/**
 * 结构化定义应用服务
 * <p>
 * 负责"结构化对象模板"的业务编排，覆盖：
 * <ul>
 *     <li>从 JSON 样例推断结构（{@link StructureInferenceEngine}）</li>
 *     <li>渲染产物（schema/UI schema/sample）</li>
 *     <li>草稿/版本/激活/差异/校验</li>
 *     <li>发布审计记录</li>
 * </ul>
 * </p>
 *
 * @author heartbeat-team
 */
@Service
public class StructureApplicationService {

    @Resource
    private StructureInferenceEngine inferenceEngine;
    @Resource
    private ArtifactGeneratorRegistry generatorRegistry;
    @Resource
    private StructurePayloadValidator payloadValidator;
    @Resource
    private StructureDefinitionRepository repository;
    @Resource
    private StructurePublishAuditRepository publishAuditRepository;
    @Resource
    private CurrentUserProvider currentUserProvider;
    @Resource
    private StructureDtoMapper structureDtoMapper;
    @Resource
    private ObjectMapper objectMapper;

    /**
     * 预览：从样例推断结构并渲染所有产物，不落库
     */
    public StructurePreviewDTO preview(List<JsonNode> samples,
                                       ValidationMode mode,
                                       JsonNode uiOverrides) {
        requireSamples(samples);
        GenerationOptions options = new GenerationOptions(mode, uiOverrides);
        return preview(samples, options);
    }

    private StructurePreviewDTO preview(List<JsonNode> samples, GenerationOptions options) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        InferenceResult inference = inferenceEngine.infer(samples);
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, JsonNode> artifacts = new LinkedHashMap<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map.Entry<String, ArtifactGenerator> entry : generatorRegistry.all().entrySet()) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            artifacts.put(entry.getKey(), entry.getValue().generate(inference.getRoot(), options));
        }
        // 返回已经完成封装的业务结果。
        return new StructurePreviewDTO(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                inference.getRoot(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                artifacts,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                inference.getWarnings(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                digest(samples)
        );
    }

    /**
     * 保存草稿：将最新推断写入该结构定义的草稿
     */
    @Transactional
    public StructureDefinitionDTO saveDraft(String definitionId,
                                            List<JsonNode> samples,
                                            ValidationMode mode,
                                            JsonNode fieldOverrides) {
        StructureDefinition definition = getDomain(definitionId);
        StructureDraft draft = buildDraft(samples, mode, fieldOverrides, Instant.now());
        definition.saveDraft(draft);
        return structureDtoMapper.toDTO(repository.save(definition));
    }

    /**
     * 用历史版本内容覆盖当前草稿（便于基于版本重新编辑）
     */
    @Transactional
    public StructureDefinitionDTO copyVersionToDraft(String definitionId, int versionNo) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinition definition = getDomain(definitionId);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureVersion version = definition.getVersion(versionNo);
        // 创建当前流程需要的临时对象，承载后续处理数据。
        StructureDraft draft = new StructureDraft(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getStructureModel(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getGenerationConfig(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getFieldOverrides(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getArtifacts(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getWarnings(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getValidationMode(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getSampleDigest(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                Instant.now()
        );
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        definition.saveDraft(draft);
        // 返回已经完成封装的业务结果。
        return structureDtoMapper.toDTO(repository.save(definition));
    }

    /**
     * 把当前草稿发布为新版本
     */
    @Transactional
    public StructureDefinitionDTO createVersionFromDraft(String definitionId) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinition definition = getDomain(definitionId);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDraft draft = definition.getDraft();
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (draft == null) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new DomainException(StructureErrorCode.STRUCTURE_DRAFT_MISSING, "结构定义没有可保存的草稿");
        }
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Instant now = Instant.now();
        // 创建当前流程需要的临时对象，承载后续处理数据。
        definition.addVersion(new StructureVersion(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                definition.nextVersionNo(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                draft.getStructureModel(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                draft.getGenerationConfig(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                draft.getFieldOverrides(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                draft.getArtifacts(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                draft.getWarnings(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                draft.getValidationMode(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                draft.getSampleDigest(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                now
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        ));
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        definition.clearDraft(now);
        // 返回已经完成封装的业务结果。
        return structureDtoMapper.toDTO(repository.save(definition));
    }

    /**
     * 创建新的结构化定义（首个版本，可选立即激活）
     */
    @Transactional
    public StructureDefinitionDTO create(String name,
                                         // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                         String description,
                                         // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                                         List<JsonNode> samples,
                                         // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                         ValidationMode mode,
                                         // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                                         JsonNode uiOverrides,
                                         // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                         boolean activate) {
        // 读取扩展参数载体，为后续动态处理准备数据。
        GenerationOptions options = new GenerationOptions(mode, uiOverrides);
        // 读取扩展参数载体，为后续动态处理准备数据。
        StructurePreviewDTO preview = preview(samples, options);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Instant now = Instant.now();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinition definition = StructureDefinition.create(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                name,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                description,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                now
        );
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        definition.addVersion(toVersion(1, preview, options, now));
        // 根据当前业务条件选择对应处理路径。
        if (activate) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            definition.activate(1, now);
        }
        // 返回已经完成封装的业务结果。
        return structureDtoMapper.toDTO(repository.save(definition));
    }

    /**
     * 基于最新样例为已有结构定义追加新版本
     */
    @Transactional
    public StructureDefinitionDTO createVersion(String definitionId,
                                                // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                                                List<JsonNode> samples,
                                                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                                ValidationMode mode,
                                                // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                                                JsonNode uiOverrides) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinition definition = getDomain(definitionId);
        // 读取扩展参数载体，为后续动态处理准备数据。
        GenerationOptions options = new GenerationOptions(mode, uiOverrides);
        // 读取扩展参数载体，为后续动态处理准备数据。
        StructurePreviewDTO preview = preview(samples, options);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        Instant now = Instant.now();
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        definition.addVersion(toVersion(definition.nextVersionNo(), preview, options, now));
        // 返回已经完成封装的业务结果。
        return structureDtoMapper.toDTO(repository.save(definition));
    }

    /**
     * 激活指定版本（并写入发布审计）
     */
    @Transactional
    public StructureDefinitionDTO activate(String definitionId, int versionNo) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinition definition = getDomain(definitionId);
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        definition.activate(versionNo, Instant.now());
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinition saved = repository.save(definition);
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        publishAuditRepository.record(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseDefinitionId(definitionId),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                versionNo,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseUserId(currentUserProvider.currentUserId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "SUCCESS",
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                "结构定义发布到版本 " + versionNo
        );
        // 返回已经完成封装的业务结果。
        return structureDtoMapper.toDTO(saved);
    }

    /**
     * 列出全部结构定义
     */
    public List<StructureDefinitionDTO> list() {
        // 创建结果集合，承接后续逐项组装的数据。
        List<StructureDefinitionDTO> result = new ArrayList<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureDefinition definition : repository.findAll()) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            result.add(structureDtoMapper.toDTO(definition));
        }
        // 返回已经完成封装的业务结果。
        return result;
    }

    /**
     * 查询单个结构定义
     */
    public StructureDefinitionDTO get(String definitionId) {
        return structureDtoMapper.toDTO(getDomain(definitionId));
    }

    /**
     * 比较两个版本（或版本与草稿）的结构与 UI 覆盖差异
     */
    public StructureVersionDiffDTO diff(String definitionId,
                                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                        Integer fromVersionNo,
                                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                        Integer toVersionNo,
                                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                        boolean toDraft) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinition definition = getDomain(definitionId);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureVersion from = fromVersionNo == null
                // 条件成立时使用前一个分支计算出的业务值。
                ? definition.getActiveVersion()
                // 条件不成立时使用兜底业务值。
                : definition.getVersion(fromVersionNo);
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        StructureNode toModel;
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        String toFieldOverrides;
        // 根据当前业务条件选择对应处理路径。
        if (toDraft) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            StructureDraft draft = definition.getDraft();
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (draft == null) {
                // 对非法业务状态立即失败，避免错误继续扩散。
                throw new DomainException(StructureErrorCode.STRUCTURE_DRAFT_MISSING, "结构定义没有可比较的草稿");
            }
            // 计算当前分支的中间结果，供后续判断或组装使用。
            toModel = draft.getStructureModel();
            // 计算当前分支的中间结果，供后续判断或组装使用。
            toFieldOverrides = draft.getFieldOverrides();
        } else {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            StructureVersion to = toVersionNo == null
                    // 条件成立时使用前一个分支计算出的业务值。
                    ? definition.getActiveVersion()
                    // 条件不成立时使用兜底业务值。
                    : definition.getVersion(toVersionNo);
            // 计算当前分支的中间结果，供后续判断或组装使用。
            toModel = to.getStructureModel();
            // 计算当前分支的中间结果，供后续判断或组装使用。
            toFieldOverrides = to.getFieldOverrides();
        }

        // 创建结果集合，承接后续逐项组装的数据。
        List<StructureDiffItemDTO> changes = new ArrayList<>();
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        diffStructure(flattenModel(from.getStructureModel()), flattenModel(toModel), changes);
        // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
        diffOverrides(flattenJson(from.getFieldOverrides()), flattenJson(toFieldOverrides), changes);
        // 返回已经完成封装的业务结果。
        return new StructureVersionDiffDTO(definitionId, from.getVersionNo(), toVersionNo, toDraft, changes);
    }

    /**
     * 用指定版本的 Schema 校验一段 payload
     */
    public ValidationResultDTO validate(String definitionId,
                                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                        Integer versionNo,
                                        // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                                        JsonNode payload,
                                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                        ValidationMode modeOverride) {
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinition definition = getDomain(definitionId);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureVersion version = versionNo == null
                // 条件成立时使用前一个分支计算出的业务值。
                ? definition.getActiveVersion()
                // 条件不成立时使用兜底业务值。
                : definition.getVersion(versionNo);
        // 计算当前分支的中间结果，供后续判断或组装使用。
        ValidationMode mode = modeOverride == null
                // 条件成立时使用前一个分支计算出的业务值。
                ? ValidationMode.valueOf(version.getValidationMode())
                // 条件不成立时使用兜底业务值。
                : modeOverride;
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        List<ValidationError> errors = payloadValidator.validate(version.getStructureModel(), payload, mode);
        // 返回已经完成封装的业务结果。
        return new ValidationResultDTO(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                CollectionUtils.isEmpty(errors),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                definitionId,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                version.getVersionNo(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                mode,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                errors
        );
    }

    private StructureDefinition getDomain(String definitionId) {
        long id = parseDefinitionId(definitionId);
        return repository.findById(id).orElseThrow(() ->
                new DomainException(
                        StructureErrorCode.STRUCTURE_NOT_FOUND,
                        "结构定义不存在: " + definitionId
                ));
    }

    private StructureVersion toVersion(int versionNo,
                                       // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                       StructurePreviewDTO preview,
                                       // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                       GenerationOptions options,
                                       // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                       Instant now) {
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> artifacts = new LinkedHashMap<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map.Entry<String, JsonNode> artifact : preview.getArtifacts().entrySet()) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            artifacts.put(artifact.getKey(), writeJson(artifact.getValue()));
        }
        // 返回已经完成封装的业务结果。
        return new StructureVersion(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                versionNo,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                preview.getStructureModel(),
                // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                writeJson(options.toGenerationConfig(objectMapper)),
                // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                writeJson(options.getFieldOverrides()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                artifacts,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                preview.getWarnings(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                options.getValidationMode().name(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                preview.getSampleDigest(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                now
        );
    }

    private StructureDraft buildDraft(List<JsonNode> samples,
                                      // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                      ValidationMode mode,
                                      // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                                      JsonNode fieldOverrides,
                                      // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                                      Instant now) {
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        requireSamples(samples);
        // 读取扩展参数载体，为后续动态处理准备数据。
        GenerationOptions options = new GenerationOptions(mode, fieldOverrides);
        // 读取扩展参数载体，为后续动态处理准备数据。
        StructurePreviewDTO preview = preview(samples, options);
        // 创建有序字段容器，保证响应或领域记录的字段顺序稳定。
        Map<String, String> artifacts = new LinkedHashMap<>();
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map.Entry<String, JsonNode> artifact : preview.getArtifacts().entrySet()) {
            // 写入对外字段，保持调用方依赖的响应结构稳定。
            artifacts.put(artifact.getKey(), writeJson(artifact.getValue()));
        }
        // 返回已经完成封装的业务结果。
        return new StructureDraft(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                preview.getStructureModel(),
                // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                writeJson(options.toGenerationConfig(objectMapper)),
                // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                writeJson(options.getFieldOverrides()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                artifacts,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                preview.getWarnings(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                options.getValidationMode().name(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                preview.getSampleDigest(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                now
        );
    }

    private Map<String, StructureNode> flattenModel(StructureNode root) {
        Map<String, StructureNode> result = new LinkedHashMap<>();
        collectModel(root, result);
        return result;
    }

    private void collectModel(StructureNode node, Map<String, StructureNode> result) {
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put(node.getPath(), node);
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureNode child : node.getProperties().values()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            collectModel(child, result);
        }
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (node.getItems() != null) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            collectModel(node.getItems(), result);
        }
    }

    private void diffStructure(Map<String, StructureNode> before,
                               // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                               Map<String, StructureNode> after,
                               // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                               List<StructureDiffItemDTO> changes) {
        // 创建去重集合，避免重复标识影响后续查询。
        Set<String> paths = new LinkedHashSet<>();
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        paths.addAll(before.keySet());
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        paths.addAll(after.keySet());
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (String path : paths) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            StructureNode oldNode = before.get(path);
            // 计算当前分支的中间结果，供后续判断或组装使用。
            StructureNode newNode = after.get(path);
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (oldNode == null) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "ADDED", null, describeNode(newNode)));
                // 跳过当前不需要展示的节点，继续处理下一条数据。
                continue;
            }
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (newNode == null) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "REMOVED", describeNode(oldNode), null));
                // 跳过当前不需要展示的节点，继续处理下一条数据。
                continue;
            }
            // 比对当前业务状态，决定是否进入该处理分支。
            if (!oldNode.getTypes().equals(newNode.getTypes())) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "TYPE_CHANGED",
                        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                        oldNode.getTypes().toString(), newNode.getTypes().toString()));
            }
            // 根据当前业务条件选择对应处理路径。
            if (oldNode.isRequired() != newNode.isRequired()) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "REQUIRED_CHANGED",
                        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                        String.valueOf(oldNode.isRequired()), String.valueOf(newNode.isRequired())));
            }
            // 根据当前业务条件选择对应处理路径。
            if (oldNode.isNullable() != newNode.isNullable()) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "NULLABLE_CHANGED",
                        // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                        String.valueOf(oldNode.isNullable()), String.valueOf(newNode.isNullable())));
            }
        }
    }

    private String describeNode(StructureNode node) {
        return node == null ? null : node.getTypes().toString();
    }

    private Map<String, String> flattenJson(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        collectJson("", readJson(json), result);
        return result;
    }

    private void collectJson(String path, JsonNode node, Map<String, String> result) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (node == null || node.isMissingNode() || node.isNull()) {
            // 返回已经完成封装的业务结果。
            return;
        }
        // 根据当前业务条件选择对应处理路径。
        if (node.isObject()) {
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            node.fields().forEachRemaining(entry -> {
                // 规范化文本值，降低空字符串和空对象带来的分支复杂度。
                String childPath = StringUtils.isEmpty(path) ? entry.getKey() : path + "." + entry.getKey();
                // 处理 JSON 序列化或反序列化，完成对象与文本之间的转换。
                collectJson(childPath, entry.getValue(), result);
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            });
            // 返回已经完成封装的业务结果。
            return;
        }
        // 写入对外字段，保持调用方依赖的响应结构稳定。
        result.put(path, node.toString());
    }

    private void diffOverrides(Map<String, String> before,
                               // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                               Map<String, String> after,
                               // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                               List<StructureDiffItemDTO> changes) {
        // 创建去重集合，避免重复标识影响后续查询。
        Set<String> paths = new LinkedHashSet<>();
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        paths.addAll(before.keySet());
        // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
        paths.addAll(after.keySet());
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (String path : paths) {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            String oldValue = before.get(path);
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            String newValue = after.get(path);
            // 先处理空值或缺省场景，避免后续业务流程出现空指针。
            if (oldValue == null && newValue != null) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                changes.add(new StructureDiffItemDTO("DISPLAY", path, "ADDED", null, newValue));
            } else if (oldValue != null && newValue == null) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                changes.add(new StructureDiffItemDTO("DISPLAY", path, "REMOVED", oldValue, null));
            } else if (oldValue != null && !oldValue.equals(newValue)) {
                // 加入当前处理结果，供后续批量返回或继续组装。
                changes.add(new StructureDiffItemDTO("DISPLAY", path, "CHANGED", oldValue, newValue));
            }
        }
    }

    private void requireSamples(List<JsonNode> samples) {
        if (CollectionUtils.isEmpty(samples)) {
            throw new DomainException(StructureErrorCode.SAMPLE_EMPTY, "至少需要一份 JSON 样例");
        }
    }

    private String digest(List<JsonNode> samples) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 按签名算法处理字节数据，保证验签结果可重复计算。
            byte[] hash = digest.digest(writeJson(samples).getBytes(StandardCharsets.UTF_8));
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            StringBuilder result = new StringBuilder();
            // 逐条遍历集合数据，完成业务结果组装或状态处理。
            for (byte value : hash) {
                // 按签名算法处理字节数据，保证验签结果可重复计算。
                result.append(String.format("%02x", value));
            }
            // 返回已经完成封装的业务结果。
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", ex);
        }
    }

    private String writeJson(Object value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("JSON 序列化失败", ex);
        }
    }

    private JsonNode readJson(String value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            // 对非法业务状态立即失败，避免错误继续扩散。
            throw new IllegalStateException("JSON 反序列化失败", ex);
        }
    }

    private long parseDefinitionId(String definitionId) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            long id = Long.parseLong(definitionId);
            // 根据当前业务条件选择对应处理路径。
            if (id > 0) {
                // 返回已经完成封装的业务结果。
                return id;
            }
        } catch (NumberFormatException ignored) {
        }
        // 对非法业务状态立即失败，避免错误继续扩散。
        throw new DomainException(StructureErrorCode.STRUCTURE_NOT_FOUND, "结构定义不存在: " + definitionId);
    }

    private long parseUserId(String userId) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 计算当前步骤所需的中间值，供后续业务判断使用。
            long id = Long.parseLong(userId);
            // 返回已经完成封装的业务结果。
            return id > 0 ? id : 1L;
        } catch (NumberFormatException ignored) {
            // 返回已经完成封装的业务结果。
            return 1L;
        }
    }
}
