package io.quarkus.sample.audit;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.bind.Jsonb;
import jakarta.websocket.DecodeException;
import jakarta.websocket.Decoder;
import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;

public class AuditLogEncoder implements Encoder.Text<AuditLogSocket.AuditLogEntry>, Decoder.Text<AuditLogSocket.AuditLogEntry> {

    private final Jsonb jsonb;
    
    public AuditLogEncoder() {
        this.jsonb = CDI.current().select(Jsonb.class).get();
    }
    
    @Override
    public String encode(AuditLogSocket.AuditLogEntry object) throws EncodeException {
        return jsonb.toJson(object);        
    }

    @Override
    public AuditLogSocket.AuditLogEntry decode(String s) throws DecodeException {
        return jsonb.fromJson(s, AuditLogSocket.AuditLogEntry.class);
    }

    @Override
    public boolean willDecode(String s) {
        return true;
    }
    
    @Override
    public void init(EndpointConfig config) {

    }

    @Override
    public void destroy() {

    }
}
