package com.example.docgen.config;

import com.aspose.words.FontSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Applies the Aspose license (Words + PDF) and the font folder once at startup.
 *
 * License sources, in order: ASPOSE_LICENSE_BASE64 (best for ECS: inject from Secrets Manager),
 * then ASPOSE_LICENSE_PATH. With neither, Aspose runs in evaluation mode: watermark on every page
 * and long documents are cut off.
 */
@Component
public class AsposeSetup implements InitializingBean {

    private static final Logger log = LoggerFactory.getLogger(AsposeSetup.class);

    private final DocGenProperties.Aspose config;
    private volatile boolean licensed;

    public AsposeSetup(DocGenProperties properties) {
        this.config = properties.aspose();
    }

    @Override
    public void afterPropertiesSet() {
        applyLicense();
        if (notBlank(config.fontsDir())) {
            FontSettings.getDefaultInstance().setFontsFolders(new String[]{config.fontsDir()}, true);
            log.info("Aspose fonts folder: {}", config.fontsDir());
        }
    }

    private void applyLicense() {
        try {
            byte[] license = null;
            if (notBlank(config.licenseBase64())) {
                license = Base64.getDecoder().decode(config.licenseBase64().trim());
            } else if (notBlank(config.licensePath()) && Files.isReadable(Path.of(config.licensePath()))) {
                license = Files.readAllBytes(Path.of(config.licensePath()));
            }
            if (license == null) {
                log.warn("No Aspose license configured - running in EVALUATION mode "
                        + "(watermark on every page, long documents truncated)");
                return;
            }
            new com.aspose.words.License().setLicense(new ByteArrayInputStream(license));
            new com.aspose.pdf.License().setLicense(new ByteArrayInputStream(license));
            licensed = true;
            log.info("Aspose license applied");
        } catch (Exception e) {
            log.error("Could not apply Aspose license - continuing in evaluation mode", e);
        }
    }

    public boolean isLicensed() {
        return licensed;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
