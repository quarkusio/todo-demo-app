package io.quarkus.sample.agents;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.quarkus.logging.Log;

import static io.quarkus.sample.agents.A2AUtils.*;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ApplicationScoped
public class WeatherAgentProducer {

    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface WeatherAgent {}

    @Inject AgentDispatcher agentDispatcher;

    @Inject
    @ConfigProperty(name = "agent.weather.url")
    private String url;

    @Produces @WeatherAgent
    public AgentCard getCard() throws A2AClientError {
        AgentCard publicAgentCard = new A2ACardResolver(url).getAgentCard();
        Log.infov("Weather card loaded: {0}", publicAgentCard.name());
        return publicAgentCard;
    }


    @Produces @WeatherAgent
    public Client getA2aClient() throws A2AClientError, A2AClientException {
        // Create a CompletableFuture to handle async response
        final CompletableFuture<String> messageResponse
                = new CompletableFuture<>();

        // Create consumers for handling client events
        List<BiConsumer<ClientEvent, AgentCard>> consumers
                = getConsumers(messageResponse);

        // Create error handler for streaming errors
        Consumer<Throwable> streamingErrorHandler = (error) -> {
            Log.errorv("JDK streaming error occured {0}", error.getMessage());
            Log.errorv("JDK streaming error occured {0}", error);
            error.printStackTrace();
            //messageResponse.completeExceptionally(error);
        };
        ClientConfig clientConfig = new ClientConfig.Builder()
                .setAcceptedOutputModes(List.of("Text"))
                .build();

        // Create the client with both JSON-RPC and gRPC transport support.
        // The A2A server agent's preferred transport is gRPC, since the client
        // also supports gRPC, this is the transport that will get used
        Client client = Client.builder(getCard())
                .addConsumers(consumers)
                .streamingErrorHandler(streamingErrorHandler)
                .withTransport(JSONRPCTransport.class,
                        new JSONRPCTransportConfig())
                .clientConfig(clientConfig)
                .build();
        return client;
    }

    private List<BiConsumer<ClientEvent, AgentCard>> getConsumers(
            final CompletableFuture<String> messageResponse) {
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();
        consumers.add(
                (event, agentCard) -> {
                    if (event instanceof MessageEvent messageEvent) {
                        Message responseMessage = messageEvent.getMessage();
                        String text = extractTextFromParts(responseMessage.getParts());
                        System.out.println("Received message: " + text);
                        agentDispatcher.receiveMessageFromAgent(responseMessage);
                        //messageResponse.complete(text);
                    } else if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                        UpdateEvent updateEvent = taskUpdateEvent.getUpdateEvent();
                        if (updateEvent
                                instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
                            System.out.println(
                                    "Received status-update: "
                                            + taskStatusUpdateEvent.getStatus().state().asString());
                            agentDispatcher.sendToActivityLog(taskStatusUpdateEvent);
                            if (taskStatusUpdateEvent.isFinal()) {
                                //agentDispatcher.sendTaskArtifacts(taskUpdateEvent);
//                                StringBuilder textBuilder = new StringBuilder();
//                                List<Artifact> artifacts
//                                        = taskUpdateEvent.getTask().getArtifacts();
//                                for (Artifact artifact : artifacts) {
//                                    textBuilder.append(extractTextFromParts(artifact.parts()));
//                                }
//                                String text = textBuilder.toString();
                                //messageResponse.complete(text);
                            }
                        } else if (updateEvent instanceof TaskArtifactUpdateEvent
                                taskArtifactUpdateEvent) {
                            agentDispatcher.sendTaskArtifacts(taskUpdateEvent);
//                            List<Part<?>> parts = taskArtifactUpdateEvent
//                                    .getArtifact()
//                                    .parts();
//                            String text = extractTextFromParts(parts);
//                            System.out.println("Received artifact-update: " + text);
                        }
                    } else if (event instanceof TaskEvent taskEvent) {
                        System.out.println("Received task event: "
                                + taskEvent.getTask().getId());
                        agentDispatcher.sendToActivityLog(taskEvent);
                    }
                });
        return consumers;
    }

}
