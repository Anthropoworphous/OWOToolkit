package main.structure.index;

public class XY extends Index {
    public XY(int x, int y, int width) {
        this.width = width;
        this.x = x;
        this.y = y;
    }
    public XY(int index, int width) {
        this.width = width;
        x = index % width + 1;
        y = index / width + 1;
    }
    public XY(Index index, int width) {
        this.width = width;
        x = index.toIndex() % width + 1;
        y = index.toIndex() / width + 1;
    }

    int width, x, y;

    public int x() {
        return x;
    }
    public void x(int x) {
        this.x = x;
    }
    public int y() {
        return y;
    }
    public void y(int y) {
        this.y = y;
    }
    public int width() {
        return width;
    }
    public void width(int width) {
        this.width = width;
    }

    //xy special move :) 69% less brain cells consuming
    public XY offset(int x, int y) {
        return new XY(this.x+x, this.y+y, width);
    }
    public XY move(int x, int y) {
        this.x += x;
        this.y += y;
        return this;
    }

    @Override
    public int toIndex() {
        return (y - 1) * width + (x - 1);
    }

    @Override
    public void setIndex(int index) {
        x = index % width + 1;
        y = index / width + 1;
    }
}
