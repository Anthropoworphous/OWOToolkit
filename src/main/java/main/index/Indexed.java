package main.index;

import java.util.function.*;

public class Indexed<T> {
    private int index;
    private T value;

    public Indexed(int index, T value) {
        this.index = index;
        this.value = value;
    }

    public int index() { return index; }
    public T value() { return value; }
    public void index(int index) { this.index = index; }
    public void value(T value) { this.value = value; }

    public int indexing(int offset) {
        return index += offset;
    }
    public int indexing(IntUnaryOperator indexer) {
        return index = indexer.applyAsInt(index);
    }
    public int indexing(IntFunction<T> indexer) { return index = indexer.apply(value); }

    @FunctionalInterface
    private interface IntFunction<T> {
        int apply(T value);
    }
}
