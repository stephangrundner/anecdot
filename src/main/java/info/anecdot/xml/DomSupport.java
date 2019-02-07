package info.anecdot.xml;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author Stephan Grundner
 */
public interface DomSupport {

    class NodeListIterator implements Iterator<Node> {

        private final NodeList nodeList;
        private int i = 0;

        @Override
        public boolean hasNext() {
            return i < nodeList.getLength();
        }

        @Override
        public Node next() {
            return nodeList.item(i++);
        }

        public NodeListIterator(NodeList nodeList) {
            this.nodeList = nodeList;
        }
    }

    class NamedNodeMapIterator implements Iterator<Node> {

        private final NamedNodeMap namedNodeMap;
        private int i = 0;

        @Override
        public boolean hasNext() {
            return i < namedNodeMap.getLength();
        }

        @Override
        public Node next() {
            return namedNodeMap.item(i++);
        }

        public NamedNodeMapIterator(NamedNodeMap namedNodeMap) {
            this.namedNodeMap = namedNodeMap;
        }
    }

    static Stream<Node> nodes(NodeList nodes) {
        NodeListIterator nodeListIterator = new NodeListIterator(nodes);
        return StreamSupport.stream(Spliterators
                .spliterator(nodeListIterator, 0L, 0), false);
    }

    static Stream<Node> nodes(NamedNodeMap nodes) {
        NamedNodeMapIterator namedNodeMapIterator = new NamedNodeMapIterator(nodes);
        return StreamSupport.stream(Spliterators
                .spliterator(namedNodeMapIterator, 0L, 0), false);
    }

    XPath getXPath();

    default NodeList nodeList(String expression, Object source) {
        try {
            return (NodeList) getXPath().evaluate(expression, source, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new RuntimeException(e);
        }
    }

    default Stream<Node> nodes(String expression, Object source) {
        return nodes(nodeList(expression, source));
    }
}
