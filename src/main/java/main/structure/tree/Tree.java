package main.structure.tree;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Tree<T> {
    private Map<Connected<T>, Set<Connected<T>>> layers = new HashMap<>();

    //getter
    public Map<Connected<T>, Set<Connected<T>>> layers() {
        return layers;
    }
    //setter
    public void layers(Map<Connected<T>, Set<Connected<T>>> layers) {
        this.layers = layers;
    }
    //end

    public void remove(Connected<T> parent, Connected<T> child) {
        if (layers.containsKey(parent)) {
            layers.get(parent).remove(child);
        }
    }
    public void remove(Connected<T> child) {
        layers.keySet().forEach(parent -> remove(parent, child));
    }
}