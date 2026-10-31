package de.schaffbar.core_pos.domain.workshop_usage;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.billing.BillableDuration;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;
import de.schaffbar.core_pos.shared.id.WorkshopUsageId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface WorkshopUsageViews {

    WorkshopUsageViews MAPPER = Mappers.getMapper(WorkshopUsageViews.class);

    // ------------------------------------------------------------------------
    // mapper

    WorkshopUsageView toWorkshopUsageView(WorkshopUsage workshopUsage);

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record WorkshopUsageView( //
            @NotNull WorkshopUsageId id, //
            @NotNull CustomerId customerId, //
            @NotNull WorkshopSessionId workshopSessionId, //
            @NotNull @PastOrPresent Instant entryTime, //
            @PastOrPresent Instant exitTime, //
            BillableDuration billableDuration // null while the usage is running
    ) {}

}
