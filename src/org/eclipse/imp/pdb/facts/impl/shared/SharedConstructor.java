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
package org.eclipse.imp.pdb.facts.impl.shared;

import java.util.Map;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.Constructor;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * Implementation of shareable constructors.
 * 
 * @author Arnold Lankamp
 */
public class SharedConstructor extends Constructor implements IShareable{
	
	protected SharedConstructor(Type constructorType, IValue[] children){
		super(constructorType, children);
	}
	
	public IConstructor set(int i, IValue newChild){
		int nrOfChildren = children.length;
		IValue[] newChildren = new IValue[nrOfChildren];
		System.arraycopy(children, 0, newChildren, 0, nrOfChildren);
		
		newChildren[i] = newChild;
		
		return SharedValueFactory.getInstance().createConstructorUnsafe(constructorType, newChildren);
	}
	
	public IConstructor set(String label, IValue newChild){
		int nrOfChildren = children.length;
		IValue[] newChildren = new IValue[nrOfChildren];
		System.arraycopy(children, 0, newChildren, 0, nrOfChildren);
		
		newChildren[constructorType.getFieldIndex(label)] = newChild;
		
		return SharedValueFactory.getInstance().createConstructorUnsafe(constructorType, newChildren);
	}
	
	public IConstructor setAnnotation(String label, IValue value){
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, children, getUpdatedAnnotations(label, value));
	}
	
	public IConstructor setAnnotations(Map<String, IValue> newAnnos){
		if(newAnnos.isEmpty()) return this;
		
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, children, getUpdatedAnnotations(newAnnos));
	}
	
	public IConstructor joinAnnotations(Map<String, IValue> newAnnos){
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, children, getUpdatedAnnotations(newAnnos));
	}
	
	public IConstructor removeAnnotation(String label){
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, children, getUpdatedAnnotations(label));
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
