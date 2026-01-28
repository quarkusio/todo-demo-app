package io.quarkus.sample.agents.webstocket;

public record AgentMessage(Kind kind, String todoId, String payload) {
    
}
