package io.quarkus.sample;

import io.quarkus.sample.audit.AuditType;
import io.quarkus.panache.common.Sort;
import io.quarkus.sample.ai.TodoAiService;
import io.vertx.core.eventbus.EventBus;
import jakarta.inject.Inject;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api")
@Tag(name = "Todo Resource", description = "All Todo Operations")
public class TodoResource {

    @Inject
    TodoAiService ai; 
    
    @Inject
    EventBus bus; 
    
    @OPTIONS
    @Operation(hidden = true)
    public Response opt() {
        return Response.ok().build();
    }

    @GET
    @Operation(description = "Get all the todos")
    public List<Todo> getAll() {
        return Todo.listAll(Sort.by("order"));
    }

    @GET
    @Path("/{id}")
    @Operation(description = "Get a specific todo by id")
    public Todo getOne(@PathParam("id") Long id) {
        Todo entity = Todo.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Todo with id of " + id + " does not exist.", Status.NOT_FOUND);
        }
        return entity;
    }

    @POST
    @Transactional
    @Operation(description = "Create a new todo")
    public Response create(@Valid Todo item) {
        item.persist();
        bus.publish(AuditType.TODO_ADDED.name(), item);
        return Response.status(Status.CREATED).entity(item).build();
    }

    @PATCH
    @Path("/{id}")
    @Transactional
    @Operation(description = "Update an exiting todo")
    public Response update(@Valid Todo todo, @PathParam("id") Long id) {
        Todo entity = Todo.findById(id);
        if(entity.completed!=todo.completed && todo.completed){
            bus.publish(AuditType.TODO_CHECKED.name(), todo);
        }else if(entity.completed!=todo.completed && !todo.completed){
            bus.publish(AuditType.TODO_UNCHECKED.name(), todo);
        }
        
        entity.id = id;
        entity.completed = todo.completed;
        entity.order = todo.order;
        entity.title = todo.title;
        entity.description = todo.description;
        entity.url = todo.url;
        
        return Response.ok(entity).build();
    }

    @DELETE
    @Transactional
    @Operation(description = "Remove all completed todos")
    public Response deleteCompleted() {
        Todo.deleteCompleted();
        return Response.noContent().build();
    }

    @DELETE
    @Transactional
    @Path("/{id}")
    @Operation(description = "Delete a specific todo")
    public Response deleteOne(@PathParam("id") Long id) {
        Todo entity = Todo.findById(id);
        if (entity == null) {
            throw new WebApplicationException("Todo with id of " + id + " does not exist.", Status.NOT_FOUND);
        }
        entity.delete();
        bus.publish(AuditType.TODO_REMOVED.name(), entity);
        return Response.noContent().build();
    }

    @GET
    @Path("/suggest")
    @Operation(description = "Suggest something to do")
    @Transactional
    public Todo suggest() {
        Todo suggestion = new Todo();
        
        String title = ai.suggestSomethingTodo(1,"Quarkus");
        title = title.trim();
        suggestion.title = title;
        suggestion.persistAndFlush();        
        return suggestion;
    }
    
}