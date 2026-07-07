package top.kx.heartbeat.application.mp.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.mp.request.MpMaterialRequest;

import java.util.List;

/**
 * 定义公众号管理持久化端口，隔离应用层与具体数据访问实现。
 */
public interface MpMaterialRepository {


    List<DomainRecord> listMaterials(String accountId);


    DomainRecord saveMaterial(MpMaterialRequest request);
}
