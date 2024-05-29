package io.quarkus.sample.audit;

import io.quarkus.logging.Log;
import io.quarkus.sample.Todo;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.impl.ConcurrentHashSet;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import java.util.Set;

@ServerEndpoint(value = "/audit", encoders = AuditLogEncoder.class, decoders = AuditLogEncoder.class)
@ApplicationScoped
public class AuditLogSocket {

    Set<Session> sessions = new ConcurrentHashSet<>();
    
    public record AuditLogEntry(AuditType type, Todo todo) {
    }
    
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
    }
    
    @ConsumeEvent("TODO_ADDED")               
    public void add(Todo todo) {
        log(new AuditLogEntry(AuditType.TODO_ADDED, todo));
    }
    
    @ConsumeEvent("TODO_CHECKED")
    public void check(Todo todo) {
        log(new AuditLogEntry(AuditType.TODO_CHECKED, todo));
    }
    
    @ConsumeEvent("TODO_UNCHECKED")
    public void uncheck(Todo todo) {
        log(new AuditLogEntry(AuditType.TODO_UNCHECKED, todo));
    }
    
    @ConsumeEvent("TODO_REMOVED")
    public void remove(Todo todo) {
        log(new AuditLogEntry(AuditType.TODO_REMOVED, todo));
    }
    
    private void log(AuditLogEntry entry){
        sessions.forEach(s -> {
            s.getAsyncRemote().sendObject(entry, result -> {
                if (result.getException() != null) {
                    Log.error("Unable to send message: " + result.getException());
                }
            });
        });
        
    }
    
}
