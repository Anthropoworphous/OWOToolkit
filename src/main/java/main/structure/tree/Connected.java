package main.structure.tree;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unused"})
public class Connected implements Cloneable {
    public Connected(IConnectable value) {
        this.value = value;
    }

    //store value that's getting connected
    private IConnectable value;
    public Connected setValue(IConnectable value) { this.value = value; return this; }
    public @Nullable IConnectable getValue() { return value; }

    //fields
    private boolean infertile = false;
    private Connected parent = null;
    private List<Connected> child = new ArrayList<>();

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
        List<Connected> out = new ArrayList<>();
        do {
            out.add(child);
            child = child.parent;
        } while (!child.isOrphan());
        return out;
    }

//non-static
    //quarry
    @Override public String toString() {
        return (isOrphan() ? "null" : parent.value.toString())
                + " -> " + (value == null ? "null" : value.toString())
                + " -> [" + child.stream().map(c -> c.value.toString()).collect(Collectors.joining(", "))
                + "] ";
    }
    public List<List<IConnectable>> toList() {
        return getChildlessChilds().stream()
                .map(c -> getSlimTreeFromChild(c).stream()
                        .map(cc -> cc.value)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
    public List<Connected> getChildlessChilds() {
        List<Connected> list = new ArrayList<>();
        for (Connected c : child) {
            list.addAll(c.isChildless() ?
                    Collections.singletonList(c) :
                    c.getChildlessChilds());
        }
        return list;
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
        return (child.size() == 0);
    }

    public @Nullable Connected getParent() {
        return parent;
    }
    public @Nullable List<Connected> getChild() {
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

    //modify
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
            this.child.addAll(child);
            child.forEach(this::adopted);
        } else {
            throw new RuntimeException("Parent is infertile, couldn't add child");
        }
        return this;
    }
    public Connected adopt(Connected... child) {
        if (!infertile) {
            this.child.addAll(List.of(child));
            this.child.forEach(this::adopted);
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
        return this;
    }
    public Connected disown(int index) {
        child.remove(index).abandon();
        return this;
    }
    public Connected disown(Connected child) {
        int index = this.child.indexOf(child);
        this.child.remove(index).abandon();
        return this;
    }
    public Connected neuter(boolean bool) {
        infertile = bool;
        return this;
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



    /**
     * empty marker interface
     */
    public interface IConnectable {}

    /**
     * might be wack but hey i tried ok
     * build connected class tree like the name says
     */
    public static class FamillyTreeBuilder {
        public FamillyTreeBuilder() {
            root = Connected.root();
            branches = new ArrayList<>();
            branches.add(root);
            enter();
        }

        private final Connected root;
        private final List<Connected> branches;

        public FamillyTreeBuilder add(IConnectable c) {
            Connected child = root.replicate(c);
            branches.get(0).adopt(child);
            branches.set(branches.size()-1, child);
            return this;
        }

        public FamillyTreeBuilder enter() {
            branches.add(0, branches.get(branches.size()-1));
            return this;
        }
        public FamillyTreeBuilder exit() {
            branches.remove(0);
            return this;
        }
        public FamillyTreeBuilder root() {
            branches.clear();
            branches.add(root);
            return this;
        }

        public Connected build() {
            return root;
        }
    }
}