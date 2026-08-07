package iti.jets.java.homenursing.ai.rag;


import iti.jets.java.homenursing.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Component
public class FaqIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FaqIngestionService.class);

    private static final Pattern SECTION_HEADER = Pattern.compile("^\\d+\\.\\s+[A-Z].*");
    private static final Pattern QUESTION_LINE = Pattern.compile("^.*\\?\\s*$");
    private static final Duration MARKER_TTL = Duration.ofDays(30);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final TokenService tokenService;
    private final Resource faqPdf;
    private final ExecutorService ingestionExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Value("${faq.ingestion.retry-delay-ms:60000}")
    private long retryDelayMs;

    @Value("${faq.ingestion.max-attempts:10}")
    private int maxAttempts;

    private int attempts;
    private boolean pending;

    public FaqIngestionService(VectorStore vectorStore,
                               JdbcTemplate jdbcTemplate,
                               TokenService tokenService,
                               org.springframework.core.io.ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.tokenService = tokenService;
        this.faqPdf = resourceLoader.getResource("classpath:documents/faqs.pdf");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!faqPdf.exists()) {
            log.warn("classpath:documents/faqs.pdf not found; skipping FAQ ingestion");
            return;
        }
        attempts = 0;
        pending = true;
        ingestionExecutor.submit(this::ingestSafely);
    }

    @Scheduled(fixedDelayString = "${faq.ingestion.retry-delay-ms:60000}")
    public void retryIfPending() {
        if (!pending) {
            return;
        }
        attempts++;
        if (attempts > maxAttempts) {
            pending = false;
            log.error("FAQ ingestion failed after {} attempts; giving up until next restart", maxAttempts);
            return;
        }
        log.warn("FAQ ingestion still pending; retry {}/{}", attempts, maxAttempts);
        ingestionExecutor.submit(this::ingestSafely);
    }

    private void ingestSafely() {
        try {
            String hash = sha256(faqPdf);
            String markerKey = "faq:ingested:" + hash;
            if (tokenService.get(markerKey) != null) {
                log.info("FAQs already ingested (hash {}); nothing to do", hash);
                pending = false;
                return;
            }

            List<Document> chunks = buildChunks();
            if (chunks.isEmpty()) {
                log.warn("No FAQ content extracted from faqs.pdf; skipping ingestion");
                pending = false;
                return;
            }

            Integer existing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
            if (existing != null && existing > 0) {
                int removed = jdbcTemplate.update("""
                        DELETE FROM vector_store
                        WHERE metadata->>'source' = 'faqs.pdf'
                           OR metadata->>'file_name' = 'faqs.pdf'
                        """);
                log.info("Removed {} stale FAQ documents before re-ingestion", removed);
            }

            vectorStore.add(chunks);
            tokenService.set(markerKey, "yes", MARKER_TTL);
            log.info("Ingested {} FAQ chunks (hash {})", chunks.size(), hash);
            pending = false;
        } catch (Exception e) {
            log.warn("FAQ ingestion attempt failed: {}", e.getMessage());
        }
    }

    private String sha256(Resource resource) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = resource.getContentAsByteArray();
        return HexFormat.of().formatHex(digest.digest(bytes));
    }

    private List<Document> buildChunks() {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                faqPdf,
                PdfDocumentReaderConfig.builder()
                        .withPagesPerDocument(1)
                        .build()
        );

        List<Document> chunks = new ArrayList<>();
        String section = "General";
        String question = null;
        String questionPage = "?";
        List<String> answerLines = new ArrayList<>();
        String lastPage = "?";
        int droppedLines = 0;
        Set<String> seenSections = new HashSet<>();
        seenSections.add(section);

        for (Document page : pdfReader.get()) {
            lastPage = String.valueOf(page.getMetadata().getOrDefault("page_number", "?"));
            for (String rawLine : page.getText().split("\n")) {
                String line = rawLine.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (QUESTION_LINE.matcher(line).matches()) {
                    droppedLines += flushPair(chunks, section, question, questionPage, answerLines);
                    question = line;
                    questionPage = lastPage;
                    answerLines.clear();
                } else if (SECTION_HEADER.matcher(line).matches()) {
                    droppedLines += flushPair(chunks, section, question, questionPage, answerLines);
                    section = line;
                    seenSections.add(section);
                    question = null;
                    answerLines.clear();
                } else {
                    answerLines.add(line);
                }
            }
        }
        droppedLines += flushPair(chunks, section, question, questionPage, answerLines);
        if (droppedLines > 0) {
            log.warn("FAQ parser skipped {} orphaned content lines (no question context)", droppedLines);
        }
        log.info("FAQ parser produced {} Q&A chunks across {} sections", chunks.size(), seenSections.size());
        return chunks;
    }

    private int flushPair(List<Document> chunks, String section, String question, String questionPage,
                          List<String> answerLines) {
        if (question == null || answerLines.isEmpty()) {
            return question == null ? answerLines.size() : 0;
        }
        String content = "Q: " + question + "\nA: " + String.join(" ", answerLines);
        List<Document> split = TokenTextSplitter.builder().build().apply(List.of(new Document(content)));
        for (Document doc : split) {
            doc.getMetadata().put("source", "faqs.pdf");
            doc.getMetadata().put("section", section);
            doc.getMetadata().put("page", questionPage);
        }
        chunks.addAll(split);
        return 0;
    }
}
