package dev.leaveassistant.policy;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** Reads a PDF with the Spring AI PDF reader: one entry per page, numbered from 1. */
@Component
class PolicyPdfReader {

    List<PolicyPage> read(Resource pdf) {
        List<Document> documents = new PagePdfDocumentReader(pdf).get();
        List<PolicyPage> pages = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            String text = documents.get(i).getText();
            pages.add(new PolicyPage(i + 1, text == null ? "" : text));
        }
        return pages;
    }
}
