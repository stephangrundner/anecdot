package info.anecdot;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Nodes {

    public static class NodeListIterator implements Iterator<Node> {

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

    public static class NamedNodeMapIterator implements Iterator<Node> {

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

    public static Stream<Node> stream(NodeList nodes) {
        NodeListIterator nodeListIterator = new NodeListIterator(nodes);
        return StreamSupport.stream(Spliterators
                .spliterator(nodeListIterator, 0L, 0), false);
    }

    public static Stream<Node> stream(NamedNodeMap nodes) {
        NamedNodeMapIterator namedNodeMapIterator = new NamedNodeMapIterator(nodes);
        return StreamSupport.stream(Spliterators
                .spliterator(namedNodeMapIterator, 0L, 0), false);
    }
}