package springVibe.dev.reactLab.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springVibe.dev.reactLab.service.DocsEtcService;

import java.util.List;

@RestController
@RequestMapping("/api/docs/etc")
public class DocsEtcApiController {
    private final DocsEtcService docsEtcService;

    public DocsEtcApiController(DocsEtcService docsEtcService) {
        this.docsEtcService = docsEtcService;
    }

    @GetMapping
    public List<DocsEtcService.DocItem> list() {
        return docsEtcService.list();
    }

    @GetMapping("/{id}")
    public DocsEtcService.Doc get(@PathVariable String id) {
        return docsEtcService.get(id);
    }
}

