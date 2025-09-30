package io.quarkus.sample.agent;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.sample.agents.AgentDispatcher;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.subscription.MultiEmitter;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.control.ActivateRequestContext;

import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.eclipse.microprofile.reactive.messaging.Channel;
import io.quarkus.logging.Log;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.OnOverflow;


@WebSocket(path = "/todo-agent/{todoId}")
@ActivateRequestContext
public class TodoAgentWebSocket {
    public static final String WEBSOCKET_CHANNEL = "to-websocket";
    //private MultiEmitter<? super String> emitter;
    private final Jsonb jsonb = JsonbBuilder.create();

    @Channel(WEBSOCKET_CHANNEL)
    @OnOverflow(OnOverflow.Strategy.BUFFER)
    private MutinyEmitter<String> emitter;

    @Channel(WEBSOCKET_CHANNEL)
    private Multi<String> agentStream;

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

    @OnOpen
    //@Incoming("to-webstocket")
    public Multi<String> onOpen(@PathParam String todoId) {
        Log.info("Opening of websocket");
//        this.agentStream = Multi.createFrom().emitter(emitter -> {
//            //this.emitter = emitter;
//            Log.info("emitter " + emitter);
//            this.sendActivityLogMessage(todoId,"ok we will find agents for todo " + todoId );
//        });
        return agentStream.onSubscription().invoke( () -> {
            this.sendActivityLogMessage(todoId, "ok we will find agents for todo " + todoId);
        } ).log();
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
        emitter.sendAndForget(jsonb.toJson(new AgentMessage(Kind.activity_log, todoId, message)));
        Log.info("Activity log message sent");
    }
    
    private void sendAgentRequestMessage(String todoId, String message){
        emitter.sendAndForget(jsonb.toJson(new AgentMessage(Kind.agent_request, todoId, message)));
    } 
}
