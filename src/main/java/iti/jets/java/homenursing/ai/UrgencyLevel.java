package iti.jets.java.homenursing.ai;

/**
 * Supported urgency levels the assistant can record via the setUrgency tool.
 * The value is echoed back to the client in the chat response's urgency signal.
 */
public enum UrgencyLevel {
    HOSPITALIZATION,
    EMERGENCY
}