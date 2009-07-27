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
import org.eclipse.imp.pdb.facts.impl.fast.AnnotatedConstructor;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;

/**
 * Specialized implementation for shareable constructors with annotations.
 * 
 * @author Arnold Lankamp
 */
public class SharedAnnotatedConstructor extends AnnotatedConstructor implements IShareable{
	
	protected SharedAnnotatedConstructor(Type constructorType, IValue[] children, ShareableHashMap<String, IValue> annotations){
		super(constructorType, children, annotations);
	}
	
	public IConstructor set(int i, IValue arg){
		IValue[] newChildren = children.clone();
		newChildren[i] = arg;
		
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, newChildren, annotations);
	}
	
	public IConstructor set(String label, IValue newChild){
		IValue[] newChildren = children.clone();
		newChildren[constructorType.getFieldIndex(label)] = newChild;
		
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, newChildren, annotations);
	}
	
	public IConstructor setAnnotation(String label, IValue value){
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, children, getUpdatedAnnotations(label, value));
	}
	
	public IConstructor setAnnotations(Map<String, IValue> newAnnos){
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, children, getUpdatedAnnotations(newAnnos));
	}
	
	public IConstructor joinAnnotations(Map<String, IValue> newAnnos){
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, children, getUpdatedAnnotations(newAnnos));
	}
	
	public IConstructor removeAnnotation(String label){
		return SharedValueFactory.getInstance().createAnnotatedConstructorUnsafe(constructorType, children, getUpdatedAnnotations(label));
	}
	
	public IConstructor removeAnnotations(){
		return SharedValueFactory.getInstance().createConstructorUnsafe(constructorType, children);
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
