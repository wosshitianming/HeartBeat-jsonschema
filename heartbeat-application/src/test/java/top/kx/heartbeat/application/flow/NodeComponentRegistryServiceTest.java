package top.kx.heartbeat.application.flow;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import top.kx.heartbeat.application.flow.runtime.NodeExecutorRegistry;
import top.kx.heartbeat.application.flow.runtime.node.UppercaseCodeNode;
import top.kx.heartbeat.domain.flow.model.ComponentRuntime;
import top.kx.heartbeat.domain.flow.model.NodeComponentManifest;
import top.kx.heartbeat.domain.flow.model.NodeComponentStatus;
import top.kx.heartbeat.domain.flow.repository.NodeComponentRepository;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NodeComponentRegistryServiceTest {

    @Mock
    private NodeComponentRepository repository;

    @Mock
    private NodeExecutorRegistry nodeExecutorRegistry;

    @InjectMocks
    private NodeComponentRegistryService service;

    @Test
    void includesBuiltInsAndCodeNodesWhenDatabaseIsEmpty() {
        NodeComponentManifest codeManifest = new UppercaseCodeNode().manifest();
        when(repository.findAllActive()).thenReturn(Collections.emptyList());
        when(nodeExecutorRegistry.codeManifests()).thenReturn(Collections.singletonList(codeManifest));

        List<NodeComponentManifest> manifests = service.listActive();

        assertNotNull(find(manifests, "trigger.manual", "1.0.0"));
        assertNotNull(find(manifests, "transform.text.uppercase", "1.0.0"));
    }

    @Test
    void codeManifestOverridesDatabaseCopyWithSameTypeAndVersion() {
        NodeComponentManifest database = new NodeComponentManifest();
        database.setType("transform.text.uppercase");
        database.setVersion("1.0.0");
        database.setName("stale database copy");
        database.setStatus(NodeComponentStatus.ACTIVE.getCode());
        ComponentRuntime staleRuntime = new ComponentRuntime();
        staleRuntime.setExecutor("database:stale");
        database.setRuntime(staleRuntime);
        NodeComponentManifest codeManifest = new UppercaseCodeNode().manifest();
        when(repository.findAllActive()).thenReturn(Collections.singletonList(database));
        when(nodeExecutorRegistry.codeManifests()).thenReturn(Collections.singletonList(codeManifest));

        NodeComponentManifest resolved = find(service.listActive(), "transform.text.uppercase", "1.0.0");

        assertNotNull(resolved);
        assertEquals("文本转大写", resolved.getName());
        assertEquals("code:transform.text.uppercase", resolved.getRuntime().getExecutor());
    }

    private NodeComponentManifest find(List<NodeComponentManifest> manifests, String type, String version) {
        return manifests.stream()
                .filter(item -> type.equals(item.getType()) && version.equals(item.getVersion()))
                .findFirst()
                .orElse(null);
    }
}
