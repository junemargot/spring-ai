package hello.springai.reader;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DocumentReaderService {

    // ===== PDF =====
    @Value("classpath:documents/pdf/7. 자바 메모리 구조와 static.pdf")
    private Resource pdfResource;

    // ===== TXT 파일들 =====
    @Value("classpath:documents/text/rag_intro.txt")
    private Resource ragIntro;

    @Value("classpath:documents/text/java_memory.txt")
    private Resource javaMemory;

    @Value("classpath:documents/text/db_indexing.txt")
    private Resource dbIndexing;

    @Value("classpath:documents/text/msa_guide.txt")
    private Resource msaGuide;

    @Value("classpath:documents/text/cicd_devops.txt")
    private Resource cicdDevops;

    // ===== JSON =====
    @Value("classpath:documents/json/books.json")
    private Resource jsonResource;

    // ===== PDF 읽기 메서드 =====
    public List<Document> loadPdfDocuments() {
        PagePdfDocumentReader reader =
                new PagePdfDocumentReader(pdfResource);
        return reader.get();
    }

    // ===== TXT 읽기 메서드 =====
    public List<Document> loadTextDocuments() {
        List<Document> allDocs = new ArrayList<>();

        allDocs.addAll(loadSingleText(ragIntro, "rag_intro.txt"));
        allDocs.addAll(loadSingleText(javaMemory, "java_memory.txt"));
        allDocs.addAll(loadSingleText(dbIndexing, "db_indexing.txt"));
        allDocs.addAll(loadSingleText(msaGuide, "msa_guide.txt"));
        allDocs.addAll(loadSingleText(cicdDevops, "cicd_devops.txt"));

        return allDocs;
    }

    private List<Document> loadSingleText(Resource resource, String fileName) {
        TextReader reader = new TextReader(resource);
        reader.getCustomMetadata().put("filename", fileName);
        reader.getCustomMetadata().put("type", "text");
        return reader.get();
    }

    // ===== JSON 읽기 메서드 ====
    public List<Document> loadJsonDocuments() {
        JsonReader reader = new JsonReader(
                jsonResource,
                "title", "author", "summary", "category"
        );
        return reader.get();
    }

    // ===== 전체 읽기 메서드 =====
    public List<Document> loadAllDocuments() {
        List<Document> allDocs = new ArrayList<>();

        allDocs.addAll(loadPdfDocuments());
        allDocs.addAll(loadTextDocuments());
        allDocs.addAll(loadJsonDocuments());

        return allDocs;
    }

    // ===== 타입별 읽기 메서드 =====
    public List<Document> loadByType(String type) {
        return switch(type.toLowerCase()) {
            case "pdf" -> loadPdfDocuments();
            case "text", "txt" -> loadTextDocuments();
            case "json" -> loadJsonDocuments();
            case "all" -> loadAllDocuments();
            default -> new ArrayList<>();
        };
    }
}
