package de.schaffbar.core_pos.builder;

import static de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView.WorkshopSessionViewBuilder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionStatus;
import de.schaffbar.core_pos.domain.workshop_session.WorkshopSessionViews.WorkshopSessionView;
import de.schaffbar.core_pos.shared.id.WorkshopSessionId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ViewBuilder {

    // ------------------------------------------------------------------------
    // workshop session

    public static WorkshopSessionViewBuilder openWorkshopSessionView() {
        return WorkshopSessionView.builder() //
                .id(WorkshopSessionId.random()) //
                .status(WorkshopSessionStatus.OPEN);
    }

}
