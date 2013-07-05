/*******************************************************************************
* Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Arnold Lankamp - interfaces and implementation
*    Paul Klint - Implemented replace
*******************************************************************************/
package org.eclipse.imp.pdb.facts.impl.fast;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.AbstractNode;
import org.eclipse.imp.pdb.facts.impl.AbstractValue;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

/**
 * Implementation of INode.
 * 
 * @author Arnold Lankamp
 */
/*package*/ class Node extends AbstractNode implements INode {
	protected final static Type NODE_TYPE = TypeFactory.getInstance().nodeType();
	protected final static Type VALUE_TYPE = TypeFactory.getInstance().valueType();
	
	protected final String name;
	protected final IValue[] children;
	protected final String[] keyArgNames;
	
	/*package*/ Node(String name, IValue[] children){
		super();
		
		this.name = (name != null ? name.intern() : null); // Handle (weird) special case.
		this.children = children;
		this.keyArgNames = null;
	}

	/*package*/ public Node(String name, IList children) {
		super();
		IValue[] childArray = new IValue[children.length()];
		this.name = (name != null ? name.intern() : null); // Handle (weird) special case.
		for(int i = 0; i < childArray.length; i++){
			childArray[i] = children.get(i);
		}
		this.children = childArray;
		this.keyArgNames = null;
	}
	
	/*package*/ Node(String name, IValue[] children, Map<String, IValue> keyArgValues){
		super();
		
		this.name = (name != null ? name.intern() : null); // Handle (weird) special case.
		
		if(keyArgValues != null){
			int nkw = keyArgValues.size();
			IValue[] extendedChildren = new IValue[children.length + nkw];
			for(int i = 0; i < children.length;i++){
				extendedChildren[i] = children[i];
			}

			String keyArgNames[]= new String[nkw];
			int k = 0;
			for(String kw : keyArgValues.keySet()){
				keyArgNames[k++] = kw;
			}
			for(int i = 0; i < nkw; i++){
				extendedChildren[children.length + i] = keyArgValues.get(keyArgNames[i]);
			}
			this.children = extendedChildren;
			this.keyArgNames = keyArgNames;
		} else {
			this.children = children;
			this.keyArgNames = null;
		}
	}

	@Override
	public Type getType(){
		return NODE_TYPE;
	}
	
	@Override
	public int arity(){
		return children.length;
	}

	@Override
	protected IValueFactory getValueFactory() {
		return ValueFactory.getInstance();
	}

	@Override
	public IValue get(int i){
		return children[i];
	}
	
	@Override
	public String getName(){
		return name;
	}
	
	@Override
	public String[] getKeywordArgumentNames() {
		return keyArgNames;
	}
	
	@Override
	public Iterator<IValue> iterator(){
		return new Iterator<IValue>(){
			private int i = 0;

			@Override
			public boolean hasNext(){
				return i < children.length;
			}

			@Override
			public IValue next(){
				if(!hasNext()) throw new NoSuchElementException("There are no more elements in this iteration.");
				
				return children[i++];
			}

			@Override
			public void remove(){
				throw new UnsupportedOperationException("Removal is not supported by this iterator.");
			}
		};
	}
	
	@Override
	public Iterable<IValue> getChildren(){
		return this;
	}

	@Override
	public INode set(int i, IValue arg){
		IValue[] newChildren = children.clone();
		newChildren[i] = arg;
		
		return new Node(name, newChildren);
	}
	
	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E{
		return v.visitNode(this);
	}
	
	@Override
	public boolean hasAnnotation(String label){
		return false;
	}
	
	@Override
	public boolean hasAnnotations(){
		return false;
	}
	
	@Override
	public IValue getAnnotation(String label){
		return null;
	}
	
	@Override
	public Map<String, IValue> getAnnotations(){
		return new ShareableHashMap<>();
	}
	
	@Override
	public INode setAnnotation(String label, IValue value){
		return new AnnotatedNode(name, children, getUpdatedAnnotations(label, value));
	}
	
	@Override
	public INode setAnnotations(Map<String, IValue> newAnnos){
		return new AnnotatedNode(name, children, getSetAnnotations(newAnnos));
	}
	
	@Override
	public INode joinAnnotations(Map<String, IValue> newAnnos){
		return new AnnotatedNode(name, children, getUpdatedAnnotations(newAnnos));
	}
	
	@Override
	public INode removeAnnotation(String label){
		return new AnnotatedNode(name, children, getUpdatedAnnotations(label));
	}
	
	@Override
	public INode removeAnnotations(){
		return this;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label, IValue value){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		newAnnotations.put(label, value);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(String label){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		newAnnotations.remove(label);
		return newAnnotations;
	}
	
	protected ShareableHashMap<String, IValue> getUpdatedAnnotations(Map<String, IValue> newAnnos){
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		
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
		ShareableHashMap<String, IValue> newAnnotations = new ShareableHashMap<>();
		
		Iterator<Map.Entry<String, IValue>> newAnnosIterator = newAnnos.entrySet().iterator();
		while(newAnnosIterator.hasNext()){
			Map.Entry<String, IValue> entry = newAnnosIterator.next();
			String key = entry.getKey();
			IValue value = entry.getValue();
			
			newAnnotations.put(key, value);
		}
		
		return newAnnotations;
	}

	@Override
	public int hashCode(){
		int hash = 0;
		
		for(int i = children.length - 1; i >= 0; i--){
			hash = (hash << 23) + (hash >> 5);
			hash ^= children[i].hashCode();
		}
		return hash;
	}

	@Override
	public boolean equals(Object o){
		if(o == this) return true;
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			Node other = (Node) o;
			
			if(name != other.name) return false; // Yes '==' works here, since it has been interned.
			
			IValue[] otherChildren = other.children;
			int nrOfChildren = children.length;
			if(otherChildren.length == nrOfChildren){
				int nrOfPosChildren = positionalArity();
				if(other.positionalArity() != nrOfPosChildren){
					return false;
				}
				for(int i = nrOfPosChildren - 1; i >= 0; i--){
					if(!otherChildren[i].equals(children[i])) return false;
				}
				if(nrOfPosChildren < nrOfChildren){
					if(keyArgNames == null)
						return false;
					for(int i = 0; i < keyArgNames.length; i++){
						String kw = keyArgNames[i];
						int k = other.getKeywordIndex(kw);
						if(k < 0 || !children[i].equals(otherChildren[k])){
							return false;
						}
					}
				}
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean isEqual(IValue value){
		if(value == this) return true;
		if(value == null) return false;
		
		if(value instanceof Node){
			Node other = (Node) value;
			
			if(name != other.name) {
				return false; // Yes '==' works here, since it has been interned.
			}
			
			IValue[] otherChildren = other.children;
			int nrOfChildren = children.length;
			
			if(otherChildren.length == nrOfChildren){
				int nrOfPosChildren = positionalArity();
				if(other.positionalArity() != nrOfPosChildren){
					return false;
				}
				for(int i = nrOfPosChildren - 1; i >= 0; i--){
					if(!otherChildren[i].isEqual(children[i])) return false;
				}
				if(nrOfPosChildren < nrOfChildren){
					if(keyArgNames == null)
						return false;
					for(int i = 0; i < keyArgNames.length; i++){
						String kw = keyArgNames[i];
						int k = other.getKeywordIndex(kw);
						if(k < 0 || !children[nrOfPosChildren + i].isEqual(otherChildren[k])){
							return false;
						}
					}
				}
				return true;
			}
		}
		return false;
	}
	
}
