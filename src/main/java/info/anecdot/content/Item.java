package info.anecdot.content;

/**
 * @author Stephan Grundner
 */
public class Item extends Fragment {

    private Site site;
    private String uri;
    private String type;
    private boolean page = true;

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isPage() {
        return page;
    }

    public void setPage(boolean page) {
        this.page = page;
    }
}
