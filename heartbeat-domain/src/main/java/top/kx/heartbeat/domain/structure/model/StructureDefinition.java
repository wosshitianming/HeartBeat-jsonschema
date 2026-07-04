package top.kx.heartbeat.domain.structure.model;

import lombok.AccessLevel;
import lombok.Getter;
import top.kx.heartbeat.domain.common.exception.DomainException;
import top.kx.heartbeat.domain.structure.StructureErrorCode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 可复用结构定义聚合。
 */
@Getter
public class StructureDefinition {

    private final Long id;
    private final String name;
    private final String description;
    @Getter(AccessLevel.NONE)
    private final List<StructureVersion> versions;
    private Integer activeVersionNo;
    private String status;
    private StructureDraft draft;
    private final Instant createTime;
    private Instant updateTime;

    public StructureDefinition(Long id,
                               String name,
                               String description,
                               Integer activeVersionNo,
                               String status,
                               StructureDraft draft,
                               List<StructureVersion> versions,
                               Instant createTime,
                               Instant updateTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.activeVersionNo = activeVersionNo;
        this.status = status == null ? "DRAFT" : status;
        this.draft = draft;
        this.versions = new ArrayList<>(versions);
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public static StructureDefinition create(String name, String description, Instant now) {
        return new StructureDefinition(null, name, description, null, "DRAFT", null, new ArrayList<StructureVersion>(), now, now);
    }

    public void addVersion(StructureVersion version) {
        if (version.getVersionNo() != nextVersionNo()) {
            throw new IllegalArgumentException("版本号必须连续递增");
        }
        versions.add(version);
        if (activeVersionNo == null) {
            status = "SAVED";
        }
        updateTime = version.getCreateTime();
    }

    public void activate(int versionNo, Instant now) {
        getVersion(versionNo);
        activeVersionNo = versionNo;
        status = "ONLINE";
        updateTime = now;
    }

    public void saveDraft(StructureDraft draft) {
        this.draft = draft;
        if (activeVersionNo == null) {
            status = "DRAFT";
        }
        updateTime = draft.getUpdateTime();
    }

    public void clearDraft(Instant now) {
        this.draft = null;
        updateTime = now;
    }

    public StructureVersion getVersion(int versionNo) {
        for (StructureVersion version : versions) {
            if (version.getVersionNo() == versionNo) {
                return version;
            }
        }
        throw new DomainException(
                StructureErrorCode.STRUCTURE_VERSION_NOT_FOUND,
                "结构版本不存在: " + versionNo
        );
    }

    public StructureVersion getActiveVersion() {
        if (activeVersionNo == null) {
            throw new DomainException(
                    StructureErrorCode.STRUCTURE_ACTIVE_VERSION_MISSING,
                    "结构定义尚未启用版本"
            );
        }
        return getVersion(activeVersionNo);
    }

    public int nextVersionNo() {
        return versions.size() + 1;
    }

    public List<StructureVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }
}
