package main.structure.tree;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class Connected implements Cloneable {
    public Connected(IConnectable value) {
        this.value = value;
    }

    //store value that's getting connected
    private IConnectable value;
    public Connected setValue(IConnectable value) { this.value = value; return this; }
    @SuppressWarnings("unchecked") public <T> T getValue(Class<T> clazz) {
        return (T)value;
    }
    public @Nullable IConnectable getValue() { return value; }

    //fields
    private boolean infertile = false;
    private transient Connected parent = null;
    private List<Connected> child = new ArrayList<>();
    private int generation = 0;
    //end

//static
    public static Connected root() {
        return new Connected(new IConnectable() {
            @Override
            public String toString() {
                return "ROOT";
            }
        });
    }

    public static List<Connected> getSlimTreeFromChild(Connected child) {
        ArrayList<Connected> out;

        for(out = new ArrayList<>(); !child.isOrphan(); child = child.parent) {
            out.add(child);
        } out.add(child);

        return out;
    }

//non-static
    //get
    @Override public String toString() {
        return (isOrphan() ? "null" : parent.value.toString())
                + " -> " + (value == null ? "null" : value.toString())
                + " -> [" + child.stream().map(c -> c.value.toString()).collect(Collectors.joining(", "))
                + "] ";
    }

    public List<List<Connected>> toList() {
        return getChildlessChild().stream()
                .map(Connected::getSlimTreeFromChild)
                .collect(Collectors.toList());
    }

    public List<Set<Connected>> toLayer() {
        List<Set<Connected>> out = new ArrayList<>();

        childLabor(c -> {
            for (int i = c.generation - out.size(); i >= 0; i--) {
                out.add(new HashSet<>());
            }
            out.get(c.generation).add(c);
        });

        return out;
    }

    public List<Connected> getChildlessChild() {
        List<Connected> list = new ArrayList<>();
        for (Connected c : child) {
            list.addAll(c.isChildless() ?
                    Collections.singletonList(c) :
                    c.getChildlessChild());
        }
        return list;
    }
    public Connected getParentlessParent() {
        return isOrphan() ? this : parent.getParentlessParent();
    }

    /**
     * use a list of filters to check on the family tree layer by layer
     * root is skipped, check start at root's child
     * @param filters list of filter, uses a different filter every layer
     * @param hasToReachTheEnd the final layer also have to be childless if true
     * @return all child(of deepest layer the filters can reach)
     * that matched the filter from the top of the family tree
     */
    public List<Connected> multiLayerFilter(
            List<Function<IConnectable, Boolean>> filters, boolean hasToReachTheEnd) {
        return multiLayerFilter(filters, 0, hasToReachTheEnd);
    }
    private List<Connected> multiLayerFilter(
            List<Function<IConnectable, Boolean>> filters, int lv, boolean hasToReachTheEnd) {
        List<Connected> output = new ArrayList<>();
        for (Connected c : child) {
            if (filters.get(lv).apply(c.value)) {
                if (lv == filters.size()-1) { //if this is the final loop, return itself all the way to top lv (0)
                    if (!hasToReachTheEnd || c.isChildless())
                        output.add(c);
                } else if (!c.isChildless()) { //if childless + not the end = not enough value to match
                    output.addAll(c.multiLayerFilter(filters, lv + 1, hasToReachTheEnd));
                }
            }
        }
        return output;
    }
    public List<Connected> allLayerFilter(Function<IConnectable, Boolean> filter, boolean eliminateFailedParent) {
        return child.stream()   //below, filter only apply if eliminateFailedParent is true
                .filter(child -> !eliminateFailedParent || filter.apply(child.value))
                .flatMap(child -> child.isChildless() ? this.child.stream() :
                        child.allLayerFilter(filter, true).stream())
                .collect(Collectors.toList());
    }


    public boolean isOrphan() {
        return (parent == null);
    }
    public boolean isChildless() {
        return (child == null || child.size() == 0);
    }

    public int getGeneration() { return generation; }
    public Optional<Connected> getParent() {
        return Optional.of(parent);
    }
    public List<Connected> getChild() {
        return child;
    }

    /**
     * create similer child using existing instance
     * make sure the new child is adopted
     * @param value value of the new child
     * @return new child
     */
    public Connected replicate(IConnectable value) {
        return new Connected(value);
    }

    //set
    public Connected adopt(Connected child) {
        if (!infertile) {
            this.child.add(child.adopted(this));
        } else {
            throw new RuntimeException("Parent is infertile, couldn't add child");
        }
        return this;
    }
    public Connected adopt(List<Connected> child) {
        if (!infertile) {
            child.forEach(this::adopted);
            this.child.addAll(child);
        } else {
            throw new RuntimeException("Parent is infertile, couldn't add child");
        }
        return this;
    }
    public Connected reproduce(IConnectable child) {
        if (!infertile) {
            this.child.add(new Connected(child).adopted(this));
        } else {
            throw new RuntimeException("Parent is infertile, couldn't add child");
        }
        return this;
    }
    public Connected reproduce(List<IConnectable> child) {
        if (!infertile) {
            child.forEach(c -> this.child.add(new Connected(c).adopted(this)));
        } else {
            throw new RuntimeException("Parent is infertile, couldn't add child");
        }
        return this;
    }
    public Connected reproduce(IConnectable... child) {
        if (!infertile) {
            List.of(child).forEach(c -> this.child.add(new Connected(c).adopted(this)));
        } else {
            throw new RuntimeException("Parent is infertile, couldn't add child");
        }
        return this;
    }

    public Connected adopted(Connected parent) {
        this.parent = parent;
        childLabor(c -> c.generation++);
        return this;
    }

    /**
     * quickly create a 1 wide vertical familly tree
     * @param child will get put in the tree with the order of the list
     * @return root
     */
    public Connected treeAdopt(List<Connected> child) {
        Connected current = this;
        for (Connected c : child) {
            current.adopt(c);
            current = c;
        }
        return this;
    }
    /**
     * quickly create a 1 wide vertical familly tree
     * @param child will get put in the tree with the order of the list
     * @return root
     */
    public Connected treeReproduce(List<IConnectable> child) {
        Connected current = this;
        for (IConnectable c : child) {
            Connected connected = new Connected(c);
            current.adopt(connected);
            current = connected;
        }
        return this;
    }

    public Connected abandon() {
        parent = null;
        generation = 0;
        return this;
    }
    public Connected disown(int index) {
        child.remove(index).abandon();
        return this;
    }
    public Connected disown(Connected child) {
        child.abandon();
        this.child.remove(child);
        return this;
    }
    public Connected neuter(boolean bool) {
        infertile = bool;
        return this;
    }

    //interaction

    /**
     * Self inclusive
     * @param work work
     */
    public void parentsWork(Consumer<Connected> work) {
        if (!isOrphan()) {
            parent.parentsWork(work);
        }
        work.accept(this);
    }

    /**
     * Self inclusive
     * @param task task
     */
    public void childLabor(Consumer<Connected> task) {
        if (!isChildless()) {
            child.forEach(c -> c.childLabor(task));
        }
        task.accept(this);
    }

    @Override
    public Connected clone() {
        Connected clone;
        try {
            clone = (Connected) super.clone();
        } catch (CloneNotSupportedException e) {
            clone = new Connected(value);
        }
        clone.infertile = infertile;
        clone.parent = parent.clone();
        clone.child = new ArrayList<>();
        for (Connected c : child) {
            Connected childClone = c.clone();
            clone.child.add(childClone);
        }
        return clone;
    }
}