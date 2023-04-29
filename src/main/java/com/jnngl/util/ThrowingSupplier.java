package com.jnngl.util;

public interface ThrowingSupplier<R, E extends Throwable> {
    
    R get() throws E;
}
