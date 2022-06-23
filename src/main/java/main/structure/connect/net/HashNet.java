package main.structure.connect.net;

import java.util.*;

public class HashNet<T> implements Net<T> {
    private final Node<T> origin = new HashNode<>(null, null);

    @Override
    public Node<T> origin() {
        return origin;
    }

    private static class HashNode<T> extends HashMap<T, Node<T>> implements Node<T> {
        private final Node<T> parent;
        private T value;

        public HashNode(Node<T> parent, T value) {
            this.parent = parent;
            this.value = value;
        }

        @Override
        public Node<T> parent() {
            return parent;
        }

        @Override
        public T value() {
            return value;
        }
        @Override
        public void value(T value) {
            this.value = value;
        }

        @Override
        public boolean isStart() { return parent != null; }
        @Override
        public boolean isEnd() { return isEmpty(); }

        @Override
        public Collection<Node<T>> children() {
            return values();
        }
        @Override
        public Node<T> getChild(T value) {
            return get(value);
        }
    }
}
