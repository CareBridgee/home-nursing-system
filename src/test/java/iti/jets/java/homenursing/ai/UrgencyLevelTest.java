package iti.jets.java.homenursing.ai;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

@Tag("unit")
class UrgencyLevelTest {

    @Test
    void exposesExactlyTheSupportedLevelsInOrder() {
        assertThat(UrgencyLevel.values()).containsExactly(
                UrgencyLevel.HOSPITALIZATION,
                UrgencyLevel.EMERGENCY);
    }

    @Test
    void valueOfResolvesEachDeclaredConstant() {
        assertThat(UrgencyLevel.valueOf("HOSPITALIZATION")).isEqualTo(UrgencyLevel.HOSPITALIZATION);
        assertThat(UrgencyLevel.valueOf("EMERGENCY")).isEqualTo(UrgencyLevel.EMERGENCY);
    }

    @Test
    void namesMatchTheContractUsedByTheReservationTools() {
        assertThat(UrgencyLevel.HOSPITALIZATION.name()).isEqualTo("HOSPITALIZATION");
        assertThat(UrgencyLevel.EMERGENCY.name()).isEqualTo("EMERGENCY");
    }

    @Test
    void valueOfRejectsUnknownLevels() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UrgencyLevel.valueOf("TRIAGE"))
                .withMessageContaining("TRIAGE");
    }
}
