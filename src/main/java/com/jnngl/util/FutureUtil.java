package com.jnngl.util;

import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

public class FutureUtil {
    
    public static <T> Supplier<T> withCompletionException(ThrowingSupplier<T, Exception> function) {
        return () -> {
            try {
                return function.get();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        };
    }
}
