package top.kx.heartbeat.application.mp.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.request.MpMaterialRequest;

import java.util.List;

public interface MpMaterialRepository {

    List<DomainRecord> listMaterials(String accountId);

    DomainRecord saveMaterial(MpMaterialRequest request);
}
