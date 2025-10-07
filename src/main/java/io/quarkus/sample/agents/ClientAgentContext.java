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

    public void setCurrentA2AAgent(AGENT currentAgent) {
        this.currentAgent = currentAgent;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public AGENT getCurrentA2AAgent() {
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

    public void resetOnTaskTerminalState() {
        //do not reset context id as it is reuable across tasks
        //do not reset current agent as it is reusable for a give task
        //do not reset todoId because technically we should ahndle post cancel / post completed recalls
        taskId = null;
    }
}
