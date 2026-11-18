package com.example.docgen.service;

import java.util.Collection;

public class UnknownDocumentTypeException extends RuntimeException {

    public UnknownDocumentTypeException(String documentType, Collection<String> known) {
        super("Unknown document type '" + documentType + "'. Known types: " + known);
    }
}
