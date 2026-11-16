package de.schaffbar.core_pos.domain.rfid_tag.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import de.schaffbar.core_pos.domain.rfid_tag.RfidTagViews.RfidTagView;
import de.schaffbar.core_pos.shared.id.RfidTagId;
import de.schaffbar.core_pos.shared.id.ValueObjectMapper;

@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface RfidTagApiModel extends ValueObjectMapper {

    RfidTagApiModel MAPPER = Mappers.getMapper(RfidTagApiModel.class);

    // ------------------------------------------------------------------------
    // mapping view to response

    @Mapping(target = "id", source = "id.value")
    RfidTagApiDto toRfidTagApiDto(RfidTagView rfidTag);

    // ------------------------------------------------------------------------
    // response

    record RfidTagApiDto( //
            @NotBlank String id, //
            boolean active //
    ) {}

    // ------------------------------------------------------------------------
    // request body

    record CreateRfidTagRequestBody( //
            @NotNull @Valid RfidTagId rfidTagId //
    ) {}

}
