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
import org.eclipse.imp.pdb.facts.impl.fast.Node;
import org.eclipse.imp.pdb.facts.impl.util.sharing.IShareable;

/**
 * Implementation of shareable nodes.
 * 
 * @author Arnold Lankamp
 */
public class SharedNode extends Node implements IShareable{
	
	protected SharedNode(String name, IValue[] children){
		super(name, children);
	}
	
	public INode set(int i, IValue arg){
		int nrOfChildren = children.length;
		IValue[] newChildren = new IValue[nrOfChildren];
		System.arraycopy(children, 0, newChildren, 0, nrOfChildren);
		
		newChildren[i] = arg;
		
		return SharedValueFactory.getInstance().createNodeUnsafe(name, newChildren);
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
	
	public boolean equivalent(IShareable shareable){
		return super.equals(shareable);
	}
	
	public boolean equals(Object o){
		return (this == o);
	}
}
