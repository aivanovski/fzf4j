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

package de.gesundkrank.fzf4j.matchers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import de.gesundkrank.fzf4j.models.OrderBy;
import de.gesundkrank.fzf4j.models.Result;
import de.gesundkrank.fzf4j.utils.ResultComparator;


/**
 * Class to filter and rank and items
 * Based on: https://github.com/junegunn/fzf/blob/master/src/algo/algo.go
 */
public class FuzzyMatcherV1 {

    final static int SCORE_MATCH = 16;
    final static int SCORE_GAP_START = -3;
    final static int SCORE_GAP_EXTENSION = -1;

    final static int BONUS_BOUNDARY = SCORE_MATCH / 2;
    final static int BONUS_NON_WORD = SCORE_MATCH / 2;
    final static int BONUS_CAMEL_123 = BONUS_BOUNDARY + SCORE_GAP_EXTENSION;
    final static int BONUS_CONSECUTIVE = -(SCORE_GAP_START + SCORE_GAP_EXTENSION);
    final static int BONUS_FIRST_CHAR_MULTIPLIER = 2;

    private final List<String> items;
    private final OrderBy orderBy;

    public FuzzyMatcherV1(final List<String> items, final OrderBy orderBy) {
        this.items = items;
        this.orderBy = orderBy;
    }

    public List<Result> match(String pattern) {
        if (pattern.isEmpty()) {
            return IntStream.range(0, items.size()).parallel()
                    .mapToObj(i -> new Result(items.get(i), i))
                    .collect(Collectors.toList());
        }

        return IntStream.range(0, items.size()).parallel()
                .mapToObj(i -> match(items.get(i), pattern, i))
                .filter(Result::isMatch)
                .sorted(new ResultComparator(orderBy))
                .collect(Collectors.toList());
    }

    private Result match(String text, String pattern, int itemIndex) {
        var queryIndex = 0;
        var startIndex = -1;
        var endIndex = -1;

        for (int textIndex = 0; textIndex < text.length(); textIndex++) {
            final char textChar = text.charAt(textIndex);
            final char queryChar = pattern.charAt(queryIndex);

            if (textChar == queryChar) {

                if (startIndex == -1) {
                    startIndex = textIndex;
                }

                if (queryIndex == pattern.length() - 1) {
                    endIndex = textIndex + 1;
                    break;
                }

                queryIndex++;
            }
        }

        if (startIndex != -1 && endIndex != -1) {
            for (int textIndex = endIndex - 1; textIndex > startIndex; textIndex--) {
                final var textChar = text.charAt(textIndex);
                final var queryChar = pattern.charAt(queryIndex);

                if (textChar == queryChar) {
                    if (queryIndex == 0) {
                        startIndex = textIndex;
                        break;
                    }

                    queryIndex--;
                }
            }

            return calculateScore(text, pattern, startIndex, endIndex, itemIndex);
        }

        return new Result(text, itemIndex);
    }

    private Result calculateScore(
            final String text, String pattern, int startIndex, int endIndex, int itemIndex
    ) {
        var patternIndex = 0;
        var score = 0;
        var consecutive = 0;
        var firstBonus = 0;
        var inGap = false;
        var pos = new int[pattern.length()];

        var prevClass = startIndex > 0 ? CharClass.forChar(text.charAt(startIndex - 1))
                                       : CharClass.NON_WORD;

        for (var i = startIndex; i < endIndex; i++) {
            final var c = text.charAt(i);
            final var charClass = CharClass.forChar(c);

            if (c == pattern.charAt(patternIndex)) {
                pos[patternIndex] = i;

                score += SCORE_MATCH;
                var bonus = bonusFor(prevClass, charClass);

                if (consecutive == 0) {
                    firstBonus += bonus;
                } else {
                    // Break consecutive chunk
                    if (bonus == BONUS_BOUNDARY) {
                        firstBonus = bonus;
                    }
                    bonus = Math.max(Math.max(bonus, firstBonus), BONUS_CONSECUTIVE);
                }

                if (patternIndex == 0) {
                    score += bonus * BONUS_FIRST_CHAR_MULTIPLIER;
                } else {
                    score += bonus;
                }
                inGap = false;
                consecutive++;
                patternIndex++;
            } else {
                if (inGap) {
                    score += SCORE_GAP_EXTENSION;
                } else {
                    score += SCORE_GAP_START;
                }

                inGap = true;
                consecutive = 0;
                firstBonus = 0;
            }
            prevClass = charClass;
        }

        return new Result(text, startIndex, endIndex, score, pos, itemIndex);
    }

    private int bonusFor(CharClass prevClass, CharClass charClass) {
        if (prevClass == CharClass.NON_WORD && charClass != CharClass.NON_WORD) {
            return BONUS_BOUNDARY;
        } else if (prevClass == CharClass.LOWER && charClass == CharClass.UPPER ||
                   prevClass != CharClass.NUMBER && charClass == CharClass.NUMBER) {
            // camelCase letter123
            return BONUS_CAMEL_123;
        } else if (charClass == CharClass.NON_WORD) {
            return BONUS_NON_WORD;
        }
        return 0;
    }


    private enum CharClass {
        LOWER, UPPER, LETTER, NUMBER, NON_WORD;

        public static CharClass forChar(char c) {
            if (Character.isLowerCase(c)) {
                return LOWER;
            } else if (Character.isUpperCase(c)) {
                return UPPER;
            } else if (Character.isDigit(c)) {
                return NUMBER;
            } else if (Character.isLetter(c)) {
                return LETTER;
            }
            return NON_WORD;
        }
    }
}