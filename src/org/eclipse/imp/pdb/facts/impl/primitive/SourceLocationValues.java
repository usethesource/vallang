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
import java.net.URISyntaxException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
	/*package*/ static ISourceLocation newSourceLocation(ISourceLocation loc, int offset) {
		IURI uri = ((Incomplete)loc).uri;
		if (offset < 0) {
			throw new IllegalArgumentException("offset should be positive");
		}
	
		if (offset < java.lang.Byte.MAX_VALUE ) {
			return new SourceLocationValues.Byte(uri, (byte) offset);
		}

		if (offset < Character.MAX_VALUE) {
			return new SourceLocationValues.Char(uri, (char) offset);
		}

		return new SourceLocationValues.Int(uri, offset);
	}
	
	/*package*/ static ISourceLocation newSourceLocation(ISourceLocation loc, int offset, int length) {
		IURI uri = ((Incomplete)loc).uri;
		if (offset < 0) {
			throw new IllegalArgumentException("offset should be positive");
		}
		
		if (length < 0) {
			throw new IllegalArgumentException("length should be positive");
		}

		if (offset < java.lang.Byte.MAX_VALUE && length < java.lang.Byte.MAX_VALUE) {
			return new SourceLocationValues.ByteByte(uri, (byte) offset, (byte) length);
		}

		if (offset < Character.MAX_VALUE && length < Character.MAX_VALUE) {
			return new SourceLocationValues.CharChar(uri, (char) offset, (char) length);
		}

		return new SourceLocationValues.IntInt(uri, offset, length);
	}
	
	/*package*/ static ISourceLocation newSourceLocation(ISourceLocation loc, int offset, int length, int beginLine, int endLine, int beginCol, int endCol) {
		IURI uri = ((Incomplete)loc).uri;
		
		if (offset < 0) {
			throw new IllegalArgumentException("offset should be positive");
		}
		
		if (length < 0) {
			throw new IllegalArgumentException("length should be positive");
		}
		
		if (beginLine < 0) {
			throw new IllegalArgumentException("beginLine should be positive");
		}
		
		if (beginCol < 0) {
			throw new IllegalArgumentException("beginCol should be positive");
		}
		
		if (endCol < 0) {
			throw new IllegalArgumentException("endCol should be positive");
		}
		
		if (endLine < beginLine) {
			throw new IllegalArgumentException("endLine should be larger than or equal to beginLine");
		}
		
		if (endLine == beginLine && endCol < beginCol) {
			throw new IllegalArgumentException("endCol should be larger than or equal to beginCol, if on the same line");
		}

		if (offset < Character.MAX_VALUE
				&& length < Character.MAX_VALUE
				&& beginLine < java.lang.Byte.MAX_VALUE
				&& endLine < java.lang.Byte.MAX_VALUE
				&& beginCol < java.lang.Byte.MAX_VALUE
				&& endCol < java.lang.Byte.MAX_VALUE) {
			return new SourceLocationValues.CharCharByteByteByteByte(uri, (char) offset, (char) length, (byte) beginLine, (byte) endLine, (byte) beginCol, (byte) endCol);
		} 
		else if (offset < Character.MAX_VALUE
				&& length < Character.MAX_VALUE
				&& beginLine < Character.MAX_VALUE
				&& endLine < Character.MAX_VALUE
				&& beginCol < Character.MAX_VALUE
				&& endCol < Character.MAX_VALUE) {
			return new SourceLocationValues.CharCharCharCharCharChar(uri, (char) offset, (char) length, (char) beginLine, (char) endLine, (char) beginCol, (char) endCol);
		} 
		else if (beginLine < Character.MAX_VALUE
				&& endLine < Character.MAX_VALUE
				&& beginCol < java.lang.Byte.MAX_VALUE
				&& endCol < java.lang.Byte.MAX_VALUE) {
			return new SourceLocationValues.IntIntCharCharByteByte(uri, offset, length, (char) beginLine, (char) endLine, (byte) beginCol, (byte) endCol);
		}
		else if (beginCol < java.lang.Byte.MAX_VALUE
				&& endCol < java.lang.Byte.MAX_VALUE) {
			return new SourceLocationValues.IntIntIntIntByteByte(uri, offset, length, beginLine, endLine, (byte) beginCol, (byte) endCol);
		}

		return new SourceLocationValues.IntIntIntIntIntInt(uri, offset, length, beginLine, endLine, beginCol, endCol);
	}	
	
	private final static Lock locationCacheLock = new ReentrantLock(true);
	@SuppressWarnings("serial")
	private final static LinkedHashMap<URI,ISourceLocation>  locationCache = new LinkedHashMap<URI,ISourceLocation>(400*4/3, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<URI,ISourceLocation> eldest) {
                return size() > 400;
            }
        };
        
	private final static Lock reverseLocationCacheLock = new ReentrantLock(true);
	@SuppressWarnings("serial")
	private final static LinkedHashMap<IURI,URI>  reverseLocationCache = new LinkedHashMap<IURI,URI>(400*4/3, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<IURI, URI> eldest) {
                return size() > 400;
            }
        };
	
	/*package*/ static ISourceLocation newSourceLocation(URI uri) throws URISyntaxException {
		try {
			// lock around the location cache, except if it takes to long to lock, then just skip the cache
			if (locationCacheLock.tryLock(10, TimeUnit.MILLISECONDS)) {
				try {
					ISourceLocation result = locationCache.get(uri);
					if (result == null) {
						result = newSourceLocation(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), uri.getFragment());
						locationCache.put(uri, result);
					}
					return result;
				}
				finally {
					locationCacheLock.unlock();
				}
			}
		} catch (InterruptedException e) {
		}
		// we couldn't get the lock, lets continue without cache
		return newSourceLocation(uri.getScheme(), uri.getAuthority(), uri.getPath(), uri.getQuery(), uri.getFragment());
	}
	
	/*package*/ static ISourceLocation newSourceLocation(String scheme, String authority,
			String path, String query, String fragment) throws URISyntaxException {
		IURI u = SourceLocationURIValues.newURI(scheme, authority, path, query, fragment);
		return new SourceLocationValues.OnlyURI(u);
	}
	
	/*package*/ static ISourceLocation newSourceLocation(IURI uri) {
		return new SourceLocationValues.OnlyURI(uri);
	}

	private abstract static class Complete extends Incomplete {
		private Complete(IURI uri) {
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
		
		@Override
		public ISourceLocation setOffset(int offset)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, getLength(), getBeginLine(), getEndLine(), getBeginColumn(), getEndColumn());
		}
		
		@Override
		public ISourceLocation setLength(int length)
				throws UnsupportedOperationException, IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, getOffset(), length, getBeginLine(), getEndLine(), getBeginColumn(), getEndColumn());
		}
		
		@Override
		public ISourceLocation setOffsetLength(int offset, int length)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length, getBeginLine(), getEndLine(), getBeginColumn(), getEndColumn());
		}
		
		@Override
		public ISourceLocation setBegin(int line, int col)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, getOffset(), getLength(), line, getEndLine(), col, getEndColumn());
		}
		
		
		@Override
		public ISourceLocation setEnd(int line, int col)
				throws IllegalArgumentException, UnsupportedOperationException {
			return SourceLocationValues.newSourceLocation(this, getOffset(), getLength(), getBeginLine(), line, getBeginColumn(), col);
		}
		
		@Override
		public ISourceLocation setBeginEnd(int beginLine, int endLine,
				int beginCol, int endCol) throws IllegalArgumentException,
				UnsupportedOperationException {
			return SourceLocationValues.newSourceLocation(this, getOffset(), getLength(), beginLine, endLine, beginCol, endCol);
		}
		
		@Override
		public ISourceLocation setBeginLine(int line)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, getOffset(), getLength(), line, getEndLine(), getBeginColumn(), getEndColumn());
		}
		
		@Override
		public ISourceLocation setBeginColumn(int col)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, getOffset(), getLength(), getBeginLine(), getEndLine(), col, getEndColumn());
		}
		
		@Override
		public ISourceLocation setEndColumn(int col)
				throws IllegalArgumentException, UnsupportedOperationException {
			return SourceLocationValues.newSourceLocation(this, getOffset(), getLength(), getBeginLine(), getEndLine(), getBeginColumn(), col);
		}
		
		@Override
		public ISourceLocation setEndLine(int line)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, getOffset(), getLength(), getBeginLine(), line, getBeginColumn(), getEndColumn());
		}
		
		@Override
		public ISourceLocation setPosition(int offset, int length,
				int beginLine, int endLine, int beginCol, int endCol)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length, beginLine, endLine, beginCol, endCol);
		}
	}
	
	
	private abstract static class Incomplete extends AbstractValue implements ISourceLocation {
		protected IURI uri;

		public Incomplete(IURI uri) {
			this.uri = uri;
		}
		
		/*package*/ abstract ISourceLocation setURI(IURI newURI);
		
		@Override
		public ISourceLocation setPosition(int offset, int length,
				int startLine, int endLine, int startCol, int endCol)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length, startLine, endLine, startCol, endCol);
		}

		@Override
		public ISourceLocation setOffset(int offset)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset);
		}
		
		@Override
		public ISourceLocation setBegin(int line, int col)
				throws IllegalArgumentException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ISourceLocation setOffsetLength(int offset, int length)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length);
		}
		
		@Override
		public ISourceLocation setLength(int length)
				throws UnsupportedOperationException, IllegalArgumentException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ISourceLocation setBeginColumn(int col)
				throws IllegalArgumentException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ISourceLocation setBeginEnd(int beginLine, int endLine,
				int beginCol, int endCol) throws IllegalArgumentException,
				UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ISourceLocation setBeginLine(int line)
				throws IllegalArgumentException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ISourceLocation setEnd(int line, int col)
				throws IllegalArgumentException, UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ISourceLocation setEndColumn(int col)
				throws IllegalArgumentException, UnsupportedOperationException {
			throw new UnsupportedOperationException();
		}

		@Override
		public ISourceLocation setEndLine(int line)
				throws IllegalArgumentException {
			throw new UnsupportedOperationException();
		}
		
		@Override
		public ISourceLocation getTop() {
			if (!hasOffsetLength() && !hasLineColumn()) {
				return this;
			}
			else {
				return SourceLocationValues.newSourceLocation(uri);
			}
		}
		
		@Override
		public Boolean hasAuthority() {
			return uri.hasAuthority();
		}
		
		@Override
		public Boolean hasFragment() {
			return uri.hasFragment();
		}
		
		@Override
		public Boolean hasPath() {
			return uri.hasPath();
		}
		
		@Override
		public Boolean hasQuery() {
			return uri.hasQuery();
		}
		
		@Override
		public String getAuthority() throws UnsupportedOperationException {
			return uri.getAuthority();
		}
		
		@Override
		public String getFragment() throws UnsupportedOperationException {
			return uri.getFragment();
		}
		
		@Override
		public String getPath() throws UnsupportedOperationException {
			return uri.getPath();
		}
		
		@Override
		public String getQuery() throws UnsupportedOperationException {
			return uri.getQuery();
		}
		
		@Override
		public String getScheme() {
			return uri.getScheme();
		}
		
		@Override
		public ISourceLocation setAuthority(String authority) throws URISyntaxException {
			return setURI(uri.setAuthority(authority));
		}
		
		@Override
		public ISourceLocation setFragment(String fragment) throws URISyntaxException {
			return setURI(uri.setFragment(fragment));
		}
		
		@Override
		public ISourceLocation setPath(String path) throws URISyntaxException {
			return setURI(uri.setPath(path));
		}
		
		@Override
		public ISourceLocation setQuery(String query) throws URISyntaxException {
			return setURI(uri.setQuery(query));
		}
		
		@Override
		public ISourceLocation setScheme(String scheme) throws URISyntaxException {
			return setURI(uri.setScheme(scheme));
		}
		
		@Override
		public URI getURI() {
			try {
				if (reverseLocationCacheLock.tryLock(10, TimeUnit.MILLISECONDS)) {
					try {
						URI result = reverseLocationCache.get(uri);
						if (result == null) {
							result = uri.getURI();
							try {
								// assure correct encoding, side effect of JRE's implementation of URIs
								result = new URI(result.toASCIIString());
							} catch (URISyntaxException e) {
							} 
							reverseLocationCache.put(uri, result);
						}
						return result; 
					}
					finally {
						reverseLocationCacheLock.unlock();
					}
				}
			} catch (InterruptedException e) {
			}
			// we could not get the lock, the cache was to busy, lets continue
			URI result = uri.getURI();
			try {
				// assure correct encoding, side effect of JRE's implementation of URIs
				result = new URI(result.toASCIIString());
			} catch (URISyntaxException e) {
			} 
			return result;
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
		public boolean hasLength() {
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
	
	private static class IntIntIntIntIntInt extends Complete {
		protected final int offset;
		protected final int length;
		protected final int beginLine;
		protected final int endLine;
		protected final int beginCol;
		protected final int endCol;
		
		private IntIntIntIntIntInt(IURI uri, int offset, int length, int beginLine, int endLine, int beginCol, int endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}
		
		@Override
		public boolean hasLength() {
			return true;
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new IntIntIntIntIntInt(newURI, offset, length, beginLine, endLine, beginCol, endCol);
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
		
		@Override
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
		
		@Override
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
	
	private static class CharCharByteByteByteByte extends Complete {
		protected final char offset;
		protected final char length;
		protected final byte beginLine;
		protected final byte endLine;
		protected final byte beginCol;
		protected final byte endCol;

		private CharCharByteByteByteByte(IURI uri, char offset, char length, byte beginLine, byte endLine, byte beginCol, byte endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}
		
		
		@Override
		public boolean hasLength() {
			return true;
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new CharCharByteByteByteByte(newURI, offset, length, beginLine, endLine, beginCol, endCol);
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
		
		@Override
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
		
		@Override
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
	
	private static class CharCharCharCharCharChar extends Complete {
		protected final char offset;
		protected final char length;
		protected final char beginLine;
		protected final char endLine;
		protected final char beginCol;
		protected final char endCol;
		
		private CharCharCharCharCharChar(IURI uri, char offset, char length, char beginLine, char endLine, char beginCol, char endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}
		
		@Override
		public boolean hasLength() {
			return true;
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new CharCharCharCharCharChar(newURI, offset, length, beginLine, endLine, beginCol, endCol);
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
		
		@Override
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
		
		@Override
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

	private static class OnlyURI extends Incomplete {
		
		private OnlyURI(IURI uri){
			super(uri);
		}

		@Override
		public int hashCode(){
			return uri.hashCode();
		}
		
		
		@Override
		public ISourceLocation setOffset(int offset)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset);
		}
		
		@Override
		public ISourceLocation setOffsetLength(int offset, int length)
				throws IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length);
		}

		@Override
		ISourceLocation setURI(IURI newURI) {
			return SourceLocationValues.newSourceLocation(newURI);
		}
		
		@Override
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				OnlyURI otherSourceLocation = (OnlyURI) o;
				return uri.equals(otherSourceLocation.uri);
			}
			
			return false;
		}
	}

	private static class IntIntIntIntByteByte extends Complete {
		protected final int offset;
		protected final int length;
		protected final int beginLine;
		protected final int endLine;
		protected final byte beginCol;
		protected final byte endCol;
		
		private IntIntIntIntByteByte(IURI uri, int offset, int length, int beginLine, int endLine, byte beginCol, byte endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}

		@Override
		public boolean hasLength() {
			return true;
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new IntIntIntIntByteByte(newURI, offset, length, beginLine, endLine, beginCol, endCol);
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
		
		@Override
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
		
		@Override
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

	private static class IntIntCharCharByteByte extends Complete {
		protected final int offset;
		protected final int length;
		protected final char beginLine;
		protected final char endLine;
		protected final byte beginCol;
		protected final byte endCol;
		
		private IntIntCharCharByteByte(IURI uri, int offset, int length, char beginLine, char endLine, byte beginCol, byte endCol){
			super(uri);
			
			this.offset = offset;
			this.length = length;
			this.beginLine = beginLine;
			this.endLine = endLine;
			this.beginCol = beginCol;
			this.endCol = endCol;
		}
		
		@Override
		public boolean hasLength() {
			return true;
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new IntIntCharCharByteByte(newURI, offset, length, beginLine, endLine, beginCol, endCol);
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
		
		@Override
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
		
		@Override
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

	private static class ByteByte extends Incomplete {
		protected final byte offset;
		protected final byte length;
		
		private ByteByte(IURI uri, byte offset, byte length){
			super(uri);
			
			this.offset = offset;
			this.length = length;
		}
		
		@Override
		public boolean hasLength() {
			return true;
		}
		
		@Override
		public ISourceLocation setLength(int length)
				throws UnsupportedOperationException, IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length);
		}
		
		@Override
		public ISourceLocation setBeginEnd(int beginLine, int endLine,
				int beginCol, int endCol) throws IllegalArgumentException,
				UnsupportedOperationException {
			return SourceLocationValues.newSourceLocation(this, offset, length, beginLine, endLine, beginCol, endCol);
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new ByteByte(newURI, offset, length);
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
		
		@Override
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		@Override
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

	private static class CharChar extends Incomplete {
		protected final char offset;
		protected final char length;
		
		private CharChar(IURI uri, char offset, char length){
			super(uri);
			
			this.offset = offset;
			this.length = length;
		}
		
		@Override
		public boolean hasLength() {
			return true;
		}
		
		@Override
		public ISourceLocation setLength(int length)
				throws UnsupportedOperationException, IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length);
		}
		
		@Override
		public ISourceLocation setBeginEnd(int beginLine, int endLine,
				int beginCol, int endCol) throws IllegalArgumentException,
				UnsupportedOperationException {
			return SourceLocationValues.newSourceLocation(this, offset, length, beginLine, endLine, beginCol, endCol);
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new CharChar(newURI, offset, length);
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
		
		@Override
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		@Override
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
	
	private static class IntInt extends Incomplete {
		protected final int offset;
		protected final int length;
		
		private IntInt(IURI uri, int offset, int length){
			super(uri);
			
			this.offset = offset;
			this.length = length;
		}
		
		@Override
		public boolean hasLength() {
			return length != -1;
		}
		
		@Override
		public ISourceLocation setLength(int length)
				throws UnsupportedOperationException, IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length);
		}
		
		@Override
		public ISourceLocation setBeginEnd(int beginLine, int endLine,
				int beginCol, int endCol) throws IllegalArgumentException,
				UnsupportedOperationException {
			return SourceLocationValues.newSourceLocation(this, offset, length, beginLine, endLine, beginCol, endCol);
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new IntInt(newURI, offset, length);
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
		
		@Override
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= (offset << 8);
			hash ^= (length << 29);
			
			return hash;
		}
		
		@Override
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

	private static class Byte extends Incomplete {
		protected final byte offset;
		
		private Byte(IURI uri, byte offset){
			super(uri);
			
			this.offset = offset;
		}
		
		@Override
		public ISourceLocation setLength(int length)
				throws UnsupportedOperationException, IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length);
		}
		
		@Override
		public boolean hasLength() {
			return false;
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new Byte(newURI, offset);
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
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= (offset << 8);
			
			return hash;
		}
		
		@Override
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				Byte otherSourceLocation = (Byte) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (offset == otherSourceLocation.offset));
			}
			
			return false;
		}
	}

	private static class Char extends Incomplete {
		protected final char offset;
		
		private Char(IURI uri, char offset){
			super(uri);
			
			this.offset = offset;
		}
		
		@Override
		public boolean hasLength() {
			return false;
		}
		
		@Override
		public ISourceLocation setLength(int length)
				throws UnsupportedOperationException, IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length);
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new Char(newURI, offset);
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
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= (offset << 8);
			
			return hash;
		}
		
		@Override
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				Char otherSourceLocation = (Char) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (offset == otherSourceLocation.offset));
			}
			
			return false;
		}
	}
	
	private static class Int extends Incomplete {
		protected final int offset;
		
		private Int(IURI uri, int offset){
			super(uri);
			
			this.offset = offset;
		}
		
		@Override
		public boolean hasLength() {
			return false;
		}
		
		@Override
		public ISourceLocation setLength(int length)
				throws UnsupportedOperationException, IllegalArgumentException {
			return SourceLocationValues.newSourceLocation(this, offset, length);
		}
		
		@Override
		ISourceLocation setURI(IURI newURI) {
			return new Int(newURI, offset);
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
		public int hashCode(){
			int hash = uri.hashCode();
			hash ^= (offset << 8);
			
			return hash;
		}
		
		@Override
		public boolean equals(Object o){
			if(o == null) return false;
			
			if(o.getClass() == getClass()){
				Int otherSourceLocation = (Int) o;
				return (uri.equals(otherSourceLocation.uri)
						&& (offset == otherSourceLocation.offset));
			}
			
			return false;
		}
	}

}
