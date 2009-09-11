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

import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of IString.
 * 
 * @author Arnold Lankamp
 */
public class StringValue extends Value implements IString {
	private final static Type STRING_TYPE = TypeFactory.getInstance().stringType();
	
	protected final String value;
	
	protected StringValue(String value){
		super();
		
		this.value = value.intern();
	}

	public Type getType(){
		return STRING_TYPE;
	}
	
	public String getValue(){
		return value;
	}
	
	public IString concat(IString other){
		StringBuilder buffer = new StringBuilder();
		buffer.append(value);
		buffer.append(other.getValue());
		
		return ValueFactory.getInstance().string(buffer.toString());
	}
	
	public int compare(IString other){
		int result = value.compareTo(other.getValue());
		
		if(result > 0) return 1;
		if(result < 0) return -1;
		
		return 0;
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitString(this);
	}
	
	public int hashCode(){
		return value.hashCode();
	}
	
	public boolean equals(Object o){
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			StringValue otherString = (StringValue) o;
			return value == otherString.value; // Yes ==  works here since it has been interned.
		}
		
		return false;
	}
	
	public boolean isEqual(IValue value){
		return equals(value);
	}
}
