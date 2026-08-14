package iti.jets.java.homenursing.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class FaqSearchTool {

    private static final Logger log = LoggerFactory.getLogger(FaqSearchTool.class);

    private final VectorStore vectorStore;

    @Value("${faq.search.top-k:4}")
    private int topK;

    @Value("${faq.search.similarity-threshold:0.35}")
    private double similarityThreshold;

    public FaqSearchTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Searches the FAQ knowledge base for platform policies, pricing, booking, and cancellation rules.")
    public String searchFaqs(@ToolParam(description = "A simple string of the user's question. Do not use JSON format.") String query) {
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .similarityThreshold(similarityThreshold)
                            .build()
            );

            if (results.isEmpty()) {
                return "No relevant FAQ content found.";
            }

            return results.stream()
                    .map(d -> "[Section: " + d.getMetadata().getOrDefault("section", "?") + "] " + d.getText())
                    .collect(Collectors.joining("\n---\n"));
        } catch (Exception e) {
            log.warn("FAQ search failed: {}", e.getMessage());
            return "FAQ search is temporarily unavailable. Please ask again in a moment.";
        }
    }
}
