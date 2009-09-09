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

import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;

/**
 * Specialized implementation for nodes with annotations.
 * 
 * @author Arnold Lankamp
 */
public class AnnotatedNode extends Node{
	protected final ShareableHashMap<String, IValue> annotations;
	
	protected AnnotatedNode(String name, IValue[] children, ShareableHashMap<String, IValue> annotations){
		super(name, children);
		
		this.annotations = annotations;
	}

	public INode set(int i, IValue arg){
		IValue[] newChildren = children.clone();
		newChildren[i] = arg;
		
		return new AnnotatedNode(name, newChildren, annotations);
	}
	
	public boolean hasAnnotation(String label){
		return annotations.containsKey(label);
	}
	
	public boolean hasAnnotations(){
		return true;
	}
	
	public IValue getAnnotation(String label){
		return annotations.get(label);
	}
	
	public Map<String, IValue> getAnnotations(){
		return new ShareableHashMap<String, IValue>(annotations);
	}
	
	public INode removeAnnotations(){
		return new Node(name, children);
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label, IValue value){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>(annotations);
		newAnnotations.put(label, value);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>(annotations);
		newAnnotations.remove(label);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(Map<String, IValue> newAnnos){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>(annotations);
		
		Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
		while(newAnnosIterator.hasNext()){
			Map.Entry<String, IValue> entry = newAnnosIterator.next();
			String key = entry.getKey();
			IValue value = entry.getValue();
			
			newAnnotations.put(key, value);
		}
		
		return newAnnotations;
	}
	
	public boolean equals(Object o){
		if(o == this) return true;
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			AnnotatedNode other = (AnnotatedNode) o;
			
			if(name != other.name) return false;
			
			IValue[] otherChildren = other.children;
			int nrOfChildren = children.length;
			if(otherChildren.length == nrOfChildren){
				for(int i = nrOfChildren - 1; i >= 0; i--){
					if(!otherChildren[i].equals(children[i])) return false;
				}
				return annotations.equals(other.annotations);
			}
		}
		
		return false;
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		if(name == null){ // Special case, which shouldn't be supported imo.
			sb.append(children[0]);
			return sb.toString();
		}
		
		sb.append(name);
		sb.append("(");
		
		int size = children.length;
		if(size > 0){
			int i = 0;
			sb.append(children[i]);
			
			for(i = 1; i < size; i++){
				sb.append(",");
				sb.append(children[i]);
			}
		}
		
		sb.append(")");
		
		// Annos
		sb.append('[');
		Map<String, IValue> annotations = getAnnotations();
		Iterator<Map.Entry<String, IValue>> annotationsIterator = annotations.entrySet().iterator();
		
		Map.Entry<String, IValue> entry = annotationsIterator.next();
		sb.append("@" + entry.getKey() + "=");
		sb.append(entry.getValue().toString());
		
		while(annotationsIterator.hasNext()){
			sb.append(",");
			
			entry = annotationsIterator.next();
			sb.append("@" + entry.getKey() + "=");
			sb.append(entry.getValue().toString());
		}
		sb.append(']');
		
		return sb.toString();
	}
}
