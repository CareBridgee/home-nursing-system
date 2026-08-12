package iti.jets.java.homenursing.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NurseOfferStatusConstraintMigrationTest {

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final NurseOfferStatusConstraintMigration migration =
            new NurseOfferStatusConstraintMigration(jdbcTemplate);

    private static final String WITH_WITHDRAWN = """
            CHECK ((status)::text = ANY ((ARRAY['PENDING'::text, 'ACCEPTED'::text,
            'REJECTED'::text, 'WITHDRAWN'::text])::text[]))""";

    private static final String WITHOUT_WITHDRAWN = """
            CHECK ((status)::text = ANY ((ARRAY['PENDING'::text, 'ACCEPTED'::text,
            'REJECTED'::text])::text[]))""";

    @Test
    void run_withoutConstraintFound_doesNothing() {
        when(jdbcTemplate.queryForList(anyString(), anyString())).thenReturn(List.of());
        migration.run(mock(ApplicationArguments.class));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void run_whenConstraintAlreadyHasWithdrawn_doesNothing() {
        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(List.of(Map.of("def", WITH_WITHDRAWN)));
        migration.run(mock(ApplicationArguments.class));
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    void run_whenConstraintMissingWithdrawn_repairsConstraint() {
        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenReturn(List.of(Map.of("def", WITHOUT_WITHDRAWN)));
        migration.run(mock(ApplicationArguments.class));
        verify(jdbcTemplate, org.mockito.Mockito.times(2)).execute(anyString());
    }

    @Test
    void run_whenDatabaseFails_swallowsException() {
        when(jdbcTemplate.queryForList(anyString(), anyString()))
                .thenThrow(new RuntimeException("connection lost"));
        assertDoesNotThrow(() -> migration.run(mock(ApplicationArguments.class)));
    }
}