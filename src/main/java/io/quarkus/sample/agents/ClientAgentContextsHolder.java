package io.quarkus.sample.agents;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ClientAgentContextsHolder {
    private ConcurrentMap<String, ClientAgentContext> todoIdToContext = new ConcurrentHashMap<>();
    private ConcurrentMap<String, ClientAgentContext> taskIdToContext = new ConcurrentHashMap<>();
    private ConcurrentMap<String, ClientAgentContext> contextIdToContext = new ConcurrentHashMap<>();

    public ClientAgentContext getContextFromTodoId(String todoId) {
        return todoIdToContext.get(todoId);
    }

    public void addOrUpdateContext(ClientAgentContext context) {
        //workaround to allow multiplexing from A2A clients
        if (context.getContextId() == null) {
            context.setContextId(UUID.randomUUID().toString());
        }
        contextIdToContext.put(context.getContextId(), context);
        todoIdToContext.put(context.getTodoId(), context);
        if (context.getTaskId() != null) {
            taskIdToContext.put(context.getTaskId(), context);
        }
    }

    public ClientAgentContext getContextFromContextId(String contextId) {
        return contextIdToContext.get(contextId);
    }

    public ClientAgentContext getContextFromTaskId(String taskId) {
        return taskIdToContext.get(taskId);
    }
}
