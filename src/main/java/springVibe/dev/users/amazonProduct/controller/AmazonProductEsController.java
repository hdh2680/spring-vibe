package springVibe.dev.users.amazonProduct.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springVibe.dev.users.amazonProduct.service.AmazonProductService;

import java.util.Map;

@RestController
@RequestMapping("/users/amazonProduct/es")
public class AmazonProductEsController {
    private final AmazonProductService service;

    public AmazonProductEsController(AmazonProductService service) {
        this.service = service;
    }

    @PostMapping("/reindex-all")
    public ResponseEntity<?> reindexAll() {
        int indexed = service.reindexAll();
        return ResponseEntity.ok(Map.of("indexed", indexed));
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clear() {
        service.clearIndex();
        return ResponseEntity.ok(Map.of("cleared", true));
    }
}

