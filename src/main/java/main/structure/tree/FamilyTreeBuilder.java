package main.structure.tree;

import java.util.ArrayList;
import java.util.List;

public class FamilyTreeBuilder {
    public FamilyTreeBuilder() {
        root = Connected.root();
        branches = new ArrayList<>();
        branches.add(root);
        enter();
    }

    private final Connected root;
    private final List<Connected> branches; //0 = current parent, else, order = (child => parent)

    public FamilyTreeBuilder add(IConnectable c) {
        Connected child = root.replicate(c);
        branches.get(0).adopt(child);
        branches.set(branches.size()-1, child);
        return this;
    }

    public FamilyTreeBuilder addFlat(List<IConnectable> cs) {
        if (cs == null || cs.size() == 0) { return this; }

        Connected child = null; //for sure won't be null in the future
        for (IConnectable c : cs) {
            child = root.replicate(c);
            branches.get(0).adopt(child);
        }
        branches.set(branches.size()-1, child);
        return this;
    }
    public FamilyTreeBuilder addDeep(List<IConnectable> cs) {
        if (cs == null || cs.size() == 0) { return this; }

        cs.forEach(c -> {
            Connected child = root.replicate(c);
            branches.get(0).adopt(child);
            branches.set(branches.size()-1, child);
            enter();
        });
        exit();
        return this;
    }

    public FamilyTreeBuilder enter() {
        branches.add(0, branches.get(branches.size()-1));
        return this;
    }
    public FamilyTreeBuilder exit() {
        branches.remove(0);
        return this;
    }
    public FamilyTreeBuilder root() {
        branches.clear();
        branches.add(root);
        return this;
    }

    public Connected build() {
        return root;
    }
}
