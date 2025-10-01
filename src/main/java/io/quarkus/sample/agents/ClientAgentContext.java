package io.quarkus.sample.agents;

import io.quarkus.sample.Todo;

public class ClientAgentContext {
    private Todo todo;
    private AGENT currentAgent = AGENT.NONE;
    private String taskId;
    private String contextId;
    private String todoId;

    public ClientAgentContext(Todo todo, String todoId) {
        this.todo =  todo;
        this.todoId = todoId;
    }

    public Todo getTodo() {
        return todo;
    }

    public void setCurrentAgent(AGENT currentAgent) {
        this.currentAgent = currentAgent;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public AGENT getCurrentAgent() {
        return currentAgent;
    }

    public String getTaskId() {
        return taskId;
    }

    public String getContextId() {
        return contextId;
    }

    public String getTodoId() {
        return todoId;
    }

    public void reset() {
        taskId = null;
        contextId = null;
        currentAgent = AGENT.NONE;
        //todoID not reset because technically we should ahndle post cancel / post completed recalls
    }
}
