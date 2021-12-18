package main.structure.tree;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings({"unused"})
public class Connected {
    public Connected(IConnectable value) {
        this.value = value;
    }

    private IConnectable value;
    public Connected setValue(IConnectable value) { this.value = value; return this; }
    public @Nullable IConnectable getValue() { return value; }

    private boolean infertile = false;
    private Connected parent = null;
    private final List<Connected> child = new ArrayList<>();

//static
    public static Connected root() {
        return new Connected(null);
    }
    public static List<Connected> getOneWideTreeFromChild(Connected child) {
        List<Connected> out = new ArrayList<>();
        do {
            out.add(child);
            child = child.parent;
        } while (!child.isOrphan());
        return out;
    }

//non-static

    //quarry
    public List<List<IConnectable>> toList() {
        return getChildlessChilds().stream()
                .map(c -> getOneWideTreeFromChild(c).stream()
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


        //modify
    /**
     * use a list of filters to check on the family tree layer by layer
     * @param filters list of filter, uses a different filter every lv
     * @param lv how deep the loop is in
     * @return all child(of deepest layer the filters can reach)
     * that matched the filter from the top of the family tree
     */
    public List<Connected> multiLayerFilter(List<Function<IConnectable, Boolean>> filters, int lv) {
        if (filters.size()-1 == lv) {
            return Collections.singletonList(this);
        } else {
            return child.stream()
                    .filter(child -> filters.get(lv).apply(child.value))
                    .flatMap(child -> child.multiLayerFilter(filters, lv+1).stream())
                    .collect(Collectors.toList());
        }
    }
    public List<Connected> allLayerFilter(Function<IConnectable, Boolean> filter, boolean eliminateFailedParent) {
        return child.stream()   //below, filter only apply if eliminateFailedParent is true
                .filter(child -> !eliminateFailedParent || filter.apply(child.value))
                .flatMap(child -> child.allLayerFilter(filter, true).stream())
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

    public Connected replicate(IConnectable value) {
        return new Connected(value);
    }

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
        }

        private final Connected root;
        private final List<Connected> branches;

        public FamillyTreeBuilder add(IConnectable c) {
            branches.get(0).adopt(root.replicate(c));
            branches.set(0, root.replicate(c));
            return this;
        }

        public FamillyTreeBuilder enter() {
            branches.add(0, branches.get(0));
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
