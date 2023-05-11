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
