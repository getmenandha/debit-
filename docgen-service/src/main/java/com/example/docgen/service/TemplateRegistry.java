package com.example.docgen.service;

import com.example.docgen.config.DocGenProperties;
import com.example.docgen.config.DocGenProperties.DocumentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads Word templates (from the classpath or a mounted folder), plus each type's JSON Schema
 * and sample payload. The template version is a short SHA-256 of the template files, so every
 * generated document can say exactly which template produced it.
 */
@Component
public class TemplateRegistry {

    private static final Logger log = LoggerFactory.getLogger(TemplateRegistry.class);

    /** A document type with its template bytes loaded. */
    public record TemplateSet(String documentType, DocumentType config, byte[] main, List<byte[]> appended,
                              String version) {}

    private final ResourceLoader resources;
    private final String location;
    private final Map<String, DocumentType> types;
    private final Map<String, TemplateSet> cache = new ConcurrentHashMap<>();

    public TemplateRegistry(DocGenProperties properties, ResourceLoader resources) {
        this.resources = resources;
        String loc = properties.templates().location();
        this.location = loc.endsWith("/") ? loc : loc + "/";
        this.types = new LinkedHashMap<>(properties.templates().types());
        log.info("Template location {} with document types {}", location, types.keySet());
    }

    public List<String> documentTypes() {
        return List.copyOf(types.keySet());
    }

    public DocumentType config(String documentType) {
        DocumentType t = types.get(documentType);
        if (t == null) throw new UnknownDocumentTypeException(documentType, types.keySet());
        return t;
    }

    public TemplateSet get(String documentType) {
        DocumentType config = config(documentType);
        return cache.computeIfAbsent(documentType, k -> load(k, config));
    }

    /** Drops cached templates so the next request reloads them (e.g. after a template folder sync). */
    public void evictAll() {
        cache.clear();
    }

    public byte[] schema(String documentType) {
        return classpath("/schemas/" + config(documentType).schema());
    }

    public byte[] sample(String documentType) {
        return classpath("/samples/" + config(documentType).sample());
    }

    private TemplateSet load(String documentType, DocumentType config) {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] main = read(location + config.template());
            sha.update(main);
            List<byte[]> appended = new ArrayList<>();
            for (String part : config.appendOrEmpty()) {
                byte[] b = read(location + part);
                sha.update(b);
                appended.add(b);
            }
            String version = HexFormat.of().formatHex(sha.digest()).substring(0, 12);
            log.info("Loaded template {} ({}) version {}", documentType, config.template(), version);
            return new TemplateSet(documentType, config, main, List.copyOf(appended), version);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load template for " + documentType + ": " + e.getMessage(), e);
        }
    }

    private byte[] read(String path) throws IOException {
        Resource r = resources.getResource(path);
        if (!r.exists()) throw new IOException("Not found: " + path);
        try (InputStream in = r.getInputStream()) {
            return in.readAllBytes();
        }
    }

    private byte[] classpath(String path) {
        try {
            return read("classpath:" + path);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }
}
