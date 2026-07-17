package top.kx.heartbeat.infrastructure.flow.repository;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowRunRuntimeMapperXmlTest {

    @Test
    void runtimeMapperXmlParsesAndRegistersStatements() throws Exception {
        String resource = "mapper/flow/FlowRunRuntimeMapper.xml";
        Configuration configuration = new Configuration();
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        assertTrue(configuration.hasStatement(
                "top.kx.heartbeat.infrastructure.persistence.mapper.flow.FlowRunRuntimeMapper.summarize"));
        assertTrue(configuration.hasStatement(
                "top.kx.heartbeat.infrastructure.persistence.mapper.flow.FlowRunRuntimeMapper.updateRunRuntime"));
    }
}
