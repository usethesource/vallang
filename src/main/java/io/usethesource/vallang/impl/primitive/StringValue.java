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
 *******************************************************************************/
package io.usethesource.vallang.impl.primitive;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collections;
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
 * Implementation of IString.
 */
/* package */ public class StringValue {
	private final static Type STRING_TYPE = TypeFactory.getInstance().stringType();
	private static int MAX_FLAT_STRING = 512;
	
	private static int balance = -1;
	
	/*
	 * balance == -1 : Unbalanced tree
	 * balance == 0  : Fully balanced tree
	 * balance == n  : Balancing tree occurs when balanceFactor is greater equal to n
	 */
	static public void setBalance(int balance) {
		StringValue.balance = balance;
	}
	
	static public void  setMaxFlatString(int maxFlatString) {
		MAX_FLAT_STRING = maxFlatString;
	}

	/* package */ static IString newString(String value) {
		if (value == null) {
			value = "";
		}

		return newString(value, containsSurrogatePairs(value));
	}
	/*
	 * static IString newTreeString(String value) { if
	 * (value.length()<MAX_FLAT_STRING) return newString(value); IString root =
	 * newString(value.substring(0, MAX_FLAT_STRING));
	 * value=value.substring(MAX_FLAT_STRING); while(!value.isEmpty()) { String s =
	 * value.substring(0, MAX_FLAT_STRING); value=value.substring(MAX_FLAT_STRING);
	 * root.lazyConcat(newString(s)); } return root; }
	 */

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

	private static class FullUnicodeString extends AbstractValue implements IString, IStringTreeNode {
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
		public IString concat(IString other) {
			if (length() + other.length() <= MAX_FLAT_STRING) {
				StringBuilder buffer = new StringBuilder();
				buffer.append(value);
				buffer.append(other.getValue());

				return StringValue.newString(buffer.toString(), true);
			} else {
				return lazyConcat((IStringTreeNode) other);
			}
		}

		@Override
		public int compare(IString other) {
			int result = value.compareTo(other.getValue());

			if (result > 0)
				return 1;
			if (result < 0)
				return -1;

			return 0;
		}

		@Override
		public <T, E extends Throwable> T accept(IValueVisitor<T, E> v) throws E {
			return v.visitString(this);
		}

		public int hashCode() {
			return value.hashCode();
		}

		public boolean equals(Object o) {
			if (o == null)
				return false;
			if (this == o)
				return true;
			if (o.getClass() == getClass()) {
				FullUnicodeString otherString = (FullUnicodeString) o;
				return value.equals(otherString.value);
			}

			return false;
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
		public int balanceFactor() {
			return 1;
		}

		@Override
		public IString balance() {
			// TODO Auto-generated method stub
			return this;
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
			if (o.getClass() == getClass()) {
				SimpleUnicodeString otherString = (SimpleUnicodeString) o;
				return value.equals(otherString.value);
			}

			return false;
		}

		// Common operations which do not need to be slow
		@Override
		public int length() {
			return value.length();
		}
		
		@Override
		public int depth() {
			return 1;
		}

		@Override
		public int charAt(int index) {
			return value.charAt(index);
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
		public IString concat(IString other) {
			if (length() + other.length() <= MAX_FLAT_STRING) {
				StringBuilder buffer = new StringBuilder();
				buffer.append(value);
				buffer.append(other.getValue());

				return StringValue.newString(buffer.toString(), other.getClass() != getClass());
			} else {
				return lazyConcat((IStringTreeNode) other); // IString lazyConcat?
			}
		}
	}

	private static interface IStringTreeNode extends IString {
		int depth();
		default IStringTreeNode lazyConcat(IStringTreeNode other) {
			// System.out.println("lazyConcat");
		   BinaryBalancedLazyConcatString result = new BinaryBalancedLazyConcatString(this, (IStringTreeNode) other);
		   if (StringValue.balance==0) result = BinaryBalancedLazyConcatString.balance(result);
		   else
		   if (StringValue.balance>0 && Math.abs(result.balanceFactor())>=StringValue.balance) result = BinaryBalancedLazyConcatString.balance(result);
		   return result;
		}

		default IStringTreeNode rotateLeft() {
			// TODO; this is just an idea to start implementig an AVL tree
			// should be overriden in BinaryBalancedLazyConcatString
			
			return this;
		}

		default IStringTreeNode rotateRight() {
			// TODO; this is just an idea to start implementig an AVL tree
			// should be overriden in BinaryBalancedLazyConcatString
			return this;
		}
		
		default IStringTreeNode rotateLeftRight() {
			// TODO; this is just an idea to start implementig an AVL tree
			// should be overriden in BinaryBalancedLazyConcatString
			
			return this;
		}

		default IStringTreeNode rotateRightLeft() {
			// TODO; this is just an idea to start implementig an AVL tree
			// should be overriden in BinaryBalancedLazyConcatString
			return this;
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


	private static class BinaryBalancedLazyConcatString extends AbstractValue implements IStringTreeNode {
		private final IStringTreeNode left; /* must remain final for immutability's sake */
		private final IStringTreeNode right; /* must remain final for immutability's sake */
		private final int length;
		private final int depth;

		public BinaryBalancedLazyConcatString(IStringTreeNode left, IStringTreeNode right) {
			this.left = left;
			this.right = right;
			this.length = left.length() + right.length();
			this.depth = Math.max(left.depth(), right.depth())+1;
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
		public boolean isEqual(IValue other) {
			if (!(other instanceof IString)) {
				return false;
			}

			if (other == this) {
				return true;
			}

			IString o = (IString) other;
			if (length() != o.length()) {
				return false;
			}
			IString[][] r = BinaryBalancedLazyConcatString.refine(BinaryBalancedLazyConcatString.flatten(this), BinaryBalancedLazyConcatString.flatten(o));
			if (r[0].length!=r[1].length) return false;
			int i = 0;
			while (i<r[0].length && r[0][i].isEqual(r[1][i])) i++;
			return i==r[0].length;
		}

		static boolean isLeaf(IString a) {
			return !(a instanceof BinaryBalancedLazyConcatString);
		}

		static IString[] concat(IString[] first, IString[] second) {
			List<IString> both = new ArrayList<IString>(first.length + second.length);
			Collections.addAll(both, first);
			Collections.addAll(both, second);
			return both.toArray(new IString[both.size()]);
		}

		static IString[] flatten(IString a) {
			IString[] result;
			if (!isLeaf(a)) {
				result = concat(flatten(((BinaryBalancedLazyConcatString) a).left),
						flatten(((BinaryBalancedLazyConcatString) a).right));
				return result;
			}
			return new IString[] { a };

		}

		static IString[][] refine(IString[] a, IString[] b) {
			int pa = 0, pb = 0;
			int ia = 0, ib = 0;
			ArrayList<IString> ra = new ArrayList<IString>(), rb = new ArrayList<IString>();
			while (ia < a.length || ib < b.length) {
				final int len = Math.min((ia < a.length ? (a[ia].length() - pa) : Integer.MAX_VALUE),
						ib < b.length ? (b[ib].length() - pb) : Integer.MAX_VALUE);
				if (ia < a.length) {	
					IString sa = a[ia].substring(pa, pa+len);
					ra.add(sa);
					pa += len;
					if (pa == a[ia].length()) {
						pa = 0;
						ia++;
					}
				}
				if (ib < b.length) {
					IString sb = b[ib].substring(pb, pb+len);
					rb.add(sb);
					pb += len;
					if (ib < b.length && pb == b[ib].length()) {
						pb = 0;
						ib++;
					}
				}
			}
			return new IString[][] { (IString[]) ra.toArray(new IString[ra.size()]), (IString[]) rb.toArray(new IString[rb.size()]) };
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
		public IString concat(IString other) {
			return lazyConcat((IStringTreeNode) other);
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
		public int depth() {
			return depth;
		}

		@Override
		public IString substring(int start, int end) {
			assert end >= start;

			if (end <=left.length()) { // Bert
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
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		// TODO: this needs a unit test
		public int charAt(int index) {
			if (index < left.length()) {
				return left.charAt(index);
			} else {
				return right.charAt(index - left.length());
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
			left.write(w);
			right.write(w);
		}
		
		@Override
		public IStringTreeNode rotateRight() {
			BinaryBalancedLazyConcatString p =  new BinaryBalancedLazyConcatString(((StringValue.BinaryBalancedLazyConcatString) (this.left)).right, this.right) ;
			return new BinaryBalancedLazyConcatString(((StringValue.BinaryBalancedLazyConcatString) (this.left)).left, p);
		}
		
		@Override
		public IStringTreeNode rotateLeft() {
			BinaryBalancedLazyConcatString p =  new BinaryBalancedLazyConcatString(this.left, ((StringValue.BinaryBalancedLazyConcatString) (this.right)).left) ;
			return new BinaryBalancedLazyConcatString(p, ((StringValue.BinaryBalancedLazyConcatString) (this.right)).right);	
		}
		
		@Override
		public IStringTreeNode rotateRightLeft() {
			BinaryBalancedLazyConcatString r = (BinaryBalancedLazyConcatString) this.right;
			IStringTreeNode rotateRight = new BinaryBalancedLazyConcatString(this.left, r.rotateRight());
			return rotateRight.rotateLeft();
		}
		
		@Override
		public IStringTreeNode rotateLeftRight() {
			BinaryBalancedLazyConcatString l= (BinaryBalancedLazyConcatString) this.left;
			IStringTreeNode rotateLeft = new BinaryBalancedLazyConcatString(l.rotateLeft(), this.right);
			return rotateLeft.rotateRight();
		}
		
		static  BinaryBalancedLazyConcatString balance(BinaryBalancedLazyConcatString t) {
			IStringTreeNode l = t.left; 
			IStringTreeNode r = t.right; 
			if (t.balanceFactor()<=-0 || t.balanceFactor()>=0) 
			{
			    if (l instanceof BinaryBalancedLazyConcatString && l.depth()>2)  l = balance((BinaryBalancedLazyConcatString) l);
			    if (r instanceof BinaryBalancedLazyConcatString && r.depth()>2)  r = balance((BinaryBalancedLazyConcatString) r);
			}	          
			
			BinaryBalancedLazyConcatString p = new BinaryBalancedLazyConcatString(l, r);
			while (p.balanceFactor()>=2) {
				  if (p.right.balanceFactor()<=0) 
				       p = (BinaryBalancedLazyConcatString) p.rotateRightLeft();
				  else
					   p = (BinaryBalancedLazyConcatString) p.rotateLeft();
				  
			} 
			while (p.balanceFactor()<=-2) {
				 if (p.left.balanceFactor()>=0) 
				       p = (BinaryBalancedLazyConcatString) p.rotateLeftRight();
				 else
					   p = (BinaryBalancedLazyConcatString) p.rotateRight();
				  
			}
			return p;
		}

		@Override
		public int balanceFactor() {
			return this.right.depth()-this.left.depth();
		}

		@Override
		public IString balance() {
			// TODO Auto-generated method stub
			return BinaryBalancedLazyConcatString.balance(this);
		}
	}

}
