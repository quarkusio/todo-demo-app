package io.quarkus.sample.agents;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientAgentContextsHolder {
    private ConcurrentMap<String, ClientAgentContext> todoIdToContext = new ConcurrentHashMap<>();
    private ConcurrentMap<String, ClientAgentContext> taskIdToContext = new ConcurrentHashMap<>();

    public ClientAgentContext getContextFromTodoId(String todoId) {
        return todoIdToContext.get(todoId);
    }

    public void addOrUpdateContext(ClientAgentContext event) {
        todoIdToContext.put(event.getTodoId(), event);
        if (event.getTaskId() != null) {
            taskIdToContext.put(event.getTaskId(), event);
        }
    }

}
