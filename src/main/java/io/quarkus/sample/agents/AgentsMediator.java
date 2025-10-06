package io.quarkus.sample.agents;

import io.a2a.client.Client;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskIdParams;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.quarkus.logging.Log;
import io.quarkus.sample.Todo;
import io.quarkus.sample.agents.webstocket.AgentMessage;
import io.quarkus.sample.agents.webstocket.Kind;
import io.vertx.core.eventbus.EventBus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static io.quarkus.sample.agents.A2AUtils.extractTextFromParts;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ApplicationScoped
public class AgentsMediator {
    @Inject AgentProducers agentProducers;
    private ClientAgentContextsHolder contextsHolder = new ClientAgentContextsHolder();

    @Inject
    private EventBus bus;

    @Inject
    AgentSelector agentSelector;


    public CompletionStage<Void> onConversationStart(@ObservesAsync ClientAgentContext event) {
        contextsHolder.addOrUpdateContext(event);
        initiateSubAgentConversation(event);
        return CompletableFuture.completedFuture(null);
    }

    public void initiateSubAgentConversation(ClientAgentContext context) {
        var todo = context.getTodo();
        var agentDescriptors = buildDescriptors();
        var proposedAgent = agentSelector.findRelevantAgent(todo.title, todo.description, agentDescriptors);
        AGENT currentAgent;
        currentAgent = verifyAgentPresence(agentDescriptors, proposedAgent);

        context.setCurrentAgent(currentAgent);
        Log.infov("Selected agent {0} for todo '{1}' ", currentAgent, todo.title);

        var todoId = context.getTodoId();
        switch (currentAgent) {
            case WEATHER,MOVIE -> {
                bus.publish(todoId,new AgentMessage(Kind.agent_message, todoId, "The " + currentAgent + " agent will look into your todo"));
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

    private static AGENT verifyAgentPresence(List<AgentDescriptor> agents, AGENT proposedAgent) {
        AGENT currentAgent;
        //verify selected agent is active
        if (agents.stream().anyMatch(agentDescriptor -> agentDescriptor.getAgent() == proposedAgent)) {
            currentAgent = proposedAgent;
        }
        else {
            currentAgent = AGENT.NONE;
        }
        return currentAgent;
    }

    private List<AgentDescriptor> buildDescriptors() {
        var result = new ArrayList<AgentDescriptor>();
        agentProducers.getCards().forEach(
                (agent, card) -> result.add( new AgentDescriptor(agent, card))
        );
        return result;
    }

    private void sendNoAgentMessage(String todoId) {
        bus.publish(todoId,new AgentMessage(Kind.agent_message, todoId, "No agent has been found for your need, sorry about that!"));
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
            case WEATHER, MOVIE -> {
                try {
                    return agentProducers.getA2aClient(currentAgent);
                } catch (A2AClientException e) {
                    bus.publish(todoId, new AgentMessage(
                            Kind.activity_log, todoId,
                            "A2A Client cannot be built for " + currentAgent + "\n" + e.getMessage())
                    );
                    throw new RuntimeException(e);
                }
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
            switch (context.getCurrentAgent()) {
                case NONE -> sendNoAgentMessage(todoId);
                default -> getCurrentClient(context).sendMessage(a2aMessage);
            }
        } catch (A2AClientException e) {
            bus.publish(todoId, new AgentMessage(Kind.activity_log, todoId, "Oops, something failed\n" + e.getMessage()));
        }
    }

    public void cancel(String todoId) {
        ClientAgentContext context = contextsHolder.getContextFromTodoId(todoId);
        if (context.getTaskId() !=  null) {
            try {
                getCurrentClient(context).cancelTask(new TaskIdParams(context.getTaskId()));
                context.resetOnTaskTerminalState();
            } catch (A2AClientException e) {
                //let's ignore cancellation exception, we are done on our side
            }
        }
    }

    public void receiveMessageFromAgent(Message responseMessage) {
        var context = contextsHolder.getContextFromContextId(responseMessage.getContextId());
        var payload = A2AUtils.extractTextFromParts(responseMessage.getParts());
        bus.publish(context.getTodoId(), new AgentMessage(Kind.agent_message, context.getTodoId(), payload));
    }

    public void sendTaskArtifacts(TaskArtifactUpdateEvent taskUpdateEvent) {
        var context = contextsHolder.getContextFromTaskId(taskUpdateEvent.getTaskId());
        StringBuilder textBuilder = new StringBuilder();
        var artifact = taskUpdateEvent.getArtifact();
        List<Artifact> artifacts;
        if (artifact != null) {
            textBuilder.append(extractTextFromParts(artifact.parts()));
        }
        else {
            bus.publish(context.getTodoId(), new AgentMessage(Kind.activity_log, context.getTodoId(), "Received empty task artifact update event for task " + taskUpdateEvent.getTaskId()));
        }
        var payload = textBuilder.toString();
        bus.publish(context.getTodoId(), new AgentMessage(Kind.agent_message, context.getTodoId(), payload));
    }

    public void sendTaskArtifacts(Task task) {
        var context = contextsHolder.getContextFromTaskId(task.getId());
        StringBuilder textBuilder = new StringBuilder();
        List<Artifact> artifacts = task.getArtifacts();
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                textBuilder.append(extractTextFromParts(artifact.parts()));
            }
        }
        else {
            textBuilder.append(extractTextFromParts(task.getStatus().message().getParts()));
        }
        var payload = textBuilder.toString();
        bus.publish(context.getTodoId(), new AgentMessage(Kind.agent_message, context.getTodoId(), payload));
    }

    public void sendInputRequired(String taskId, TaskStatus status) {
        var context = contextsHolder.getContextFromTaskId(taskId);
        var payload = A2AUtils.extractTextFromParts(status.message().getParts());
        bus.publish(context.getTodoId(), new AgentMessage(Kind.agent_message, context.getTodoId(), payload));
    }

    public void sendToActivityLog(TaskStatusUpdateEvent taskStatusUpdateEvent) {
        var taskId = taskStatusUpdateEvent.getTaskId();
        var context = contextsHolder.getContextFromTaskId(taskId);
        if (context == null) {
            context = contextsHolder.getContextFromContextId(taskStatusUpdateEvent.getContextId());
            context.setTaskId(taskId);
            contextsHolder.addOrUpdateContext(context);
        }
        switch (taskStatusUpdateEvent.getStatus().state()) {
            case COMPLETED, CANCELED, FAILED, REJECTED, UNKNOWN -> {
                context.resetOnTaskTerminalState();
            }
        }
        String payload = "Received status-update for " + taskId + ": "
                + taskStatusUpdateEvent.getStatus().state().asString()
                + (taskStatusUpdateEvent.isFinal()?" (final state)":"");
        bus.publish(context.getTodoId(), new AgentMessage(Kind.activity_log, context.getTodoId(), payload));
    }

    public void sendToActivityLog(TaskEvent taskEvent) {
        var taskId = taskEvent.getTask().getId();
        var context = contextsHolder.getContextFromTaskId(taskId);
        if (context == null) {
            context = contextsHolder.getContextFromContextId(taskEvent.getTask().getContextId());
            contextsHolder.addOrUpdateContext(context);
        }
        switch (taskEvent.getTask().getStatus().state()) {
            case COMPLETED, CANCELED, FAILED, REJECTED, UNKNOWN -> {
                context.resetOnTaskTerminalState();
            }
        }
        String payload = "Received task event for " + taskId + " with status " + taskEvent.getTask().getStatus().state();
        bus.publish(context.getTodoId(), new AgentMessage(Kind.activity_log, context.getTodoId(), payload));
    }

    public void sendToActivityLog(TaskArtifactUpdateEvent taskArtifactUpdateEvent) {
        var taskId = taskArtifactUpdateEvent.getTaskId();
        var context = contextsHolder.getContextFromTaskId(taskId);
        if (context == null) {
            context = contextsHolder.getContextFromContextId(taskArtifactUpdateEvent.getContextId());
            contextsHolder.addOrUpdateContext(context);
        }
        String payload = "Received task artifact update event for " + taskId + " (last chunk =  " + taskArtifactUpdateEvent.isLastChunk() + ")";
        bus.publish(context.getTodoId(), new AgentMessage(Kind.activity_log, context.getTodoId(), payload));
    }



}
