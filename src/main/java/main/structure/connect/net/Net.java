package main.structure.connect.net;

import main.index.Indexed;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Net<T> {
    Node<T> origin();

    interface Node<T> {
        Node<T> parent();
        T value();
        void value(T value);

        boolean isStart();
        boolean isEnd();

        Collection<Node<T>> children();
        Node<T> getChild(T value);

        default List<T> backtrack() {
            List<T> result = new ArrayList<>();
            for (Node<T> node = this; !node.isStart(); node = node.parent()) {
                result.add(node.value());
            }
            Collections.reverse(result);
            return result;
        }
    }
}
