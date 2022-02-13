package main.structure.index;

import org.jetbrains.annotations.NotNull;

public abstract class Index implements Comparable<Index> {

    public Index move(int offset) {
        setIndex(toIndex() + offset);
        return this;
    }
    public Index move(Index offset) {
        setIndex(toIndex() + offset.toIndex());
        return this;
    }

    public Index offset(int offset) {
        return new ID(toIndex() + offset);
    }
    public Index offset(Index offset) {
        return new ID(toIndex() + offset.toIndex());
    }

    @Override
    public int compareTo(@NotNull Index o) {
        return this.toIndex() - o.toIndex();
    }

    @Override
    public String toString() {
        return "index: " + this.toIndex();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof ID && ((ID) obj).toIndex() == this.toIndex();
    }

    @Override
    public int hashCode() {
        return toIndex();
    }

    public abstract int toIndex();
    public abstract void setIndex(int index);
}