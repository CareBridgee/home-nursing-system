package iti.jets.java.homenursing.testutil;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Full-context integration test base.
 *
 * <p>Boots pgvector + redis containers (same images as compose.yaml) and wires them via
 * {@link ServiceConnection}. The containers are started ONCE per JVM in a static block
 * (not via the JUnit @Container extension, which would stop them per test class and
 * break the shared Spring context cache - see .idea/TESTING.md Phase 3 troubleshooting).
 * The AI {@link VectorStore} and {@link ChatClient} are mocked so the boot-time FAQ
 * ingestion runner (see .idea/TESTING.md, Phase 0) completes harmlessly; AI-specific
 * tests re-provide real beans via @TestConfiguration.
 */
@ActiveProfiles("test")
@SpringBootTest
public abstract class BaseIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("pgvector/pgvector:pg16");

    @ServiceConnection
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379).withCommand("redis-server");

    static {
        POSTGRES.start();
        REDIS.start();
    }

    @MockitoBean
    protected VectorStore vectorStore;

    @MockitoBean
    protected ChatClient chatClient;
}
