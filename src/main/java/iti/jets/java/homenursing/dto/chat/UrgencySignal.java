package iti.jets.java.homenursing.dto.chat;

public record UrgencySignal(
        boolean urgent,
        String level,
        String advice
) {
    public static UrgencySignal none() {
        return new UrgencySignal(false, null, null);
    }
}