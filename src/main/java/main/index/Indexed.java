package main.index;

import java.util.function.*;

public interface Indexed {
    int index();
    void index(int index);

    default void offset(int offset) {
        index(index() + offset);
    }
    default void offset(IntUnaryOperator offset) {
        index(offset.applyAsInt(index()));
    }
    default void offset(Indexed offset) {
        index(index() + offset.index());
    }
}
