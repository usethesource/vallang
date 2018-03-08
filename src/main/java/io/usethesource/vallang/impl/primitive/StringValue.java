/*******************************************************************************
 * Copyright (c) 2009-2017 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Arnold Lankamp - interfaces and implementation - CWI
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *   * Jurgen Vinju - lazy concat - CWI
 *   * Bert Lisser - balanced trees - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.primitive;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import io.usethesource.vallang.IAnnotatable;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IWithKeywordParameters;
import io.usethesource.vallang.impl.AbstractValue;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.visitors.IValueVisitor;

/**
 * Implementations of IString.
 */
/* package */ public class StringValue {
	private final static Type STRING_TYPE = TypeFactory.getInstance().stringType();

	private static int DEFAULT_MAX_FLAT_STRING = 512;
	private static int MAX_FLAT_STRING = DEFAULT_MAX_FLAT_STRING;

	private static int DEFAULT_MAX_UNBALANCE = 0;
	private static int MAX_UNBALANCE = DEFAULT_MAX_UNBALANCE;

	/** for testing purposes we can set the max flat string value */
	static synchronized public void setMaxFlatString(int maxFlatString) {
		MAX_FLAT_STRING = maxFlatString;
	}

	/** for testing purposes we can set the max flat string value */
	static synchronized public void resetMaxFlatString() {
		MAX_FLAT_STRING = DEFAULT_MAX_FLAT_STRING;
	}

	/** for testing and tuning purposes we can set the max unbalance factor */
	static synchronized public void setMaxUnbalance(int maxUnbalance) {
		MAX_UNBALANCE = maxUnbalance;
	}

	static synchronized public void resetMaxUnbalance() {
		MAX_UNBALANCE = DEFAULT_MAX_UNBALANCE;
	}

	public static IString newString(String value) {
		if (value == null) {
			value = "";
		}

		return newString(value, containsSurrogatePairs(value));
	}

	/* package */ static IString newString(String value, boolean fullUnicode) {
		if (value == null) {
			value = "";
		}

		if (fullUnicode) {
			return new FullUnicodeString(value);
		}

		return new SimpleUnicodeString(value);
	}

	private static boolean containsSurrogatePairs(String str) {
		if (str == null) {
			return false;
		}
		int len = str.length();
		for (int i = 1; i < len; i++) {
			if (Character.isSurrogatePair(str.charAt(i - 1), str.charAt(i))) {
				return true;
			}
		}
		return false;
	}

	private static class FullUnicodeString extends AbstractValue implements IStringTreeNode {
		protected final String value;

		private FullUnicodeString(String value) {
			super();

			this.value = value;
		}

		@Override
		public int depth() {
			return 1;
		}

		@Override
		public Type getType() {
			return STRING_TYPE;
		}

		@Override
		public String getValue() {
			return value;
		}
		
		@Override
		public String getCompactValue() {
			return value;
		}

		@Override
		public String indentedGetValue(IString whiteSpace) {
			String string = getValue();
			StringBuffer stringBuffer = new StringBuffer(10000);
			for (Character c : string.toCharArray()) {
				stringBuffer.append(c);
				if (c == '\n') {
					stringBuffer.append(whiteSpace.getValue());
				}
			}
			return stringBuffer.toString();
		}

		@Override
		public IString concat(IString other) {
			// if (other instanceof IndentedString) return new BinaryBalancedLazyConcatString(this, (IStringTreeNode) other);
			if (length() + other.length() <= MAX_FLAT_STRING)
					 {
				StringBuilder buffer = new StringBuilder();
				buffer.append(value);
				buffer.append(other.getValue());

				return StringValue.newString(buffer.toString(), true);
			} else {
				return BinaryBalancedLazyConcatString.build(this, (IStringTreeNode) other);
			}
		}

		@Override
		public int compare(IString other) {
			int result = value.compareTo(other.getValue());

			if (result > 0) {
				return 1;
			}
			if (result < 0) {
				return -1;
			}

			return 0;
		}

		@Override
		public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
			return v.visitString(this);
		}

		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (this == o) {
				return true;
			}

			if (o instanceof FullUnicodeString) {
				FullUnicodeString otherString = (FullUnicodeString) o;
				return value.equals(otherString.value);
			}
			if (o.getClass() == BinaryBalancedLazyConcatString.class || o.getClass() == IndentedString.class) {
				return o.equals(this);
			}

			return false;
		}

		@Override
		/**
		 * Note that this algorithm can not be changed, unless you also have change
		 * BinaryBalancedTreeNode.hashCode() (to not break the hashCode/equals
		 * contract).
		 */
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public boolean isEqual(IValue value) {
			return equals(value);
		}

		@Override
		public IString reverse() {
			StringBuilder b = new StringBuilder(value);
			return newString(b.reverse().toString(), true);
		}

		@Override
		public int length() {
			return value.codePointCount(0, value.length());
		}

		@Override
		public int indentedLength(IString whiteSpace) {
			String string = getValue();
			int numberOfNewlines = 0;
			int pos = string.indexOf('\n', 0);
			while (pos >= 0) {
				pos = string.indexOf('\n', pos + 1);
				numberOfNewlines++;
			}
			return this.length() + numberOfNewlines * whiteSpace.length();
		}

		private int codePointAt(java.lang.String str, int i) {
			return str.codePointAt(str.offsetByCodePoints(0, i));
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
			return codePointAt(value, index);
		}
		
		static int codePointIndexOf(String s, int codePoint, int from) {
		    int n = 0;
		    for (int i = 0; i < s.length(); ) {
		        int cp = s.codePointAt(i);
		        if (cp == codePoint && n>=from) {
		            return n;
		        }
		        i += Character.charCount(cp);
		        ++n;
		    }
		    return -1;
		}

		@Override
		public int indentedCharAt(int index, IString whiteSpace) {
			String string = this.getValue();
			int startIndex = codePointIndexOf(string, '\n', 0);
			if (index < startIndex)
				return this.charAt(index);
			int offset = 0;
			int posLastNewline = -1;
			while (startIndex >= 0 && (startIndex + offset) < index) {
				posLastNewline = startIndex;
				startIndex = codePointIndexOf(string, '\n', startIndex + 1);
				offset += whiteSpace.length();
			}
			offset -= whiteSpace.length();
			if ((posLastNewline + offset) == index)
				return '\n';
			int index0 = index - offset;
			if (index0 - (posLastNewline + 1) < whiteSpace.length())
				return whiteSpace.charAt(index0 - (posLastNewline + 1));
			return this.charAt(index0 - whiteSpace.length());
		}

		private int nextCP(CharBuffer cbuf) {
			int cp = Character.codePointAt(cbuf, 0);
			if (cbuf.position() < cbuf.capacity()) {
				cbuf.position(cbuf.position() + Character.charCount(cp));
			}
			return cp;
		}

		private void skipCP(CharBuffer cbuf) {
			if (cbuf.hasRemaining()) {
				int cp = Character.codePointAt(cbuf, 0);
				cbuf.position(cbuf.position() + Character.charCount(cp));
			}
		}

		@Override
		public IString replace(int first, int second, int end, IString repl) {
			StringBuilder buffer = new StringBuilder();

			int valueLen = value.codePointCount(0, value.length());
			CharBuffer valueBuf;

			int replLen = repl.length();
			CharBuffer replBuf = CharBuffer.wrap(repl.getValue());

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
					buffer.appendCodePoint(nextCP(replBuf));
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
						buffer.appendCodePoint(nextCP(replBuf));
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
					buffer.appendCodePoint(nextCP(replBuf));
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
						buffer.appendCodePoint(nextCP(replBuf));
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
		public void indentedWrite(Writer w, IString whitespace) throws IOException {
			w.write(indentedGetValue(whitespace));
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				private int cur = 0;

				public boolean hasNext() {
					return cur < length();
				}

				public Integer next() {
					return charAt(cur++);
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public IString indent(IString whiteSpace) {
			return new IndentedString(this, whiteSpace);

		}
	}

	private static class SimpleUnicodeString extends FullUnicodeString {

		public SimpleUnicodeString(String value) {
			super(value);
		}

		@Override
		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (this == o)
				return true;
			if (o instanceof FullUnicodeString) {
				FullUnicodeString otherString = (FullUnicodeString) o;
				return value.equals(otherString.value);
			}
			if (o.getClass() == BinaryBalancedLazyConcatString.class || o.getClass() == IndentedString.class) {
				return o.equals(this);
			}

			return false;
		}

		// Common operations which do not need to be slow
		@Override
		public int length() {
			return value.length();
		}

		@Override
		public int charAt(int index) {
			return value.charAt(index);
		}
		
		@Override
		public int indentedCharAt(int index, IString whiteSpace) {
			String string = this.getValue();
			int startIndex = string.indexOf('\n', 0);
			if (index < startIndex)
				return this.charAt(index);
			int offset = 0;
			int posLastNewline = -1;
			while (startIndex >= 0 && (startIndex + offset) < index) {
				posLastNewline = startIndex;
				startIndex = string.indexOf('\n', startIndex + 1);
				offset += whiteSpace.length();
			}
			offset -= whiteSpace.length();
			if ((posLastNewline + offset) == index)
				return '\n';
			int index0 = index - offset;
			if (index0 - (posLastNewline + 1) < whiteSpace.length())
				return whiteSpace.charAt(index0 - (posLastNewline + 1));
			return this.charAt(index0 - whiteSpace.length());
		}

		@Override
		public IString substring(int start) {
			return newString(value.substring(start), false);
		}

		@Override
		public IString substring(int start, int end) {
			return newString(value.substring(start, end), false);
		}

		@Override
		public IString reverse() {
			return newString(new StringBuilder(value).reverse().toString(), false);
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				private int cur = 0;

				public boolean hasNext() {
					return cur < value.length();
				}

				public Integer next() {
					return (int) value.charAt(cur++);
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}
	}

	/**
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
		default IStringTreeNode left() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Should be overridden by the binary node and never called on the leaf nodes
		 * because they are not out of balance.
		 */
		default IStringTreeNode right() {
			throw new UnsupportedOperationException();
		}

		default IStringTreeNode rotateRight() {
			return this;
		}

		default IStringTreeNode rotateLeft() {
			return this;
		}

		default IStringTreeNode rotateRightLeft() {
			return this;
		}

		default IStringTreeNode rotateLeftRight() {
			return this;
		}

		default void collectLeafIterators(List<Iterator<Integer>> w) {
			// System.out.println("collectLeafIterators:"+this.getClass());
			w.add(iterator());
		}

		default int indentedCharAt(int index, IString whiteSpace) {
			throw new UnsupportedOperationException();
		}

		default public int indentedLength(IString whiteSpace) {
			throw new UnsupportedOperationException();
		}

		default public void indentedWrite(Writer w, IString whiteSpace) throws IOException {
			throw new UnsupportedOperationException();
		}

		default public String indentedGetValue(IString whiteSpace) {
			throw new UnsupportedOperationException();
		}
		
	}

	private static class BinaryBalancedLazyConcatString extends AbstractValue implements IStringTreeNode {
		private final IStringTreeNode left; /* must remain final for immutability's sake */
		private final IStringTreeNode right; /* must remain final for immutability's sake */
		private final int length;
		int ilength = -1;
		private final int depth;
		private int hash = 0;

		public static IStringTreeNode build(IStringTreeNode left, IStringTreeNode right) {
			assert left.invariant();
			assert right.invariant();

			IStringTreeNode result = balance(left, right);

			assert result.invariant();
			assert result.left().invariant();
			assert result.right().invariant();

			return result;
		}

		private static IStringTreeNode balance(IStringTreeNode left, IStringTreeNode right) {
			IStringTreeNode result = new BinaryBalancedLazyConcatString(left, right);

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

		private BinaryBalancedLazyConcatString(IStringTreeNode left, IStringTreeNode right) {
			this.left = left;
			this.right = right;
			this.length = left.length() + right.length();
			this.depth = Math.max(left.depth(), right.depth()) + 1;

			// Integer[] newline2pos = {};
		}

		@Override
		public Type getType() {
			return STRING_TYPE;
		}

		@Override
		public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
			return v.visitString(this);
		}

		@Override
		public boolean isEqual(IValue value) {
			return this.equals(value);
		}

		@Override
		public boolean equals(Object other) {
			if (!(other instanceof IStringTreeNode)) {
				return false;
			}

			if (other == this) {
				return true;
			}

			IStringTreeNode o = (IStringTreeNode) other;

			if (length() != o.length()) {
				return false;
			}
			return this.compare(o) == 0;
		}

		@Override
		public boolean match(IValue other) {
			return isEqual(other);
		}

		@Override
		public boolean isAnnotatable() {
			return false;
		}

		@Override
		public IAnnotatable<? extends IValue> asAnnotatable() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean mayHaveKeywordParameters() {
			return false;
		}

		@Override
		public IWithKeywordParameters<? extends IValue> asWithKeywordParameters() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getValue() {
			try (StringWriter w = new StringWriter((int) (length * 1.5 /* leave some room for surrogate pairs */))) {
				write(w);
				return w.toString();
			} catch (IOException e) {
				// this will not happen with a StringWriter
				return "";
			}
		}
		
		@Override
		public String getCompactValue() {
			return getValue();
		}

		@Override
		public String indentedGetValue(IString whitespace) {
			try (StringWriter w = new StringWriter((int) (length * 1.5 /* leave some room for surrogate pairs */))) {
				indentedWrite(w, whitespace);
				return w.toString();
			} catch (IOException e) {
				// this will not happen with a StringWriter
				return "";
			}
		}

		@Override
		public IString concat(IString other) {
			return BinaryBalancedLazyConcatString.build(this, (IStringTreeNode) other);
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
		public int indentedLength(IString whitespace) {
			if (ilength < 0)
				ilength = left.indentedLength(whitespace) + right.indentedLength(whitespace);
			return ilength;
		}

		@Override
		public IStringTreeNode left() {
			return left;
		}

		@Override
		public IStringTreeNode right() {
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

			if (end <= left.length()) { // Bert
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
		public IString substring(int start) {
			return substring(start, length);
		}

		@Override
		public int compare(IString other) {
			IStringTreeNode o = (IStringTreeNode) other;
			// System.out.println("Compare:"+this.getClass()+" "+other.getClass());
			Iterator<Integer> it1 = this.iterator();
			Iterator<Integer> it2 = o.iterator();

			while (it1.hasNext() && it2.hasNext()) {
				Integer c1 = it1.next();
				Integer c2 = it2.next();

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
		// TODO: this needs a unit test
		public int charAt(int index) {
			if (index < left.length()) {
				return left.charAt(index);
			} else {
				// System.out.println("index="+index+" "+" length="+ left.length());
				return right.charAt(index - left.length());
			}
		}

		@Override
		// TODO: this needs a unit test
		public int indentedCharAt(int index, IString whitespace) {
			if (index < left.indentedLength(whitespace)) {
				return left.indentedCharAt(index, whitespace);
			} else {
				// System.out.println("right: index="+index+" "+" length="+ left.length());
				return right.indentedCharAt(index - left.indentedLength(whitespace), whitespace);
			}
		}

		@Override
		// TODO this needs a unit test
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
			// System.out.println("write:"+left.getClass()+" "+right.getClass());
			left.write(w);
			right.write(w);
		}

		@Override
		public void indentedWrite(Writer w, IString whitespace) throws IOException {
			// System.out.println("write:"+left.getClass()+" "+right.getClass());
			left.indentedWrite(w, whitespace);
			right.indentedWrite(w, whitespace);
		}

		@Override
		public IStringTreeNode rotateRight() {
			BinaryBalancedLazyConcatString p = new BinaryBalancedLazyConcatString(left().right(), right());
			p = (BinaryBalancedLazyConcatString) balance(p.left, p.right);
			return new BinaryBalancedLazyConcatString(left().left(), p);
		}

		@Override
		public IStringTreeNode rotateLeft() {
			BinaryBalancedLazyConcatString p = new BinaryBalancedLazyConcatString(left(), right().left());
			p = (BinaryBalancedLazyConcatString) balance(p.left, p.right);
			return new BinaryBalancedLazyConcatString(p, right().right());
		}

		@Override
		public IStringTreeNode rotateRightLeft() {
			IStringTreeNode rotateRight = new BinaryBalancedLazyConcatString(left(), right().rotateRight());
			return rotateRight.rotateLeft();
		}

		@Override
		public IStringTreeNode rotateLeftRight() {
			IStringTreeNode rotateLeft = new BinaryBalancedLazyConcatString(left().rotateLeft(), right());
			return rotateLeft.rotateRight();
		}

		@Override
		/**
		 * Note that we used the hashcode algorithm for java.lang.String here, which is
		 * necessary because that is also used by the other implementations of IString
		 * which must implement together with this class the hashCode/equals contract.
		 */
		public int hashCode() {
			int h = hash;
			if (h == 0) {
				for (Integer c : this) {
					if (!Character.isBmpCodePoint(c)) {
						h = 31 * h + Character.highSurrogate(c);
						h = 31 * h + Character.lowSurrogate(c);
					} else {
						h = 31 * h + c;
					}
				}

				hash = h;
			}

			return h;
		}

		@Override
		public Iterator<Integer> iterator() {
			final List<Iterator<Integer>> leafs = new ArrayList<>(
					this.length / (StringValue.DEFAULT_MAX_FLAT_STRING / 2));

			/**
			 * Because the trees can be quite unbalanced and therefore very deep, allocating
			 * an iterator for every depth becomes quite expensive. We collect here the
			 * necessary iterators of the leaf nodes only.
			 */
			collectLeafIterators(leafs);

			return new Iterator<Integer>() {
				int current = 0;

				@Override
				public boolean hasNext() {
					while (current < leafs.size() && !leafs.get(current).hasNext()) {
						current++;
					}

					return current < leafs.size();
				}

				@Override
				public Integer next() {
					return leafs.get(current).next();
				}
			};
		};

		@Override
		public void collectLeafIterators(List<Iterator<Integer>> w) {
			left.collectLeafIterators(w);
			right.collectLeafIterators(w);
			;
		}

		@Override
		public IString indent(IString whiteSpace) {
			return new IndentedString(this, whiteSpace);
		}
	}

	private static class IndentedString extends AbstractValue implements IString, IStringTreeNode {

		final private IString whiteSpace;
		final private IString istring;
		int length = -1;

		IndentedString(IString istring, IString whiteSpace) {
			this.whiteSpace = whiteSpace;
			this.istring = istring;
		}

		public IString indent(IString whiteSpace) {
			return new IndentedString(this.istring, this.whiteSpace.concat(whiteSpace));
		}


		@Override
		public String getValue() {
			return indentedGetValue(whiteSpace);
			// return istring.getValue();
		}
		
		@Override
		public String getCompactValue() {
			return istring.getValue();
		}

		@Override
		public IString concat(IString other) {
			return BinaryBalancedLazyConcatString.build(this, (IStringTreeNode) other);
		}
        
		@Override
		public String indentedGetValue(IString whitespace) {
			return ((IStringTreeNode) istring).indentedGetValue(concatWhitespace(whitespace, istring));
		}

		@Override
		public IString reverse() {
			String s = getValue();
			return newString(s).reverse();
		}

		IString expand() {
			return newString(getValue());
		}

		@Override
		public IString substring(int start, int end) {
			String value = this.getValue();
			return newString(value.substring(value.offsetByCodePoints(0, start), value.offsetByCodePoints(0, end)));
		}

		@Override
		public IString substring(int start) {
			String value = this.getValue();
			return newString(value.substring(value.offsetByCodePoints(0, start)));
		}

		@Override
		public int compare(IString other) {
			//System.out.println("Compare:"+this.getClass()+" "+other.getClass()+
			// "|"+expand());
			int result = expand().compare(other);
			if (result == 0) {
				return 0;
			} else if (result < 0) {
				return -1;
			} else { // result > 0
				return 1;
			}
		}

		@Override
		public int charAt(int index) {
			return ((IStringTreeNode) this.istring).indentedCharAt(index, this.whiteSpace);
		}
		
		@Override
		public int indentedCharAt(int index, IString whitespace) {
			return ((IStringTreeNode) istring).indentedCharAt(index, concatWhitespace(whitespace, istring));
		}

		@Override
		public IString replace(int first, int second, int end, IString repl) {
			if (repl instanceof IndentedString)
				repl = ((IndentedString) repl).expand();
			IString result = this.expand();
			return result.replace(first, second, end, repl);
		};

		@Override
		public void write(Writer w) throws IOException {
			indentedWrite(w, whiteSpace);
		}

		@Override
		public void indentedWrite(Writer w, IString whitespace) throws IOException {
			((IStringTreeNode) istring).indentedWrite(w, concatWhitespace(whitespace, istring));
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				private int cur = 0;

				public boolean hasNext() {
					return cur < length();
				}

				public Integer next() {
					return charAt(cur++);
				}

				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public boolean equals(Object other) {
			if (other == this) {
				return true;
			}
			if (!(other instanceof IString))
				return false;
			return this.compare((IString) other) == 0;
		}

		@Override
		public boolean isEqual(IValue value) {
			return this.equals(value);
		}

		@Override
		public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
			return v.visitString(this);
		}

		IString concatWhitespace(IString whiteSpace, IString other) {
			if (other instanceof IndentedString)
				return whiteSpace.concat(((IndentedString) other).whiteSpace);
			return whiteSpace;
		}

		@Override
		public int indentedLength(IString whitespace) {
			// System.out.println("IndentedLength:"+istring.getClass()+"
			// "+istring.getValue());
			return ((IStringTreeNode) istring).indentedLength(concatWhitespace(whitespace, istring));
		}

		@Override
		public int length() {
			// TODO Auto-generated method stub
			if (length >= 0)
				return length;
			length = indentedLength(this.whiteSpace);
			return length;
		}
	}
}
