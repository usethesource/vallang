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

import java.net.URL;

import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IValue;
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
	
	protected final URL url;
	protected final int offset;
	protected final int length;
	protected final int beginLine;
	protected final int endLine;
	protected final int beginCol;
	protected final int endCol;
	
	protected SourceLocationValue(URL url, int offset, int length, int beginLine, int endLine, int beginCol, int endCol){
		super();
		
		this.url = url;
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
	
	public URL getURL(){
		return url;
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
		int hash = url.hashCode();
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
			return (url.toExternalForm().equals(otherSourceLocation.url.toExternalForm())
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
		StringBuilder buffer = new StringBuilder();
		
		buffer.append("loc(");
		buffer.append(url);
		buffer.append("?off=");
		buffer.append(offset);
		buffer.append("&len=");
		buffer.append(length);
		buffer.append("&start=");
		buffer.append(beginLine);
		buffer.append(",");
		buffer.append(endLine);
		buffer.append("&end=");
		buffer.append(beginCol);
		buffer.append(",");
		buffer.append(endCol);
		buffer.append(")");
		
		return buffer.toString();
    }
}


