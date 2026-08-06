package iti.jets.java.homenursing.ai.rag;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FaqIngestionService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(FaqIngestionService.class);

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final Resource faqPdf;

    public FaqIngestionService(VectorStore vectorStore,
                               JdbcTemplate jdbcTemplate,
                               org.springframework.core.io.ResourceLoader resourceLoader) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.faqPdf = resourceLoader.getResource("classpath:documents/faqs.pdf");
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!faqPdf.exists()) {
            log.warn("classpath:documents/faqs.pdf not found; skipping FAQ ingestion");
            return;
        }
        try {
            Integer existing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
            if (existing != null && existing > 0) {
                return; // already ingested, skip
            }
            ingest();
        } catch (Exception e) {
            log.warn("FAQ ingestion skipped: {}", e.getMessage());
        }
    }

    public void ingest() {
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                faqPdf,
                PdfDocumentReaderConfig.builder()
                        .withPagesPerDocument(1)
                        .build()
        );

        List<Document> pages = pdfReader.get();
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .build();

        List<Document> chunks = splitter.apply(pages);

        vectorStore.add(chunks);
    }
}