package io.quarkus.sample;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.quarkus.sample.agents.AGENT;
import io.quarkus.sample.agents.AgentDescriptor;
import io.quarkus.sample.agents.AgentSelector;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.util.ArrayList;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class TodoAgentsTest {

    @Inject
    AgentSelector agentSelector;

    @Test
    public void testMatchingAgent() {
        var agents = buildAgentDescriptors();
        var agent = agentSelector.findRelevantAgent("Find what is the benefit of a healthy life", "", agents);
        assertEquals(AGENT.NONE, agent, "There should be no matching agent");
        agent = agentSelector.findRelevantAgent("Find the weather in San Francisco", "", agents);
        assertEquals(AGENT.WEATHER, agent, "There should be the WEATHER matching agent");
    }

    private static ArrayList<AgentDescriptor> buildAgentDescriptors() {
        var card = new AgentCard.Builder()
                .name("Weather Agent")
                .description("Helps with weather in the USA, give the weather of a city or a region.")
                .url("http://example.com")
                .capabilities(new AgentCapabilities.Builder().build())
                .protocolVersion("0.3.0")
                .version("0.0")
                .defaultInputModes(new ArrayList<>())
                .defaultOutputModes(new ArrayList<>())
                .skills(new ArrayList<>())
                .build();
        AgentDescriptor descriptor = new AgentDescriptor(AGENT.WEATHER, card);
        var agents = new ArrayList<AgentDescriptor>() {{
            add(descriptor);
        }};
        return agents;
    }
}