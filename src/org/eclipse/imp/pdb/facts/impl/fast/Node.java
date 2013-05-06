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
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.impl.util.collections.ShareableValuesList;
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
/*package*/ class Node extends Value implements INode{
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

	public Type getType(){
		return NODE_TYPE;
	}
	
	public int arity(){
		return children.length;
	}
	
	public int positionalArity(){
		if(keyArgNames == null)
			return children.length;
		else
			return children.length - keyArgNames.length;
	}
	
	public IValue get(int i){
		return children[i];
	}
	
	public String getName(){
		return name;
	}
	
	@Override
	public boolean hasKeywordArguments(){
		return keyArgNames != null;
	}
	
	@Override
	public String[] getKeywordArgumentNames() {
		return keyArgNames;
	}
	
	@Override
	public int getKeywordIndex(String name){
		if(keyArgNames != null){
			for(int i = 0; i < keyArgNames.length; i++){
				if(name.equals(keyArgNames[i])){
					return children.length - keyArgNames.length + i;
				}
			}
		}
		return -1;
	}
	
	public IValue getKeywordArgumentValue(String name){
		if(keyArgNames != null){
			int k = getKeywordIndex(name);
			if(k >= 0)
				return children[k];
		}
		return null;
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
		return new ShareableHashMap<>();
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
	
	public INode replace(int first, int second, int end, IList repl)
			throws FactTypeUseException, IndexOutOfBoundsException {
		ShareableValuesList newChildren = new ShareableValuesList();
		int rlen = repl.length();
		int increment = Math.abs(second - first);
		if(first < end){
			int childIndex = 0;
			// Before begin
			while(childIndex < first){
				newChildren.append(children[childIndex++]);
			}
			int replIndex = 0;
			boolean wrapped = false;
			// Between begin and end
			while(childIndex < end){
				newChildren.append(repl.get(replIndex++));
				if(replIndex == rlen){
					replIndex = 0;
					wrapped = true;
				}
				childIndex++; //skip the replaced element
				for(int j = 1; j < increment && childIndex < end; j++){
					newChildren.append(children[childIndex++]);
				}
			}
			if(!wrapped){
				while(replIndex < rlen){
					newChildren.append(repl.get(replIndex++));
				}
			}
			// After end
			int dlen = children.length;
			while( childIndex < dlen){
				newChildren.append(children[childIndex++]);
			}
		} else {
			// Before begin (from right to left)
			int childIndex = children.length - 1;
			while(childIndex > first){
				newChildren.insert(children[childIndex--]);
			}
			// Between begin (right) and end (left)
			int replIndex = 0;
			boolean wrapped = false;
			while(childIndex > end){
				newChildren.insert(repl.get(replIndex++));
				if(replIndex == repl.length()){
					replIndex = 0;
					wrapped = true;
				}
				childIndex--; //skip the replaced element
				for(int j = 1; j < increment && childIndex > end; j++){
					newChildren.insert(children[childIndex--]);
				}
			}
			if(!wrapped){
				while(replIndex < rlen){
					newChildren.insert(repl.get(replIndex++));
				}
			}
			// Left of end
			while(childIndex >= 0){
				newChildren.insert(children[childIndex--]);
			}
		}
		return new Node(name, new ListWriter(VALUE_TYPE, newChildren).done());
	}

	
}
