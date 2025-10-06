package io.quarkus.sample.agents.webstocket;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.sample.Todo;
import io.quarkus.sample.agents.AgentProducers;
import io.quarkus.sample.agents.AgentsMediator;
import io.quarkus.sample.agents.ClientAgentContext;
import io.quarkus.vertx.LocalEventBusCodec;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.WebSocketConnection;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import jakarta.enterprise.context.control.ActivateRequestContext;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import io.quarkus.logging.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@WebSocket(path = "/todo-agent/{todoId}")
@ActivateRequestContext
public class TodoAgentWebSocket {

    @Inject
    AgentsMediator agentsMediator;

    @Inject
    EventBus eventBus;

    @Inject
    Event<ClientAgentContext> agentEvent;

    @Inject
    AgentProducers agentProducers;
    boolean agentsReady = false;

    Map<String, MessageConsumer<AgentMessage>> consumers = new ConcurrentHashMap<>();

    @OnOpen
    public AgentMessage onOpen(@PathParam String todoId, WebSocketConnection connection) {
        Log.info("Opening of websocket for todo " + todoId);


        MessageConsumer<AgentMessage> consumer = eventBus.consumer(todoId, message -> {
            Log.info("Message received from event bus: " + message.body());
            connection.sendText(message.body()).subscribe().asCompletionStage();
        });
        consumers.put(todoId, consumer);
        Todo todo = Todo.findById(Long.parseLong(todoId));

        agentsReady = !agentProducers.getCards().isEmpty();
        if (! agentsReady) {
            Log.warn("Unable to connect to a2a servers, did you start them?");
        } if (!agentsReady) {
            return noAIMessage(todoId);
        }
        else {
            agentEvent.fireAsync(new ClientAgentContext(todo, todoId));
            return new AgentMessage(Kind.agent_message, todoId, "Searching an agent for '" + todo.title + "'");
        }
    }

    private static AgentMessage noAIMessage(String todoId) {
        return new AgentMessage(Kind.agent_message, todoId, "Unable to find A2A agents (are they started), Do with AI is not available.");
    }

    @OnTextMessage
    void onTextMessage(@PathParam String todoId, AgentMessage agentMessage) {
        if (!agentsReady) {
            eventBus.publish(todoId, noAIMessage(todoId));
            return;
        }
        if (agentMessage.kind().equals(Kind.user_message)) {
            String userMessage = agentMessage.payload();
            // TODO: Should we check the todoId from the path against the one in the message ? Do we need both ?
            agentsMediator.passUserMessage(todoId, userMessage);
        } else if (agentMessage.kind().equals(Kind.cancel)) {
            agentsMediator.cancel(todoId);
        } else {
            System.out.println(">>>> Ignore !");
            // TODO: Ignore ? Maybe add an error type ? We also need to handle json parsing errors
        }
    }

    @OnClose
    void onClose(@PathParam String todoId) {
        Log.info("Closing of websocket");
        MessageConsumer<AgentMessage> consumer = consumers.remove(todoId);
        if (consumer != null) {
            consumer.unregister();
        }
    }

    public void init(@Observes StartupEvent event) {
        eventBus.registerDefaultCodec(AgentMessage.class,
                new LocalEventBusCodec<AgentMessage>() {

                });
    }
}