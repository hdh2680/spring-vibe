package springVibe.dev.users.amazonProduct.controller;

import springVibe.dev.users.amazonProduct.service.AmazonCategoryImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/amazon/categories")
public class AmazonCategoryAdminApiController {

    private final AmazonCategoryImportService importService;

    public record ImportResponse(int totalRows, int savedRows, int skippedRows, boolean replaceAll, String resourceLocation) {
    }

    /**
     * 카테고리 CSV를 DB에 적재한다.
     *
     * - 기본 CSV 위치: classpath:/static/docs/amazonProduct/amazon_categories.csv
     * - replaceAll=true면 기존 데이터를 비우고 다시 적재한다.
     */
    @PostMapping("/import")
    public ResponseEntity<ImportResponse> importCategories(
        @RequestParam(defaultValue = AmazonCategoryImportService.DEFAULT_CSV_CLASSPATH) String resourceLocation,
        @RequestParam(defaultValue = "false") boolean replaceAll
    ) {
        AmazonCategoryImportService.ImportResult r = importService.importFromCsvResource(resourceLocation, replaceAll);
        return ResponseEntity.ok(new ImportResponse(r.totalRows(), r.savedRows(), r.skippedRows(), replaceAll, resourceLocation));
    }
}

