package top.kx.heartbeat.domain.tool;

import top.kx.heartbeat.domain.tool.model.GeneratedColumn;
import top.kx.heartbeat.domain.tool.model.GeneratedTable;

import java.util.List;

public interface CodegenMetadataRepository {

    List<GeneratedTable> findAllTables();

    GeneratedTable saveTable(GeneratedTable table);

    void replaceColumns(Long tableId, List<GeneratedColumn> columns);
}
