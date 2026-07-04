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
        InferenceResult inference = inferenceEngine.infer(samples);
        Map<String, JsonNode> artifacts = new LinkedHashMap<>();
        for (Map.Entry<String, ArtifactGenerator> entry : generatorRegistry.all().entrySet()) {
            artifacts.put(entry.getKey(), entry.getValue().generate(inference.getRoot(), options));
        }
        return new StructurePreviewDTO(
                inference.getRoot(),
                artifacts,
                inference.getWarnings(),
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
        StructureDefinition definition = getDomain(definitionId);
        StructureVersion version = definition.getVersion(versionNo);
        StructureDraft draft = new StructureDraft(
                version.getStructureModel(),
                version.getGenerationConfig(),
                version.getFieldOverrides(),
                version.getArtifacts(),
                version.getWarnings(),
                version.getValidationMode(),
                version.getSampleDigest(),
                Instant.now()
        );
        definition.saveDraft(draft);
        return structureDtoMapper.toDTO(repository.save(definition));
    }

    /**
     * 把当前草稿发布为新版本
     */
    @Transactional
    public StructureDefinitionDTO createVersionFromDraft(String definitionId) {
        StructureDefinition definition = getDomain(definitionId);
        StructureDraft draft = definition.getDraft();
        if (draft == null) {
            throw new DomainException(StructureErrorCode.STRUCTURE_DRAFT_MISSING, "结构定义没有可保存的草稿");
        }
        Instant now = Instant.now();
        definition.addVersion(new StructureVersion(
                definition.nextVersionNo(),
                draft.getStructureModel(),
                draft.getGenerationConfig(),
                draft.getFieldOverrides(),
                draft.getArtifacts(),
                draft.getWarnings(),
                draft.getValidationMode(),
                draft.getSampleDigest(),
                now
        ));
        definition.clearDraft(now);
        return structureDtoMapper.toDTO(repository.save(definition));
    }

    /**
     * 创建新的结构化定义（首个版本，可选立即激活）
     */
    @Transactional
    public StructureDefinitionDTO create(String name,
                                         String description,
                                         List<JsonNode> samples,
                                         ValidationMode mode,
                                         JsonNode uiOverrides,
                                         boolean activate) {
        GenerationOptions options = new GenerationOptions(mode, uiOverrides);
        StructurePreviewDTO preview = preview(samples, options);
        Instant now = Instant.now();
        StructureDefinition definition = StructureDefinition.create(
                name,
                description,
                now
        );
        definition.addVersion(toVersion(1, preview, options, now));
        if (activate) {
            definition.activate(1, now);
        }
        return structureDtoMapper.toDTO(repository.save(definition));
    }

    /**
     * 基于最新样例为已有结构定义追加新版本
     */
    @Transactional
    public StructureDefinitionDTO createVersion(String definitionId,
                                                List<JsonNode> samples,
                                                ValidationMode mode,
                                                JsonNode uiOverrides) {
        StructureDefinition definition = getDomain(definitionId);
        GenerationOptions options = new GenerationOptions(mode, uiOverrides);
        StructurePreviewDTO preview = preview(samples, options);
        Instant now = Instant.now();
        definition.addVersion(toVersion(definition.nextVersionNo(), preview, options, now));
        return structureDtoMapper.toDTO(repository.save(definition));
    }

    /**
     * 激活指定版本（并写入发布审计）
     */
    @Transactional
    public StructureDefinitionDTO activate(String definitionId, int versionNo) {
        StructureDefinition definition = getDomain(definitionId);
        definition.activate(versionNo, Instant.now());
        StructureDefinition saved = repository.save(definition);
        publishAuditRepository.record(
                parseDefinitionId(definitionId),
                versionNo,
                parseUserId(currentUserProvider.currentUserId()),
                "SUCCESS",
                "结构定义发布到版本 " + versionNo
        );
        return structureDtoMapper.toDTO(saved);
    }

    /**
     * 列出全部结构定义
     */
    public List<StructureDefinitionDTO> list() {
        List<StructureDefinitionDTO> result = new ArrayList<>();
        for (StructureDefinition definition : repository.findAll()) {
            result.add(structureDtoMapper.toDTO(definition));
        }
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
                                        Integer fromVersionNo,
                                        Integer toVersionNo,
                                        boolean toDraft) {
        StructureDefinition definition = getDomain(definitionId);
        StructureVersion from = fromVersionNo == null
                ? definition.getActiveVersion()
                : definition.getVersion(fromVersionNo);
        StructureNode toModel;
        String toFieldOverrides;
        if (toDraft) {
            StructureDraft draft = definition.getDraft();
            if (draft == null) {
                throw new DomainException(StructureErrorCode.STRUCTURE_DRAFT_MISSING, "结构定义没有可比较的草稿");
            }
            toModel = draft.getStructureModel();
            toFieldOverrides = draft.getFieldOverrides();
        } else {
            StructureVersion to = toVersionNo == null
                    ? definition.getActiveVersion()
                    : definition.getVersion(toVersionNo);
            toModel = to.getStructureModel();
            toFieldOverrides = to.getFieldOverrides();
        }

        List<StructureDiffItemDTO> changes = new ArrayList<>();
        diffStructure(flattenModel(from.getStructureModel()), flattenModel(toModel), changes);
        diffOverrides(flattenJson(from.getFieldOverrides()), flattenJson(toFieldOverrides), changes);
        return new StructureVersionDiffDTO(definitionId, from.getVersionNo(), toVersionNo, toDraft, changes);
    }

    /**
     * 用指定版本的 Schema 校验一段 payload
     */
    public ValidationResultDTO validate(String definitionId,
                                        Integer versionNo,
                                        JsonNode payload,
                                        ValidationMode modeOverride) {
        StructureDefinition definition = getDomain(definitionId);
        StructureVersion version = versionNo == null
                ? definition.getActiveVersion()
                : definition.getVersion(versionNo);
        ValidationMode mode = modeOverride == null
                ? ValidationMode.valueOf(version.getValidationMode())
                : modeOverride;
        List<ValidationError> errors = payloadValidator.validate(version.getStructureModel(), payload, mode);
        return new ValidationResultDTO(
                CollectionUtils.isEmpty(errors),
                definitionId,
                version.getVersionNo(),
                mode,
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
                                       StructurePreviewDTO preview,
                                       GenerationOptions options,
                                       Instant now) {
        Map<String, String> artifacts = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> artifact : preview.getArtifacts().entrySet()) {
            artifacts.put(artifact.getKey(), writeJson(artifact.getValue()));
        }
        return new StructureVersion(
                versionNo,
                preview.getStructureModel(),
                writeJson(options.toGenerationConfig(objectMapper)),
                writeJson(options.getFieldOverrides()),
                artifacts,
                preview.getWarnings(),
                options.getValidationMode().name(),
                preview.getSampleDigest(),
                now
        );
    }

    private StructureDraft buildDraft(List<JsonNode> samples,
                                      ValidationMode mode,
                                      JsonNode fieldOverrides,
                                      Instant now) {
        requireSamples(samples);
        GenerationOptions options = new GenerationOptions(mode, fieldOverrides);
        StructurePreviewDTO preview = preview(samples, options);
        Map<String, String> artifacts = new LinkedHashMap<>();
        for (Map.Entry<String, JsonNode> artifact : preview.getArtifacts().entrySet()) {
            artifacts.put(artifact.getKey(), writeJson(artifact.getValue()));
        }
        return new StructureDraft(
                preview.getStructureModel(),
                writeJson(options.toGenerationConfig(objectMapper)),
                writeJson(options.getFieldOverrides()),
                artifacts,
                preview.getWarnings(),
                options.getValidationMode().name(),
                preview.getSampleDigest(),
                now
        );
    }

    private Map<String, StructureNode> flattenModel(StructureNode root) {
        Map<String, StructureNode> result = new LinkedHashMap<>();
        collectModel(root, result);
        return result;
    }

    private void collectModel(StructureNode node, Map<String, StructureNode> result) {
        result.put(node.getPath(), node);
        for (StructureNode child : node.getProperties().values()) {
            collectModel(child, result);
        }
        if (node.getItems() != null) {
            collectModel(node.getItems(), result);
        }
    }

    private void diffStructure(Map<String, StructureNode> before,
                               Map<String, StructureNode> after,
                               List<StructureDiffItemDTO> changes) {
        Set<String> paths = new LinkedHashSet<>();
        paths.addAll(before.keySet());
        paths.addAll(after.keySet());
        for (String path : paths) {
            StructureNode oldNode = before.get(path);
            StructureNode newNode = after.get(path);
            if (oldNode == null) {
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "ADDED", null, describeNode(newNode)));
                continue;
            }
            if (newNode == null) {
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "REMOVED", describeNode(oldNode), null));
                continue;
            }
            if (!oldNode.getTypes().equals(newNode.getTypes())) {
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "TYPE_CHANGED",
                        oldNode.getTypes().toString(), newNode.getTypes().toString()));
            }
            if (oldNode.isRequired() != newNode.isRequired()) {
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "REQUIRED_CHANGED",
                        String.valueOf(oldNode.isRequired()), String.valueOf(newNode.isRequired())));
            }
            if (oldNode.isNullable() != newNode.isNullable()) {
                changes.add(new StructureDiffItemDTO("STRUCTURE", path, "NULLABLE_CHANGED",
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
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> {
                String childPath = StringUtils.isEmpty(path) ? entry.getKey() : path + "." + entry.getKey();
                collectJson(childPath, entry.getValue(), result);
            });
            return;
        }
        result.put(path, node.toString());
    }

    private void diffOverrides(Map<String, String> before,
                               Map<String, String> after,
                               List<StructureDiffItemDTO> changes) {
        Set<String> paths = new LinkedHashSet<>();
        paths.addAll(before.keySet());
        paths.addAll(after.keySet());
        for (String path : paths) {
            String oldValue = before.get(path);
            String newValue = after.get(path);
            if (oldValue == null && newValue != null) {
                changes.add(new StructureDiffItemDTO("DISPLAY", path, "ADDED", null, newValue));
            } else if (oldValue != null && newValue == null) {
                changes.add(new StructureDiffItemDTO("DISPLAY", path, "REMOVED", oldValue, null));
            } else if (oldValue != null && !oldValue.equals(newValue)) {
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
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(writeJson(samples).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : hash) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("当前 JVM 不支持 SHA-256", ex);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 序列化失败", ex);
        }
    }

    private JsonNode readJson(String value) {
        try {
            return objectMapper.readTree(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("JSON 反序列化失败", ex);
        }
    }

    private long parseDefinitionId(String definitionId) {
        try {
            long id = Long.parseLong(definitionId);
            if (id > 0) {
                return id;
            }
        } catch (NumberFormatException ignored) {
        }
        throw new DomainException(StructureErrorCode.STRUCTURE_NOT_FOUND, "结构定义不存在: " + definitionId);
    }

    private long parseUserId(String userId) {
        try {
            long id = Long.parseLong(userId);
            return id > 0 ? id : 1L;
        } catch (NumberFormatException ignored) {
            return 1L;
        }
    }
}
