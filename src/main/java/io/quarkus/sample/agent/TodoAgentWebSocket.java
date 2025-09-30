package io.quarkus.sample.agent;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import jakarta.enterprise.context.control.ActivateRequestContext;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;


@WebSocket(path = "/todo-agent/{todoId}")
@ActivateRequestContext
public class TodoAgentWebSocket {
    private MultiEmitter<? super String> emitter;
    private Multi<String> agentStream;
    private final Jsonb jsonb = JsonbBuilder.create();
    
    @OnOpen
    Multi<String> onOpen(@PathParam String todoId) {
        this.agentStream = Multi.createFrom().emitter(emitter -> {
            this.emitter = emitter;
            this.sendActivityLogMessage(todoId,"ok we will find agents for todo " + todoId );
        });
        
        return agentStream;
    }

    @OnTextMessage
    void onTextMessage(@PathParam String todoId, String message) {
        AgentMessage agentMessage = jsonb.fromJson(message, AgentMessage.class);
        
        if(agentMessage.kind().equals(Kind.user_message)){
            String userMessage = agentMessage.payload();
            // TODO: Should we check the todoId from the path against the one in the message ? Do we need both ?
            this.sendAgentRequestMessage(todoId, "Parroting for " + todoId + " : " + userMessage);
        }else if(agentMessage.kind().equals(Kind.cancel)) {
            System.out.println(">>>> Cancel !");
            // TODO: Cancel 
        }else {
            System.out.println(">>>> Ignore !");
            // TODO: Ignore ? Maybe add an error type ? We also need to handle json parsing errors
        }
    }
    
    private void sendActivityLogMessage(String todoId, String message){
        emitter.emit(jsonb.toJson(new AgentMessage(Kind.activity_log, todoId, message)));
    }
    
    private void sendAgentRequestMessage(String todoId, String message){
        emitter.emit(jsonb.toJson(new AgentMessage(Kind.agent_request, todoId, message)));
    } 
}
