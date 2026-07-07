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

    @Override
    public StructureDefinition save(StructureDefinition definition) {
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long tenantId = currentTenantId();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureDefinitionDO entity = toDefinitionDO(definition);
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        entity.setTenantId(tenantId);
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (entity.getId() == null || definitionMapper.selectByPrimaryKey(entity.getId()) == null) {
            // 将当前业务变更写入持久化层，保持数据状态同步。
            definitionMapper.insertSelective(entity);
        } else {
            // 将当前业务变更写入持久化层，保持数据状态同步。
            definitionMapper.updateByPrimaryKeySelective(entity);
        }
        // 计算当前步骤所需的中间值，供后续业务判断使用。
        Long definitionId = entity.getId();
        // 将当前业务变更写入持久化层，保持数据状态同步。
        draftMapper.deleteByExample(draftByDefinition(tenantId, definitionId));
        // 先处理空值或缺省场景，避免后续业务流程出现空指针。
        if (definition.getDraft() != null) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            StructureDraftDOWithBLOBs draftEntity = toDraftDO(definition.getDraft());
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            draftEntity.setTenantId(tenantId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            draftEntity.setDefinitionId(definitionId);
            // 将当前业务变更写入持久化层，保持数据状态同步。
            draftMapper.insertSelective(draftEntity);
        }
        // 将当前业务变更写入持久化层，保持数据状态同步。
        artifactMapper.deleteByExample(artifactByDefinition(tenantId, definitionId));
        // 将当前业务变更写入持久化层，保持数据状态同步。
        versionMapper.deleteByExample(versionByDefinition(tenantId, definitionId));
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureVersion version : definition.getVersions()) {
            // 计算当前分支的中间结果，供后续判断或组装使用。
            StructureVersionDOWithBLOBs versionEntity = toVersionDO(version);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            versionEntity.setTenantId(tenantId);
            // 设置持久化字段，保证数据库记录具备完整业务属性。
            versionEntity.setDefinitionId(definitionId);
            // 将当前业务变更写入持久化层，保持数据状态同步。
            versionMapper.insertSelective(versionEntity);
            // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
            insertArtifacts(tenantId, definitionId, versionEntity.getId(), version);
        }
        // 返回已经完成封装的业务结果。
        return findById(definitionId).orElse(definition);
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
        // 创建查询条件对象，后续通过 Criteria 精确约束查询范围。
        StructureDefinitionDOExample example = new StructureDefinitionDOExample();
        // 组装查询条件，确保 Mapper 只读取当前业务需要的数据。
        example.createCriteria().andTenantIdEqualTo(currentTenantId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("id DESC");
        // 返回已经完成封装的业务结果。
        return definitionMapper.selectByExample(example)
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .stream()
                // 使用流式转换批量映射数据，减少中间状态暴露。
                .map(this::toDomain)
                // 使用流式转换批量映射数据，减少中间状态暴露。
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
        // 创建结果集合，承接后续逐项组装的数据。
        List<StructureVersion> versions = new ArrayList<>();
        // 计算当前分支的中间结果，供后续判断或组装使用。
        StructureVersionDOExample example = versionByDefinition(entity.getTenantId(), entity.getId());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        example.setOrderByClause("version_no ASC");
        // 逐条遍历集合数据，完成业务结果组装或状态处理。
        for (StructureVersionDOWithBLOBs version : versionMapper.selectByExampleWithBLOBs(example)) {
            // 加入当前处理结果，供后续批量返回或继续组装。
            versions.add(toDomainVersion(version));
        }
        // 返回已经完成封装的业务结果。
        return new StructureDefinition(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getId(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getName(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getDescription(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getActiveVersionNo(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getStatus(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                readDraft(entity.getTenantId(), entity.getId()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                versions,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                mapTime(entity.getCreateTime()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                mapTime(entity.getUpdateTime())
        );
    }

    private StructureDraft readDraft(Long tenantId, Long definitionId) {
        StructureDraftDOWithBLOBs entity = first(draftMapper.selectByExampleWithBLOBs(draftByDefinition(tenantId, definitionId)));
        if (entity == null) {
            return null;
        }
        return toDomainDraft(entity);
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

    private StructureArtifactDOExample artifactByDefinition(Long tenantId, Long definitionId) {
        StructureArtifactDOExample example = new StructureArtifactDOExample();
        example.createCriteria()
                .andTenantIdEqualTo(tenantId)
                .andDefinitionIdEqualTo(definitionId);
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
        record.setStructureModel(toJson(version.getStructureModel()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setGenerationConfig(toJson(version.getGenerationConfig()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setFieldOverrides(toJson(version.getFieldOverrides()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setArtifacts(toJson(version.getArtifacts()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setWarnings(toJson(version.getWarnings()));
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
        record.setStructureModel(toJson(draft.getStructureModel()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setGenerationConfig(toJson(draft.getGenerationConfig()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setFieldOverrides(toJson(draft.getFieldOverrides()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setArtifacts(toJson(draft.getArtifacts()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setWarnings(toJson(draft.getWarnings()));
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setValidationMode(draft.getValidationMode());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setSampleDigest(draft.getSampleDigest());
        // 设置持久化字段，保证数据库记录具备完整业务属性。
        record.setCreateTime(mapTime(draft.getUpdateTime()));
        // 返回已经完成封装的业务结果。
        return record;
    }

    private StructureVersion toDomainVersion(StructureVersionDOWithBLOBs entity) {
        // 返回已经完成封装的业务结果。
        return new StructureVersion(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getVersionNo(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                null,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getGenerationConfig(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getFieldOverrides(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseArtifacts(entity.getArtifacts()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseWarnings(entity.getWarnings()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getValidationMode(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getSampleDigest(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                mapTime(entity.getCreateTime())
        );
    }

    private StructureDraft toDomainDraft(StructureDraftDOWithBLOBs entity) {
        // 返回已经完成封装的业务结果。
        return new StructureDraft(
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                null,
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getGenerationConfig(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getFieldOverrides(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseArtifacts(entity.getArtifacts()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                parseWarnings(entity.getWarnings()),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getValidationMode(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                entity.getSampleDigest(),
                // 承接上一行判断后的处理动作，保持当前业务分支语义完整。
                mapTime(entity.getCreateTime())
        );
    }

    private Instant mapTime(Date value) {
        return value == null ? null : value.toInstant();
    }

    private Date mapTime(Instant value) {
        return value == null ? null : Date.from(value);
    }

    private String toJson(Object value) {
        // 进入可能失败的处理区间，后续异常会统一转换为业务可理解的结果。
        try {
            // 返回已经完成封装的业务结果。
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception ex) {
            // 返回已经完成封装的业务结果。
            return "null";
        }
    }

    private Map<String, String> parseArtifacts(String value) {
        return new HashMap<>();
    }

    private List<top.kx.heartbeat.domain.structure.model.InferenceWarning> parseWarnings(String value) {
        return new ArrayList<>();
    }

    private Long currentTenantId() {
        Long tenantId = TenantContext.getTenantId();
        return tenantId == null ? 1L : tenantId;
    }
}
