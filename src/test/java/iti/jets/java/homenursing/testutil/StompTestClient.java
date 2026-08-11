package iti.jets.java.homenursing.testutil;

import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.io.Closeable;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * STOMP test client for integration (real server, RANDOM_PORT) and live tests.
 * Connects to {@code wsUrl} with a Bearer token; captures frames per subscription.
 */
public class StompTestClient implements Closeable {

    private final WebSocketStompClient stomp =
            new WebSocketStompClient(new StandardWebSocketClient());
    private final String wsUrl;
    private final String bearer;
    private final List<String> errors = Collections.synchronizedList(new ArrayList<>());
    private final List<Subscription> subscriptions = new ArrayList<>();
    private StompSession session;

    public record Captured(String destination, String payload) {
    }

    private static final class Subscription {
        private final String id;
        private final String destination;
        private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        private Subscription(String id, String destination) {
            this.id = id;
            this.destination = destination;
        }
    }

    public StompTestClient(String wsUrl, String bearer) {
        this.wsUrl = wsUrl;
        this.bearer = bearer;
        stomp.setMessageConverter(new StringMessageConverter());
    }

    /** Connects; throws if CONNECTED is not received within the budget. */
    public StompTestClient connect(Duration timeout) {
        StompHeaders headers = new StompHeaders();
        if (bearer != null) {
            headers.add("Authorization", "Bearer " + bearer);
        }
        StompSessionHandlerAdapter handler = new StompSessionHandlerAdapter() {
            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                errors.add("transport: " + exception.getMessage());
            }

            @Override
            public void handleException(StompSession session, StompCommand command,
                                        StompHeaders headers, byte[] payload, Throwable exception) {
                errors.add("exception: " + exception.getMessage());
            }
        };
        try {
            session = stomp.connectAsync(wsUrl, new WebSocketHttpHeaders(), headers, handler)
                    .get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new IllegalStateException("STOMP connect failed to " + wsUrl + ": " + e.getMessage(), e);
        }
        return this;
    }

    /** Subscribes; captured messages can be awaited via {@link #awaitMessage}. */
    public String subscribe(String destination) {
        Subscription sub = new Subscription("s" + (subscriptions.size() + 1), destination);
        subscriptions.add(sub);
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                sub.messages.add(payload == null ? null : payload.toString());
            }
        });
        return sub.id;
    }

    /** Sends a raw string payload to the given destination. */
    public void send(String destination, String payload) {
        session.send(destination, payload);
    }

    public void send(String destination, Object payload) {
        session.send(destination, payload);
    }

    /** Waits until at least one message arrives on the subscription (within budget). */
    public String awaitMessage(String subscriptionId, Duration timeout) {
        Subscription sub = find(subscriptionId);
        try {
            String msg = sub.messages.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (msg == null) {
                throw new AssertionError("No message on subscription " + subscriptionId
                        + " (errors: " + errors + ")");
            }
            return msg;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted waiting for message", e);
        }
    }

    /** Waits (Awaitility) until a message arrives on any subscription. */
    public Captured awaitAnyMessage(Duration timeout) {
        await().atMost(timeout).until(() -> subscriptions.stream().anyMatch(s -> !s.messages.isEmpty()));
        for (Subscription sub : subscriptions) {
            String msg = sub.messages.poll();
            if (msg != null) {
                return new Captured(sub.destination, msg);
            }
        }
        throw new AssertionError("No message captured");
    }

    public boolean isConnected() {
        return session != null && session.isConnected();
    }

    public List<String> errors() {
        return List.copyOf(errors);
    }

    public void disconnect() {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    private Subscription find(String id) {
        return subscriptions.stream()
                .filter(s -> s.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Unknown subscription id: " + id));
    }
}
