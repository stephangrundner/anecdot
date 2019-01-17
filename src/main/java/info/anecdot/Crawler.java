package info.anecdot;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * @author Stephan Grundner
 */
@Deprecated
public class Crawler {

    private static final Logger LOG = LoggerFactory.getLogger(Crawler.class);

    public void crawl(String url) {
        Connection connection = Jsoup.connect(url);
        Document document;
        try {
            document = connection.get();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        LOG.info("Visited {}", url);

        document.select("a[href]").forEach(element -> {
            String nextUrl = element.attr("abs:href");
            crawl(nextUrl);
        });

        document.select("img").forEach(element -> {
            String src = element.attr("abs:src");
            try {
                UrlResource image = new UrlResource(src);
                try (InputStream inputStream = image.getInputStream()) {

                    inputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
