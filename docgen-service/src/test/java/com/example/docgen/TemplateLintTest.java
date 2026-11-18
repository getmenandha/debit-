package com.example.docgen;

import com.example.docgen.render.WordRenderer;
import com.example.docgen.service.TemplateRegistry;
import com.example.docgen.service.TemplateRegistry.TemplateSet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Template lint, run in CI on every template change: renders each document type's sample payload in
 * STRICT mode (no ALLOW_MISSING_MEMBERS). Production may allow missing members so empty arrays render,
 * but that also hides template mistakes such as .NET-style Count() instead of Java's count(), or a typo
 * in a field name. Strict mode turns those into build failures.
 */
@SpringBootTest(properties = "docgen.warm-up=false")
class TemplateLintTest {

    @Autowired TemplateRegistry registry;
    @Autowired WordRenderer renderer;

    @Test
    void everyTemplateRendersStrictlyWithItsSample() {
        for (String type : registry.documentTypes()) {
            TemplateSet t = registry.get(type);
            String locale = t.config().locale() == null ? "en-US" : t.config().locale();
            WordRenderer.Rendered r = assertDoesNotThrow(
                    () -> renderer.render(t.main(), t.appended(), registry.sample(type), type, locale, false),
                    type + ": template failed strict render");
            assertThat(r.document().getText()).as(type + ": unrendered tag").doesNotContain("<<");
        }
    }
}
