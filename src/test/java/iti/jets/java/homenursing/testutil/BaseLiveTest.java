package iti.jets.java.homenursing.testutil;

import org.junit.jupiter.api.Tag;

/**
 * Base for Java "live" tests — the Java mirror of the .idea/e2e .mjs suites.
 * Runs against a running server (docker image or remote deployment); target is read
 * from the same env vars used by .idea/e2e/run-all.mjs.
 */
@Tag("live")
public abstract class BaseLiveTest {

    protected static final String BASE = env("E2E_BASE",
            env("E2E_BASE_HTTP", env("BASE_URL", "http://127.0.0.1:8080")));
    protected static final String WS_BASE = BASE.replace("http", "ws") + "/ws";
    protected static final String NURSE_PHONE = env("E2E_NURSE_PHONE",
            env("NURSE_PHONE", "+2015113753480"));
    protected static final String ADMIN_KEY = env("E2E_ADMIN_KEY", "");
    protected static final String SERVICE_NAME = env("E2E_SERVICE_NAME", "General Nursing");
    protected static final long TIMEOUT_MS = Long.parseLong(env("E2E_TIMEOUT_MS", "6000"));

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return (value == null || value.isBlank()) ? fallback : value;
    }

    /** Mirrors the fresh-phone rule of e2e.mjs: +20 + time-derived digits, no collisions. */
    protected static String freshPhone() {
        return "+20" + (System.nanoTime() % 10_000_000_000L);
    }
}
