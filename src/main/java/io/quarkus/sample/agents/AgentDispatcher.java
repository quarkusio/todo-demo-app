package io.quarkus.sample.agents;

import io.a2a.client.Client;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.Message;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TextPart;
import io.quarkus.logging.Log;
import io.quarkus.sample.Todo;
import io.quarkus.sample.agent.AgentMessage;
import io.quarkus.sample.agent.Kind;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ApplicationScoped
public class AgentDispatcher {
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
                context.reset();
            } catch (A2AClientException e) {
                //let's ignore cancellation exception, we are done on our side
            }
        }
    }


    public CompletionStage<Void> onStarAgentEvent(@ObservesAsync ClientAgentContext event) {
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
                bus.publish(todoId,new AgentMessage(Kind.activity_log, todoId, "The weather agent will look into your todo"));
                try {
                    getCurrentClient(context).sendMessage(new Message.Builder()
                            .role(Message.Role.USER)
                            .parts(new TextPart(todoAsPrompt(todo)))
                            .build()
                    );
                } catch (A2AClientException e) {
                    bus.publish(todoId, new AgentMessage(Kind.activity_log, todoId, "Oops, something failed\n" + e.getMessage()));
                    throw new RuntimeException(e);
                }
            }
            case NONE -> {
                bus.publish(todoId,new AgentMessage(Kind.activity_log, todoId, "No agent has been found for your need, sorry about that!"));
            }
        }
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
                IllegalStateException illegalStateException = new IllegalStateException("An agent shoud have been picked to call getCurrentClient value: " + currentAgent);
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
            getCurrentClient(context).sendMessage(a2aMessage);
        } catch (A2AClientException e) {
            bus.publish(todoId, new AgentMessage(Kind.activity_log, todoId, "Oops, something failed\n" + e.getMessage()));
            throw new RuntimeException(e);
        }
    }
}
