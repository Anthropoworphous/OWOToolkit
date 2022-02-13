package main.index;

public class ID extends Index {
    public ID(int id) {
        this.id = id;
    }
    public ID(Index index) {
        this.id = index.toIndex();
    }

    private int id;

    @Override
    public int toIndex() {
        return id;
    }

    @Override
    public void setIndex(int index) {
        id = index;
    }

}
