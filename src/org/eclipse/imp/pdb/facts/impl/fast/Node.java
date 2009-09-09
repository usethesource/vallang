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
import java.util.NoSuchElementException;

import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of INode.
 * 
 * @author Arnold Lankamp
 */
public class Node implements INode{
	protected final static Type NODE_TYPE = TypeFactory.getInstance().nodeType();
	
	protected final String name;
	protected final IValue[] children;
	
	protected Node(String name, IValue[] children){
		super();
		
		this.name = (name != null ? name.intern() : null); // Handle (weird) special case.
		this.children = children;
	}

	public Type getType(){
		return NODE_TYPE;
	}
	
	public int arity(){
		return children.length;
	}
	
	public IValue get(int i){
		return children[i];
	}
	
	public String getName(){
		return name;
	}
	
	public Iterator<IValue> iterator(){
		return new Iterator<IValue>(){
			private int i = 0;

			public boolean hasNext(){
				return i < children.length;
			}

			public IValue next(){
				if(!hasNext()) throw new NoSuchElementException("There are no more elements in this iteration.");
				
				return children[i++];
			}

			public void remove(){
				throw new UnsupportedOperationException("Removal is not supported by this iterator.");
			}
		};
	}
	
	public Iterable<IValue> getChildren(){
		return this;
	}

	public INode set(int i, IValue arg){
		IValue[] newChildren = children.clone();
		newChildren[i] = arg;
		
		return new Node(name, newChildren);
	}
	
	public <T> T accept(IValueVisitor<T> v) throws VisitorException{
		return v.visitNode(this);
	}
	
	public boolean hasAnnotation(String label){
		return false;
	}
	
	public boolean hasAnnotations(){
		return false;
	}
	
	public IValue getAnnotation(String label){
		return null;
	}
	
	public Map<String, IValue> getAnnotations(){
		return new ShareableHashMap<String, IValue>();
	}
	
	public INode setAnnotation(String label, IValue value){
		return new AnnotatedNode(name, children, getUpdatedAnnotations(label, value));
	}
	
	public INode setAnnotations(Map<String, IValue> newAnnos){
		return new AnnotatedNode(name, children, getSetAnnotations(newAnnos));
	}
	
	public INode joinAnnotations(Map<String, IValue> newAnnos){
		return new AnnotatedNode(name, children, getUpdatedAnnotations(newAnnos));
	}
	
	public INode removeAnnotation(String label){
		return new AnnotatedNode(name, children, getUpdatedAnnotations(label));
	}
	
	public INode removeAnnotations(){
		return this;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label, IValue value){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>();
		newAnnotations.put(label, value);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>();
		newAnnotations.remove(label);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(Map<String, IValue> newAnnos){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>();
		
		Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
		while(newAnnosIterator.hasNext()){
			Map.Entry<String, IValue> entry = newAnnosIterator.next();
			String key = entry.getKey();
			IValue value = entry.getValue();
			
			newAnnotations.put(key, value);
		}
		
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getSetAnnotations(Map<String, IValue> newAnnos){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<String, IValue>();
		
		Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
		while(newAnnosIterator.hasNext()){
			Map.Entry<String, IValue> entry = newAnnosIterator.next();
			String key = entry.getKey();
			IValue value = entry.getValue();
			
			newAnnotations.put(key, value);
		}
		
		return newAnnotations;
	}
	
	public int hashCode(){
		int hash = 0;
		
		for(int i = children.length - 1; i >= 0; i--){
			hash = (hash << 23) + (hash >> 5);
			hash ^= children[i].hashCode();
		}
		
		return hash;
	}
	
	public boolean equals(Object o){
		if(o == this) return true;
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			Node other = (Node) o;
			
			if(name != other.name) return false; // Yes '==' works here, since it has been interned.
			
			IValue[] otherChildren = other.children;
			int nrOfChildren = children.length;
			if(otherChildren.length == nrOfChildren){
				for(int i = nrOfChildren - 1; i >= 0; i--){
					if(!otherChildren[i].equals(children[i])) return false;
				}
				return true;
			}
		}
		
		return false;
	}
	
	public boolean isEqual(IValue value){
		if(value == this) return true;
		if(value == null) return false;
		
		if(value instanceof Node){
			Node other = (Node) value;
			
			if(name != other.name) return false; // Yes '==' works here, since it has been interned.
			
			IValue[] otherChildren = other.children;
			int nrOfChildren = children.length;
			if(otherChildren.length == nrOfChildren){
				for(int i = nrOfChildren - 1; i >= 0; i--){
					if(!otherChildren[i].isEqual(children[i])) return false;
				}
				return true;
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
		
		return sb.toString();
	}
}
