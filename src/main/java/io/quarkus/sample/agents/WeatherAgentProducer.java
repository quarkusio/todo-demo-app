package io.quarkus.sample.agents;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.spec.A2AClientError;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Artifact;
import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.TextPart;
import io.a2a.spec.UpdateEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Qualifier;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ApplicationScoped
public class WeatherAgentProducer {
    //private static final Logger LOG = Logger.getLogger(WeatherAgentProducer.class);

    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Qualifier
    public @interface WeatherAgent {}

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
    public Client getA2aClient() throws A2AClientError {
        // Create a CompletableFuture to handle async response
        final CompletableFuture<String> messageResponse
                = new CompletableFuture<>();

        // Create consumers for handling client events
        List<BiConsumer<ClientEvent, AgentCard>> consumers
                = getConsumers(messageResponse);

        // Create error handler for streaming errors
        Consumer<Throwable> streamingErrorHandler = (error) -> {
            System.out.println("Streaming error occurred: " + error.getMessage());
            error.printStackTrace();
            messageResponse.completeExceptionally(error);
        };
        //Client client = Client.builder(getCard())
        return null;
    }

    private static List<BiConsumer<ClientEvent, AgentCard>> getConsumers(
            final CompletableFuture<String> messageResponse) {
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();
        consumers.add(
                (event, agentCard) -> {
                    if (event instanceof MessageEvent messageEvent) {
                        Message responseMessage = messageEvent.getMessage();
                        String text = extractTextFromParts(responseMessage.getParts());
                        System.out.println("Received message: " + text);
                        messageResponse.complete(text);
                    } else if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                        UpdateEvent updateEvent = taskUpdateEvent.getUpdateEvent();
                        if (updateEvent
                                instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
                            System.out.println(
                                    "Received status-update: "
                                            + taskStatusUpdateEvent.getStatus().state().asString());
                            if (taskStatusUpdateEvent.isFinal()) {
                                StringBuilder textBuilder = new StringBuilder();
                                List<Artifact> artifacts
                                        = taskUpdateEvent.getTask().getArtifacts();
                                for (Artifact artifact : artifacts) {
                                    textBuilder.append(extractTextFromParts(artifact.parts()));
                                }
                                String text = textBuilder.toString();
                                messageResponse.complete(text);
                            }
                        } else if (updateEvent instanceof TaskArtifactUpdateEvent
                                taskArtifactUpdateEvent) {
                            List<Part<?>> parts = taskArtifactUpdateEvent
                                    .getArtifact()
                                    .parts();
                            String text = extractTextFromParts(parts);
                            System.out.println("Received artifact-update: " + text);
                        }
                    } else if (event instanceof TaskEvent taskEvent) {
                        System.out.println("Received task event: "
                                + taskEvent.getTask().getId());
                    }
                });
        return consumers;
    }

    private static String extractTextFromParts(final List<Part<?>> parts) {
        final StringBuilder textBuilder = new StringBuilder();
        if (parts != null) {
            for (final Part<?> part : parts) {
                if (part instanceof TextPart textPart) {
                    textBuilder.append(textPart.getText());
                }
            }
        }
        return textBuilder.toString();
    }
}
