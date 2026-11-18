package de.schaffbar.core_pos.domain.tool_certification;

import static de.schaffbar.core_pos.builder.CommandBuilder.createToolCertificationCommand;
import static de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus.ACTIVE;
import static de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus.PAUSED;
import static de.schaffbar.core_pos.domain.tool_certification.ToolCertificationStatus.REVOKED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.schaffbar.core_pos.domain.tool_certification.ToolCertificationCommands.CreateToolCertificationCommand;
import de.schaffbar.core_pos.shared.event.EventType;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.event.outbox.OutboxEventStore;
import de.schaffbar.core_pos.shared.event.payload.ToolCertificationEventPayload.ToolCertificationCreatedPayload;
import de.schaffbar.core_pos.shared.exception.CertificationAlreadyExistsException;
import de.schaffbar.core_pos.shared.exception.CertificationPermanentlyRevokedException;
import de.schaffbar.core_pos.shared.exception.InvalidCertificationStateTransitionException;
import de.schaffbar.core_pos.shared.exception.ResourceNotFoundException;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.ToolId;

@ExtendWith(MockitoExtension.class)
class ToolCertificationServiceTest {

    @Mock
    private ToolCertificationRepository toolCertificationRepository;

    @Mock
    private OutboxEventStore outboxEventStore;

    @Captor
    private ArgumentCaptor<List<SchaffbarEvent>> events;

    @InjectMocks
    private ToolCertificationService toolCertificationService;

    // ------------------------------------------------------------------------
    // hasActiveCertification

    @Test
    void hasActiveCertification_isTrueForAnActiveCertification() {
        // given
        ToolCertification certification = givenCertification(ACTIVE);

        // when
        boolean active = this.toolCertificationService.hasActiveCertification(certification.getCustomerId(), certification.getToolId());

        // then
        assertThat(active).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = ToolCertificationStatus.class, names = {"PAUSED", "REVOKED"})
    void hasActiveCertification_isFalseForAPausedOrRevokedCertification(ToolCertificationStatus status) {
        // given
        ToolCertification certification = givenCertification(status);

        // when
        boolean active = this.toolCertificationService.hasActiveCertification(certification.getCustomerId(), certification.getToolId());

        // then
        assertThat(active).isFalse();
    }

    @Test
    void hasActiveCertification_isFalseWithoutCertification() {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenNoCertification(customerId, toolId);

        // when
        boolean active = this.toolCertificationService.hasActiveCertification(customerId, toolId);

        // then
        assertThat(active).isFalse();
    }

    // ------------------------------------------------------------------------
    // createToolCertification

    @Test
    void createToolCertification_savesTheCertificationAndItsCreatedEvent() {
        // given
        CreateToolCertificationCommand command = createToolCertificationCommand().build();
        givenNoCertification(command.customerId(), command.toolId());

        // when
        this.toolCertificationService.createToolCertification(command);

        // then
        verify(this.toolCertificationRepository).save(any());
        ToolCertificationCreatedPayload payload = expectSavedEvent(EventType.TOOL_CERTIFICATION_CREATED) //
                .deserializeAndValidatePayload(ToolCertificationCreatedPayload.class);
        assertThat(payload.customerId()).isEqualTo(command.customerId());
        assertThat(payload.toolId()).isEqualTo(command.toolId());
        assertThat(payload.certifiedBy()).isEqualTo(command.certifiedBy());
        assertThat(payload.status()).isEqualTo(ACTIVE);
        assertThat(payload.certifiedAt()).isNotNull();
    }

    @ParameterizedTest
    @EnumSource(value = ToolCertificationStatus.class, names = {"ACTIVE", "PAUSED"})
    void createToolCertification_rejectsAnExistingCertification(ToolCertificationStatus status) {
        // given
        ToolCertification existing = givenCertification(status);
        CreateToolCertificationCommand command = createCommandFor(existing);

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationService.createToolCertification(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(CertificationAlreadyExistsException.class) //
                .hasFieldOrPropertyWithValue("code", CertificationAlreadyExistsException.CODE);
        verify(this.toolCertificationRepository, never()).save(any());
        expectNoEvent();
    }

    @Test
    void createToolCertification_rejectsARevokedCertification() {
        // given
        ToolCertification revoked = givenCertification(REVOKED);
        CreateToolCertificationCommand command = createCommandFor(revoked);

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationService.createToolCertification(command));

        // then
        assertThat(thrown) //
                .isInstanceOf(CertificationPermanentlyRevokedException.class) //
                .hasFieldOrPropertyWithValue("code", CertificationPermanentlyRevokedException.CODE);
        verify(this.toolCertificationRepository, never()).save(any());
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // pauseToolCertification

    @Test
    void pauseToolCertification_pausesTheCertificationAndSavesTheEvent() {
        // given
        ToolCertification certification = givenCertification(ACTIVE);

        // when
        this.toolCertificationService.pauseToolCertification(certification.getCustomerId(), certification.getToolId());

        // then
        assertThat(certification.getStatus()).isEqualTo(PAUSED);
        expectSavedEvents(EventType.TOOL_CERTIFICATION_PAUSED, List.of(certification));
    }

    @Test
    void pauseToolCertification_rejectsAnUnknownCertification() {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenNoCertification(customerId, toolId);

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationService.pauseToolCertification(customerId, toolId));

        // then
        expectNotFound(thrown);
        expectNoEvent();
    }

    @ParameterizedTest
    @EnumSource(value = ToolCertificationStatus.class, names = {"PAUSED", "REVOKED"})
    void pauseToolCertification_rejectsACertificationThatIsNotActive(ToolCertificationStatus status) {
        // given
        ToolCertification certification = givenCertification(status);

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationService.pauseToolCertification(certification.getCustomerId(), certification.getToolId()));

        // then
        expectRejectedTransition(thrown, certification, status);
    }

    // ------------------------------------------------------------------------
    // reactivateToolCertification

    @Test
    void reactivateToolCertification_activatesTheCertificationAndSavesTheEvent() {
        // given
        ToolCertification certification = givenCertification(PAUSED);

        // when
        this.toolCertificationService.reactivateToolCertification(certification.getCustomerId(), certification.getToolId());

        // then
        assertThat(certification.getStatus()).isEqualTo(ACTIVE);
        expectSavedEvents(EventType.TOOL_CERTIFICATION_REACTIVATED, List.of(certification));
    }

    @Test
    void reactivateToolCertification_rejectsAnUnknownCertification() {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenNoCertification(customerId, toolId);

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationService.reactivateToolCertification(customerId, toolId));

        // then
        expectNotFound(thrown);
        expectNoEvent();
    }

    @ParameterizedTest
    @EnumSource(value = ToolCertificationStatus.class, names = {"ACTIVE", "REVOKED"})
    void reactivateToolCertification_rejectsACertificationThatIsNotPaused(ToolCertificationStatus status) {
        // given
        ToolCertification certification = givenCertification(status);

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationService.reactivateToolCertification(certification.getCustomerId(), certification.getToolId()));

        // then
        expectRejectedTransition(thrown, certification, status);
    }

    // ------------------------------------------------------------------------
    // revokeToolCertification

    @ParameterizedTest
    @EnumSource(value = ToolCertificationStatus.class, names = {"ACTIVE", "PAUSED"})
    void revokeToolCertification_revokesTheCertificationAndSavesTheEvent(ToolCertificationStatus status) {
        // given
        ToolCertification certification = givenCertification(status);

        // when
        this.toolCertificationService.revokeToolCertification(certification.getCustomerId(), certification.getToolId());

        // then
        assertThat(certification.getStatus()).isEqualTo(REVOKED);
        expectSavedEvents(EventType.TOOL_CERTIFICATION_REVOKED, List.of(certification));
    }

    @Test
    void revokeToolCertification_rejectsAnUnknownCertification() {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenNoCertification(customerId, toolId);

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationService.revokeToolCertification(customerId, toolId));

        // then
        expectNotFound(thrown);
        expectNoEvent();
    }

    @Test
    void revokeToolCertification_rejectsARevokedCertification() {
        // given
        ToolCertification certification = givenCertification(REVOKED);

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationService.revokeToolCertification(certification.getCustomerId(), certification.getToolId()));

        // then
        expectRejectedTransition(thrown, certification, REVOKED);
    }

    // ------------------------------------------------------------------------
    // deleteToolCertification

    @Test
    void deleteToolCertification_deletesTheCertificationAndSavesTheEvent() {
        // given
        ToolCertification certification = givenCertification(ACTIVE);

        // when
        this.toolCertificationService.deleteToolCertification(certification.getCustomerId(), certification.getToolId());

        // then
        verify(this.toolCertificationRepository).delete(certification);
        expectSavedEvents(EventType.TOOL_CERTIFICATION_DELETED, List.of(certification));
    }

    @Test
    void deleteToolCertification_rejectsAnUnknownCertification() {
        // given
        CustomerId customerId = CustomerId.random();
        ToolId toolId = ToolId.random();
        givenNoCertification(customerId, toolId);

        // when
        Throwable thrown = catchThrowable(() -> this.toolCertificationService.deleteToolCertification(customerId, toolId));

        // then
        expectNotFound(thrown);
        verify(this.toolCertificationRepository, never()).delete(any());
        expectNoEvent();
    }

    // ------------------------------------------------------------------------
    // deleteToolCertifications

    @Test
    void deleteToolCertifications_ofATool_deletesThemAndSavesOneEventEach() {
        // given
        ToolId toolId = ToolId.random();
        List<ToolCertification> certifications = givenCertificationsOf(toolId);

        // when
        this.toolCertificationService.deleteToolCertifications(toolId);

        // then
        verify(this.toolCertificationRepository).deleteAll(certifications);
        expectSavedEvents(EventType.TOOL_CERTIFICATION_DELETED, certifications);
    }

    @Test
    void deleteToolCertifications_ofAToolWithoutCertifications_savesNoEvent() {
        // given
        ToolId toolId = ToolId.random();
        when(this.toolCertificationRepository.findByToolId(toolId)).thenReturn(List.of());

        // when
        this.toolCertificationService.deleteToolCertifications(toolId);

        // then
        verify(this.toolCertificationRepository, never()).deleteAll(any());
        expectNoEvent();
    }

    @Test
    void deleteToolCertifications_ofACustomer_deletesThemAndSavesOneEventEach() {
        // given
        CustomerId customerId = CustomerId.random();
        List<ToolCertification> certifications = givenCertificationsOf(customerId);

        // when
        this.toolCertificationService.deleteToolCertifications(customerId);

        // then
        verify(this.toolCertificationRepository).deleteAll(certifications);
        expectSavedEvents(EventType.TOOL_CERTIFICATION_DELETED, certifications);
    }

    @Test
    void deleteToolCertifications_ofACustomerWithoutCertifications_savesNoEvent() {
        // given
        CustomerId customerId = CustomerId.random();
        when(this.toolCertificationRepository.findByCustomerId(customerId)).thenReturn(List.of());

        // when
        this.toolCertificationService.deleteToolCertifications(customerId);

        // then
        verify(this.toolCertificationRepository, never()).deleteAll(any());
        expectNoEvent();
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
        when(this.toolCertificationRepository.findByCustomerIdAndToolId(certification.getCustomerId(), certification.getToolId())) //
                .thenReturn(Optional.of(certification));

        return certification;
    }

    private void givenNoCertification(CustomerId customerId, ToolId toolId) {
        when(this.toolCertificationRepository.findByCustomerIdAndToolId(customerId, toolId)).thenReturn(Optional.empty());
    }

    private List<ToolCertification> givenCertificationsOf(ToolId toolId) {
        List<ToolCertification> certifications = List.of( //
                ToolCertification.of(createToolCertificationCommand().toolId(toolId).build()), //
                ToolCertification.of(createToolCertificationCommand().toolId(toolId).build()));
        when(this.toolCertificationRepository.findByToolId(toolId)).thenReturn(certifications);

        return certifications;
    }

    private List<ToolCertification> givenCertificationsOf(CustomerId customerId) {
        List<ToolCertification> certifications = List.of( //
                ToolCertification.of(createToolCertificationCommand().customerId(customerId).build()), //
                ToolCertification.of(createToolCertificationCommand().customerId(customerId).build()));
        when(this.toolCertificationRepository.findByCustomerId(customerId)).thenReturn(certifications);

        return certifications;
    }

    private static CreateToolCertificationCommand createCommandFor(ToolCertification existing) {
        return createToolCertificationCommand() //
                .customerId(existing.getCustomerId()) //
                .toolId(existing.getToolId()) //
                .build();
    }

    // ------------------------------------------------------------------------
    // then

    private SchaffbarEvent expectSavedEvent(EventType type) {
        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .singleElement() //
                .satisfies(event -> assertThat(event.getType()).isEqualTo(type));

        return this.events.getValue().getFirst();
    }

    private void expectSavedEvents(EventType type, List<ToolCertification> certifications) {
        Tuple[] expected = certifications.stream() //
                .map(certification -> tuple(type, certification.getId().getValue().toString())) //
                .toArray(Tuple[]::new);

        verify(this.outboxEventStore).save(this.events.capture());
        assertThat(this.events.getValue()) //
                .extracting(SchaffbarEvent::getType, SchaffbarEvent::getAggregateId) //
                .containsExactly(expected);
    }

    private void expectNotFound(Throwable thrown) {
        assertThat(thrown) //
                .isInstanceOf(ResourceNotFoundException.class) //
                .hasFieldOrPropertyWithValue("resource", ResourceNotFoundException.Resource.TOOL_CERTIFICATION);
    }

    private void expectRejectedTransition(Throwable thrown, ToolCertification certification, ToolCertificationStatus unchangedStatus) {
        assertThat(thrown) //
                .isInstanceOf(InvalidCertificationStateTransitionException.class) //
                .hasFieldOrPropertyWithValue("code", InvalidCertificationStateTransitionException.CODE);
        assertThat(certification.getStatus()).isEqualTo(unchangedStatus);
        expectNoEvent();
    }

    private void expectNoEvent() {
        verifyNoInteractions(this.outboxEventStore);
    }

}
