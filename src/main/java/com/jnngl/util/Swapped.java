/*
 * All Rights Reserved
 *
 * Copyright (c) 2023 JNNGL
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

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
