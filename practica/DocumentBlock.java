package es.udc.fi.ri.practica;

import java.util.List;

public class DocumentBlock {
    private List<CovidDocument> documents;

    public DocumentBlock(List<CovidDocument> documents) {
        this.documents = documents;
    }

    public List<CovidDocument> getDocuments() {
        return documents;
    }
}
