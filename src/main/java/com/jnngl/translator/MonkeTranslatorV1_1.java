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

package com.jnngl.translator;

public class MonkeTranslatorV1_1 extends MonkeTranslatorV1 {

    @Override
    public String getName() {
        return "MonkeLang v1.1";
    }

    @Override
    public String[] getDictionary() {
        return new String[] {
                "у", "У", "а", "А", "y", " ", "A", "a"
        };
    }
}
