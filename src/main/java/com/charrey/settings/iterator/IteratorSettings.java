package com.charrey.settings.iterator;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
public abstract class IteratorSettings implements Comparable<IteratorSettings> {


    public final int iterationStrategy;

    IteratorSettings(int iterationStrategy) {
        this.iterationStrategy = iterationStrategy;
    }

    public abstract int serialized();

    @Override
    public int compareTo(@NotNull IteratorSettings o) {
        return Integer.compare(serialized(), o.serialized());
    }

    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}