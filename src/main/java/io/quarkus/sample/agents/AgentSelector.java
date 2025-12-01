package io.quarkus.sample.agents;

import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

import java.util.List;

@RegisterAiService
public interface AgentSelector {
    @UserMessage("""
           You are an agent that must decide which service to call.
           Your role is to analyze the task title and description given at the end and find the most suitable service to call.
           
           If you are uncertain or do not find a suitable service, answer NONE.
           
           The services are represented by an enum, this is what you must return.
           Do not return anything else. Do not even return a newline or a leading field. Only the enum.
           
           There is the list of services and their description.
           {#for a in agents}
           {a.agent}:
           - name: {a.name}
           - description: {a.description ?: ''}
           
           {/for}
           
           Here is a list of examples and expected output
           
           Example 1:
           Todo title: Find holidays for next winter
           Todo description:
           NONE
           
           Example 2:
           Todo title: Book a movie theater ticket for tonight
           Todo description: I like horror movies and superheroes movies
           NONE
            
           Example 3:
           Todo title: What is the weather in New York?
           Todo description: 
           WEATHER
          
          Here is the task I want you to find a service for  
           Task title: {todoTitle}
           Task description: {todoDescription}
           """)
    AGENT findRelevantAgent(String todoTitle, String todoDescription, List<AgentDescriptor> agents);
}
