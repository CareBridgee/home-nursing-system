package iti.jets.java.homenursing.ai;

import iti.jets.java.homenursing.ai.rag.FaqIngestionService;
import iti.jets.java.homenursing.service.TokenService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FaqIngestionServiceTest {

    private static final String COUNT_SQL = "SELECT COUNT(*) FROM vector_store";
    private static final String DELETE_SQL = """
            DELETE FROM vector_store
            WHERE metadata->>'source' = 'faqs.pdf'
               OR metadata->>'file_name' = 'faqs.pdf'
            """;

    @Mock
    private VectorStore vectorStore;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private TokenService tokenService;
    @Mock
    private ResourceLoader resourceLoader;
    @Mock
    private ApplicationArguments applicationArguments;

    private FaqIngestionService service;

    private void newService(Resource resource) {
        when(resourceLoader.getResource("classpath:documents/faqs.pdf")).thenReturn(resource);
        service = new FaqIngestionService(vectorStore, jdbcTemplate, tokenService, resourceLoader);
        ReflectionTestUtils.setField(service, "retryDelayMs", 60_000L);
        ReflectionTestUtils.setField(service, "maxAttempts", 10);
    }

    private Resource pdfResource(List<String> lines) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                cs.newLineAtOffset(50, 750);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -16);
                }
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return new ByteArrayResource(out.toByteArray());
        }
    }

    private Resource contentPdf() throws Exception {
        return pdfResource(List.of(
                "Gift card pricing details",
                "What is the refund policy?",
                "Full refund within 24 hours of booking.",
                "1. Pricing",
                "How much does a visit cost?",
                "25 USD per visit hour.",
                "Is there a discount?",
                "2. Cancellation",
                ""));
    }

    private Resource orphanOnlyPdf() throws Exception {
        return pdfResource(List.of("Just some random content line", "Another line without any questions"));
    }

    @Test
    void run_pdfMissing_skipsIngestion() throws Exception {
        Resource missing = org.mockito.Mockito.mock(Resource.class);
        when(missing.exists()).thenReturn(false);
        newService(missing);

        service.run(applicationArguments);

        verifyNoInteractions(vectorStore, tokenService, jdbcTemplate);
    }

    @Test
    void run_submitsIngestionSynchronously() throws Exception {
        newService(contentPdf());
        when(tokenService.get(anyString())).thenReturn(null);
        when(jdbcTemplate.queryForObject(COUNT_SQL, Integer.class)).thenReturn(0);

        service.run(applicationArguments);

        ArgumentCaptor<List<Document>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, timeout(5000)).add(chunksCaptor.capture());
        assertEquals(2, chunksCaptor.getValue().size());
        assertEquals("faqs.pdf", chunksCaptor.getValue().get(0).getMetadata().get("source"));
        assertTrue(chunksCaptor.getValue().get(0).getMetadata().containsKey("section"));
        assertTrue(chunksCaptor.getValue().get(0).getMetadata().containsKey("page"));
        verify(tokenService, timeout(5000)).set(anyString(), eq("yes"), eq(Duration.ofDays(30)));
        assertFalse((Boolean) ReflectionTestUtils.getField(service, "pending"));
    }

    @Test
    void retryIfPending_notPending_returnsImmediately() throws Exception {
        newService(contentPdf());
        ReflectionTestUtils.setField(service, "pending", false);

        service.retryIfPending();

        assertEquals(0, ReflectionTestUtils.getField(service, "attempts"));
        verifyNoInteractions(vectorStore, tokenService, jdbcTemplate);
    }

    @Test
    void retryIfPending_overMaxAttempts_givesUp() throws Exception {
        newService(contentPdf());
        ReflectionTestUtils.setField(service, "pending", true);
        ReflectionTestUtils.setField(service, "attempts", 10);

        service.retryIfPending();

        assertEquals(11, ReflectionTestUtils.getField(service, "attempts"));
        assertFalse((Boolean) ReflectionTestUtils.getField(service, "pending"));
        verifyNoInteractions(vectorStore, tokenService, jdbcTemplate);
    }

    @Test
    void retryIfPending_withinLimit_resubmitsIngestion() throws Exception {
        newService(contentPdf());
        ReflectionTestUtils.setField(service, "pending", true);
        ReflectionTestUtils.setField(service, "attempts", 0);
        when(tokenService.get(anyString())).thenReturn(null);
        when(jdbcTemplate.queryForObject(COUNT_SQL, Integer.class)).thenReturn(0);

        service.retryIfPending();

        assertTrue((Integer) ReflectionTestUtils.getField(service, "attempts") >= 1);
        verify(vectorStore, timeout(5000)).add(any(List.class));
        verify(tokenService, timeout(5000)).set(anyString(), eq("yes"), eq(Duration.ofDays(30)));
    }

    @Test
    void ingestSafely_markerPresent_skipsVectorStore() throws Exception {
        newService(contentPdf());
        when(tokenService.get(anyString())).thenReturn("yes");

        ReflectionTestUtils.invokeMethod(service, "ingestSafely");

        verify(vectorStore, never()).add(any(List.class));
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Integer.class));
        assertFalse((Boolean) ReflectionTestUtils.getField(service, "pending"));
    }

    @Test
    void ingestSafely_noContentExtracted_skipsIngestion() throws Exception {
        newService(orphanOnlyPdf());
        when(tokenService.get(anyString())).thenReturn(null);

        ReflectionTestUtils.invokeMethod(service, "ingestSafely");

        verify(vectorStore, never()).add(any(List.class));
        verify(jdbcTemplate, never()).queryForObject(anyString(), eq(Integer.class));
        assertFalse((Boolean) ReflectionTestUtils.getField(service, "pending"));
    }

    @Test
    void ingestSafely_cleanPdf_noDroppedLines() throws Exception {
        newService(pdfResource(List.of(
                "1. Pricing",
                "How much does a visit cost?",
                "25 USD per visit hour.")));
        when(tokenService.get(anyString())).thenReturn(null);
        when(jdbcTemplate.queryForObject(COUNT_SQL, Integer.class)).thenReturn(0);

        ReflectionTestUtils.invokeMethod(service, "ingestSafely");

        verify(jdbcTemplate, never()).update(anyString());
        verify(vectorStore).add(any(List.class));
        verify(tokenService).set(anyString(), eq("yes"), eq(Duration.ofDays(30)));
    }

    @Test
    void ingestSafely_existingRows_deleteStaleDocs() throws Exception {
        newService(contentPdf());
        when(tokenService.get(anyString())).thenReturn(null);
        when(jdbcTemplate.queryForObject(COUNT_SQL, Integer.class)).thenReturn(5);
        when(jdbcTemplate.update(DELETE_SQL)).thenReturn(3);

        ReflectionTestUtils.invokeMethod(service, "ingestSafely");

        verify(jdbcTemplate).update(DELETE_SQL);
        verify(vectorStore).add(any(List.class));
        verify(tokenService).set(anyString(), eq("yes"), eq(Duration.ofDays(30)));
    }

    @Test
    void ingestSafely_countNull_skipsStaleDelete() throws Exception {
        newService(contentPdf());
        when(tokenService.get(anyString())).thenReturn(null);
        when(jdbcTemplate.queryForObject(COUNT_SQL, Integer.class)).thenReturn(null);

        ReflectionTestUtils.invokeMethod(service, "ingestSafely");

        verify(jdbcTemplate, never()).update(anyString());
        verify(vectorStore).add(any(List.class));
        verify(tokenService).set(anyString(), eq("yes"), eq(Duration.ofDays(30)));
    }

    @Test
    void ingestSafely_vectorStoreThrows_swallowsException() throws Exception {
        newService(contentPdf());
        when(tokenService.get(anyString())).thenReturn(null);
        when(jdbcTemplate.queryForObject(COUNT_SQL, Integer.class)).thenReturn(0);
        doThrow(new RuntimeException("vector store down")).when(vectorStore).add(any(List.class));

        ReflectionTestUtils.invokeMethod(service, "ingestSafely");

        verify(tokenService, never()).set(anyString(), anyString(), any());
        assertFalse((Boolean) ReflectionTestUtils.getField(service, "pending"));
    }
}