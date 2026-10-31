package de.schaffbar.core_pos.domain.workshop_session;

import static java.util.Objects.isNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import de.schaffbar.core_pos.shared.billing.BillingTotal;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Entity
@Table(name = "WORKSHOP_SESSION", schema = "SCHAFFBAR")
class WorkshopSession {

    @Id
    private UUID id;

    @NotNull
    private UUID customerId;

    @NotNull
    private Instant startTime;

    private Instant closeTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    private WorkshopSessionStatus status;

    private Long workshopMinutes;

    private Long workshopUnits;

    private Long toolMinutes;

    private Long toolUnits;

    @NotNull
    @Version
    private Instant updatedAt;

    // ------------------------------------------------------------------------
    // static constructor

    public static WorkshopSession of(CustomerId customerId) {
        WorkshopSession workshopSession = new WorkshopSession();
        workshopSession.setId(WorkshopSessionId.random().getValue());
        workshopSession.setCustomerId(customerId.getValue());
        workshopSession.setStartTime(Instant.now());
        workshopSession.setStatus(WorkshopSessionStatus.OPEN);

        return workshopSession;
    }

    // ------------------------------------------------------------------------
    // query

    public WorkshopSessionId getId() {
        return WorkshopSessionId.of(this.id);
    }

    public CustomerId getCustomerId() {
        return CustomerId.of(this.customerId);
    }

    public BillingTotal getWorkshopTotal() {
        return isNull(this.workshopMinutes) ? null : new BillingTotal(this.workshopMinutes, this.workshopUnits);
    }

    public BillingTotal getToolTotal() {
        return isNull(this.toolMinutes) ? null : new BillingTotal(this.toolMinutes, this.toolUnits);
    }

    // ------------------------------------------------------------------------
    // command

    public List<SchaffbarEvent> close(BillingTotal workshopTotal, BillingTotal toolTotal) {
        this.closeTime = Instant.now();
        this.status = WorkshopSessionStatus.PAID;
        this.workshopMinutes = workshopTotal.minutes();
        this.workshopUnits = workshopTotal.units();
        this.toolMinutes = toolTotal.minutes();
        this.toolUnits = toolTotal.units();

        return List.of(WorkshopSessionEventFactory.workshopSessionClosed(this));
    }

}
