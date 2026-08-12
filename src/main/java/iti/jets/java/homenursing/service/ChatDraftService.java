package iti.jets.java.homenursing.service;

import iti.jets.java.homenursing.dto.chat.ReservationDraft;
import iti.jets.java.homenursing.dto.chat.UrgencySignal;

import java.util.UUID;

public interface ChatDraftService {

    ReservationDraft getDraft(UUID profileId);

    void updateField(UUID profileId, String field, String value);

    boolean isUrgent(UUID profileId);

    String urgencyLevel(UUID profileId);

    void setUrgency(UUID profileId, boolean urgent, String level, String reason);

    void reset(UUID profileId);

    void clearServiceType(UUID profileId);
}