package hello.springai.config;

import hello.springai.reader.DocumentReaderService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class DocumentInitializer {

    private final DocumentReaderService documentReaderService;
    private final VectorStore elasticsearchVectorStore;

    public DocumentInitializer(DocumentReaderService documentReaderService, VectorStore elasticsearchVectorStore) {
        this.documentReaderService = documentReaderService;
        this.elasticsearchVectorStore = elasticsearchVectorStore;
    }

    @PostConstruct
    public void initializeDocuments() {
        log.info("=== 문서 로딩 시작 ===");

        try {
//            List<Document> allDocuments = new ArrayList<>();

//            log.info("PDF 읽기 시작...");
//            List<Document> pdfDocs = documentReaderService.loadPdfDocuments();
//            log.info("PDF 읽기 완료: {} 개", pdfDocs.size());
//
//            List<Document> textDocs = documentReaderService.loadTextDocuments();
//            allDocuments.addAll(textDocs);
//            log.info("TXT 읽기 완료: {} 개", textDocs.size());
//
//            List<Document> jsonDocs = documentReaderService.loadJsonDocuments();
//            allDocuments.addAll(jsonDocs);
//            log.info("JSON 읽기 완료: {} 개", jsonDocs.size());
//
//            log.info("읽어온 문서 개수: {}", allDocuments.size());

            // 1. 모든 문서 읽기
            List<Document> documents = documentReaderService.loadAllDocuments();
            log.info("읽어온 문서 개수: {}", documents.size());

            // 2. 문서 분할 (청크 크기: 500 토큰, 오버랩: 100토큰)
            TokenTextSplitter splitter = new TokenTextSplitter(
                    500,
                    100,
                    50,
                    10000,
                    true
            );

            List<Document> splitDocuments = splitter.apply(documents);
            log.info("분할된 문서 개수: {}", splitDocuments.size());

            // 3. VectorStore에 저장 (임베딩 자동 생성)
            if(!splitDocuments.isEmpty()) {
                elasticsearchVectorStore.add(splitDocuments);
                log.info("=== VectorStore 저장 완료: {}개 문서 ===", splitDocuments.size());
            } else {
                log.warn("저장할 문서가 없습니다.");
            }

        } catch(Exception e) {
            log.error("문서 로딩 중 오류 발생", e);
        }
    }
}
