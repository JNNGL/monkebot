package com.jnngl.util;

import java.util.function.Supplier;

public class Swapped<T> {
    private final T first;
    private final T second;
    private T current;

    public Swapped(T first, T second) {
        this.first = first;
        this.second = current = second;
    }

    public Swapped(Supplier<T> supplier) {
        this(supplier.get(), supplier.get());
    }

    public T getNext() {
        return current = (current == first ? second : first);
    }

    public T getFirst() {
        return first;
    }

    public T getSecond() {
        return second;
    }
}
