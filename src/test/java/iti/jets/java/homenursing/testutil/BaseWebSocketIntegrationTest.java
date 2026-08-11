package iti.jets.java.homenursing.testutil;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Integration base for WebSocket tests: containers (inherited from
 * {@link BaseIntegrationTest}) + a real server on a random port so STOMP clients work.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseWebSocketIntegrationTest extends BaseIntegrationTest {
}
