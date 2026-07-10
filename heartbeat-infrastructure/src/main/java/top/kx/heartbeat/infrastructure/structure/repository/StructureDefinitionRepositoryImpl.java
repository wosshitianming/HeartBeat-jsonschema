package top.kx.heartbeat.infrastructure.structure.repository;


import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.structure.model.StructureDefinition;
import top.kx.heartbeat.domain.structure.model.StructureDraft;
import top.kx.heartbeat.domain.structure.model.StructureVersion;
import top.kx.heartbeat.domain.structure.repository.StructureDefinitionRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.*;
import top.kx.heartbeat.infrastructure.persistence.mapper.structure.StructureArtifactDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.structure.StructureDefinitionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.structure.StructureDraftDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.structure.StructureVersionDOMapper;
import top.kx.heartbeat.infrastructure.structure.converter.StructureModelJsonCodec;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Repository
public class StructureDefinitionRepositoryImpl implements StructureDefinitionRepository {

    @Resource
    private StructureDefinitionDOMapper definitionMapper;
    @Resource
    private StructureDraftDOMapper draftMapper;
    @Resource
    private StructureVersionDOMapper versionMapper;
    @Resource
    private StructureArtifactDOMapper artifactMapper;
    @Resource
    private StructureModelJsonCodec jsonCodec;

    @Override
    public StructureDefinition save(StructureDefinition definition) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long tenantId = currentTenantId();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinitionDO entity = toDefinitionDO(definition);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setTenantId(tenantId);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (entity.getId() == null) {
            // 将当前业务变更写入持久化层，保持数据状态同步。
            definitionMapper.insertSelective(entity);
        } else {
            StructureDefinitionDOExample existing = new StructureDefinitionDOExample();
            existing.createCriteria()
                    .andTenantIdEqualTo(tenantId)
                    .andIdEqualTo(entity.getId());
            if (definitionMapper.countByExample(existing) == 0) {
                throw new IllegalStateException("结构定义不存在或不属于当前租户: " + entity.getId());
            }
            // 将当前业务变更写入持久化层，保持数据状态同步。
            definitionMapper.updateByExampleSelective(entity, existing);
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long definitionId = entity.getId();
        saveDraft(tenantId, definitionId, definition.getDraft());
        appendVersions(tenantId, definitionId, definition.getVersions());
        // 返回已经完成封装的业务结果。
        return findById(definitionId).orElse(definition);
    }

    private void saveDraft(Long tenantId, Long definitionId, StructureDraft draft) {
        StructureDraftDOExample example = draftByDefinition(tenantId, definitionId);
        StructureDraftDOWithBLOBs existing = first(draftMapper.selectByExampleWithBLOBs(example));
        if (draft == null) {
            if (existing != null) {
                draftMapper.deleteByExample(example);
            }
            return;
        }

        StructureDraftDOWithBLOBs entity = toDraftDO(draft);
        entity.setTenantId(tenantId);
        entity.setDefinitionId(definitionId);
        if (existing == null) {
            draftMapper.insertSelective(entity);
            return;
        }
        entity.setCreateTime(existing.getCreateTime());
        if (sameDraft(existing, entity)) {
            return;
        }
        draftMapper.updateByExampleSelective(entity, example);
    }

    private boolean sameDraft(StructureDraftDOWithBLOBs left, StructureDraftDOWithBLOBs right) {
        return Objects.equals(left.getStructureModel(), right.getStructureModel())
                && Objects.equals(left.getGenerationConfig(), right.getGenerationConfig())
                && Objects.equals(left.getFieldOverrides(), right.getFieldOverrides())
                && Objects.equals(left.getArtifacts(), right.getArtifacts())
                && Objects.equals(left.getWarnings(), right.getWarnings())
                && Objects.equals(left.getValidationMode(), right.getValidationMode())
                && Objects.equals(left.getSampleDigest(), right.getSampleDigest())
                && Objects.equals(left.getUpdateTime(), right.getUpdateTime());
    }

    private void appendVersions(Long tenantId, Long definitionId, List<StructureVersion> versions) {
        StructureVersionDOExample example = versionByDefinition(tenantId, definitionId);
        Map<Integer, StructureVersionDOWithBLOBs> existing = new HashMap<>();
        for (StructureVersionDOWithBLOBs version : versionMapper.selectByExampleWithBLOBs(example)) {
            existing.put(version.getVersionNo(), version);
        }

        Set<Integer> expectedVersionNumbers = versions.stream()
                .map(StructureVersion::getVersionNo)
                .collect(Collectors.toSet());
        if (!expectedVersionNumbers.containsAll(existing.keySet())) {
            throw new IllegalStateException("结构定义版本只能追加，不能删除历史版本");
        }

        for (StructureVersion version : versions) {
            if (existing.containsKey(version.getVersionNo())) {
                continue;
            }
            StructureVersionDOWithBLOBs entity = toVersionDO(version);
            entity.setTenantId(tenantId);
            entity.setDefinitionId(definitionId);
            versionMapper.insertSelective(entity);
            insertArtifacts(tenantId, definitionId, entity.getId(), version);
        }
    }

    @Override
    public Optional<StructureDefinition> findById(long id) {
        Long tenantId = currentTenantId();
        StructureDefinitionDOExample example = new StructureDefinitionDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andIdEqualTo(id);
        return Optional.ofNullable(first(definitionMapper.selectByExample(example)))
                .map(this::toDomain);
    }

    @Override
    public List<StructureDefinition> findAll() {
        Long tenantId = currentTenantId();
        StructureDefinitionDOExample example = new StructureDefinitionDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId);
        example.setOrderByClause("id DESC");
        List<StructureDefinitionDO> definitions = definitionMapper.selectByExample(example);
        if (definitions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, StructureDraftDOWithBLOBs> drafts = readDrafts(tenantId);
        Map<Long, List<StructureVersionDOWithBLOBs>> versions = readVersions(tenantId);
        return definitions.stream()
                .map(entity -> toDomain(
                        entity,
                        drafts.get(entity.getId()),
                        versions.getOrDefault(entity.getId(), Collections.emptyList())
                ))
                .collect(Collectors.toList());
    }

    private void insertArtifacts(Long tenantId, Long definitionId, Long versionId, StructureVersion version) {
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (versionId == null) {
            // 返回已经完成封装的业务结果。
            return;
        }
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (Map.Entry<String, String> artifact : version.getArtifacts().entrySet()) {
            // 创建数据库记录对象，承载即将写入的业务字段。
            StructureArtifactDO entity = new StructureArtifactDO();
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setTenantId(tenantId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setDefinitionId(definitionId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setVersionId(versionId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setArtifactKey(artifact.getKey());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setArtifactJson(artifact.getValue());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            entity.setCreateTime(mapTime(version.getCreateTime()));
            // 将当前业务变更写入持久化层，保持数据状态同步。
            artifactMapper.insertSelective(entity);
        }
    }

    private StructureDefinition toDomain(StructureDefinitionDO entity) {
        StructureVersionDOExample example = versionByDefinition(entity.getTenantId(), entity.getId());
        example.setOrderByClause("version_no ASC");
        StructureDraftDOWithBLOBs draft = first(draftMapper.selectByExampleWithBLOBs(
                draftByDefinition(entity.getTenantId(), entity.getId())
        ));
        return toDomain(entity, draft, versionMapper.selectByExampleWithBLOBs(example));
    }

    private StructureDefinition toDomain(StructureDefinitionDO entity,
                                         StructureDraftDOWithBLOBs draft,
                                         List<StructureVersionDOWithBLOBs> versionEntities) {
        List<StructureVersion> versions = versionEntities.stream()
                .map(this::toDomainVersion)
                .collect(Collectors.toList());
        return new StructureDefinition(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getActiveVersionNo(),
                entity.getStatus(),
                draft == null ? null : toDomainDraft(draft),
                versions,
                mapTime(entity.getCreateTime()),
                mapTime(entity.getUpdateTime())
        );
    }

    private Map<Long, StructureDraftDOWithBLOBs> readDrafts(Long tenantId) {
        StructureDraftDOExample example = new StructureDraftDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId);
        Map<Long, StructureDraftDOWithBLOBs> result = new HashMap<>();
        for (StructureDraftDOWithBLOBs draft : draftMapper.selectByExampleWithBLOBs(example)) {
            result.putIfAbsent(draft.getDefinitionId(), draft);
        }
        return result;
    }

    private Map<Long, List<StructureVersionDOWithBLOBs>> readVersions(Long tenantId) {
        StructureVersionDOExample example = new StructureVersionDOExample();
        example.createCriteria().andTenantIdEqualTo(tenantId);
        example.setOrderByClause("definition_id ASC, version_no ASC");
        Map<Long, List<StructureVersionDOWithBLOBs>> result = new HashMap<>();
        for (StructureVersionDOWithBLOBs version : versionMapper.selectByExampleWithBLOBs(example)) {
            result.computeIfAbsent(version.getDefinitionId(), ignored -> new ArrayList<>()).add(version);
        }
        return result;
    }

    private StructureDraftDOExample draftByDefinition(Long tenantId, Long definitionId) {
        StructureDraftDOExample example = new StructureDraftDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andDefinitionIdEqualTo(definitionId);
        return example;
    }

    private StructureVersionDOExample versionByDefinition(Long tenantId, Long definitionId) {
        StructureVersionDOExample example = new StructureVersionDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andDefinitionIdEqualTo(definitionId);
        return example;
    }

    private StructureArtifactDOExample artifactByVersion(Long tenantId, Long versionId) {
        StructureArtifactDOExample example = new StructureArtifactDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andVersionIdEqualTo(versionId);
        return example;
    }

    private <T> T first(List<T> records) {
        return records.isEmpty() ? null : records.get(0);
    }

    private StructureDefinitionDO toDefinitionDO(StructureDefinition definition) {
        // 创建数据库记录对象，承载即将写入的业务字段。
        StructureDefinitionDO record = new StructureDefinitionDO();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setId(definition.getId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setName(definition.getName());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setDescription(definition.getDescription());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setActiveVersionNo(definition.getActiveVersionNo());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setStatus(definition.getStatus());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setCreateTime(mapTime(definition.getCreateTime()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setUpdateTime(mapTime(definition.getUpdateTime()));
        // 返回已经完成封装的业务结果。
        return record;
    }

    private StructureVersionDOWithBLOBs toVersionDO(StructureVersion version) {
        // 创建当前流程需要的临时对象，承载后续处理数据。
        StructureVersionDOWithBLOBs record = new StructureVersionDOWithBLOBs();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setVersionNo(version.getVersionNo());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setStructureModel(jsonCodec.writeModel(version.getStructureModel()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setGenerationConfig(jsonCodec.normalizeJson(version.getGenerationConfig()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setFieldOverrides(jsonCodec.normalizeJson(version.getFieldOverrides()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setArtifacts(jsonCodec.writeArtifacts(version.getArtifacts()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setWarnings(jsonCodec.writeWarnings(version.getWarnings()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setValidationMode(version.getValidationMode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setSampleDigest(version.getSampleDigest());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setCreateTime(mapTime(version.getCreateTime()));
        // 返回已经完成封装的业务结果。
        return record;
    }

    private StructureDraftDOWithBLOBs toDraftDO(StructureDraft draft) {
        // 创建当前流程需要的临时对象，承载后续处理数据。
        StructureDraftDOWithBLOBs record = new StructureDraftDOWithBLOBs();
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setStructureModel(jsonCodec.writeModel(draft.getStructureModel()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setGenerationConfig(jsonCodec.normalizeJson(draft.getGenerationConfig()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setFieldOverrides(jsonCodec.normalizeJson(draft.getFieldOverrides()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setArtifacts(jsonCodec.writeArtifacts(draft.getArtifacts()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setWarnings(jsonCodec.writeWarnings(draft.getWarnings()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setValidationMode(draft.getValidationMode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setSampleDigest(draft.getSampleDigest());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        Date updateTime = mapTime(draft.getUpdateTime());
        record.setCreateTime(updateTime);
        record.setUpdateTime(updateTime);
        // 返回已经完成封装的业务结果。
        return record;
    }

    private StructureVersion toDomainVersion(StructureVersionDOWithBLOBs entity) {
        // 返回已经完成封装的业务结果。
        return new StructureVersion(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getVersionNo(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                jsonCodec.readModel(entity.getStructureModel()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                jsonCodec.normalizeJson(entity.getGenerationConfig()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                jsonCodec.normalizeJson(entity.getFieldOverrides()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                readVersionArtifacts(entity),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                jsonCodec.readWarnings(entity.getWarnings()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getValidationMode(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getSampleDigest(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                mapTime(entity.getCreateTime())
        );
    }

    private Map<String, String> readVersionArtifacts(StructureVersionDOWithBLOBs entity) {
        try {
            return jsonCodec.readArtifacts(entity.getArtifacts());
        } catch (IllegalStateException aggregateFailure) {
            Map<String, String> artifacts = new LinkedHashMap<>();
            if (entity.getId() != null) {
                for (StructureArtifactDO artifact : artifactMapper.selectByExample(
                        artifactByVersion(entity.getTenantId(), entity.getId())
                )) {
                    if (artifact.getArtifactKey() != null && artifact.getArtifactJson() != null) {
                        artifacts.put(artifact.getArtifactKey(), artifact.getArtifactJson());
                    }
                }
            }
            if (!artifacts.isEmpty()) {
                return artifacts;
            }
            throw aggregateFailure;
        }
    }

    private StructureDraft toDomainDraft(StructureDraftDOWithBLOBs entity) {
        // 返回已经完成封装的业务结果。
        return new StructureDraft(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                jsonCodec.readModel(entity.getStructureModel()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                jsonCodec.normalizeJson(entity.getGenerationConfig()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                jsonCodec.normalizeJson(entity.getFieldOverrides()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                jsonCodec.readArtifacts(entity.getArtifacts()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                jsonCodec.readWarnings(entity.getWarnings()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getValidationMode(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getSampleDigest(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                mapTime(entity.getUpdateTime() == null ? entity.getCreateTime() : entity.getUpdateTime())
        );
    }

    private Instant mapTime(Date value) {
        return value == null ? null : value.toInstant();
    }

    private Date mapTime(Instant value) {
        return value == null ? null : Date.from(value);
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
