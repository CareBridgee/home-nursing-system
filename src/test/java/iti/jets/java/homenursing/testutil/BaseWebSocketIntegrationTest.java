package iti.jets.java.homenursing.testutil;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Integration base for WebSocket tests: containers (inherited from
 * {@link BaseIntegrationTest}) + HTTP/WS API helpers (inherited from
 * {@link ApiIntegrationTestBase}) + a real server on a random port so STOMP clients work.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseWebSocketIntegrationTest extends ApiIntegrationTestBase {

    @LocalServerPort
    protected int port;

    protected String wsUrl() {
        return "ws://localhost:" + port + "/ws";
    }
}
