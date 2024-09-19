/*******************************************************************************
 * Copyright (c) 2009-2018 CWI, SWAT.engineering
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Arnold Lankamp - interfaces and implementation - CWI
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Jurgen Vinju - balanced trees and lazy indent - CWI
 *   * Bert Lisser - balanced trees and lazy indent - CWI
 *   * Davy Landman - smart unicode implementations - CWI, SWAT.engineering
 *******************************************************************************/
package io.usethesource.vallang.impl.primitive;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.nio.Buffer;
import java.nio.CharBuffer;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;
import java.util.PrimitiveIterator.OfInt;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.impl.persistent.ValueFactory;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

/**
 * Find here the implementations of IString, which all are (must be) sub-classes of  {@link AbstractString}
 * as an internal design invariant.
 *
 * The challenges solved by this implementation:
 *   - cater for and optimize for the normal case of strings containing only normal ASCII characters, while
 *     still allowing all 24-bit unicode characters, see {@link FullUnicodeString} and {@link SimpleUnicodeString}
 *   - optimize string {@link IString#concat(IString)} method, in combination with {@link IString#write(Writer)} and {@link IString#iterator()},
 *     see {@link IStringTreeNode} and {@link LazyConcatString}.
 *   - optimize the {@link IString#indent(IString)} method, in combination with {@link IString#write(Writer)} and {@link IString#iterator()},
 *     see {@link IIndentableString} and {@link IndentedString}
 *   - allow non-canonical representations of the same string, in particular making sure that equals() and hashCode() works
 *     well across and between different representations of (accidentally) the same string, see {@link AbstractString#hashCode()},
 *     and {@link AbstractString#equals()} and their overrides.
 *
 * The 'normal' case is defined by what people normally do in Rascal, i.e. using string template expanders a lot,
 * and reading and writing to text files, searching through strings. The other operations on strings are implemented,
 * but less optimally. They're traded for the optimization of {@link IString#concat(IString)} and {@link IString#indent(IString)}.
 *
 * TODO: make a faster path for short strings (i.e. English words) which do not contain high surrogate pairs
 */
/* package */ public class StringValue {
    private static final char NEWLINE = '\n';
    private static final Type STRING_TYPE = TypeFactory.getInstance().stringType();

    private static final int DEFAULT_MAX_FLAT_STRING = 512; /* typical buffer size maximum */
    private static int MAX_FLAT_STRING = DEFAULT_MAX_FLAT_STRING;

    private static final int DEFAULT_MAX_UNBALANCE = 0;
    private static int MAX_UNBALANCE = DEFAULT_MAX_UNBALANCE;

    /** for testing purposes we can set the max flat string value */
    public static synchronized void setMaxFlatString(int maxFlatString) {
        MAX_FLAT_STRING = maxFlatString;
    }

    /** for testing purposes we can set the max flat string value */
    public static synchronized void resetMaxFlatString() {
        MAX_FLAT_STRING = DEFAULT_MAX_FLAT_STRING;
    }

    /** for testing and tuning purposes we can set the max unbalance factor */
    public static synchronized void setMaxUnbalance(int maxUnbalance) {
        MAX_UNBALANCE = maxUnbalance;
    }

    public static synchronized void resetMaxUnbalance() {
        MAX_UNBALANCE = DEFAULT_MAX_UNBALANCE;
    }

    public static IString newString(String value) {
        if (value == null || value.isEmpty()) {
            return EmptyString.getInstance();
        }

        // we do not reuse newString(String value, boolean fullUnicode),
        // or vice versa, because we want to run over the string only once
        // to collect the count and the containsSurrogatePairs

        boolean containsSurrogatePairs = false;
        int count = 0;
        int len = value.length();

        char prev = '\0'; // make sure Character.isSurrogatePair(prev, cur) is not true for the first char

        for (int i = 0; i < len; i++) {
            char cur = value.charAt(i);

            containsSurrogatePairs |= Character.isSurrogatePair(prev, cur);

            if (cur == NEWLINE) {
                count++;
            }

            prev = cur;
        }

        // end-of-file counts as a line terminator, unless we terminated the string with a newline,
        // we should count this last line too:
        char last = value.charAt(len - 1) ;
        if (last != NEWLINE) {
            count++;
        }

        return newString(value, containsSurrogatePairs, count);
    }

    public static IString newString(String value, boolean fullUnicode) {
        if (value == null || value.isEmpty()) {
            return EmptyString.getInstance();
        }

        int count = 0;

        int len = value.length();

        for (int i = 0; i < len; i++) {
            char cur = value.charAt(i);

            if (cur == NEWLINE) {
                count++;
            }
        }

        // end-of-file counts as a line terminator, unless we terminated the string with a newline,
        // we should count this last line too:
        char last = value.charAt(len - 1) ;
        if (last != NEWLINE) {
            count++;
        }


        return newString(value, fullUnicode, count);
    }

    /* package */ static IString newString(String value, boolean fullUnicode, int lineCount) {
        if (value == null || value.isEmpty()) {
            return EmptyString.getInstance();
        }

        if (fullUnicode) {
            return new FullUnicodeString(value, lineCount);
        }

        return new SimpleUnicodeString(value, lineCount);
    }

    /**
     * Empty strings are so ubiquitous that we (a) make only one instance and specialize all
     * of its operations for speed.
     *
     */
    private static class EmptyString extends AbstractString {
        private static class InstanceHolder {
            public static EmptyString instance = new EmptyString();
        }

        public static EmptyString getInstance() {
            return InstanceHolder.instance;
        }

        private EmptyString() {  }

        @Override
        boolean hasNonBMPCodePoints() {
            return false;
        }

        @Override
        public String getValue() {
            return "";
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return other == this;
        }

        @Override
        public IString reverse() {
            return this;
        }

        @Override
        public int length() {
            return 0;
        }

        @Override
        public IString substring(int start, int end) {
            if (start == 0 && end == 0) {
                return this;
            }

            throw new IndexOutOfBoundsException();
        }

        @Override
        public int charAt(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public IString replace(int first, int second, int end, IString repl) {
            if (first == 0 && end == 0) {
                return repl;
            }

            throw new IndexOutOfBoundsException();
        }

        @Override
        public void write(Writer w) throws IOException {
        }

        @Override
        public void indentedWrite(Writer w, Deque<IString> whiteSpace, boolean indentFirstLine) {
        }

        @Override
        public PrimitiveIterator.OfInt iterator() {
            return new PrimitiveIterator.OfInt() {

                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public int nextInt() {
                    throw new NoSuchElementException();
                }
            };
        }

        @Override
        public IString indent(IString whitespace, boolean indentFirstLine) {
            return !indentFirstLine ? this : whitespace;
        }

        @Override
        public int lineCount() {
            return 0;
        }

        @Override
        public boolean isNewlineTerminated() {
            return false;
        }

        @Override
        public IString concat(IString other) {
            return other;
        }
    }

    private static class FullUnicodeString extends AbstractString {

        protected final String value;
        protected final int lineCount;

        private FullUnicodeString(String value, int lineCount) {
            super();

            this.value = value;
            this.lineCount = lineCount;
        }

        @Override
        protected boolean hasNonBMPCodePoints() {
            return true;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public boolean isNewlineTerminated() {
            return value.isEmpty() ? false : value.charAt(value.length() - 1) == NEWLINE;
        }

        @Override
        public int lineCount() {
            return lineCount;
        }

        @Override
        public IString concat(IString other) {
            if (other.length() == 0) {
                return this;
            }

            // We fuse the strings, but only if this does not introduce strings with multiple newlines,
            // and only the string would not grow beyond MAX_FLAT_STRING.
            // The reason for the first is that single line strings flush faster to the write buffers.
            // The reason for the second is that longer strings require in O(n) codepoint access, while the
            // balanced trees amortize access time to in O(log^2(n)).
            AbstractString o = (AbstractString) other;
            int newLineCount;

            if (length() + other.length() <= MAX_FLAT_STRING && (newLineCount = IIndentableString.concatLineCount(this, o)) <= 1) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(getValue());
                buffer.append(other.getValue());

                return StringValue.newString(buffer.toString(), hasNonBMPCodePoints() || o.hasNonBMPCodePoints(), newLineCount);
            } else {
                // For longer strings with many newlines it's usually better to concatenate lazily
                // This makes concatenation in O(1) as opposed to O(n) where n is the length of the resulting string.
                return LazyConcatString.build(this, (AbstractString) other);
            }
        }

        /**
         * Note that this algorithm can not be changed, unless you also have change
         * BinaryBalancedTreeNode.hashCode() and DefaultString.hashCode() (to not break the hashCode/equals
         * contract).
         */
        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return super.equals(other);
        }

        @Override
        public IString reverse() {
            return newString(new StringBuilder(value).reverse().toString(), true, lineCount);
        }

        @Override
        public int length() {
            return value.codePointCount(0, value.length());
        }

        @Override
        public IString substring(int start, int end) {
            return newString(value.substring(value.offsetByCodePoints(0, start), value.offsetByCodePoints(0, end)));
        }

        @Override
        public IString substring(int start) {
            return newString(value.substring(value.offsetByCodePoints(0, start)));
        }

        @Override
        public int charAt(int index) {
            return value.codePointAt(value.offsetByCodePoints(0, index));
        }

        private int nextCP(CharBuffer cbuf) {
            int cp = Character.codePointAt(cbuf, 0);
            Buffer buffer = (Buffer) cbuf; // to select the right overloaded `position` method in Java 9
            if (buffer.position() < buffer.capacity()) {
                buffer.position(buffer.position() + Character.charCount(cp));
            }
            return cp;
        }

        private void skipCP(CharBuffer cbuf) {
            if (cbuf.hasRemaining()) {
                Buffer buffer = (Buffer) cbuf; // to select the right overloaded `position` method in Java 9

                int cp = Character.codePointAt(cbuf, 0);
                buffer.position(buffer.position() + Character.charCount(cp));
            }
        }

        @Override
        public IString replace(int first, int second, int end, IString repl) {
            StringBuilder buffer = new StringBuilder();

            int valueLen = value.codePointCount(0, value.length());
            CharBuffer valueBuf;

            int replLen = repl.length();
            Buffer replBuf = CharBuffer.wrap(repl.getValue());

            int increment = Math.abs(second - first);
            if (first <= end) {
                valueBuf = CharBuffer.wrap(value);
                int valueIndex = 0;
                // Before begin (from left to right)
                while (valueIndex < first) {
                    buffer.appendCodePoint(nextCP(valueBuf));
                    valueIndex++;
                }
                int replIndex = 0;
                boolean wrapped = false;
                // Between begin and end
                while (valueIndex < end) {
                    buffer.appendCodePoint(nextCP((CharBuffer) replBuf));
                    replIndex++;
                    if (replIndex == replLen) {
                        replBuf.position(0);
                        replIndex = 0;
                        wrapped = true;
                    }
                    skipCP(valueBuf);
                    valueIndex++; // skip the replaced element
                    for (int j = 1; j < increment && valueIndex < end; j++) {
                        buffer.appendCodePoint(nextCP(valueBuf));
                        valueIndex++;
                    }
                }
                if (!wrapped) {
                    while (replIndex < replLen) {
                        buffer.appendCodePoint(nextCP((CharBuffer) replBuf));
                        replIndex++;
                    }
                }
                // After end

                while (valueIndex < valueLen) {
                    buffer.appendCodePoint(nextCP(valueBuf));
                    valueIndex++;
                }
            } else {
                // Before begin (from right to left)

                // Place reversed value of fValue in valueBuffer for better sequential code
                // point access
                // Also add code points to buffer in reverse order and reverse again at the end
                valueBuf = CharBuffer.wrap(new StringBuilder(value).reverse().toString());

                int valueIndex = valueLen - 1;
                while (valueIndex > first) {
                    buffer.appendCodePoint(nextCP(valueBuf));
                    valueIndex--;
                }
                // Between begin (right) and end (left)
                int replIndex = 0;
                boolean wrapped = false;
                while (valueIndex > end) {
                    buffer.appendCodePoint(nextCP((CharBuffer) replBuf));
                    replIndex++;
                    if (replIndex == repl.length()) {
                        replBuf.position(0);
                        replIndex = 0;
                        wrapped = true;
                    }
                    skipCP(valueBuf);
                    valueIndex--; // skip the replaced element
                    for (int j = 1; j < increment && valueIndex > end; j++) {
                        buffer.appendCodePoint(nextCP(valueBuf));
                        valueIndex--;
                    }
                }
                if (!wrapped) {
                    while (replIndex < replLen) {
                        buffer.appendCodePoint(nextCP((CharBuffer) replBuf));
                        replIndex++;
                    }
                }
                // Left of end
                while (valueIndex >= 0) {
                    buffer.appendCodePoint(nextCP(valueBuf));
                    valueIndex--;
                }
                buffer.reverse();
            }

            String res = buffer.toString();
            return StringValue.newString(res);
        }

        @Override
        public void write(Writer w) throws IOException {
            w.write(value);
        }

        @Override
        public void indentedWrite(Writer w, Deque<IString> whitespace, boolean indentFirstLine) throws IOException {
            if (value.isEmpty()) {
                return;
            }

            if (indentFirstLine) {
                writeWhitespace(w, whitespace);
            }

            if (lineCount <= 1) {
                // single lines can be streamed immediately.
                // this is a hot and fast path since most indented strings are build from
                // templates which are concatenated line-by-line
                w.write(value);
                return;
            }

            // otherwise we have to find the newline characters one-by-one.
            // this implementation tries to quickly find the next newline using indexOf, and streams
            // line by line to optimize copying the characters onto the stream in bigger blobs than 1 character.
            for (int pos = value.indexOf(NEWLINE), next = 0; ; next = pos + 1, pos = value.indexOf(NEWLINE, next)) {
                if (pos == -1) {
                    // no more newlines, so write the entire line
                    if (next != value.length()) {
                        w.write(value, next, value.length() - next);
                    }
                    // and we are done
                    return;
                }
                else {
                    // write until the currently found newline
                    // and continue to find the next newline.
                    w.write(value, next, pos - next + 1);

                    // write the indent for the next line
                    if (pos < value.length() - 1) {
                        writeWhitespace(w, whitespace);
                    }
                }
            }
        }

        private void writeWhitespace(Writer w, Deque<IString> whitespace) throws IOException {
            for (Iterator<IString> it = whitespace.descendingIterator(); it.hasNext(); ) {
                it.next().write(w);
            }
        }

        @Override
        public PrimitiveIterator.OfInt iterator() {
            return new PrimitiveIterator.OfInt() {
                int cur = 0;

                public boolean hasNext() {
                    return cur < value.length();
                }

                public int nextInt() {
                    final int length = value.length();
                    final String val = value;

                    if (cur >= length) {
                        throw new NoSuchElementException();
                    }

                    char c1 = val.charAt(cur++);
                    if (Character.isHighSurrogate(c1) && cur < length) {
                        char c2 = val.charAt(cur);
                        if (Character.isLowSurrogate(c2)) {
                            cur++;
                            return Character.toCodePoint(c1, c2);
                        }
                    }
                    return c1;
                }
            };
        }
    }

    /**
     * This class knows its contents do not contain any higher surrogate pairs,
     * allowing it to implement some indexing functions a lot faster, i.e. in O(1)
     * as opposed to in O(n).
     *
     */
    private static class SimpleUnicodeString extends FullUnicodeString {
        public SimpleUnicodeString(String value, int lineCount) {
            super(value, lineCount);
        }

        @Override
        protected boolean hasNonBMPCodePoints() {
            return false;
        }

        @Override
        public int length() {
            return value.length();
        }

        @Override
        public int charAt(int index) {
            return value.charAt(index);
        }

        @Override
        public IString substring(int start, int end) {
            return newString(value.substring(start, end), false);
        }

        @Override
        public IString reverse() {
            return newString(new StringBuilder(value).reverse().toString(), false, lineCount);
        }

        @Override
        public PrimitiveIterator.OfInt iterator() {
            return new PrimitiveIterator.OfInt() {
                private int cur = 0;

                @Override
                public boolean hasNext() {
                    return cur < value.length();
                }

                @Override
                public int nextInt() {
                    return (int) value.charAt(cur++);
                }
            };
        }
    }

    /**
     * About Lazy indentation
     * ---
     *
     * The next expensive operation on larger strings is indentation. The {@link #indent(IString)}
     * method should take every non-empty line of the current string and indent it with a given
     * string of whitespace characters.
     *
     * This operation is a bottleneck, after the concatenation bottleneck, since it requires
     * duplicating the entire input in memory if done naively, and that several times depending on the
     * nesting depth of indentation. A typical expanded string template would require making a number
     * of copies in O(d * n), where d is the nesting depth and n is the length of the original string.
     *
     * A lazy implementation simply remembers that indentation has to be done (in O(1)), and streams the
     * indented value on request directly to a writer (in O(1)). Since large generated strings are bound to be
     * streamed and not further analyzed, this is beneficial for the typical use.
     *
     * However, all other operations must still work, so relatively complex "indented" versions of
     * all IStringimplementations are distributed over the implementation classes of IString. These
     * operations are slower than their non-indented counterparts since they have to discover where
     * the newlines are over and over again. For example {@link #charAt(int)} goes from being in O(1)
     * to O(n) since we have to find all exact positions of newline characters.
     *
     * Always caching these positions would have too much of a
     * memory overhead. There might be something to say for caching these values for very large
     * strings, while recomputing them on-the-fly for smaller strings, in the future.
     *
     */
    private interface IIndentableString extends IString {

        /**
         * The non-empty lines are the ones which would be indented. This
         * exists to be able to quickly compute the exact length of an indented string,
         * and to be able to blt buffers of single lines directly to the underlying
         * I/O buffer.
         *
         * @return the number of _non-empty_ lines in a string.
         */
        int lineCount();

        /**
         * When concatenating indentable strings, the {@link #lineCount()} can
         * be computed exactly in O(1) by knowing if the left string does or does not
         * end in a newline:<br>
         *
         * Example:<br>
         * concat("#####\n", "@@@@@@") has two lines, <br>
         *    because both consituents have a single line and the left ends in a newline.<br>
         * concat("#####", "@@@@@@") has a single line, even though both consituents have<br>
         *    already a single line, concatenated they still form a single line. <br>
         *         <br>
         * @return true iff the last character of this string is a \n character.
         */
        default boolean isNewlineTerminated() {
            return length() != 0 && charAt(length() - 1) == NEWLINE;
        }

        /**
         * Utility function for use in the construction of IIndentableString implementations.
         * There are details to be handled with respect to the final line of left and the
         * first line of right.
         *
         * @param left
         * @param right
         * @return the sum of linecounts of left and right, which depends on whether the concatenation
         *        merges the last line of left with the first line of right or that such
         *        a merge does not happen.
         */
        public static int concatLineCount(IIndentableString left, IIndentableString right) {
            return left.lineCount() - (left.isNewlineTerminated() ? 0 : 1) + right.lineCount();
        }

        /**
         * Specialized (hopefully optimized) implementations of streaming an indented version
         * of an IString. Implementations should avoid allocating memory and try to write as many
         * bytes as possible in one block into the writer.
         *
         * @param w                writer to write to
         * @param whiteSpace       the whitespace to write before non-empty lines
         * @param indentFirstLine  whether or not to indent the first line
         * @throws IOException
         */
        public default void indentedWrite(Writer w, Deque<IString> indentStack, boolean indentFirstLine) throws IOException {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * About balanced concat trees
     * ---
     *
     * Concatenation must be fast when generating large strings (for example by a
     * Rascal program which uses `+` and template expansion. With the basic
     * implementation the left-hand side of the concat would always need to be
     * copied using System.arraycopy somewhere. For large prefixes this becomes a
     * bottleneck. The worst case execution time is in O(n^2) where n is the length
     * of the produced string. The smaller the steps taken (the more concat
     * operations) towards this length, the worse it gets.
     *
     * A simple solution is to build a binary tree of concatenation nodes which can
     * later be streamed in a linear fashion. That's a good solution because it is
     * an easy immutable implementation. However, the trees can become quite
     * unbalanced and therefore extremely deep (consider a number of Rascal for
     * loops in a template). The recursion required to stream such a tree would run
     * out of stack space regularly (we know from experience).
     *
     * So the current implementation of a lazy concat string _balances_ the tree to
     * maintain an invariant of an (almost) balanced tree. The worst case depth of
     * the tree will alway be in O(log(length)). We flatten out strings below 512
     * characters because the System.arraycopy below that number is still really
     * efficient. The cost we pay for balancing the tree is in O(log(length)), so
     * concat is now in O(log(length)) instead of in O(1), all to avoid the
     * StackOverflow.
     *
     * Note that an implementation with a destructively updated linked list would be
     * much faster, but due to immutability and lots of sharing of IStrings this is
     * not feasible.
     *

     */
    private static interface IStringTreeNode extends IString {
        /**
         * The leaf nodes have depth one; should be computed by the binary node
         */
        default int depth() {
            return 1;
        }

        /**
         * all tree nodes must always be almost fully balanced
         */
        default boolean invariant() {
            return Math.abs(balanceFactor()) - 1 <= MAX_UNBALANCE;
        }

        /**
         * The difference in depth between the right end the left branch (=1 in a leaf)
         */
        default int balanceFactor() {
            return 0;
        }

        /**
         * Should be overridden by the binary node and never called on the leaf nodes
         * because they are not out of balance.
         */
        default AbstractString left() {
            throw new UnsupportedOperationException();
        }

        /**
         * Should be overridden by the binary node and never called on the leaf nodes
         * because they are not out of balance.
         */
        default AbstractString right() {
            throw new UnsupportedOperationException();
        }

        default AbstractString rotateRight() {
            return (AbstractString) this;
        }

        default AbstractString rotateLeft() {
            return (AbstractString) this;
        }

        default AbstractString rotateRightLeft() {
            return (AbstractString) this;
        }

        default AbstractString rotateLeftRight() {
            return (AbstractString) this;
        }
    }

    private abstract static class AbstractString implements IString, IStringTreeNode, IIndentableString {
        @Override
        public String toString() {
            return defaultToString();
        }

        @Override
        public Type getType() {
            return STRING_TYPE;
        }

        @Override
        public IString indent(IString whitespace, boolean indentFirstLine) {
            assert !whitespace.getValue().contains("\n") && !whitespace.getValue().contains("\r");

            if (whitespace.length() == 0) {
                return this;
            }

            if (!indentFirstLine && lineCount() <= 1) {
                return this;
            }

            return new IndentedString(this, whitespace, indentFirstLine);
        }

        @Override
        /**
         * sub-classes that have direct access to a wrapped String should override this
         */
        public String getValue() {
            // the IString.length() under-estimates the size of the string if the string
            // contains many surrogate pairs, but that does not happen a lot in
            // most of what we see, so we decided to go for a tight estimate for
            // "normal" ASCII strings which contain around 10% non BMP code points.

            int len = length();

            try (StringWriter w = new StringWriter(len + len / 10 /* take care not too overflow int easily */)) {
                write(w);
                return w.toString();
            } catch (IOException e) {
                // this will not happen with a StringWriter
                return "";
            }
        }



        @Override
        public IString substring(int start) {
            return substring(start, length());
        }

        @Override
        public int compare(IString other) {
            PrimitiveIterator.OfInt  it1 = this.iterator();
            PrimitiveIterator.OfInt  it2 = other.iterator();

            while (it1.hasNext() && it2.hasNext()) {
                int c1 = it1.nextInt();
                int c2 = it2.nextInt();

                int diff = c1 - c2;
                if (diff != 0) {
                    return diff < 0 ? -1 : 1;
                }
            }

            int result = this.length() - other.length();

            if (result == 0) {
                return 0;
            } else if (result < 0) {
                return -1;
            } else { // result > 0
                return 1;
            }
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (other == null) {
                return false;
            }

            if (other == this) {
                return true;
            }

            if (!(other instanceof AbstractString)) {
                return false;
            }

            AbstractString o = (AbstractString) other;

            if (o.length() != length()) {
                return false;
            }

            if (o.lineCount() != lineCount()) {
                // another quick way to bail without having to iterate.
                return false;
            }

            PrimitiveIterator.OfInt  it1 = this.iterator();
            PrimitiveIterator.OfInt  it2 = o.iterator();

            while (it1.hasNext() && it2.hasNext()) {
                int c1 = it1.nextInt();
                int c2 = it2.nextInt();

                if (c1 != c2) {
                    return false;
                }
            }

            return true;
        }

        @Override
        /**
         * Note that we used the hashcode algorithm for java.lang.String here, which is
         * necessary because that is also used by the specialized implementations of IString,
         * namely FullUnicodeString and its subclasses,
         * which must implement together with this class the hashCode/equals contract.
         */
        public int hashCode() {
            return hashCode(0);
        }

        /**
         * This can be used to continue the computation of a string hashCode from left to right
         */
        protected final int hashCode(int prefixCode) {
            int h = prefixCode;
            OfInt it = iterator();

            while (it.hasNext()) {
                int c = it.nextInt();

                if (!Character.isBmpCodePoint(c)) {
                    h = 31 * h + Character.highSurrogate(c);
                    h = 31 * h + Character.lowSurrogate(c);
                } else {
                    h = 31 * h + ((char) c);
                }
            }

            return h;
        }

        abstract boolean hasNonBMPCodePoints();
    }

    private static class LazyConcatString extends AbstractString {
        private final AbstractString left; /* must remain final for immutability's sake */
        private final AbstractString right; /* must remain final for immutability's sake */
        private final int length;
        private final int depth;
        private final int lineCount;
        private final boolean terminated;
        private int hash = 0;

        public static IStringTreeNode build(AbstractString left, AbstractString right) {
            assert left.invariant();
            assert right.invariant();

            IStringTreeNode result = balance(left, right);

            assert result.invariant();
            assert result.left().invariant();
            assert result.right().invariant();

            return result;
        }

        @Override
        public IString concat(IString other) {
            if (other.length() == 0) {
                return this;
            }

            return LazyConcatString.build(this, (AbstractString) other);
        }

        @Override
        protected boolean hasNonBMPCodePoints() {
            return left.hasNonBMPCodePoints() || right.hasNonBMPCodePoints();
        }

        @Override
        public int hashCode() {
            if (hash == 0) {
                hash = right.hashCode(left.hashCode());
            }

            return hash;
        }

        @Override
        public boolean equals(@Nullable Object other) {
            return super.equals(other);
        }

        private static AbstractString balance(AbstractString left, AbstractString right) {
            AbstractString result = new LazyConcatString(left, right);

            while (result.balanceFactor() - 1 > MAX_UNBALANCE) {
                if (result.right().balanceFactor() < 0) {
                    result = result.rotateRightLeft();
                } else {
                    result = result.rotateLeft();
                }
            }

            while (result.balanceFactor() + 1 < -MAX_UNBALANCE) {
                if (result.left().balanceFactor() > 0) {
                    result = result.rotateLeftRight();
                } else {
                    result = result.rotateRight();
                }
            }

            return result;
        }

        private LazyConcatString(AbstractString left, AbstractString right) {
            this.left = left;
            this.right = right;
            this.length = left.length() + right.length();
            this.depth = Math.max(left.depth(), right.depth()) + 1;
            this.lineCount = IIndentableString.concatLineCount(left, right);
            this.terminated = right.isNewlineTerminated();

//          great but really expensive asserts. good for debugging, but not for testing
//          assert this.length() == newString(getValue()).length();
//          assert this.lineCount() == ((AbstractString) newString(getValue())).lineCount();
        }


        @Override
        public int lineCount() {
            return lineCount;
        }

        @Override
        public boolean isNewlineTerminated() {
            return terminated;
        }

        @Override
        public IString reverse() {
            return right.reverse().concat(left.reverse());
        }

        @Override
        public int length() {
            return length;
        }

        @Override
        public AbstractString left() {
            return left;
        }

        @Override
        public AbstractString right() {
            return right;
        }

        @Override
        public int balanceFactor() {
            return right().depth() - left().depth();
        }

        @Override
        public int depth() {
            return depth;
        }

        @Override
        public IString substring(int start, int end) {
            assert end >= start;

            if (end <= left.length()) {
                // left, right: <-------><------>
                // slice: <--->
                return left.substring(start, end);
            } else if (start >= left.length()) {
                // left, right: <-------><------>
                // slice: <--->
                return right.substring(start - left.length(), end - left.length());
            } else {
                // left, right: <-------><------>
                // slice: <------>
                return left.substring(start, left.length()).concat(right.substring(0, end - left.length()));
            }
        }

        @Override
        public int charAt(int index) {
            if (index < left.length()) {
                return left.charAt(index);
            } else {
                return right.charAt(index - left.length());
            }
        }

        @Override
        public IString replace(int first, int second, int end, IString repl) {
            if (end < left.length()) {
                // left, right: <-------><------>
                // slice: <--->
                return left.replace(first, second, end, repl).concat(right);
            } else if (first >= left.length()) {
                // left, right: <-------><------>
                // slice: <--->
                return left.concat(
                        right.replace(first - left.length(), second - left.length(), end - left.length(), repl));
            } else {
                // left, right: <-------><------>
                // slice: <------>
                // TODO: there is a corner case here at the end of left and the beginning of
                // right regarding `second`?
                return left.replace(first, second, left.length(), repl)
                        .concat(right.replace(0, second - left.length(), end - left.length(), repl));
            }
        }

        @Override
        public void write(Writer w) throws IOException {
            left.write(w);
            right.write(w);
        }

        @Override
        public void indentedWrite(Writer w, Deque<IString> whitespace, boolean indentFirstLine) throws IOException {
            left.indentedWrite(w, whitespace, indentFirstLine);
            right.indentedWrite(w, whitespace, left.isNewlineTerminated());
        }

        @Override
        public AbstractString rotateRight() {
            LazyConcatString p = new LazyConcatString(left().right(), right());
            p = (LazyConcatString) balance(p.left, p.right);
            return new LazyConcatString(left().left(), p);
        }

        @Override
        public AbstractString rotateLeft() {
            LazyConcatString p = new LazyConcatString(left(), right().left());
            return new LazyConcatString(balance(p.left, p.right), right().right());
        }

        @Override
        public AbstractString rotateRightLeft() {
            IStringTreeNode rotateRight = new LazyConcatString(left(), right().rotateRight());
            return rotateRight.rotateLeft();
        }

        @Override
        public AbstractString rotateLeftRight() {
            IStringTreeNode rotateLeft = new LazyConcatString(left().rotateLeft(), right());
            return rotateLeft.rotateRight();
        }

        @Override
        public OfInt iterator() {
            return new OfInt() {
                final Deque<AbstractString> todo = new ArrayDeque<>(depth);
                OfInt currentLeaf = leftmostLeafIterator(todo, LazyConcatString.this);

                @Override
                public boolean hasNext() {
                    return currentLeaf.hasNext(); /* || !todo.isEmpty() is unnecessary due to post-condition of nextInt() */
                }

                @Override
                public int nextInt() {
                    int next = currentLeaf.nextInt();

                    if (!currentLeaf.hasNext() && !todo.isEmpty()) {
                        // now we back track to the previous node we went left from,
                        // take the right branch and continue with its first leaf:
                        currentLeaf = leftmostLeafIterator(todo, todo.pop());
                    }

                    assert currentLeaf.hasNext() || todo.isEmpty();
                    return next;
                }
            };
        }

        /**
         * Static helper function for the iterator() method.
         *
         * It finds the left-most leaf of the tree, and collects
         * the path of nodes to this leaf as a side-effect in the todo
         * stack.
         */
        private static OfInt leftmostLeafIterator(Deque<AbstractString> todo, IStringTreeNode start) {
            IStringTreeNode cur = start;

            while (cur.depth() > 1) {
                todo.push(cur.right());
                cur = cur.left();
            }

            return cur.iterator();
        }
    }

    private static class IndentedString extends AbstractString {
        private final IString indent;
        private final AbstractString wrapped;
        private final boolean indentFirstLine;
        private volatile @MonotonicNonNull AbstractString flattened = null;

        IndentedString(AbstractString istring, IString whiteSpace, boolean indentFirstLine) {
            assert istring != null && whiteSpace != null;
            this.indent = whiteSpace;
            this.wrapped = istring;
            this.indentFirstLine = indentFirstLine;

//            great but really expensive asserts. good for debugging, but not for testing
//            assert this.lineCount() == ((AbstractString) newString(getValue())).lineCount();
//            assert this.length() == newString(getValue()).length();
//            assert indent.length() > 0;
//            assert flattened == null;
        }

        @Override
        protected boolean hasNonBMPCodePoints() {
            if (flattened != null) {
                return flattened.hasNonBMPCodePoints();
            }

            return wrapped.hasNonBMPCodePoints();
        }

        @Override
        public IString concat(IString other) {
            if (other.length() == 0) {
                return this;
            }

            if (flattened != null) {
                return LazyConcatString.build(flattened, (AbstractString) other);
            }

            if (other instanceof IndentedString) {
                IndentedString o = (IndentedString) other;

                if (o.indent != null && o.wrapped != null && o.indent.equals(this.indent)) {
                    // we factor out the duplicate identical indentation which has two effects:
                    // (a) fewer indentation nodes and (b) longer indentation nodes because we
                    // generate directly nested indentation which is flattened/concatenated (see this.indent)
                    return LazyConcatString.build(this.wrapped, o.wrapped).indent(this.indent, indentFirstLine);
                }
            }

            return LazyConcatString.build(this, (AbstractString) other);
        }

        @Override
        public IString indent(IString indent, boolean indentFirstLine) {
            assert !indent.getValue().contains("\n") && !indent.getValue().contains("\r");

            if (indent.length() == 0) {
                return this;
            }

            if (flattened != null) {
                return new IndentedString(flattened, indent, indentFirstLine);
            }

            return new IndentedString(wrapped, indent.concat(this.indent), indentFirstLine);
        }


        /**
         * This is the basic implementation of indentation, while iterating
         * over the string we insert the indent before every non-empty line.
         */
        @Override
        public OfInt iterator() {
            if (flattened != null) {
                return flattened.iterator();
            }

            return new OfInt() {
                final OfInt content = wrapped.iterator();
                OfInt nextIndentation = indentFirstLine ? indent.iterator() : EmptyString.getInstance().iterator();

                @Override
                public boolean hasNext() {
                    return content.hasNext();
                    // || !nextIndentation.isEmpty() is not needed because if we are out of content we should not indent anyway
                    // so this makes sure that after a terminating \n no superfluous whitespace is printed before EOF.
                }

                @Override
                public int nextInt() {
                    // if it is time to print whitespace, we exhaust this first
                    if (nextIndentation.hasNext()) {
                        return nextIndentation.nextInt();
                    }

                    // done with indenting, so continue with the content
                    int cur = content.nextInt();

                    // detect if we have to start indenting
                    if (cur == NEWLINE) {
                        nextIndentation = indent.iterator();
                    }

                    return cur;
                }
            };
        }

        @Override
        public IString reverse() {
            return applyIndentation().reverse();
        }

        /**
         * When the indented string is used in an non-optimal way, say
         * by calling substring, charAt and replace, on it then we flatten it
         * to a normal string by applying the indent, and store the
         * eagerly indented string in the volatile `flattened` field.
         */
        private IString applyIndentation() {
            if (flattened == null) {
                flattened = (AbstractString) newString(getValue());
            }

            return flattened;
        }

        @Override
        public IString substring(int start, int end) {
            return applyIndentation().substring(start,end);
        }

        @Override
        public int charAt(int index) {
            return applyIndentation().charAt(index);
        }

        @Override
        public IString replace(int first, int second, int end, IString repl) {
            return applyIndentation().replace(first, second, end, repl);
        }

        @Override
        public void write(Writer w) throws IOException {
            if (flattened != null){
                flattened.write(w);
                return;
            }

            Deque<IString> indents = new ArrayDeque<>(10);
            indents.push(indent);
            wrapped.indentedWrite(w, indents, indentFirstLine);
            indents.pop();
            assert indents.isEmpty();
        }

        @Override
        public void indentedWrite(Writer w, Deque<IString> whitespace, boolean indentFirstLine) throws IOException {
            if (flattened != null) {
                flattened.indentedWrite(w, whitespace, indentFirstLine);
                return;
            }

            // TODO: this concat on whitespace is worrying for longer files which consist of about 40% of indentation.
            // for those files this concat is particular quadratic since the strings are short and they
            // are copied over and over again.
            whitespace.push(indent);
            wrapped.indentedWrite(w, whitespace, indentFirstLine);
            whitespace.pop();
        }

        @Override
        public int length() {
            if (flattened != null) {
                return flattened.length();
            }

            // for every non-empty line an indent would be added to the total number of characters
            return wrapped.length() + (wrapped.lineCount() - (indentFirstLine?0:1)) * indent.length();
        }

        @Override
        public int lineCount() {
            if (flattened != null) {
                return flattened.lineCount();
            }

            return wrapped.lineCount();
        }

        @Override
        public boolean isNewlineTerminated() {
            if (flattened != null) {
                return flattened.isNewlineTerminated();
            }

            return wrapped.isNewlineTerminated();
        }
    }

    /**
     * This embedded class is used to fine tune parameters of the string indentation feature.
     *
     * NB! make sure to run with asserts disabled.
     */
    public static class IndentationBenchmarkTool {
        private static IValueFactory vf = ValueFactory.getInstance();
        private static ThreadMXBean bean = ManagementFactory.getThreadMXBean( );

        /**
         * Comparing between lazy and eager implementations of indent, we did not observe
         * a significant effect on the differential diagnostics while testing with real
         * files as opposed to this (faster) null writer.
         */
        private static class NullWriter extends Writer {
            @Override
            public void write(char[] cbuf, int off, int len) throws IOException { }


            @Override
            public void write(String str) throws IOException { }

            @Override
            public void write(String str, int off, int len) throws IOException { }

            @Override
            public void write(char[] cbuf) throws IOException {  }

            @Override
            public void write(int c) throws IOException { }

            @Override
            public void flush() throws IOException { }

            @Override
            public void close() throws IOException { }
        }

        public static void main(String[] args) throws IOException {

            long lazyTime = 0L, eagerTime = 0L;
            boolean runLazy = true, runEager =true;



            warmup();

            if (runLazy) {
                System.err.println("Benchmarking lazy implementation... press enter");
                System.in.read();
                long start = bean.getCurrentThreadCpuTime();
                IString value = buildLargeLazyValue();

                long afterBuild = bean.getCurrentThreadCpuTime();
                System.err.println("LAZY BUILD        :" + Duration.of(afterBuild - start, ChronoUnit.NANOS).toString());


                for (int i = 0; i < 100; i++) {
                    value.write(new NullWriter());
                }

                long afterWrite = bean.getCurrentThreadCpuTime();

                lazyTime = afterWrite - start;

                System.err.println("LAZY         WRITE:" + Duration.of(afterWrite - afterBuild, ChronoUnit.NANOS).toString());
                System.err.println("LAZY BUILD + WRITE:" + Duration.of(afterWrite - start, ChronoUnit.NANOS).toString());
                System.err.println("STRING LENGTH      :" + value.length());

                value = null;
                System.gc();
            }

            if (runEager) {
                System.err.println("Benchmarking eager implementation... press enter");
                System.in.read();

                long start = bean.getCurrentThreadCpuTime();
                IString value = buildLargeEagerValue();

                long afterBuild = bean.getCurrentThreadCpuTime();
                System.err.println("EAGER BUILD        :" + Duration.of(afterBuild - start, ChronoUnit.NANOS).toString());

                for (int i = 0; i < 100; i++) {
                    value.write(new NullWriter());
                }

                long afterWrite = bean.getCurrentThreadCpuTime();

                eagerTime = afterWrite - start;
                System.err.println("EAGER         WRITE:" + Duration.of(afterWrite - afterBuild, ChronoUnit.NANOS).toString());
                System.err.println("EAGER BUILD + WRITE:" + Duration.of(eagerTime, ChronoUnit.NANOS).toString());
                System.err.println("STRING LENGTH      :" + value.length());
                value = null;
                System.gc();
            }

            if (runEager && runLazy) {
                System.err.println("LAZY is " + Math.round((lazyTime * 1.0) / (eagerTime * 1.0) * 100) + "% of EAGER\n\n");
            }
        }

        private static void warmup() throws IOException {
            for (int i = 0; i < 5; i++) {
                buildLargeLazyValue().write(new NullWriter());
            }
            System.gc();
        }

        private static IString buildLargeEagerValue() {
            IString ws = vf.string("    ");
            IString base = vf.string("if ( .. ) then if while bla bla bla aap noot mies x { ... } \n");
            IString result = vf.string("");

            for (int l = 0; l < 5; l++) {
                IString outer2 = vf.string("");

                for (int k = 0; k < 100; k++) {
                    IString outer = vf.string("");

                    for (int j = 0; j < 100; j++) {
                        IString block = base;

                        for (int i = 0; i < 100; i++) {
                            block = block.concat(newString(base.indent(ws, true).getValue()));
                        }

                        outer = outer.concat(newString(block.indent(ws, true).getValue()));
                    }

                    outer2 = outer2.concat(newString(outer.indent(ws, true).getValue()));
                }

                result = result.concat(newString(outer2.indent(ws, true).getValue()));
            }

            return result;
        }

        /**
         * Generates 388650000 characters, which is around 400Mb when written to disk in UTF8, and 800Mb in memory.
         */
        private static IString buildLargeLazyValue() {
            IString ws = vf.string("    ");
            IString base = vf.string("if ( .. ) then if while bla bla bla aap noot mies x { ... } \n");
            IString result = vf.string("");

            for (int l = 0; l < 5; l++) {
                IString outer2 = vf.string("");
                for (int k = 0; k < 100; k++) {
                    IString outer = vf.string("");

                    for (int j = 0; j < 100; j++) {
                        IString block = base;

                        for (int i = 0; i < 100; i++) {
                            block = block.concat(base.indent(ws, true));
                        }

                        outer = outer.concat(block.indent(ws, true));
                    }

                    outer2 = outer2.concat(outer.indent(ws, true));
                }

                result = result.concat(outer2.indent(ws, true));
            }

            return result;
        }
    }
}
