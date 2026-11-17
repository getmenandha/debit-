package com.example.docgen;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads the Aspose license once per JVM. Without a license both libraries run in
 * evaluation mode: an evaluation banner and watermark are added, and Aspose.PDF only
 * processes the first few pages. Those additions also cause PDF/UA validation errors,
 * so test accessibility with a temporary license.
 *
 * Set ASPOSE_LICENSE_PATH to a Total/Words+PDF license file
 * (a free 30-day temporary license is available from Aspose).
 */
public final class AsposeLicensing {

    private static volatile boolean initialised;
    private static volatile boolean licensed;

    private AsposeLicensing() {}

    public static synchronized boolean init() {
        if (initialised) return licensed;
        String path = System.getenv("ASPOSE_LICENSE_PATH");
        if (path != null && Files.isReadable(Path.of(path))) {
            try {
                new com.aspose.words.License().setLicense(path);
                new com.aspose.pdf.License().setLicense(path);
                licensed = true;
                System.out.println("[license] Aspose license applied from " + path);
            } catch (Exception e) {
                System.err.println("[license] Could not apply license, continuing in evaluation mode: " + e.getMessage());
            }
        } else {
            System.out.println("[license] ASPOSE_LICENSE_PATH not set - running in EVALUATION mode "
                    + "(watermarks + PDF/UA errors from the watermark are expected)");
        }
        initialised = true;
        return licensed;
    }

    public static boolean isLicensed() {
        return licensed;
    }
}
