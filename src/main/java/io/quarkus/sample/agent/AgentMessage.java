package io.quarkus.sample.agent;

public record AgentMessage(Kind kind, String todoId, String payload) {
    
}
