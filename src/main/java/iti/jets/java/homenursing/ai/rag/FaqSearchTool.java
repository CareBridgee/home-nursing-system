package iti.jets.java.homenursing.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FaqSearchTool {

    private final VectorStore vectorStore;

    public FaqSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Searches the FAQ knowledge base for platform policies, pricing, booking, and cancellation rules.")
    public String searchFaqs(@ToolParam(description = "A simple string of the user's question. Do not use JSON format.") String query) {
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(query)
                        .topK(4)
                        .similarityThreshold(0.5)
                        .build()
        );

        if (results.isEmpty()) {
            return "No relevant FAQ content found.";
        }

        return results.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n---\n"));
    }
}