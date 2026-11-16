package de.schaffbar.core_pos.read_facade;

import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.rfid_tag.RfidTagService;
import de.schaffbar.core_pos.domain.rfid_tag.RfidTagViews.RfidTagView;
import de.schaffbar.core_pos.shared.cqrs.Query;
import de.schaffbar.core_pos.shared.id.RfidTagId;

@Service
@Validated
@RequiredArgsConstructor
public class RfidTagReadFacade {

    private final @NonNull RfidTagService rfidTagService;

    @Query
    public List<RfidTagView> getRfidTags() {
        return this.rfidTagService.getRfidTags();
    }

    @Query
    public Optional<RfidTagView> getRfidTag(@NotNull @Valid RfidTagId rfidTagId) {
        return this.rfidTagService.getRfidTag(rfidTagId);
    }

}
