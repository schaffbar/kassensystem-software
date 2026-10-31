package de.schaffbar.core_pos.domain.rfid_tag;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.shared.id.RfidTagId;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RfidTagViews {

    RfidTagViews MAPPER = Mappers.getMapper(RfidTagViews.class);

    // ------------------------------------------------------------------------
    // mapper

    RfidTagView toRfidTagView(RfidTag rfidTag);

    // ------------------------------------------------------------------------
    // views

    @Builder
    @Validated
    record RfidTagView( //
            @NotNull RfidTagId id, //
            boolean active, //
            @NotNull Instant createdAt //
    ) {}

}
