package info.anecdot.io;

import info.anecdot.model.Fragment;
import info.anecdot.model.Document;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @author Stephan Grundner
 */
@Component
public class ItemLoader {

    private boolean hasChildElements(Node node) {
        NodeList children = node.getChildNodes();
        if (children.getLength() == 0) {
            return false;
        }

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }

        return false;
    }

    private void toFragment(Node node, Fragment fragment) {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeName().startsWith("#")) {
                continue;
            }

            Fragment payload = fromNode(child);
            String name = child.getNodeName();
            fragment.appendChild(name, payload);
        }

        if (!hasChildElements(node)) {
            fragment.setText(node.getTextContent());
        }

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            fragment.getAttributes().put(attribute.getName(), attribute.getValue());
        }
    }

    private Fragment fromNode(Node node) {
        if (node.getNodeType() == Node.DOCUMENT_NODE) {
            Element document = ((org.w3c.dom.Document) node).getDocumentElement();
            Document item = new Document();
            item.setType(document.getNodeName());
            toFragment(document, item);

            return item;
        }

        Fragment fragment = new Fragment();
        toFragment(node, fragment);

        return fragment;
    }

    public Document loadPage(Path file) throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try (InputStream inputStream = Files.newInputStream(file)) {
            DocumentBuilder db = dbf.newDocumentBuilder();
            org.w3c.dom.Document document = db.parse(inputStream);

            Document item = (Document) fromNode(document);

            BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
            item.setCreated(LocalDateTime.ofInstant(attributes.creationTime().toInstant(), ZoneOffset.UTC));
            item.setModified(LocalDateTime.ofInstant(attributes.lastModifiedTime().toInstant(), ZoneOffset.UTC));

            return item;
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
