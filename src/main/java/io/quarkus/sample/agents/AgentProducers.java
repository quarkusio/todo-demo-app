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
import io.a2a.spec.A2AClientError;
import io.a2a.spec.A2AClientException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.TaskArtifactUpdateEvent;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskStatusUpdateEvent;
import io.a2a.spec.UpdateEvent;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Produces;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static io.quarkus.sample.agents.A2AUtils.extractTextFromParts;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
@ApplicationScoped
public class AgentProducers {

    private Map<AGENT, Client> agents = new HashMap<>();

    @Inject
    AgentsMediator agentsMediator;

    @Inject
    @ConfigProperty(name = "agent.movie.url")
    private String movieUrl;

    @Inject
    @ConfigProperty(name = "agent.weather.url")
    private String weatherUrl;

    @Produces
    public Map<AGENT,AgentCard> getCards() {
        Map<AGENT, AgentCard> cards = new HashMap<>();
        addAgentCard(AGENT.WEATHER, weatherUrl, cards);
        addAgentCard(AGENT.MOVIE, movieUrl, cards);
        return cards;
    }

    private void addAgentCard(AGENT agent, String url, Map<AGENT, AgentCard> cards) {
        try {
            AgentCard publicAgentCard = new A2ACardResolver(url).getAgentCard();
            Log.infov("Agent Card loaded: {0}", publicAgentCard.name());
            cards.put(agent, publicAgentCard);
        }
        catch (Exception e) {
            Log.warnv("Failed reach {0} at {1} because {2}", agent, url, e.getMessage());
        }
    }


    public Client getA2aClient(AGENT agent) throws A2AClientException {
        var client = agents.get(agent);
        if (client == null) {
            client = buildA2aClient(agent);
            agents.put(agent, client);
        }
        return client;
    }

    private Client buildA2aClient(AGENT agent) throws A2AClientException {
        // Create consumers for handling client events
        List<BiConsumer<ClientEvent, AgentCard>> consumers = getConsumers();

        // Create error handler for streaming errors
        Consumer<Throwable> streamingErrorHandler = (error) -> {
            Log.errorv("JDK streaming error occured {0}", error.getMessage());
            //error.printStackTrace();
        };
        ClientConfig clientConfig = new ClientConfig.Builder()
                .setAcceptedOutputModes(List.of("Text"))
                .build();

        // Create the client with both JSON-RPC and gRPC transport support.
        // The A2A server agent's preferred transport is gRPC, since the client
        // also supports gRPC, this is the transport that will get used
        Client client = Client.builder(getCard(agent))
                .addConsumers(consumers)
                .streamingErrorHandler(streamingErrorHandler)
                .withTransport(JSONRPCTransport.class,
                        new JSONRPCTransportConfig())
                .clientConfig(clientConfig)
                .build();
        return client;
    }

    private AgentCard getCard(AGENT agent) {
        return getCards().get(agent);
    }

    private List<BiConsumer<ClientEvent, AgentCard>> getConsumers() {
        List<BiConsumer<ClientEvent, AgentCard>> consumers = new ArrayList<>();
        consumers.add(
                (event, agentCard) -> {
                    if (event instanceof MessageEvent messageEvent) {
                        Message responseMessage = messageEvent.getMessage();
                        String text = extractTextFromParts(responseMessage.getParts());
                        Log.infov("Received message: {0}", text);
                        agentsMediator.receiveMessageFromAgent(responseMessage);
                        //messageResponse.complete(text);
                    } else if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                        UpdateEvent updateEvent = taskUpdateEvent.getUpdateEvent();
                        Log.infov(
                                "Received TaskUpdateEvent for {0}, status: {1}",
                                taskUpdateEvent.getTask().getId(),
                                taskUpdateEvent.getTask().getStatus().state()
                        );
                        if (updateEvent
                                instanceof TaskStatusUpdateEvent taskStatusUpdateEvent) {
                            var status = taskStatusUpdateEvent.getStatus();
                            Log.infov( "Received status-update: {0} ", status.state());
                            agentsMediator.sendToActivityLog(taskStatusUpdateEvent);
                            if (taskStatusUpdateEvent.isFinal()) {
                                agentsMediator.sendTaskArtifacts(taskUpdateEvent.getTask());
                            }
                            else if (status.state() == TaskState.INPUT_REQUIRED) {
                                agentsMediator.sendInputRequired(taskStatusUpdateEvent.getTaskId(), status);
                            }
                        } else if (updateEvent instanceof TaskArtifactUpdateEvent
                                taskArtifactUpdateEvent) {
                            agentsMediator.sendToActivityLog(taskArtifactUpdateEvent);
                            agentsMediator.sendTaskArtifacts(taskArtifactUpdateEvent);
                            Log.infov("Received artifact-update for task {0}: {1}", taskArtifactUpdateEvent.getTaskId(), taskArtifactUpdateEvent.getArtifact().name());
                        }
                    } else if (event instanceof TaskEvent taskEvent) {
                        var task = taskEvent.getTask();
                        Log.infov("Received task event for {0}: status {1}", task.getId(), task.getStatus().state());
                        var state = task.getStatus().state();
                        agentsMediator.sendToActivityLog(taskEvent);
                        switch (state) {
                            case COMPLETED -> {
                                agentsMediator.sendTaskArtifacts(task);
                            }
                            case INPUT_REQUIRED -> {
                                agentsMediator.sendInputRequired(task.getId(), task.getStatus());
                            }
                        }
                    }
                });
        return consumers;
    }

}
