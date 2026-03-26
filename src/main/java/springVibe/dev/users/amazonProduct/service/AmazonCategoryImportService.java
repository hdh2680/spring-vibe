package springVibe.dev.users.amazonProduct.service;

import springVibe.dev.users.amazonProduct.domain.AmazonCategory;
import springVibe.dev.users.amazonProduct.repository.AmazonCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AmazonCategoryImportService {
    private static final Logger log = LoggerFactory.getLogger(AmazonCategoryImportService.class);

    public static final String DEFAULT_CSV_CLASSPATH = "classpath:/static/docs/amazonProduct/amazon_categories.csv";

    private final ResourceLoader resourceLoader;
    private final AmazonCategoryRepository categoryRepository;

    public record ImportResult(int totalRows, int savedRows, int skippedRows) {
    }

    @Transactional
    public ImportResult importFromCsvResource(String resourceLocation, boolean replaceAll) {
        Resource resource = resourceLoader.getResource(resourceLocation);
        if (!resource.exists()) {
            throw new IllegalStateException("CSV resource not found: " + resourceLocation);
        }

        if (replaceAll) {
            categoryRepository.deleteAllInBatch();
        }

        int total = 0;
        int skipped = 0;
        List<AmazonCategory> batch = new ArrayList<>(512);

        try (CSVParser parser = CSVParser.parse(
            new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8),
            CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .build()
        )) {
            for (CSVRecord r : parser) {
                total++;
                String idStr = get(r, "id");
                String name = get(r, "category_name");
                String nameKo = get(r, "category_name_ko");

                if (idStr == null || idStr.isBlank() || name == null || name.isBlank()) {
                    skipped++;
                    continue;
                }

                Long id;
                try {
                    id = Long.parseLong(idStr.trim());
                } catch (NumberFormatException e) {
                    skipped++;
                    continue;
                }

                batch.add(new AmazonCategory(id, name.trim(), (nameKo == null) ? null : nameKo.trim()));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to import amazon categories from " + resourceLocation, e);
        }

        // saveAll() will insert or update by id.
        categoryRepository.saveAll(batch);
        log.info("Amazon categories imported. totalRows={}, savedRows={}, skippedRows={}, replaceAll={}",
            total, batch.size(), skipped, replaceAll);

        return new ImportResult(total, batch.size(), skipped);
    }

    private static String get(CSVRecord r, String key) {
        try {
            return r.isMapped(key) ? r.get(key) : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}

