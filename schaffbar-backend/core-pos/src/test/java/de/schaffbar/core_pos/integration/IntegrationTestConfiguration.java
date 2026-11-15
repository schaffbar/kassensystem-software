package de.schaffbar.core_pos.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import de.schaffbar.core_pos.integration.fixture.SchaffbarFixture;

// every @IntegrationTest shares this configuration, so Spring caches one context with one container for the whole run
@TestConfiguration(proxyBeanMethods = false)
@Import(SchaffbarFixture.class)
class IntegrationTestConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        // the migrations grant rights to schadmin, the database owner on the server
        return new PostgreSQLContainer<>(DockerImageName.parse("postgres:16.3")) //
                .withUsername("schadmin");
    }

}
