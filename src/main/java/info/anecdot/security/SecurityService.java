package info.anecdot.security;

import info.anecdot.Nodes;
import info.anecdot.XPathHelper;
import info.anecdot.content.Site;
import info.anecdot.content.SiteService;
import org.apache.commons.io.FilenameUtils;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.adapters.springsecurity.token.KeycloakAuthenticationToken;
import org.keycloak.representations.AccessToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.UrlPathHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.servlet.http.HttpServletRequest;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author Stephan Grundner
 */
@Service
public class SecurityService {

    @Autowired
    private SiteService siteService;

    private Map<String, Access> accessMap = new ConcurrentSkipListMap<>();

    public void reloadRestriction(Site site, Path file)  {
        XPathHelper xPathHelper = new XPathHelper();
        try {
            DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            try (InputStream inputStream = Files.newInputStream(file)) {
                Document document = parser.parse(inputStream);
                NodeList restrictionNodes = xPathHelper.nodeList("/access/*", document);

                List<Permission> permissions = new ArrayList<>();
                Nodes.stream(restrictionNodes).forEach(node -> {
                    String pattern = ((Element) node).getAttribute("pattern");
                    NodeList authorityNodes = xPathHelper.nodeList("authority", node);
                    List<String> authorities = Nodes.stream(authorityNodes)
                            .map(it -> "ROLE_" + it.getTextContent())
                            .collect(Collectors.toList());

                    List<String> users = Nodes.stream(xPathHelper.nodeList("user", node))
                            .map(Node::getTextContent)
                            .collect(Collectors.toList());

                    Permission.Kind kind = Permission.Kind.valueOf(node.getNodeName().toUpperCase());
                    Permission permission = new Permission(kind, pattern, authorities);
                    permission.setUsers(users);
                    permissions.add(permission);
                });

                String path = site.getBase().relativize(file).toString();
                path = FilenameUtils.removeExtension(path);
                path = FilenameUtils.removeExtension(path);
                if (!StringUtils.startsWithIgnoreCase(path, "/")) {
                    path = "/" + path;
                }

                Access access = new Access(path, permissions);
                accessMap.put(path, access);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String resolveEmail(Authentication authentication) {
        if (authentication instanceof KeycloakAuthenticationToken) {
            KeycloakAuthenticationToken authenticationToken = (KeycloakAuthenticationToken) authentication;
            KeycloakPrincipal principal = (KeycloakPrincipal) authenticationToken.getPrincipal();
            KeycloakSecurityContext securityContext = principal.getKeycloakSecurityContext();
            AccessToken accessToken = securityContext.getToken();
            String email = accessToken.getEmail();

            return email;
        }

        return null;
    }

    public boolean hasAccess() {
        UrlPathHelper pathHelper = new UrlPathHelper();
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String path = pathHelper.getPathWithinApplication(request);
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AtomicBoolean granted = new AtomicBoolean(true);

        AntPathMatcher pathMatcher = new AntPathMatcher();
        accessMap.values().forEach(access -> {
            for (Permission permission : access.getPermissions()) {
                String relativePath = path;
                if (relativePath.startsWith(access.getPath())) {
                    relativePath = relativePath.substring(access.getPath().length(), relativePath.length());

                    if (!relativePath.startsWith("/")) {
                        relativePath = "/" + relativePath;
                    }
                    boolean restricted = pathMatcher.match(permission.getPattern(), relativePath);
                    if (restricted) {
                        String email = resolveEmail(authentication);
                        boolean emailFound = permission.getUsers().contains(email);
                        boolean authorityFound = authentication.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .anyMatch(authority -> permission.getAuthorities().stream().anyMatch(it -> it.equalsIgnoreCase(authority)));

                        boolean match = (emailFound || authorityFound);

                        if (permission.getKind() == Permission.Kind.ALLOW) {
                            granted.set(match);
                        } else {

                            if (match) {
                                granted.set(false);
                            }

                        }
//                        if (match) {
//                            granted.set(permission.getKind() == Permission.Kind.ALLOW);
//                        }

                        "".toString();
                    }
                }
            }
        });

        return granted.get();
    }
}
