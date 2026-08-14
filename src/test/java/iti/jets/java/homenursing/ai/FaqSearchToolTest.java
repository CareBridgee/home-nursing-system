package iti.jets.java.homenursing.ai;

import iti.jets.java.homenursing.ai.rag.FaqSearchTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FaqSearchToolTest {

    @Mock
    private VectorStore vectorStore;

    private FaqSearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new FaqSearchTool(vectorStore);
        ReflectionTestUtils.setField(tool, "topK", 4);
        ReflectionTestUtils.setField(tool, "similarityThreshold", 0.35);
    }

    @Test
    void searchFaqs_returnsJoinedSectionsWithMetadata() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("Refunds are processed within 3 days.", Map.of("section", "Pricing")),
                new Document("Cancel at any time before the visit.", Map.of())));

        String result = tool.searchFaqs("What is the refund policy?");

        assertTrue(result.contains("[Section: Pricing] Refunds are processed within 3 days."));
        assertTrue(result.contains("[Section: ?] Cancel at any time before the visit."));
        assertTrue(result.contains("\n---\n"));

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertEquals("What is the refund policy?", captor.getValue().getQuery());
        assertEquals(4, captor.getValue().getTopK());
        assertEquals(0.35, captor.getValue().getSimilarityThreshold());
    }

    @Test
    void searchFaqs_emptyResults_returnsNoRelevantMessage() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        String result = tool.searchFaqs("How do I reset my password?");

        assertEquals("No relevant FAQ content found.", result);
    }

    @Test
    void searchFaqs_vectorStoreThrows_returnsUnavailableMessage() {
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new RuntimeException("embeddings down"));

        String result = tool.searchFaqs("Any question");

        assertEquals("FAQ search is temporarily unavailable. Please ask again in a moment.", result);
    }
}