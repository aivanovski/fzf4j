/*
 * Copyright (c) 2020 Jan Graßegger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.gesundkrank.fzf4j.models;

public class Result {

    private final String text;
    private final int start;
    private final int end;
    private final int score;
    private final int[] positions;
    private final int itemIndex;

    public Result(final String text, int itemIndex) {
        this(text, -1, -1, 0, null, itemIndex);
    }

    public Result(String text, int start, int end, int score, int[] positions, int itemIndex) {
        this.text = text;
        this.start = start;
        this.end = end;
        this.score = score;
        this.positions = positions;
        this.itemIndex = itemIndex;
    }

    public String getText() {
        return text;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public int getScore() {
        return score;
    }

    public int[] getPositions() {
        return positions;
    }

    public boolean isMatch() {
        return start != -1 && end != -1;
    }

    public int getItemIndex() {
        return itemIndex;
    }
}