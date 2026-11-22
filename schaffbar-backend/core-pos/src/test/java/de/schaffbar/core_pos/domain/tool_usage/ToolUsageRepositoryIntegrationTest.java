package de.schaffbar.core_pos.domain.tool_usage;

import static de.schaffbar.core_pos.builder.CommandBuilder.startToolUsageCommand;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import de.schaffbar.core_pos.integration.IntegrationTest;
import de.schaffbar.core_pos.shared.id.ToolId;

// what only the database shows: the partial unique index of V9 – the service checks TU-9 (and TU-3) before, but two
// requests at the switch box at the same time can both pass that check
@IntegrationTest
class ToolUsageRepositoryIntegrationTest {

    @Autowired
    private ToolUsageRepository toolUsageRepository;

    @Test
    void aToolHasAtMostOneRunningUsage() {
        // given
        ToolId toolId = ToolId.random();
        ToolUsage running = givenRunningToolUsage(toolId);

        // when – another customer at the same tool
        Throwable thrown = catchThrowable(() -> this.toolUsageRepository.saveAndFlush(ToolUsage.of(startToolUsageCommand().toolId(toolId).build())));

        // then
        assertThat(thrown).isInstanceOf(DataIntegrityViolationException.class);

        // when – after switching it off
        running.stop();
        this.toolUsageRepository.saveAndFlush(running);
        ToolUsage next = this.toolUsageRepository.saveAndFlush(ToolUsage.of(startToolUsageCommand().toolId(toolId).build()));

        // then
        assertThat(this.toolUsageRepository.findById(next.getId().getValue())).isPresent();
    }

    // ------------------------------------------------------------------------
    // given

    private ToolUsage givenRunningToolUsage(ToolId toolId) {
        return this.toolUsageRepository.saveAndFlush(ToolUsage.of(startToolUsageCommand().toolId(toolId).build()));
    }

}
