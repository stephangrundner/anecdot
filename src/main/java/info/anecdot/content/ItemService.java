package info.anecdot.content;

import org.apache.commons.collections4.map.AbstractMapDecorator;
import org.apache.commons.collections4.map.LazyMap;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * @author Stephan Grundner
 */
@Service
public class ItemService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private SiteService siteService;

    public List<Item> findItemsByHost(String host) {
        return itemRepository.findAllByHost(host);
    }

    private Item findItemBySiteAndUri(Site site, String uri) {
        if (uri.equals("/")) {
            uri = site.getHome();
        }

        return itemRepository.findByHostAndUri(site.getHost(), uri);
    }

    public Item findItemByRequestAndUri(HttpServletRequest request, String uri) {
        Site site = siteService.findSiteByRequest(request);
        return findItemBySiteAndUri(site, uri);
    }

    public Item saveItem(Item item) {
        Site site = siteService.findSiteByHost(item.getHost());
        Item existing = findItemBySiteAndUri(site, item.getUri());
        if (existing != null) {
            item.setId(existing.getId());
        }

        return itemRepository.save(item);
    }

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

        if (isRootNode(node)) {
            Item item = new Item();
            item.setType(node.getNodeName());

            payload = item;
        } else {
            payload = new Payload();
        }

        if (hasChildElements(node)) {
            NodeList children = node.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node childNode = children.item(i);
                if (childNode.getNodeName().startsWith("#")) {
                    continue;
                }

                String childName = childNode.getNodeName();
                List<Payload> sequence = payload.getSequences().get(childName);
                if (sequence == null) {
                    sequence = new ArrayList<>();
                    payload.getSequences().put(childName, sequence);
                }
                sequence.add(fromNode(childNode));
            }
        } else {
            payload.setText(node.getTextContent());
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

    private Item loadItem(Site site, Path file) throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try (InputStream inputStream = Files.newInputStream(file)) {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(inputStream);
            Item item = (Item) fromNode(document.getDocumentElement());

            Path base = site.getBase();
            String path = base.relativize(file).toString();
            path = FilenameUtils.removeExtension(path);
            if (!StringUtils.startsWithIgnoreCase(path, "/")) {
                path = "/" + path;
            }

            item.setUri(path);
            item.setHost(site.getHost());

            return item;
        } catch (SAXException | ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public Item loadItem(Site site, Path file, boolean save) throws IOException {
        Item item = loadItem(site, file);
        if (save && item != null) {
            item = saveItem(item);
        }

        return item;
    }

    private <K> Map<K, Object> createMap() {
        return LazyMap.lazyMap(new LinkedHashMap<K, Object>() {
            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        }, this::createMap);
    }

    public Map<String, Object> toMap(Payload payload, String name, Map<String, Object> parent) {
        Map<String, Object> map = createMap();

        map.put("#payload", payload);
        map.put("#name", name);
//        map.put("#name", Optional.ofNullable(payload.getOwner()).map(Payload.Sequence::getName).orElse(null));
        map.put("#value", payload.getText());
//        if (payload instanceof Item) {
//            map.put("#tags", ((Item) payload).getTags());
//        }
        map.put("#parent", new AbstractMapDecorator<String, Object>(parent) {
            @Override
            public String toString() {
                Map<String, Object> decorated = decorated();
                return decorated.getClass().getName() + '@' + System.identityHashCode(decorated);
            }
        });

        List<Object> children = new ArrayList<>();

        payload.getSequences().forEach((x, sequence) -> {
            Map<Object, Object> values = createMap();
            int i = 0;

            for (Payload child : sequence) {
                Map<String, Object> childMap = toMap(child, x, map);
                if (i == 0) {
                    values.putAll(childMap);
                }
                values.put(Integer.toString(i++), childMap);
            }

            map.put(x, values);
            children.add(values);
        });

        map.put("#children", children);

        return map;
    }

    public Map<String, Object> toMap(Payload payload) {
        return toMap(payload, null, Collections.emptyMap());
    }
}
