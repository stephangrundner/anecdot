package info.anecdot.content;

import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.xpath.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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

    private boolean isRootNode(Node node) {
        Node parentNode = node.getParentNode();
        return parentNode != null && parentNode.getNodeType() == Node.DOCUMENT_NODE;
    }

    private Payload fromNode(Node node) {
        Payload payload;

        if (!hasChildElements(node)) {
            Text text = new Text();
            text.setValue(node.getTextContent());

            payload = text;
        } else {
            Fragment fragment;

            if (isRootNode(node)) {
                Item item = new Item();
                item.setType(node.getNodeName());

                fragment = item;
            } else {
                fragment = new Fragment();
            }

            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node childNode = children.item(i);
                if (childNode.getNodeName().startsWith("#")) {
                    continue;
                }

                String childName = childNode.getNodeName();
                Payload.Sequence sequence = fragment.getSequence(childName);
                if (sequence == null) {
                    sequence = new Payload.Sequence();
                    fragment.setSequence(childName, sequence);
                }
                sequence.addPayload(fromNode(childNode));
            }

            payload = fragment;
        }


        NamedNodeMap attributes = node.getAttributes();
        if (attributes != null) {
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr attribute = (Attr) attributes.item(i);
                payload.getAttributes().put(attribute.getName(), attribute.getValue());
            }
        }

        return payload;
    }

    public Item loadItem(Path directory, Path file) throws IOException, XPathException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try (InputStream inputStream = Files.newInputStream(file)) {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(inputStream);
            Item item = (Item) fromNode(document.getDocumentElement());

            String path = directory.relativize(file).toString();
            path = FilenameUtils.removeExtension(path);
            if (!StringUtils.startsWithIgnoreCase(path, "/")) {
                path = "/" + path;
            }

            item.setUri(path);

            XPath xPath = XPathFactory.newInstance().newXPath();
//            TODO Make this customizable:
            XPathExpression findMetaTagExpression = xPath.compile("/*/meta");
            Node metaTag = (Node) findMetaTagExpression.evaluate(document, XPathConstants.NODE);
            if (metaTag != null) {
                NodeList tagNodes = (NodeList) xPath.evaluate("tags/tag/text()", metaTag, XPathConstants.NODESET);
                for (int i = 0; i < tagNodes.getLength(); i++) {
                    org.w3c.dom.Text text = (org.w3c.dom.Text) tagNodes.item(i);
                    item.getTags().add(text.getWholeText());
                }

                String dateString = (String) xPath.evaluate("date/text()", metaTag, XPathConstants.STRING);
                if (StringUtils.hasText(dateString)) {
                    LocalDate date = LocalDate.parse(dateString);
                    item.setDate(date);
                } else {
                    BasicFileAttributes fileAttributes = Files.readAttributes(file, BasicFileAttributes.class);
                    LocalDate date = fileAttributes.creationTime().toInstant().atZone(ZoneOffset.UTC).toLocalDate();
                    item.setDate(date);
                }
            }

            return item;
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }
}
