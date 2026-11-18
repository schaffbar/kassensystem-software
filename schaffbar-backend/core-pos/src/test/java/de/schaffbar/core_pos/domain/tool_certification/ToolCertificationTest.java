package de.schaffbar.core_pos.domain.tool_certification;

import static de.schaffbar.core_pos.builder.CommandBuilder.createToolCertificationCommand;
import static de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus.ACTIVE;
import static de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus.PAUSED;
import static de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus.REVOKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationCommands.CreateToolCertificationCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.payload.ToolCertificationEventPayload.ToolCertificationPausedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolCertificationEventPayload.ToolCertificationReactivatedPayload;
import de.schaffbar.core_pos.shared.event.payload.ToolCertificationEventPayload.ToolCertificationRevokedPayload;
import de.schaffbar.core_pos.shared.exception.InvalidCertificationStateTransitionException;

class ToolCertificationTest {

    // ------------------------------------------------------------------------
    // of

    @Test
    void of_createsAnActiveCertification() {
        // given
        CreateToolCertificationCommand command = createToolCertificationCommand().build();

        // when
        ToolCertification certification = ToolCertification.of(command);

        // then
        assertThat(certification.getId()).isNotNull();
        assertThat(certification.getCustomerId()).isEqualTo(command.customerId());
        assertThat(certification.getToolId()).isEqualTo(command.toolId());
        assertThat(certification.getCertifiedBy()).isEqualTo(command.certifiedBy());
        assertThat(certification.getStatus()).isEqualTo(ACTIVE);
        assertThat(certification.getCertifiedAt()).isNotNull();
    }

    @Test
    void of_withoutCertifiedBy_keepsItEmpty() {
        // given
        CreateToolCertificationCommand command = createToolCertificationCommand() //
                .certifiedBy(null) //
                .build();

        // when
        ToolCertification certification = ToolCertification.of(command);

        // then
        assertThat(certification.getCertifiedBy()).isNull();
    }

    // ------------------------------------------------------------------------
    // pause

    @Test
    void pause_pausesAnActiveCertificationAndReturnsTheEvent() {
        // given
        ToolCertification certification = givenCertification(ACTIVE);

        // when
        List<SchaffbarEvent> events = certification.pause();

        // then
        assertThat(certification.getStatus()).isEqualTo(PAUSED);

        ToolCertificationPausedPayload payload = expectSingleEvent(events, EventType.TOOL_CERTIFICATION_PAUSED, certification) //
                .deserializeAndValidatePayload(ToolCertificationPausedPayload.class);
        assertThat(payload.id()).isEqualTo(certification.getId());
        assertThat(payload.customerId()).isEqualTo(certification.getCustomerId());
        assertThat(payload.toolId()).isEqualTo(certification.getToolId());
        assertThat(payload.status()).isEqualTo(PAUSED);
    }

    @ParameterizedTest
    @EnumSource(value = ToolCertificationStatus.class, names = {"PAUSED", "REVOKED"})
    void pause_rejectsACertificationThatIsNotActive(ToolCertificationStatus status) {
        // given
        ToolCertification certification = givenCertification(status);

        // when
        Throwable thrown = catchThrowable(certification::pause);

        // then
        expectRejected(thrown, certification, status);
    }

    // ------------------------------------------------------------------------
    // reactivate

    @Test
    void reactivate_activatesAPausedCertificationAndReturnsTheEvent() {
        // given
        ToolCertification certification = givenCertification(PAUSED);

        // when
        List<SchaffbarEvent> events = certification.reactivate();

        // then
        assertThat(certification.getStatus()).isEqualTo(ACTIVE);

        ToolCertificationReactivatedPayload payload = expectSingleEvent(events, EventType.TOOL_CERTIFICATION_REACTIVATED, certification) //
                .deserializeAndValidatePayload(ToolCertificationReactivatedPayload.class);
        assertThat(payload.id()).isEqualTo(certification.getId());
        assertThat(payload.status()).isEqualTo(ACTIVE);
    }

    @ParameterizedTest
    @EnumSource(value = ToolCertificationStatus.class, names = {"ACTIVE", "REVOKED"})
    void reactivate_rejectsACertificationThatIsNotPaused(ToolCertificationStatus status) {
        // given
        ToolCertification certification = givenCertification(status);

        // when
        Throwable thrown = catchThrowable(certification::reactivate);

        // then
        expectRejected(thrown, certification, status);
    }

    // ------------------------------------------------------------------------
    // revoke

    @ParameterizedTest
    @EnumSource(value = ToolCertificationStatus.class, names = {"ACTIVE", "PAUSED"})
    void revoke_revokesAnActiveOrPausedCertificationAndReturnsTheEvent(ToolCertificationStatus status) {
        // given
        ToolCertification certification = givenCertification(status);

        // when
        List<SchaffbarEvent> events = certification.revoke();

        // then
        assertThat(certification.getStatus()).isEqualTo(REVOKED);

        ToolCertificationRevokedPayload payload = expectSingleEvent(events, EventType.TOOL_CERTIFICATION_REVOKED, certification) //
                .deserializeAndValidatePayload(ToolCertificationRevokedPayload.class);
        assertThat(payload.id()).isEqualTo(certification.getId());
        assertThat(payload.status()).isEqualTo(REVOKED);
    }

    @Test
    void revoke_rejectsARevokedCertification() {
        // given
        ToolCertification certification = givenCertification(REVOKED);

        // when
        Throwable thrown = catchThrowable(certification::revoke);

        // then
        expectRejected(thrown, certification, REVOKED);
    }

    // ------------------------------------------------------------------------
    // given

    private ToolCertification givenCertification(ToolCertificationStatus status) {
        ToolCertification certification = ToolCertification.of(createToolCertificationCommand().build());
        if (status == PAUSED) {
            certification.pause();
        }
        if (status == REVOKED) {
            certification.revoke();
        }

        return certification;
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSingleEvent(List<SchaffbarEvent> events, EventType type, ToolCertification certification) {
        assertThat(events).hasSize(1);

        SchaffbarEvent event = events.getFirst();
        assertThat(event.getType()).isEqualTo(type);
        assertThat(event.getAggregateId()).isEqualTo(certification.getId().getValue().toString());

        return event;
    }

    private void expectRejected(Throwable thrown, ToolCertification certification, ToolCertificationStatus unchangedStatus) {
        assertThat(thrown) //
                .isInstanceOf(InvalidCertificationStateTransitionException.class) //
                .hasFieldOrPropertyWithValue("code", InvalidCertificationStateTransitionException.CODE);
        assertThat(certification.getStatus()).isEqualTo(unchangedStatus);
    }

}
