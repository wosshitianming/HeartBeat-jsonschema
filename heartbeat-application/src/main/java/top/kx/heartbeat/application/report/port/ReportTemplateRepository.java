package top.kx.heartbeat.application.report.port;

import top.kx.heartbeat.application.common.model.DomainRecord;
import top.kx.heartbeat.application.report.request.ReportTemplateRequest;

import java.util.List;

public interface ReportTemplateRepository {

    List<DomainRecord> listTemplates();

    DomainRecord saveTemplate(ReportTemplateRequest request);
}
