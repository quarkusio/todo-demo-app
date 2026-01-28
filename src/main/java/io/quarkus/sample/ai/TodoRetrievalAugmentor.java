package io.quarkus.sample.ai;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;

@ApplicationScoped
public class TodoRetrievalAugmentor implements Supplier<RetrievalAugmentor> {

    @Inject
    TodoDatabaseContentRetriever contentRetriever;

    @Override
    public RetrievalAugmentor get() {
        return DefaultRetrievalAugmentor.builder()
                .contentRetriever(contentRetriever)
                .build();
    }
}
