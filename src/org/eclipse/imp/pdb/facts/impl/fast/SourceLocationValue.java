/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.io.StandardTextWriter;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of ISourceLocation.
 * 
 * @author Arnold Lankamp
 */
public class SourceLocationValue implements ISourceLocation{
	private final static Type SOURCE_LOCATION_TYPE = TypeFactory.getInstance().sourceLocationType();
	
	protected final URI uri;
	protected final int offset;
	protected final int length;
	protected final int beginLine;
	protected final int endLine;
	protected final int beginCol;
	protected final int endCol;
	
	protected SourceLocationValue(URI uri, int offset, int length, int beginLine, int endLine, int beginCol, int endCol){
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
		return SOURCE_LOCATION_TYPE;
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
			SourceLocationValue otherSourceLocation = (SourceLocationValue) o;
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
	
	public String toString(){
		try {
    		ByteArrayOutputStream stream = new ByteArrayOutputStream();
    		new StandardTextWriter().write(this, stream);
			return stream.toString();
		} catch (IOException e) {
			// this never happens
			return null;
		} 
    }
}


