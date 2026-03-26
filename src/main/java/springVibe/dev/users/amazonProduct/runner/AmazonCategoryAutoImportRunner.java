package springVibe.dev.users.amazonProduct.runner;

import springVibe.dev.users.amazonProduct.repository.AmazonCategoryRepository;
import springVibe.dev.users.amazonProduct.service.AmazonCategoryImportService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * local 프로파일에서만 동작.
 * - 카테고리 테이블이 비어 있고
 * - CSV 리소스가 존재하면
 * 자동으로 1회 적재한다.
 */
@Profile("local")
@Component
@RequiredArgsConstructor
public class AmazonCategoryAutoImportRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AmazonCategoryAutoImportRunner.class);

    private final AmazonCategoryRepository categoryRepository;
    private final AmazonCategoryImportService importService;
    private final ResourceLoader resourceLoader;

    @Override
    public void run(ApplicationArguments args) {
        long count = categoryRepository.count();
        if (count > 0) {
            return;
        }

        // Only import when the resource actually exists (repo에는 안 올려도 로컬에 있으면 됨)
        var resource = resourceLoader.getResource(AmazonCategoryImportService.DEFAULT_CSV_CLASSPATH);
        if (!resource.exists()) {
            log.info("Amazon category CSV not found. Auto import skipped. location={}", AmazonCategoryImportService.DEFAULT_CSV_CLASSPATH);
            return;
        }

        log.info("Amazon categories table is empty. Auto importing from {}", AmazonCategoryImportService.DEFAULT_CSV_CLASSPATH);
        importService.importFromCsvResource(AmazonCategoryImportService.DEFAULT_CSV_CLASSPATH, false);
    }
}

