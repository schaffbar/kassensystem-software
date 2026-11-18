package de.schaffbar.core_pos.use_case;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionService;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.domain.workshop_usage.WorkshopUsageService;
import de.schaffbar.core_pos.shared.id.CustomerId;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@Service
@Validated
@RequiredArgsConstructor
public class EnterWorkshop {

    private final @NonNull WorkshopUsageService workshopUsageService;

    private final @NonNull WorkshopSessionService workshopSessionService;

    @Transactional
    public void process(@NotNull @Valid CustomerId customerId) {

        // TODO: check if customer has a valid certificate to enter the workshop

        WorkshopSessionId sessionId = this.workshopSessionService.getOpenWorkshopSession(customerId) //
                .map(WorkshopSessionView::id) //
                .orElseGet(() -> this.workshopSessionService.startWorkshopSession(customerId));

        this.workshopUsageService.startWorkshopUsage(customerId, sessionId);
    }

}
