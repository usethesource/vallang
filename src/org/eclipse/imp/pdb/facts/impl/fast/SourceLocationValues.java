/*******************************************************************************
* Copyright (c) 2012 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - implementation
*    Jurgen Vinju - implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.net.URI;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * This is a container class for a number of implementations of ISourceLocation. Each implementation is extremely similar to the others.
 * except that different native types are used to store offsets, lengths, line and column indices. The goal is to use a minimum amount
 * of heap for each source location object, since at run-time there will be so many of them. We measured the effect of this on some real 
 * applications and showed more than 50% improvement in memory usage.
 */
public class SourceLocationValues {
	public static class Largest extends Value implements ISourceLocation{
		protected final URI uri;
		protected final int offset;
		protected final int length;
		protected final int beginLine;
		protected final int endLine;
		protected final int beginCol;
		protected final int endCol;
		
		protected Largest(URI uri, int offset, int length, int beginLine, int endLine, int beginCol, int endCol){
			super();
			
			this.uri = uri;
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		public URI getURI(){
			return uri;
		}
		
		public int getBeginLine(){
			return beginLine;
		}
		
		public int getEndLine(){
			return endLine;
		}
		
		public int getBeginColumn(){
			return beginCol;
		}
		
		public int getEndColumn(){
			return endCol;
		}
		
		public int getOffset(){
			return offset;
		}
		
		public int getLength(){
			return length;
		}
		
		public <T> T accept(IValueVisitor<T> v) throws VisitorException{
	    	return v.visitSourceLocation(this);
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
				Largest otherSourceLocation = (Largest) o;
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
		
		public boolean isEqual(IValue value){
			return equals(value);
		}
	}
	
	public static class Smallest extends Value implements ISourceLocation{
		protected final URI uri;
		protected final char offset;
		protected final char length;
		protected final byte beginLine;
		protected final byte endLine;
		protected final byte beginCol;
		protected final byte endCol;
		
		protected Smallest(URI uri, char offset, char length, byte beginLine, byte endLine, byte beginCol, byte endCol){
			super();
			
			this.uri = uri;
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		public URI getURI(){
			return uri;
		}
		
		public int getBeginLine(){
			return beginLine;
		}
		
		public int getEndLine(){
			return endLine;
		}
		
		public int getBeginColumn(){
			return beginCol;
		}
		
		public int getEndColumn(){
			return endCol;
		}
		
		public int getOffset(){
			return offset;
		}
		
		public int getLength(){
			return length;
		}
		
		public <T> T accept(IValueVisitor<T> v) throws VisitorException{
	    	return v.visitSourceLocation(this);
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
				Smallest otherSourceLocation = (Smallest) o;
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
		
		public boolean isEqual(IValue value){
			return equals(value);
		}
	}
	
	public static class MediumSized extends Value implements ISourceLocation{
		protected final URI uri;
		protected final char offset;
		protected final char length;
		protected final char beginLine;
		protected final char endLine;
		protected final char beginCol;
		protected final char endCol;
		
		protected MediumSized(URI uri, char offset, char length, char beginLine, char endLine, char beginCol, char endCol){
			super();
			
			this.uri = uri;
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		public URI getURI(){
			return uri;
		}
		
		public int getBeginLine(){
			return beginLine;
		}
		
		public int getEndLine(){
			return endLine;
		}
		
		public int getBeginColumn(){
			return beginCol;
		}
		
		public int getEndColumn(){
			return endCol;
		}
		
		public int getOffset(){
			return offset;
		}
		
		public int getLength(){
			return length;
		}
		
		public <T> T accept(IValueVisitor<T> v) throws VisitorException{
	    	return v.visitSourceLocation(this);
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
				MediumSized otherSourceLocation = (MediumSized) o;
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
		
		public boolean isEqual(IValue value){
			return equals(value);
		}
	}

	public static class NoPositions extends Value implements ISourceLocation{
		protected final URI uri;
		
		protected NoPositions(URI uri){
			super();
			
			this.uri = uri;
		}

		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		public URI getURI(){
			return uri;
		}
		
		public int getBeginLine(){
			return -1;
		}
		
		public int getEndLine(){
			return -1;
		}
		
		public int getBeginColumn(){
			return -1;
		}
		
		public int getEndColumn(){
			return -1;
		}
		
		public int getOffset(){
			return -1;
		}
		
		public int getLength(){
			return -1;
		}
		
		public <T> T accept(IValueVisitor<T> v) throws VisitorException{
	    	return v.visitSourceLocation(this);
		}
		
		public int hashCode(){
			return uri.hashCode();
		}
		
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				NoPositions otherSourceLocation = (NoPositions) o;
				return uri.equals(otherSourceLocation.uri);
			}
			
			return false;
		}
		
		public boolean isEqual(IValue value){
			return equals(value);
		}
	}

	public static class ShortColumns extends Value implements ISourceLocation{
		protected final URI uri;
		protected final int offset;
		protected final int length;
		protected final int beginLine;
		protected final int endLine;
		protected final byte beginCol;
		protected final byte endCol;
		
		protected ShortColumns(URI uri, int offset, int length, int beginLine, int endLine, byte beginCol, byte endCol){
			super();
			
			this.uri = uri;
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		public URI getURI(){
			return uri;
		}
		
		public int getBeginLine(){
			return beginLine;
		}
		
		public int getEndLine(){
			return endLine;
		}
		
		public int getBeginColumn(){
			return beginCol;
		}
		
		public int getEndColumn(){
			return endCol;
		}
		
		public int getOffset(){
			return offset;
		}
		
		public int getLength(){
			return length;
		}
		
		public <T> T accept(IValueVisitor<T> v) throws VisitorException{
	    	return v.visitSourceLocation(this);
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
				ShortColumns otherSourceLocation = (ShortColumns) o;
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
		
		public boolean isEqual(IValue value){
			return equals(value);
		}
	}

	public static class ShortColumnsMediumLines extends Value implements ISourceLocation{
		protected final URI uri;
		protected final int offset;
		protected final int length;
		protected final char beginLine;
		protected final char endLine;
		protected final byte beginCol;
		protected final byte endCol;
		
		protected ShortColumnsMediumLines(URI uri, int offset, int length, char beginLine, char endLine, byte beginCol, byte endCol){
			super();
			
			this.uri = uri;
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		public Type getType(){
			return TypeFactory.getInstance().sourceLocationType();
		}
		
		public URI getURI(){
			return uri;
		}
		
		public int getBeginLine(){
			return beginLine;
		}
		
		public int getEndLine(){
			return endLine;
		}
		
		public int getBeginColumn(){
			return beginCol;
		}
		
		public int getEndColumn(){
			return endCol;
		}
		
		public int getOffset(){
			return offset;
		}
		
		public int getLength(){
			return length;
		}
		
		public <T> T accept(IValueVisitor<T> v) throws VisitorException{
	    	return v.visitSourceLocation(this);
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
				ShortColumnsMediumLines otherSourceLocation = (ShortColumnsMediumLines) o;
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
		
		public boolean isEqual(IValue value){
			return equals(value);
		}
	}


}
