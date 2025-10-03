package io.quarkus.sample.agents;

import io.a2a.spec.AgentCard;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class AgentDescriptor {
    private final AGENT agent;
    private final AgentCard card;

    public AgentDescriptor(AGENT agent, AgentCard card) {
        this.agent = agent;
        this.card = card;
    }

    public AGENT getAgent() {
        return agent;
    }

    public String getName() {
        return card.name();
    }

    public String getDescription() {
        return card.description();
    }
}
