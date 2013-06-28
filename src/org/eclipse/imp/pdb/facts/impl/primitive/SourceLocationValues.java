/*******************************************************************************
 * Copyright (c) 2012-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Arnold Lankamp - implementation
 *   * Jurgen Vinju - implementation
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.primitive;

import java.net.URI;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.AbstractValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

/**
 * This is a container class for a number of implementations of ISourceLocation. Each implementation is extremely similar to the others.
 * except that different native types are used to store offsets, lengths, line and column indices. The goal is to use a minimum amount
 * of heap for each source location object, since at run-time there will be so many of them. We measured the effect of this on some real 
 * applications and showed more than 50% improvement in memory usage.
 */
/*package*/ class SourceLocationValues {
	private abstract static class Complete extends Incomplete {
		public Complete(URI uri) {
			super(uri);
		}

		@Override
		public boolean hasOffsetLength() {
			return true;
		}
		
		@Override
		public boolean hasLineColumn() {
			return true;
		}
	}
	
	private abstract static class Incomplete extends AbstractValue implements ISourceLocation {
		protected final URI uri;

		public Incomplete(URI uri) {
			this.uri = uri;
		}
		
		@Override
		public URI getURI() {
			return uri;
		}
		
		@Override
		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		@Override
		public boolean hasLineColumn() {
			return false;
		}
		
		@Override
		public boolean hasOffsetLength() {
			return false;
		}
		
		@Override
		public int getBeginColumn() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int getBeginLine() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int getEndColumn() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int getEndLine() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int getLength() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public int getOffset() throws UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E{
	    	return v.visitSourceLocation(this);
		}
		
		@Override
		public boolean isEqual(IValue value){
			return equals(value);
		}
	}
	
	public static class IntIntIntIntIntInt extends Complete {
		protected final int offset;
		protected final int length;
		protected final int beginLine;
		protected final int endLine;
		protected final int beginCol;
		protected final int endCol;
		
		protected IntIntIntIntIntInt(URI uri, int offset, int length, int beginLine, int endLine, int beginCol, int endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		@Override
		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		@Override
		public int getBeginLine(){
			return beginLine;
		}
		
		@Override
		public int getEndLine(){
			return endLine;
		}
		
		@Override
		public int getBeginColumn(){
			return beginCol;
		}
		
		@Override
		public int getEndColumn(){
			return endCol;
		}
		
		@Override
		public int getOffset(){
			return offset;
		}
		
		@Override
		public int getLength(){
			return length;
		}
		
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= beginLine << 3;
			hash ^= (endLine << 23);
			hash ^= (beginCol << 13);
			hash ^= (endCol << 18);
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				IntIntIntIntIntInt otherSourceLocation = (IntIntIntIntIntInt) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (beginLine == otherSourceLocation.beginLine)
						&& (endLine == otherSourceLocation.endLine)
						&& (beginCol == otherSourceLocation.beginCol)
						&& (endCol == otherSourceLocation.endCol)
						&& (offset == otherSourceLocation.offset)
						&& (length == otherSourceLocation.length));
			}
			
			return false;
		}
	}
	
	public static class CharCharByteByteByteByte extends Complete {
		protected final char offset;
		protected final char length;
		protected final byte beginLine;
		protected final byte endLine;
		protected final byte beginCol;
		protected final byte endCol;
		
		protected CharCharByteByteByteByte(URI uri, char offset, char length, byte beginLine, byte endLine, byte beginCol, byte endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		@Override
		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		@Override
		public int getBeginLine(){
			return beginLine;
		}
		
		@Override
		public int getEndLine(){
			return endLine;
		}
		
		@Override
		public int getBeginColumn(){
			return beginCol;
		}
		
		@Override
		public int getEndColumn(){
			return endCol;
		}
		
		@Override
		public int getOffset(){
			return offset;
		}
		
		@Override
		public int getLength(){
			return length;
		}
		
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= beginLine << 3;
			hash ^= (endLine << 23);
			hash ^= (beginCol << 13);
			hash ^= (endCol << 18);
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				CharCharByteByteByteByte otherSourceLocation = (CharCharByteByteByteByte) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (beginLine == otherSourceLocation.beginLine)
						&& (endLine == otherSourceLocation.endLine)
						&& (beginCol == otherSourceLocation.beginCol)
						&& (endCol == otherSourceLocation.endCol)
						&& (offset == otherSourceLocation.offset)
						&& (length == otherSourceLocation.length));
			}
			
			return false;
		}
	}
	
	public static class CharCharCharCharCharChar extends Complete {
		protected final char offset;
		protected final char length;
		protected final char beginLine;
		protected final char endLine;
		protected final char beginCol;
		protected final char endCol;
		
		protected CharCharCharCharCharChar(URI uri, char offset, char length, char beginLine, char endLine, char beginCol, char endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		@Override
		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		@Override
		public int getBeginLine(){
			return beginLine;
		}
		
		@Override
		public int getEndLine(){
			return endLine;
		}
		
		@Override
		public int getBeginColumn(){
			return beginCol;
		}
		
		@Override
		public int getEndColumn(){
			return endCol;
		}
		
		@Override
		public int getOffset(){
			return offset;
		}
		
		@Override
		public int getLength(){
			return length;
		}
		
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= beginLine << 3;
			hash ^= (endLine << 23);
			hash ^= (beginCol << 13);
			hash ^= (endCol << 18);
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				CharCharCharCharCharChar otherSourceLocation = (CharCharCharCharCharChar) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (beginLine == otherSourceLocation.beginLine)
						&& (endLine == otherSourceLocation.endLine)
						&& (beginCol == otherSourceLocation.beginCol)
						&& (endCol == otherSourceLocation.endCol)
						&& (offset == otherSourceLocation.offset)
						&& (length == otherSourceLocation.length));
			}
			
			return false;
		}
	}

	public static class OnlyURI extends Incomplete {
		
		protected OnlyURI(URI uri){
			super(uri);
		}

		public int hashCode(){
			return uri.hashCode();
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				OnlyURI otherSourceLocation = (OnlyURI) o;
				return uri.equals(otherSourceLocation.uri);
			}
			
			return false;
		}
	}

	public static class IntIntIntIntByteByte extends Complete {
		protected final int offset;
		protected final int length;
		protected final int beginLine;
		protected final int endLine;
		protected final byte beginCol;
		protected final byte endCol;
		
		protected IntIntIntIntByteByte(URI uri, int offset, int length, int beginLine, int endLine, byte beginCol, byte endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		@Override
		public int getBeginLine(){
			return beginLine;
		}
		
		@Override
		public int getEndLine(){
			return endLine;
		}
		
		@Override
		public int getBeginColumn(){
			return beginCol;
		}
		
		@Override
		public int getEndColumn(){
			return endCol;
		}
		
		@Override
		public int getOffset(){
			return offset;
		}
		
		@Override
		public int getLength(){
			return length;
		}
		
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= beginLine << 3;
			hash ^= (endLine << 23);
			hash ^= (beginCol << 13);
			hash ^= (endCol << 18);
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				IntIntIntIntByteByte otherSourceLocation = (IntIntIntIntByteByte) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (beginLine == otherSourceLocation.beginLine)
						&& (endLine == otherSourceLocation.endLine)
						&& (beginCol == otherSourceLocation.beginCol)
						&& (endCol == otherSourceLocation.endCol)
						&& (offset == otherSourceLocation.offset)
						&& (length == otherSourceLocation.length));
			}
			
			return false;
		}
	}

	public static class IntIntCharCharByteByte extends Complete {
		protected final int offset;
		protected final int length;
		protected final char beginLine;
		protected final char endLine;
		protected final byte beginCol;
		protected final byte endCol;
		
		protected IntIntCharCharByteByte(URI uri, int offset, int length, char beginLine, char endLine, byte beginCol, byte endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		@Override
		public int getBeginLine(){
			return beginLine;
		}
		
		@Override
		public int getEndLine(){
			return endLine;
		}
		
		@Override
		public int getBeginColumn(){
			return beginCol;
		}
		
		@Override
		public int getEndColumn(){
			return endCol;
		}
		
		@Override
		public int getOffset(){
			return offset;
		}
		
		@Override
		public int getLength(){
			return length;
		}
		
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= beginLine << 3;
			hash ^= (endLine << 23);
			hash ^= (beginCol << 13);
			hash ^= (endCol << 18);
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				IntIntCharCharByteByte otherSourceLocation = (IntIntCharCharByteByte) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (beginLine == otherSourceLocation.beginLine)
						&& (endLine == otherSourceLocation.endLine)
						&& (beginCol == otherSourceLocation.beginCol)
						&& (endCol == otherSourceLocation.endCol)
						&& (offset == otherSourceLocation.offset)
						&& (length == otherSourceLocation.length));
			}
			
			return false;
		}
	}

	public static class ByteByte extends Incomplete {
		protected final byte offset;
		protected final byte length;
		
		protected ByteByte(URI uri, byte offset, byte length){
			super(uri);
			
			this.offset = offset;
			this.length = length;
		}
		
		@Override
		public boolean hasOffsetLength() {
			return true;
		}
		
		@Override
		public int getOffset(){
			return offset;
		}
		
		@Override
		public int getLength(){
			return length;
		}
		
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				ByteByte otherSourceLocation = (ByteByte) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (offset == otherSourceLocation.offset)
						&& (length == otherSourceLocation.length));
			}
			
			return false;
		}
	}

	public static class CharChar extends Incomplete {
		protected final char offset;
		protected final char length;
		
		protected CharChar(URI uri, char offset, char length){
			super(uri);
			
			this.offset = offset;
			this.length = length;
		}
		
		@Override
		public boolean hasOffsetLength() {
			return true;
		}
		
		@Override
		public int getOffset(){
			return offset;
		}
		
		@Override
		public int getLength(){
			return length;
		}
		
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				CharChar otherSourceLocation = (CharChar) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (offset == otherSourceLocation.offset)
						&& (length == otherSourceLocation.length));
			}
			
			return false;
		}
	}
	
	public static class IntInt extends Incomplete {
		protected final int offset;
		protected final int length;
		
		protected IntInt(URI uri, int offset, int length){
			super(uri);
			
			this.offset = offset;
			this.length = length;
		}
		
		@Override
		public boolean hasOffsetLength() {
			return true;
		}
		
		@Override
		public boolean hasLineColumn() {
			return false;
		}

		@Override
		public int getOffset(){
			return offset;
		}
		
		@Override
		public int getLength(){
			return length;
		}
		
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				IntInt otherSourceLocation = (IntInt) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (offset == otherSourceLocation.offset)
						&& (length == otherSourceLocation.length));
			}
			
			return false;
		}
	}
}
