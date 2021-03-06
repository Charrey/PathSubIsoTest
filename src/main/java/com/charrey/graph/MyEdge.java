package com.charrey.graph;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Comparator;
import java.util.function.Supplier;

public class MyEdge implements Comparable<MyEdge>, Serializable {

    private Integer source;
    private Integer target;
    private int id;

    public MyEdge() {
    }

    public MyEdge(Integer source, Integer target) {
        this.source = source;
        this.target = target;
    }

    public void setTarget(Integer targetVertex) {
        target = targetVertex;
    }

    public void setSource(Integer sourceVertex) {
        source = sourceVertex;
    }

    @Override
    public String toString() {
        return "(" + source + ", " + target + ")";
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int sum = source + target;
        return target + (sum * (sum + 1)) / 2;
    }

    public int compareTo(@NotNull MyEdge other) {
        return Comparator.comparingInt(x -> ((MyEdge) x).source).thenComparingInt(x -> ((MyEdge) x).target).compare(this, other);
    }

    public int getSource() {
        return source;
    }

    public int getTarget() {
        return target;
    }

    public static class MyEdgeSupplier implements Supplier<MyEdge>, Serializable {
        @Override
        public MyEdge get() {
            return new MyEdge();
        }
    }


    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }
}
