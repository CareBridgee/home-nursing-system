package iti.jets.java.homenursing.service.impl;

import iti.jets.java.homenursing.dto.chat.ReservationDraft;
import iti.jets.java.homenursing.entity.ServiceType;
import iti.jets.java.homenursing.repository.ServiceTypeRepository;
import iti.jets.java.homenursing.service.ChatDraftService;
import iti.jets.java.homenursing.util.ServiceBriefBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class ChatDraftServiceImpl implements ChatDraftService {

    private static final Logger log = LoggerFactory.getLogger(ChatDraftServiceImpl.class);

    private static final class DraftState {
        UUID serviceTypeId;
        String serviceTypeName;
        String serviceDescription;
        boolean urgent;
        String urgencyLevel;
        String urgencyReason;
    }

    private final ConcurrentMap<UUID, DraftState> drafts = new ConcurrentHashMap<>();

    private final ServiceTypeRepository serviceTypeRepository;
    private final ServiceBriefBuilder serviceBriefBuilder;

    public ChatDraftServiceImpl(ServiceTypeRepository serviceTypeRepository,
                                ServiceBriefBuilder serviceBriefBuilder) {
        this.serviceTypeRepository = serviceTypeRepository;
        this.serviceBriefBuilder = serviceBriefBuilder;
    }

    @Override
    public ReservationDraft getDraft(UUID profileId) {
        DraftState state = drafts.get(profileId);
        if (state == null) {
            return ReservationDraft.empty();
        }
        return toDraft(state);
    }

    @Override
    public void updateField(UUID profileId, String field, String value) {
        if (field == null || field.isBlank() || value == null || value.isBlank()) {
            throw new IllegalArgumentException("field and value must not be blank");
        }
        DraftState state = drafts.computeIfAbsent(profileId, k -> new DraftState());
        switch (field) {
            case "serviceTypeId" -> {
                validateServiceType(state, value.trim());
                state.serviceDescription = serviceBriefBuilder.build(profileId, state.serviceTypeName);
            }
            default -> throw new IllegalArgumentException("Unknown draft field: " + field);
        }
        log.debug("Draft updated for profile {} field {}: {}", profileId, field, value);
    }

    private void validateServiceType(DraftState state, String raw) {
        UUID serviceTypeId;
        try {
            serviceTypeId = UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid service type id: " + raw);
        }
        ServiceType serviceType = serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown service type id: " + raw));
        state.serviceTypeId = serviceType.getId();
        state.serviceTypeName = serviceType.getName();
    }

    @Override
    public boolean isUrgent(UUID profileId) {
        DraftState state = drafts.get(profileId);
        return state != null && state.urgent;
    }

    @Override
    public String urgencyLevel(UUID profileId) {
        DraftState state = drafts.get(profileId);
        return state == null ? null : state.urgencyLevel;
    }

    @Override
    public void setUrgency(UUID profileId, boolean urgent, String level, String reason) {
        DraftState state = drafts.computeIfAbsent(profileId, k -> new DraftState());
        state.urgent = urgent;
        state.urgencyLevel = level;
        state.urgencyReason = reason;
        log.debug("Urgency set for profile {}: urgent={} level={} reason={}", profileId, urgent, level, reason);
    }

    @Override
    public void reset(UUID profileId) {
        drafts.remove(profileId);
    }

    private static ReservationDraft toDraft(DraftState state) {
        return new ReservationDraft(
                state.serviceTypeId,
                state.serviceTypeName,
                state.serviceDescription,
                isComplete(state));
    }

    private static boolean isComplete(DraftState state) {
        return state.serviceTypeId != null;
    }
}
