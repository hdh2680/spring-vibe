package springVibe.dev.users.devSearch.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface VelogPostDocumentRepository extends ElasticsearchRepository<VelogPostDocument, String> {
}

