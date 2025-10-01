package io.quarkus.sample;

import io.quarkus.sample.agents.AGENT;
import io.quarkus.sample.agents.AgentSelector;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

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
        var agent = agentSelector.findRelevantAgent("Find what is the benefit of a healthy life", "");
        assertEquals(AGENT.NONE, agent, "There should be no matching agent");
        agent = agentSelector.findRelevantAgent("Find the weather in San Francisco", "");
        assertEquals(AGENT.WEATHER, agent, "There should be the WEATHER matching agent");
    }
}