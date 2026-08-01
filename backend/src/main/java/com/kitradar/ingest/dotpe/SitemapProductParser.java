package com.kitradar.ingest.dotpe;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

@Component
public class SitemapProductParser {

    public List<ProductLocation> parse(String sitemapXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(sitemapXml)));
            NodeList locations = document.getElementsByTagNameNS("http://www.sitemaps.org/schemas/sitemap/0.9", "loc");
            List<ProductLocation> products = new ArrayList<>();
            for (int index = 0; index < locations.getLength(); index++) {
                location(locations.item(index).getTextContent()).ifPresent(products::add);
            }
            return List.copyOf(products);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Could not parse product sitemap", exception);
        }
    }

    private java.util.Optional<ProductLocation> location(String url) {
        URI uri = URI.create(url.trim());
        String[] parts = uri.getRawPath().split("/");
        if (parts.length < 4 || !"product".equals(parts[1])) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(new ProductLocation(parts[2], parts[3]));
    }

    public record ProductLocation(String id, String slug) {
    }
}
