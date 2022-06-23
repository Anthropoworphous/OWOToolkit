package main.structure.tree;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class Connected<T> {
    public Connected(T value, Collection<? extends Connected<T>> child) {
        this.value = value;
        for (Connected<T> c : child) {
            this.adopt(c);
        }
    }
    public Connected(Collection<? extends Connected<T>> child) {
        for (Connected<T> c : child) {
            this.adopt(c);
        }
    }
    public Connected(T value) {
        this.value = value;
    }
    public Connected() {}

    //fields
    //store value that's getting connected
    private T value = null;
    private Tree<T> tree = new Tree<>();
    private Connected<T> parent = null;
    //end

    //getter and setter
    public void value(T value) { this.value = value; }
    public Optional<T> value() { return Optional.ofNullable(value); }
    //end

    //iteration
    public Optional<Set<Connected<T>>> descent() {
        return Optional.ofNullable(tree.layers().get(this));
    }
    public Set<Connected<T>> child() {
        return descent().orElse(Collections.emptySet());
    }
    public Optional<Connected<T>> ascent() {
        return Optional.ofNullable(parent);
    }
    //end

    //static
    public static <T> Connected<T> root() {
        Connected<T> root = new Connected<>();
        root.tree = new Tree<>();
        return root;
    }

    //non-static
    //get
    @Override public String toString() {
        return (isOrphan() ? "null" : parent.value().map(Object::toString).orElse("()"))
                + " -> " + value().map(Object::toString).orElse("()")
                + " -> ["
                + descent().map(set ->
                set.stream()
                .map(c -> c.value().map(Object::toString).orElse("()"))
                .collect(Collectors.joining(", "))
                ).orElse("null")
                + "]";
    }

    public List<List<Connected<T>>> toList() {
        return childlessChild().stream()
                .map(child -> child.slimTree(false))
                .collect(Collectors.toList());
    }

    public List<Connected<T>> childlessChild() {
        List<Connected<T>> list = new ArrayList<>();
        for (Connected<T> c : child()) {
            list.addAll(c.isChildless() ?
                    Collections.singletonList(c) :
                    c.childlessChild());
        }
        return list;
    }
    public Connected<T> parentlessParent() {
        return isOrphan() ? this : parent.parentlessParent();
    }

    public List<Connected<T>> slimTree(boolean childFirst) {
        ArrayList<Connected<T>> out;
        Connected<T> child = this;

        for(out = new ArrayList<>(); !child.isOrphan(); child = child.parent) {
            if (childFirst) {
                out.add(child);
            } else {
                out.add(0, child);
            }
        } out.add(child);

        return out;
    }

    /**
     * use a list of filters to check on the family tree layer by layer
     * root is skipped, check start at root's child
     * @param filters list of filter, uses a different filter every layer
     * @param hasToReachTheEnd the final layer also have to be childless if true
     * @return all child(of deepest layer the filters can reach)
     * that matched the filter from the top of the family tree
     */
    public List<Connected<T>> multiLayerFilter(List<Predicate<T>> filters, boolean hasToReachTheEnd) {
        List<Connected<T>> result = new ArrayList<>(Collections.singletonList(this));

        for (Predicate<T> filter : filters) {
            result = result.stream()
                    .flatMap(c -> c.child().stream()
                            .filter(cc -> filter.test(cc.value))
                    ).collect(Collectors.toList());
        }

        return result.stream().filter(c -> !hasToReachTheEnd || c.isChildless()).collect(Collectors.toList());
    }
    public List<Connected<T>> allLayerFilter(Predicate<T> filter, boolean eliminateFailedParent) {
        return child()
                .stream()   //below, filter only apply if eliminateFailedParent is true
                .filter(child -> !eliminateFailedParent || filter.test(child.value))
                .flatMap(child -> child.isChildless() ?
                        child().stream() :
                        child.allLayerFilter(filter, true).stream())
                .collect(Collectors.toList());
    }

    public boolean isOrphan() {
        return (parent == null);
    }
    public boolean isChildless() {
        return (child().size() == 0);
    }

    public int getGeneration() {
        return ascent().isPresent() ? parent.getGeneration()+1 : 0;
    }

    /**
     * create similar child using existing instance
     * @return new child
     */
    public Connected<T> replicate() {
        return new Connected<>();
    }

    //set
    public Connected<T> adopt(T value) {
        return adopt(new Connected<>(value));
    }
    public Connected<T> adopt(Connected<T> child) {
        tree.layers().computeIfAbsent(this, k -> new HashSet<>());
        tree.layers().get(this).add(child.adopted(this));
        return this;
    }
    public Connected<T> adopt(Collection<? extends Connected<T>> child) {
        child.forEach(this::adopt);
        return this;
    }
    public Connected<T> adopted(Connected<T> parent) {
        this.parent = parent;
        Set<Connected<T>> c = child();
        tree = parent.tree;
        adopt(c);
        return this;
    }
    public Connected<T> disown(Connected<T> child) {
        child.parent = null;
        child.tree = new Tree<>();
        tree.remove(this, child);
        return this;
    }

    //interaction
    /**
     * Self inclusive
     * @param work work
     */
    public void parentsWork(Consumer<Connected<T>> work) {
        if (!isOrphan()) {
            parent.parentsWork(work);
        }
        work.accept(this);
    }
    /**
     * Self inclusive
     * @param task task
     */
    public void childLabor(Consumer<Connected<T>> task) {
        child().forEach(c -> c.childLabor(task));
        task.accept(this);
    }
}