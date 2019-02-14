package info.anecdot.content;

import info.anecdot.security.Access;
import info.anecdot.security.Permission;
import info.anecdot.xml.DomAndXPathSupport;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Service
public class SettingsService implements DomAndXPathSupport {

    private final Map<String, Settings> settingsByUri = new ConcurrentSkipListMap<>();

    public Collection<Settings> getAllSettings() {
        return Collections.unmodifiableCollection(settingsByUri.values());
    }

    private void eachSegmentForUri(String uri, Consumer<String> consumer) {
        if (uri.endsWith("/") && uri.length() > 1) {
            uri = uri.substring(1, uri.length() - 1);
        }

        if (!uri.startsWith("/")) {
            uri = "/" + uri;
        }

        for (int i = 0; i < uri.length(); i++) {
            char c = uri.charAt(i);
            if (c == '/' || i == uri.length() - 1) {
                String segment = uri.substring(0, i+1);
                consumer.accept(segment);
            }
        }
    }

    public void eachSettingsForUri(String uri, Consumer<Settings> consumer) {
        eachSegmentForUri(uri, segment -> {
            Settings settings = settingsByUri.get(segment);
            if (settings != null) {
                consumer.accept(settings);
            }
        });
    }

    public Set<String> getUris() {
        return Collections.unmodifiableSet(settingsByUri.keySet());
    }

    public Settings getSettings(String uri) {
        return settingsByUri.get(uri);
    }

    private void applyLocale(Node settingsNode, Settings settings) {
        nodes("/settings/locale", settingsNode).forEach(node -> {
            String languageTag = node.getTextContent();
            Locale locale = Locale.forLanguageTag(languageTag);
            settings.setLocale(locale);
        });
    }

    private void applyAccess(Node document, Settings settings) {
        NodeList restrictionNodes = nodeList("/settings/access/*", document);

        List<Permission> permissions = new ArrayList<>();
        DomAndXPathSupport.nodes(restrictionNodes).forEach(node -> {
            String pattern = ((Element) node).getAttribute("pattern");
            NodeList authorityNodes = nodeList("authority", node);
            List<String> authorities = DomAndXPathSupport.nodes(authorityNodes)
                    .map(it -> "ROLE_" + it.getTextContent())
                    .collect(Collectors.toList());

            List<String> users = nodes("user", node)
                    .map(Node::getTextContent)
                    .collect(Collectors.toList());

            Permission.Kind kind = Permission.Kind.valueOf(node.getNodeName().toUpperCase());
            Permission permission = new Permission(kind, pattern, authorities);
            permission.setUsers(users);
            permissions.add(permission);
        });

        settings.setAccess(new Access(settings.getPath(), permissions));
    }

    public void reloadSettings(Site site, Path file)  {
        String path = site.getBase().relativize(file).toString();
        path = FilenameUtils.removeExtension(path);
        path = FilenameUtils.removeExtension(path);
        if (!StringUtils.startsWithIgnoreCase(path, "/")) {
            path = "/" + path;
        }

        Settings settings = new Settings(path);

        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            try (InputStream inputStream = Files.newInputStream(file)) {
                Document document = parser.parse(inputStream);

                applyLocale(document, settings);
                applyAccess(document, settings);
            }

            settingsByUri.put(path, settings);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public XPath getXPath() {
        return XPathFactory.newInstance().newXPath();
    }
}
