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
import java.util.Collections;
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
 * Fine here the implementations of IString, which all are (must be) sub-classes of StringValue.DefaultString
 * as an internal design invariant.
 * 
 * The challenges solved by this implementation:
 *   - cater for and optimize for the normal case of strings containing only normal ASCII characters, while
 *     still allowing all 24-bit unicode characters
 *   - optimize string concatenation and the streaming of large concatenated strings to a writer
 *   - optimize the indentation operation and streaming recursively indented strings to a writer
 *   - allow non-canonical representations of the same string, in particular making sure that equals() and hashCode() works 
 *     well across and between different representations of (accidentally) the same string.
 *     
 * The 'normal' case is defined by what people normally do in Rascal, i.e. using string template expanders a lot,
 * and reading and writing to text files, searching through strings. The other operations on strings are implemented,
 * but less optimally. They're traded for the optimization of concat and indent. 
 */
/* package */ public class StringValue {
	private static final char NEWLINE = '\n';
    private static final Type STRING_TYPE = TypeFactory.getInstance().stringType();

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
		if (value == null || value.isEmpty()) {
			return EmptyString.getInstance();
		}
		
		// we do not reuse newString(String value, boolean fullUnicode),
		// or vice versa, because we want to run over the string only once
		// to collect the count and the containsSurrogatePairs

		boolean containsSurrogatePairs = false;
		int count = 0;
		
        int len = value.length();
        for (int i = 0; i < len; i++) {
            if (containsSurrogatePairs || (i > 0 && Character.isSurrogatePair(value.charAt(i - 1), value.charAt(i)))) {
                containsSurrogatePairs = true;
            }
            
            if (value.charAt(i) == NEWLINE && (i == 0 || value.charAt(i - 1) != NEWLINE)) {
                count++;
            }
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
            if (value.charAt(i) == NEWLINE && (i == 0 || value.charAt(i - 1) != NEWLINE)) {
                count++;
            }
        }
        
        return newString(value, fullUnicode, count);
    }

	/* package */ static IString newString(String value, boolean fullUnicode, int nonEmptyLineCount) {
		if (value == null || value.isEmpty()) {
			return EmptyString.getInstance();
		}

		if (fullUnicode) {
			return new FullUnicodeString(value, nonEmptyLineCount);
		}

		return new SimpleUnicodeString(value, nonEmptyLineCount);
	}

	private static class EmptyString extends DefaultString {
	    private static class InstanceHolder {
	        public static EmptyString instance = new EmptyString();
	    }

	    public static EmptyString getInstance() {
	        return InstanceHolder.instance;
	    }
	    
	    private EmptyString() { }
	    
	    @Override
	    public int hashCode() {
	        return 0;
	    }
	    
	    @Override
	    public boolean equals(Object other) {
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
            return this;
        }

        @Override
        public int charAt(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public IString replace(int first, int second, int end, IString repl) {
            return this;
        }

        @Override
        public void write(Writer w) throws IOException {
        }

        @Override
        public Iterator<Integer> iterator() {
            return Collections.<Integer>emptyIterator();
        }

        @Override
        public int nonEmptyLineCount() {
            return 0;
        }

        @Override
        public boolean isNewlineTerminated() {
            return false;
        }
	}
	
	private static class FullUnicodeString extends DefaultString {
		protected final String value;
        protected final int nonEmptyLineCount;

		private FullUnicodeString(String value, int nonEmptyLineCount) {
			super();

			this.value = value;
			this.nonEmptyLineCount = nonEmptyLineCount;
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
		public int nonEmptyLineCount() {
		    return nonEmptyLineCount;
		}
		
		@Override
		public IString concat(IString other) {
			if (length() + other.length() <= MAX_FLAT_STRING) {
				StringBuilder buffer = new StringBuilder();
				buffer.append(value);
				buffer.append(other.getValue());
				DefaultString o = (DefaultString) other;

				return StringValue.newString(buffer.toString(), true, nonEmptyLineCount - (isNewlineTerminated()?1:0) + o.nonEmptyLineCount());
			} else {
				return BinaryBalancedLazyConcatString.build(this, (DefaultString) other);
			}
		}

		@Override
		/**
		 * Note that this algorithm can not be changed, unless you also have change
		 * BinaryBalancedTreeNode.hashCode() and DefaultString.hashCode() (to not break the hashCode/equals
		 * contract).
		 */
		public int hashCode() {
			return value.hashCode();
		}

		@Override
		public IString reverse() {
			return newString(new StringBuilder(value).reverse().toString(), true, nonEmptyLineCount);
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
		public void indentedWrite(Writer w, IString whitespace, boolean indentFirstLine) throws IOException {
		    // this implementation tries to quickly find the next newline using indexOf, and streams
		    // line by line to optimize copying the characters onto the stream in bigger blobs than 1 character.
		    for (int pos = value.indexOf(NEWLINE), prev = 0, count = 0; ; prev = pos, pos = value.indexOf(NEWLINE, pos), count++) {
		        // if pos is the last character, print it an bail out:
		        if (pos == value.length() - 1) {
		            w.write(NEWLINE);
		            return;
		        }
		        
		        // if pos is an empty line, print it and continue:
		        if (value.charAt(pos + 1) == NEWLINE) {
		            w.write(NEWLINE);
		            continue;
		        }
		        
		        // otherwise we can write the indentation, skipping the first line if needed
		        if (count > 0 && indentFirstLine) {
		            whitespace.write(w);
		        }
                
		        if (pos == -1) {
		            // no more newlines, so write the entire line
		           w.write(value, prev, value.length() - prev);
		           return;
		        }
		        else {
		            // write until the currently found newline
		            w.write(value, prev, pos - prev);
		        }
		    }
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
	}

	/**
	 * This class knows its contents do not contain any higher surrogate pairs,
	 * allowing it to implement some indexing functions a lot faster, i.e. in O(1)
	 * as opposed to in O(n).
	 *
	 */
	private static class SimpleUnicodeString extends FullUnicodeString {
		public SimpleUnicodeString(String value, int nonEmptyLineCount) {
			super(value, nonEmptyLineCount);
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
			return newString(new StringBuilder(value).reverse().toString(), false, nonEmptyLineCount);
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
        int nonEmptyLineCount();
        
        /**
         * When concatenating indentable strings, the {@link #nonEmptyLineCount()} can
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
        boolean isNewlineTerminated();
        
       
        
//        default int indentedCharAt(int index, IString whiteSpace) {
//            throw new UnsupportedOperationException();
//        }
//
//        default public int indentedLength(IString whiteSpace) {
//            throw new UnsupportedOperationException();
//        }
//
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
        default public void indentedWrite(Writer w, IString whiteSpace, boolean indentFirstLine) throws IOException {
            throw new UnsupportedOperationException();
        }
//        
//        default public String indentedGetValue(IString whiteSpace) {
//            throw new UnsupportedOperationException();
//        }
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
		default DefaultString left() {
			throw new UnsupportedOperationException();
		}

		/**
		 * Should be overridden by the binary node and never called on the leaf nodes
		 * because they are not out of balance.
		 */
		default DefaultString right() {
			throw new UnsupportedOperationException();
		}

		default DefaultString rotateRight() {
			return (DefaultString) this;
		}

		default DefaultString rotateLeft() {
			return (DefaultString) this;
		}

		default DefaultString rotateRightLeft() {
			return (DefaultString) this;
		}

		default DefaultString rotateLeftRight() {
			return (DefaultString) this;
		}

		default void collectLeafIterators(List<Iterator<Integer>> w) {
			w.add(iterator());
		}
	}
	
	private abstract static class DefaultString extends AbstractValue implements IString, IStringTreeNode, IIndentableString {
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
        public IString concat(IString other) {
            return BinaryBalancedLazyConcatString.build((DefaultString) this, (DefaultString) other);
        }
        
        @Override
        public IString indent(IString whiteSpace) {
            return new IndentedString((DefaultString) this, whiteSpace);
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
            // the IString.length() under-estimates the size of the string if the string
            // contains many surrogate pairs, but that does not happen a lot in 
            // most of what we see, so we decided to go for a tight estimate for
            // "normal" ASCII strings
            
            try (StringWriter w = new StringWriter(length())) {
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
            Iterator<Integer> it1 = this.iterator();
            Iterator<Integer> it2 = other.iterator();

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
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            }
            
            if (!(other instanceof DefaultString)) {
                return false;
            }
            
            DefaultString o = (DefaultString) other;
            
            if (o.length() != length()) {
                return false;
            }
            
            Iterator<Integer> it1 = this.iterator();
            Iterator<Integer> it2 = o.iterator();

            while (it1.hasNext() && it2.hasNext()) {
                Integer c1 = it1.next();
                Integer c2 = it2.next();

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
            int h = 0;
            
            for (Integer c : this) {
                if (!Character.isBmpCodePoint(c)) {
                    h = 31 * h + Character.highSurrogate(c);
                    h = 31 * h + Character.lowSurrogate(c);
                } else {
                    h = 31 * h + c;
                }
            }

            return h;
        }
	}
	
	private static class BinaryBalancedLazyConcatString extends DefaultString {
		private final DefaultString left; /* must remain final for immutability's sake */
		private final DefaultString right; /* must remain final for immutability's sake */
		private final int length;
		private final int depth;
		private final int nonEmptyLineCount;
		private int hash = 0;

		public static IStringTreeNode build(DefaultString left, DefaultString right) {
			assert left.invariant();
			assert right.invariant();

			IStringTreeNode result = balance(left, right);

			assert result.invariant();
			assert result.left().invariant();
			assert result.right().invariant();

			return result;
		}
		
		 /**
         * Note that we used the hashcode algorithm for java.lang.String here, which is
         * necessary because that is also used by the specialized implementations of IString,
         * namely FullUnicodeString and its subclasses, 
         * which must implement together with this class the hashCode/equals contract.
         */
		@Override
		public int hashCode() {
		    if (hash == 0) {
		        int h = left.hashCode();
		        
		        // see how the hashcode is computed from left-to-right?
		        // that is why we can continue with the hashcode of the left
		        // tree without having to start from the beginning of the left.
		        for (Integer c : right) {
	                if (!Character.isBmpCodePoint(c)) {
	                    h = 31 * h + Character.highSurrogate(c);
	                    h = 31 * h + Character.lowSurrogate(c);
	                } else {
	                    h = 31 * h + c;
	                }
	            }
		        
		        hash = h;
		    }
		    
		    return hash;
		}
		
		private static DefaultString balance(DefaultString left, DefaultString right) {
		    DefaultString result = new BinaryBalancedLazyConcatString(left, right);

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

		private BinaryBalancedLazyConcatString(DefaultString left, DefaultString right) {
			this.left = left;
			this.right = right;
			this.length = left.length() + right.length();
			this.depth = Math.max(left.depth(), right.depth()) + 1;
			this.nonEmptyLineCount = left.nonEmptyLineCount() - (left.isNewlineTerminated() ? 1 : 0) + right.nonEmptyLineCount();
		}

		
		@Override
		public int nonEmptyLineCount() {
		    return nonEmptyLineCount;
		}
		
		@Override
		public boolean isNewlineTerminated() {
		    return right.length() == 0 ? left.isNewlineTerminated() : right.isNewlineTerminated();
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
		public DefaultString left() {
			return left;
		}

		@Override
		public DefaultString right() {
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
		public void indentedWrite(Writer w, IString whitespace, boolean indentFirstLine) throws IOException {
			left.indentedWrite(w, whitespace, indentFirstLine);
			right.indentedWrite(w, whitespace, (left.length() == 0 && indentFirstLine) || left.isNewlineTerminated());
		}

		@Override
		public DefaultString rotateRight() {
			BinaryBalancedLazyConcatString p = new BinaryBalancedLazyConcatString(left().right(), right());
			p = (BinaryBalancedLazyConcatString) balance(p.left, p.right);
			return new BinaryBalancedLazyConcatString(left().left(), p);
		}

		@Override
		public DefaultString rotateLeft() {
			BinaryBalancedLazyConcatString p = new BinaryBalancedLazyConcatString(left(), right().left());
			return new BinaryBalancedLazyConcatString(balance(p.left, p.right), right().right());
		}

		@Override
		public DefaultString rotateRightLeft() {
			IStringTreeNode rotateRight = new BinaryBalancedLazyConcatString(left(), right().rotateRight());
			return rotateRight.rotateLeft();
		}

		@Override
		public DefaultString rotateLeftRight() {
			IStringTreeNode rotateLeft = new BinaryBalancedLazyConcatString(left().rotateLeft(), right());
			return rotateLeft.rotateRight();
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
		}
	}

	private static class IndentedString extends DefaultString {
		private final IString indent;
		private final DefaultString wrapped;

		IndentedString(DefaultString istring, IString whiteSpace) {
			this.indent = whiteSpace;
			this.wrapped = istring;
		}

		@Override
		public IString indent(IString indent) {
		    // this special case flattens directly nested concats 
			return new IndentedString(wrapped, this.indent.concat(indent));
		}

		/**
		 * This is the basic implementation of indentation, while iterating
		 * over the string we insert the indent before every non-empty line.
		 */
		@Override
		public Iterator<Integer> iterator() {
		    return new Iterator<Integer>() {
		        final Iterator<Integer> output = wrapped.iterator();
		        Iterator<Integer> whitespace = indent.iterator();
		        int prev = 0;
		        
		        @Override
		        public boolean hasNext() {
		            return output.hasNext();
		        }

		        @Override
		        public Integer next() {
		            if (whitespace.hasNext()) {
		                return whitespace.next();
		            }

		            // done with indenting, so continue with the content
		            int cur = output.next();
		            if (cur == NEWLINE && prev != NEWLINE && output.hasNext()) {
		                // this is a non-empty, non-last line, so we start a new indentation iterator
		                whitespace = indent.iterator();
		            }
		            prev = cur;
		            return cur;
		        }
		    };
		}

		@Override
		public IString reverse() {
			return newString(getValue()).reverse();
		}

		@Override
		public IString substring(int start, int end) {
			Iterator<Integer> it = iterator();
            int counter;
            int ch = 0;
            
            // skip to the start using the iterator
            for (counter = 0; it.hasNext() && counter < start; ch = it.next());
            
            // collect all the characters to the end, again using the iterator
            StringBuilder b = new StringBuilder();
            for (; it.hasNext() && counter < end; ch = it.next()) {
                b.appendCodePoint(ch);
            }
            
            return newString(b.toString());
		}

		@Override
		public int charAt(int index) {
		    int counter = 0;
		    for (int ch : this) {
		        // let the iterator solve it.
		        if (counter++ == index) {
		            return ch;
		        }
		    }
		    
		    throw new IndexOutOfBoundsException();
		}
		
		@Override
		public IString replace(int first, int second, int end, IString repl) {
			return newString(getValue()).replace(first, second, end, repl);
		}

		@Override
		public void write(Writer w) throws IOException {
			wrapped.indentedWrite(w, this.indent, true);
		}

		@Override
		public void indentedWrite(Writer w, IString whitespace, boolean indentFirstLine) throws IOException {
			wrapped.indentedWrite(w, whitespace.concat(this.indent), indentFirstLine);
		}

		@Override
		public int length() {
		    // for every non-empty line an indent would be added to the total number of characters
		    return wrapped.length() + wrapped.nonEmptyLineCount() * indent.length();
		}
		
		@Override
		public int nonEmptyLineCount() {
		    return wrapped.nonEmptyLineCount();
		}
		
		@Override
		public boolean isNewlineTerminated() {
		    return wrapped.isNewlineTerminated();
		}
	}
}
