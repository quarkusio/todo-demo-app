package io.quarkus.sample.ai;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import io.quarkus.sample.Todo;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.stream.Collectors;

@ApplicationScoped
public class TodoDatabaseContentRetriever implements ContentRetriever {

    @Override
    public List<Content> retrieve(Query query) {
        List<Content> results = new ArrayList<>();
        List<Todo> all = Todo.listAll();
        JsonObject json = new JsonObject();
        List<String> titles = all.stream().map((t) -> t.title).collect(Collectors.toList());
        json.put("current todos", new JsonArray(titles));
        results.add(Content.from(json.toString()));
        return results;
    }
}
