package hello.springai.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStore;
import org.springframework.ai.vectorstore.elasticsearch.ElasticsearchVectorStoreOptions;
import org.springframework.ai.vectorstore.elasticsearch.SimilarityFunction;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;

@Configuration
public class VectorStoreConfig {

    private final EmbeddingModel embeddingModel;

    public VectorStoreConfig(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

//    private final OpenAiEmbeddingModel openAiEmbeddingModel;
//
//    public VectorStoreConfig(OpenAiEmbeddingModel openAiEmbeddingModel) {
//        this.openAiEmbeddingModel = openAiEmbeddingModel;
//    }

    @Bean
    public VectorStore elasticsearchVectorStore(RestClient restClient) {
        ElasticsearchVectorStoreOptions options = new ElasticsearchVectorStoreOptions();
        options.setIndexName("my-documents");
        options.setSimilarity(SimilarityFunction.cosine);
        options.setDimensions(1536);

        return ElasticsearchVectorStore.builder(restClient, embeddingModel)
                .options(options)
                .initializeSchema(true)
                .batchingStrategy(new TokenCountBatchingStrategy())
                .build();
    }

    @Bean
    public RestClient restClient() {
        SSLContext sslContext;
        try {
            sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(null, (chain, authType) -> true) // 모든 인증서 신뢰
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return RestClient.builder(new HttpHost("localhost", 9200, "https"))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder
                        .setSSLContext(sslContext)
                        .setDefaultCredentialsProvider(credentialsProvider()))
                .build();
    }

    private CredentialsProvider credentialsProvider() {
        CredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("elastic", "123456"));
        return provider;
    }
}