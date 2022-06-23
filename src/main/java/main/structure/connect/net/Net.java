package main.structure.connect.net;

import main.index.Indexed;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface Net<T> {
    Node<T> origin();

    default Optional<Node<T>> dig(Iterable<T> path) {
        // under 0 allowance only 0 or 1 result is possible
        return dig(path, 0).stream().findFirst();
    }
    default Set<Node<T>> dig(Iterable<T> path, int widthAllowance) {
        Iterator<T> it = path.iterator();
        Stream<Indexed<Node<T>>> allowance = Stream.of(new Indexed<>(widthAllowance, origin()));

        while (it.hasNext()) {
            T target = it.next();
            allowance = allowance
                    .filter(indexed -> !indexed.value().isEnd())
                    .flatMap(indexed -> indexed.value().children().stream()
                            .map(child -> new Indexed<>(widthAllowance, child))
                            .filter(iindexed -> target.equals(indexed.value()) || iindexed.indexing(-1) > 1));
        }

        return allowance.map(Indexed::value).collect(Collectors.toSet());
    }

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
