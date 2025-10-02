package io.quarkus.sample.agents;

import io.a2a.client.Client;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.quarkus.logging.Log;
import io.quarkus.sample.Todo;
import io.quarkus.sample.agent.AgentMessage;
import io.quarkus.sample.agent.Kind;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.quarkus.sample.agents.A2AUtils.extractTextFromParts;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ApplicationScoped
public class AgentsMediator {
    @Inject @WeatherAgentProducer.WeatherAgent
    private Client weatherClient;
    private ClientAgentContextsHolder contextsHolder = new ClientAgentContextsHolder();

    @Inject
    private EventBus bus;

    @Inject
    AgentSelector agentSelector;

    public void cancel(String todoId) {
        ClientAgentContext context = contextsHolder.getContextFromTodoId(todoId);
        if (context.getTaskId() !=  null) {
            try {
                getCurrentClient(context).cancelTask(new TaskIdParams(context.getTaskId()));
                context.resetOnTaskCompletion();
            } catch (A2AClientException e) {
                //let's ignore cancellation exception, we are done on our side
            }
        }
    }


    public CompletionStage<Void> onConversationStart(@ObservesAsync ClientAgentContext event) {
        contextsHolder.addOrUpdateContext(event);
        findAgent(event);
        return CompletableFuture.completedFuture(null);
    }

    public void findAgent(ClientAgentContext context) {
        var todo = context.getTodo();
        var currentAgent = agentSelector.findRelevantAgent(todo.title, todo.description);
        context.setCurrentAgent(currentAgent);
        Log.infov("Selected agent {0} for todo '{1}'", currentAgent, todo.title);

        var todoId = context.getTodoId();
        switch (currentAgent) {
            case WEATHER -> {
                bus.publish(todoId,new AgentMessage(Kind.agent_request, todoId, "The weather agent will look into your todo"));
                try {
                    getCurrentClient(context).sendMessage(new Message.Builder()
                            .role(Message.Role.USER)
                            .contextId(context.getContextId())
                            .parts(new TextPart(todoAsPrompt(todo)))
                            .build()
                    );
                } catch (A2AClientException e) {
                    bus.publish(todoId, new AgentMessage(Kind.activity_log, todoId, "Oops, something failed\n" + e.getMessage()));
                }
            }
            case NONE -> {
                sendNoAgentMessage(todoId);
            }
        }
    }

    private void sendNoAgentMessage(String todoId) {
        bus.publish(todoId,new AgentMessage(Kind.agent_request, todoId, "No agent has been found for your need, sorry about that!"));
    }

    private String todoAsPrompt(Todo todo) {
        var builder = new StringBuilder(todo.title);
        if (todo.description != null) {
            builder.append("\n").append(todo.description);
        }
        return builder.toString();
    }

    private Client getCurrentClient(ClientAgentContext context) {
        var todoId = context.getTodoId();
        var currentAgent = context.getCurrentAgent();
        switch (currentAgent) {
            case WEATHER -> {
                return weatherClient;
            }
            case NONE -> {
                IllegalStateException illegalStateException = new IllegalStateException("An agent should have been picked to call getCurrentClient value: " + currentAgent);
                bus.publish(todoId, new AgentMessage(Kind.activity_log, todoId, "Oops, something failed\n" + illegalStateException.getMessage()));
                throw illegalStateException;
            }
            default -> {
                IllegalStateException illegalStateException = new IllegalStateException("Unexcepted agent enum " + currentAgent);
                bus.publish(todoId, new AgentMessage(Kind.activity_log, todoId, "Oops, something failed\n" + illegalStateException.getMessage()));
                throw illegalStateException;
            }
        }
    }

    public void passUserMessage(String todoId, String userMessage) {
        var context = contextsHolder.getContextFromTodoId(todoId);
        Message a2aMessage = new Message.Builder()
                .contextId(context.getContextId())
                .taskId(context.getTaskId())
                .role(Message.Role.USER)
                .parts(new TextPart(userMessage))
                .build();
        try {
            if (context.getCurrentAgent() == AGENT.NONE) {
                sendNoAgentMessage(todoId);
            }
            else {
                getCurrentClient(context).sendMessage(a2aMessage);
            }
        } catch (A2AClientException e) {
            bus.publish(todoId, new AgentMessage(Kind.activity_log, todoId, "Oops, something failed\n" + e.getMessage()));
        }
    }

    public void receiveMessageFromAgent(Message responseMessage) {
        var context = contextsHolder.getContextFromContextId(responseMessage.getContextId());
        var payload = A2AUtils.extractTextFromParts(responseMessage.getParts());
        bus.publish(context.getTodoId(), new AgentMessage(Kind.agent_request, context.getTodoId(), payload));
    }

    public void sendToActivityLog(TaskStatusUpdateEvent taskStatusUpdateEvent) {
        var taskId = taskStatusUpdateEvent.getTaskId();
        var context = contextsHolder.getContextFromTaskId(taskId);
        if (context == null) {
            context = contextsHolder.getContextFromContextId(taskStatusUpdateEvent.getContextId());
            context.setTaskId(taskId);
            contextsHolder.addOrUpdateContext(context);
        }
        String payload = "Received status-update for " + taskId + ": "
                        + taskStatusUpdateEvent.getStatus().state().asString();
        bus.publish(context.getTodoId(), new AgentMessage(Kind.activity_log, context.getTodoId(), payload));
    }

    public void sendTaskArtifacts(TaskUpdateEvent taskUpdateEvent) {
        var context = contextsHolder.getContextFromTaskId(taskUpdateEvent.getTask().getId());
        StringBuilder textBuilder = new StringBuilder();
        List<Artifact> artifacts = taskUpdateEvent.getTask().getArtifacts();
        for (Artifact artifact : artifacts) {
            textBuilder.append(extractTextFromParts(artifact.parts()));
        }
        var payload = textBuilder.toString();
        bus.publish(context.getTodoId(), new AgentMessage(Kind.agent_request, context.getTodoId(), payload));
    }

    public void sendToActivityLog(TaskEvent taskEvent) {
        var taskId = taskEvent.getTask().getId();
        var context = contextsHolder.getContextFromTaskId(taskId);
        if (context == null) {
            context = contextsHolder.getContextFromContextId(taskEvent.getTask().getContextId());
            contextsHolder.addOrUpdateContext(context);
        }
        String payload = "Received task event for " + taskId;
        bus.publish(context.getTodoId(), new AgentMessage(Kind.activity_log, context.getTodoId(), payload));
    }
}
