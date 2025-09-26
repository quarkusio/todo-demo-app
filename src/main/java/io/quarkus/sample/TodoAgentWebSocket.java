package io.quarkus.sample;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.control.ActivateRequestContext;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;


@WebSocket(path = "/todo-agent/{todoId}")
@ActivateRequestContext
public class TodoAgentWebSocket {
    private MultiEmitter<? super String> emitter;
    private Multi<String> agentStream;

    @OnOpen
    Multi<String> onOpen(@PathParam String todoId) {
        this.agentStream = Multi.createFrom().emitter(emitter -> {
            this.emitter = emitter;
            // The emitter is now available for use elsewhere
        });
        emitter.emit("ok we will find agents for todo " + todoId );
        return agentStream;
    }

    @OnTextMessage
    void onTextMessage(@PathParam String todoId, String message) {
        //do something with the agent context
        emitter.emit("Parroting for " + todoId + " : " + message);
    }
}
