package de.schaffbar.core_pos.domain.workshop_usage;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.event.SchaffbarEvent;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;
import de.schaffbar.core_pos.shared.id.WorkshopUsageId;

@Getter
@Setter(AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@Entity
@Table(name = "WORKSHOP_USAGE", schema = "SCHAFFBAR")
class WorkshopUsage {

    @Id
    private UUID id;

    @NotNull
    private UUID customerId;

    @NotNull
    private UUID workshopSessionId;

    @NotNull
    private Instant entryTime;

    private Instant exitTime;

    @Version
    private Instant updatedAt;

    // ------------------------------------------------------------------------
    // static constructor

    public static WorkshopUsage of(CustomerId customerId, WorkshopSessionId workshopSessionId) {
        WorkshopUsage workshopUsage = new WorkshopUsage();
        workshopUsage.setId(WorkshopUsageId.random().getValue());
        workshopUsage.setCustomerId(customerId.getValue());
        workshopUsage.setWorkshopSessionId(workshopSessionId.getValue());
        workshopUsage.setEntryTime(Instant.now());

        return workshopUsage;
    }

    // ------------------------------------------------------------------------
    // query

    public WorkshopUsageId getId() {
        return WorkshopUsageId.of(this.id);
    }

    public CustomerId getCustomerId() {
        return CustomerId.of(this.customerId);
    }

    public WorkshopSessionId getWorkshopSessionId() {
        return WorkshopSessionId.of(this.workshopSessionId);
    }

    public BillableDuration getBillableDuration() {
        return isNull(this.exitTime) ? null : BillableDuration.between(this.entryTime, this.exitTime);
    }

    public BillableDuration getBillableDurationUntil(Instant now) {
        return BillableDuration.between(this.entryTime, nonNull(this.exitTime) ? this.exitTime : now);
    }

    // ------------------------------------------------------------------------
    // command

    public List<SchaffbarEvent> stop() {
        // the service finds only a running visit (WU-2)
        if (nonNull(this.exitTime)) {
            throw new IllegalStateException("Workshop usage [id: " + this.id + "] has already been stopped");
        }

        this.setExitTime(Instant.now());

        return List.of(WorkshopUsageEventFactory.workshopUsageLeft(this));
    }

}
