package io.quarkus.sample.agents;

import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

import java.util.List;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public class A2AUtils {
    public static String extractTextFromParts(final List<Part<?>> parts) {
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
