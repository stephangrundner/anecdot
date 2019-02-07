package info.anecdot.xml;

import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * @author Stephan Grundner
 */
public class XPathHelper {

    private XPath xPath;

    public NodeList nodeList(String expression, Object source) {
        try {
            return (NodeList) xPath.evaluate(expression, source, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    public XPathHelper() {
        xPath = XPathFactory.newInstance().newXPath();
    }
}
