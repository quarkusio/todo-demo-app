package io.quarkus.sample.agents;

import io.a2a.spec.AgentCard;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ApplicationScoped
public class AgentDispatcher {
    @Inject @WeatherAgentProducer.WeatherAgent
    private AgentCard weatherCard;

    public String sendInfo() {
        return weatherCard.name();
    }
}
