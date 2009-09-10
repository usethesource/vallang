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

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of IBool.
 * 
 * @author Arnold Lankamp
 */
public class BoolValue extends Value implements IBool{
	private final static Type BOOL_TYPE = TypeFactory.getInstance().boolType();
	
	public final static BoolValue TRUE = new BoolValue(true);
	public final static BoolValue FALSE = new BoolValue(false);
	
	protected final boolean value;
	
	protected BoolValue(boolean value){
		super();
		
		this.value = value;
	}

	public Type getType(){
		return BOOL_TYPE;
	}
	
	public boolean getValue(){ 
		return value;
	}
	
	public IBool not(){
		return value ? FALSE : TRUE;
	}
	
	public IBool equivalent(IBool other){
		return (value == other.getValue()) ? TRUE : FALSE;
	}
	
	public IBool and(IBool other){
		return value ? other : FALSE;
	}
	
	public IBool or(IBool other){
		return value ? TRUE : other;
	}
	
	public IBool xor(IBool other){
		return (value ^ other.getValue()) ? TRUE : FALSE;
	}
	
	public IBool implies(IBool other){
		return value ? other : TRUE;
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitBoolean(this);
	}
	
	public static BoolValue getBoolValue(boolean bool){
		return bool ? TRUE : FALSE;
	}
	
	public int hashCode(){
		return (value ? 1 : 0);
	}
	
	public boolean equals(Object o){
		return this == o;
	}
	
	public boolean isEqual(IValue value){
		return this == value;
	}
	
	public String getStringRepresentation(){
		return toString();
	}
}
