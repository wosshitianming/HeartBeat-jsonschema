package top.kx.heartbeat.infrastructure.structure.repository;


import org.springframework.stereotype.Repository;
import top.kx.heartbeat.domain.structure.model.StructureDefinition;
import top.kx.heartbeat.domain.structure.model.StructureDraft;
import top.kx.heartbeat.domain.structure.model.StructureVersion;
import top.kx.heartbeat.domain.structure.repository.StructureDefinitionRepository;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.StructureArtifactDO;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.StructureArtifactDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.StructureDefinitionDO;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.StructureDefinitionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.StructureDraftDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.StructureDraftDOWithBLOBs;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.StructureVersionDOExample;
import top.kx.heartbeat.infrastructure.persistence.entity.structure.StructureVersionDOWithBLOBs;
import top.kx.heartbeat.infrastructure.persistence.mapper.structure.StructureArtifactDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.structure.StructureDefinitionDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.structure.StructureDraftDOMapper;
import top.kx.heartbeat.infrastructure.persistence.mapper.structure.StructureVersionDOMapper;
import top.kx.heartbeat.infrastructure.tenant.TenantContext;

import javax.annotation.Resource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        Long tenantId = currentTenantId();
        StructureDefinitionDO entity = toDefinitionDO(definition);
        entity.setTenantId(tenantId);
        if (entity.getId() == null || definitionMapper.selectByPrimaryKey(entity.getId()) == null) {
            definitionMapper.insertSelective(entity);
        } else {
            definitionMapper.updateByPrimaryKeySelective(entity);
        }
        Long definitionId = entity.getId();
        draftMapper.deleteByExample(draftByDefinition(tenantId, definitionId));
        if (definition.getDraft() != null) {
            StructureDraftDOWithBLOBs draftEntity = toDraftDO(definition.getDraft());
            draftEntity.setTenantId(tenantId);
            draftEntity.setDefinitionId(definitionId);
            draftMapper.insertSelective(draftEntity);
        }
        artifactMapper.deleteByExample(artifactByDefinition(tenantId, definitionId));
        versionMapper.deleteByExample(versionByDefinition(tenantId, definitionId));
        for (StructureVersion version : definition.getVersions()) {
            StructureVersionDOWithBLOBs versionEntity = toVersionDO(version);
            versionEntity.setTenantId(tenantId);
            versionEntity.setDefinitionId(definitionId);
            versionMapper.insertSelective(versionEntity);
            insertArtifacts(tenantId, definitionId, versionEntity.getId(), version);
        }
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
        StructureDefinitionDOExample example = new StructureDefinitionDOExample();
        example.createCriteria().andTenantIdEqualTo(currentTenantId());
        example.setOrderByClause("id DESC");
        return definitionMapper.selectByExample(example)
                .stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private void insertArtifacts(Long tenantId, Long definitionId, Long versionId, StructureVersion version) {
        if (versionId == null) {
            return;
        }
        for (Map.Entry<String, String> artifact : version.getArtifacts().entrySet()) {
            StructureArtifactDO entity = new StructureArtifactDO();
            entity.setTenantId(tenantId);
            entity.setDefinitionId(definitionId);
            entity.setVersionId(versionId);
            entity.setArtifactKey(artifact.getKey());
            entity.setArtifactJson(artifact.getValue());
            entity.setCreateTime(mapTime(version.getCreateTime()));
            artifactMapper.insertSelective(entity);
        }
    }

    private StructureDefinition toDomain(StructureDefinitionDO entity) {
        List<StructureVersion> versions = new ArrayList<>();
        StructureVersionDOExample example = versionByDefinition(entity.getTenantId(), entity.getId());
        example.setOrderByClause("version_no ASC");
        for (StructureVersionDOWithBLOBs version : versionMapper.selectByExampleWithBLOBs(example)) {
            versions.add(toDomainVersion(version));
        }
        return new StructureDefinition(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getActiveVersionNo(),
                entity.getStatus(),
                readDraft(entity.getTenantId(), entity.getId()),
                versions,
                mapTime(entity.getCreateTime()),
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
        StructureDefinitionDO record = new StructureDefinitionDO();
        record.setId(definition.getId());
        record.setName(definition.getName());
        record.setDescription(definition.getDescription());
        record.setActiveVersionNo(definition.getActiveVersionNo());
        record.setStatus(definition.getStatus());
        record.setCreateTime(mapTime(definition.getCreateTime()));
        record.setUpdateTime(mapTime(definition.getUpdateTime()));
        return record;
    }

    private StructureVersionDOWithBLOBs toVersionDO(StructureVersion version) {
        StructureVersionDOWithBLOBs record = new StructureVersionDOWithBLOBs();
        record.setVersionNo(version.getVersionNo());
        record.setStructureModel(toJson(version.getStructureModel()));
        record.setGenerationConfig(toJson(version.getGenerationConfig()));
        record.setFieldOverrides(toJson(version.getFieldOverrides()));
        record.setArtifacts(toJson(version.getArtifacts()));
        record.setWarnings(toJson(version.getWarnings()));
        record.setValidationMode(version.getValidationMode());
        record.setSampleDigest(version.getSampleDigest());
        record.setCreateTime(mapTime(version.getCreateTime()));
        return record;
    }

    private StructureDraftDOWithBLOBs toDraftDO(StructureDraft draft) {
        StructureDraftDOWithBLOBs record = new StructureDraftDOWithBLOBs();
        record.setStructureModel(toJson(draft.getStructureModel()));
        record.setGenerationConfig(toJson(draft.getGenerationConfig()));
        record.setFieldOverrides(toJson(draft.getFieldOverrides()));
        record.setArtifacts(toJson(draft.getArtifacts()));
        record.setWarnings(toJson(draft.getWarnings()));
        record.setValidationMode(draft.getValidationMode());
        record.setSampleDigest(draft.getSampleDigest());
        record.setCreateTime(mapTime(draft.getUpdateTime()));
        return record;
    }

    private StructureVersion toDomainVersion(StructureVersionDOWithBLOBs entity) {
        return new StructureVersion(
                entity.getVersionNo(),
                null,
                entity.getGenerationConfig(),
                entity.getFieldOverrides(),
                parseArtifacts(entity.getArtifacts()),
                parseWarnings(entity.getWarnings()),
                entity.getValidationMode(),
                entity.getSampleDigest(),
                mapTime(entity.getCreateTime())
        );
    }

    private StructureDraft toDomainDraft(StructureDraftDOWithBLOBs entity) {
        return new StructureDraft(
                null,
                entity.getGenerationConfig(),
                entity.getFieldOverrides(),
                parseArtifacts(entity.getArtifacts()),
                parseWarnings(entity.getWarnings()),
                entity.getValidationMode(),
                entity.getSampleDigest(),
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
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (Exception ex) {
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
