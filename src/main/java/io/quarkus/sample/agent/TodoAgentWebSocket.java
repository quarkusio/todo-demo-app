package io.quarkus.sample.agent;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.sample.agents.AgentDispatcher;
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
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import io.quarkus.logging.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@WebSocket(path = "/todo-agent/{todoId}")
@ActivateRequestContext
public class TodoAgentWebSocket {

    @Inject
    EventBus eventBus;

    Map<String, MessageConsumer<AgentMessage>> consumers = new ConcurrentHashMap<>();

    @OnOpen
    public AgentMessage onOpen(@PathParam String todoId, WebSocketConnection connection) {
        Log.info("Opening of websocket");

        MessageConsumer<AgentMessage> consumer = eventBus.consumer(todoId, message -> {
            Log.info("Message received from event bus: " + message.body());
            connection.sendText(message.body()).subscribe().asCompletionStage();
        });
        consumers.put(todoId, consumer);
        return new AgentMessage(Kind.activity_log, todoId, "Searching a agent to work on TODO " + todoId);
    }

    @OnTextMessage
    void onTextMessage(@PathParam String todoId, AgentMessage agentMessage) {
        if (agentMessage.kind().equals(Kind.user_message)) {
            String userMessage = agentMessage.payload();
            // TODO: Should we check the todoId from the path against the one in the message ? Do we need both ?
            this.sendAgentRequestMessage(todoId, "Parroting for " + todoId + " : " + userMessage);
        } else if (agentMessage.kind().equals(Kind.cancel)) {
            System.out.println(">>>> Cancel !");
            // TODO: Cancel
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

    private void sendAgentRequestMessage(String todoId, String message) {
        eventBus.publish(todoId, new AgentMessage(Kind.agent_request, todoId, message));
    }

    private void sendAgentActivityMessage(String todoId, String message) {
        eventBus.publish(todoId, new AgentMessage(Kind.activity_log, todoId, message));
    }

    public void init(@Observes StartupEvent event) {
        eventBus.registerDefaultCodec(AgentMessage.class,
                new LocalEventBusCodec<AgentMessage>() {

                });
    }
}
    //private MultiEmitter<? super String> emitter;
    //private Multi<String> agentStream;

    //@Inject
    //private AgentDispatcher agents;

    // init message Multi.createFromItem()
    // ensuite Multi du AI service
    // concatener les deux Multis
    // Multi.createBy().concatenating().streams(Multi.createFrom().item("Starting work"), aiServiceMulti);
    //formatter les messages {kind: initialize kind:token
    // when end of message in multi, I send \n\n
    // kind
    //initiliazize
    //cancel
    //user_request


    //MultiEmitter / Emittpublicer
    // by default in memory
    //@Channel(csdcds)
    //MutinyEmitter mEmitter;

    //@Channel(csdcds)
    //Multi<String> multi;

//    void init(@Observes StartupEvent event) {
//        // Subscribe manually
//        agentStream.subscribe().with(
//                item -> System.out.println("Received: " + item),
//                failure -> System.err.println("Error: " + failure)
//        );
//    }

