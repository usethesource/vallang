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

import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.fast.AnnotatedNode;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;

/**
 * Specialized implementation for shareable nodes with annotations.
 * 
 * @author Arnold Lankamp
 */
public class SharedAnnotatedNode extends AnnotatedNode implements IShareable{
	
	public SharedAnnotatedNode(String name, IValue[] children, ShareableHashMap<String, IValue> annotations){
		super(name, children, annotations);
	}
	
	public SharedAnnotatedNode(String name, IValue[] children, Map<String, IValue> annotations){
		super(name, children, importAnnos(annotations));
	}

	public INode set(int i, IValue arg){
		IValue[] newChildren = children.clone();
		newChildren[i] = arg;
		
		return SharedValueFactory.getInstance().createAnnotatedNodeUnsafe(name, newChildren, annotations);
	}
	
	public INode setAnnotation(String label, IValue value){
		return SharedValueFactory.getInstance().createAnnotatedNodeUnsafe(name, children, getUpdatedAnnotations(label, value));
	}
	
	public INode setAnnotations(Map<String, IValue> newAnnos){
		return SharedValueFactory.getInstance().createAnnotatedNodeUnsafe(name, children, getUpdatedAnnotations(newAnnos));
	}
	
	public INode joinAnnotations(Map<String, IValue> newAnnos){
		return SharedValueFactory.getInstance().createAnnotatedNodeUnsafe(name, children, getUpdatedAnnotations(newAnnos));
	}
	
	public INode removeAnnotation(String label){
		return SharedValueFactory.getInstance().createAnnotatedNodeUnsafe(name, children, getUpdatedAnnotations(label));
	}
	
	public INode removeAnnotations(){
		return SharedValueFactory.getInstance().createNodeUnsafe(name, children);
	}
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
