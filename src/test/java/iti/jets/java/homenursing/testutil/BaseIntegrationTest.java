package iti.jets.java.homenursing.testutil;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Full-context integration test base.
 *
 * <p>Boots pgvector + redis containers (same images as compose.yaml) and wires them via
 * {@link ServiceConnection}. The AI {@link VectorStore} and {@link ChatClient} are mocked
 * so the boot-time FAQ ingestion runner (see .idea/TESTING.md, Phase 0) completes
 * harmlessly; AI-specific tests re-provide real beans via @TestConfiguration.
 */
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @Container
    @ServiceConnection
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379).withCommand("redis-server");

    @MockitoBean
    protected VectorStore vectorStore;

    @MockitoBean
    protected ChatClient chatClient;
}
