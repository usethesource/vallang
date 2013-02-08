/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   jurgen@vinju.org
*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedAnnotationTypeException;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Naive implementation of an untyped tree node, using array of children.
 */
public class Node extends Value implements INode {
	protected final static Type VALUE_TYPE = TypeFactory.getInstance().valueType();
    protected static final HashMap<String, IValue> EMPTY_ANNOTATIONS = new HashMap<String,IValue>();
	protected final IValue[] fChildren;
    protected final String fName;
	protected final HashMap<String, IValue> fAnnotations;
	protected int fHash = 0;
	protected final String[] keyArgNames;
	
	/*package*/ Node(String name, IValue[] children) {
		super(TypeFactory.getInstance().nodeType());
		fName = name;
		fChildren = children.clone();
		fAnnotations = EMPTY_ANNOTATIONS;
		keyArgNames = null;
	}
	
	/*package*/ Node(String name, Map<String,IValue> annos, IValue[] children) {
		super(TypeFactory.getInstance().nodeType());
		fName = name;
		fChildren = children.clone();
		fAnnotations = new HashMap<String,IValue>(annos.size());
		fAnnotations.putAll(annos);
		keyArgNames = null;
	}
	
	protected Node(String name, Type type, IValue[] children) {
		super(type);
		fName = name;
		fChildren = children.clone();
		fAnnotations = EMPTY_ANNOTATIONS;
		keyArgNames = null;
	}
	
	/*package*/ Node(String name, IValue[] children, Map<String, IValue> keyArgValues){
		super(TypeFactory.getInstance().nodeType());
		
		this.fName = (name != null ? name.intern() : null); // Handle (weird) special case.
		fAnnotations = EMPTY_ANNOTATIONS;
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
			this.fChildren = extendedChildren;
			this.keyArgNames = keyArgNames;
		} else {
			this.fChildren = children;
			this.keyArgNames = null;
		}
	}
	
	/**
	 * Adds one annotation
	 * @param other
	 * @param label
	 * @param anno
	 */
	@SuppressWarnings("unchecked")
	protected Node(Node other, String label, IValue anno) {
		super(other.fType);
		fName = other.fName;
		fChildren = other.fChildren;
		fAnnotations = (HashMap<String, IValue>) other.fAnnotations.clone();
		fAnnotations.put(label, anno);
		keyArgNames = null;
	}
	
	/**
	 * Removes one annotation
	 */
	@SuppressWarnings("unchecked")
	protected Node(Node other, String label) {
		super(other.fType);
		fName = other.fName;
		fChildren = other.fChildren;
		fAnnotations = (HashMap<String, IValue>) other.fAnnotations.clone();
		fAnnotations.remove(label);
		keyArgNames = null;
	}
	
	/**
	 * Removes all annotations
	 * @param other
	 */
	protected Node(Node other) {
		super(other.fType);
		fName = other.fName;
		fChildren = other.fChildren;
		fAnnotations = EMPTY_ANNOTATIONS;
		keyArgNames = null;
	}
	
	/*package*/ Node(String name) {
		this(name, new IValue[0]);
	}

	/**
	 * Replaces a child
	 * @param other
	 * @param index
	 * @param newChild
	 */
	@SuppressWarnings("unchecked")
	protected Node(Node other, int index, IValue newChild) {
		super(other.fType);
		fName = other.fName;
		fChildren = other.fChildren.clone();
		fChildren[index] = newChild;
		fAnnotations = (HashMap<String, IValue>) other.fAnnotations.clone();
		keyArgNames = null;
	}
	
	/**
	 * Adds all annotations to the annotations of the other
	 * @param other
	 * @param annotations
	 */
	@SuppressWarnings("unchecked")
	public Node(Node other, Map<String, IValue> annotations) {
		super(other.fType);
		fName = other.fName;
		fChildren = other.fChildren.clone();
		fAnnotations = (HashMap<String, IValue>) other.fAnnotations.clone();
		fAnnotations.putAll(annotations);
		keyArgNames = null;
	}

	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitNode(this);
	}

	public int arity() {
		return fChildren.length;
	}

	public IValue get(int i) throws IndexOutOfBoundsException {
		try {
		 return fChildren[i];
		}
		catch (ArrayIndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Node node does not have child at pos " + i);
		}
	}

	public Iterable<IValue> getChildren() {
		return this;
	}

	public String getName() {
		return fName;
	}

	public  INode set(int i, IValue newChild) throws IndexOutOfBoundsException {
		try {
			return new Node(this, i, newChild);
		} catch (ArrayIndexOutOfBoundsException e) {
			throw new IndexOutOfBoundsException("Node node does not have child at pos " + i);
		}
	}

	public Iterator<IValue> iterator() {
		return new Iterator<IValue>() {
			private int i = 0;

			public boolean hasNext() {
				return i < fChildren.length;
			}

			public IValue next() {
				return fChildren[i++];
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
	
	@Override
	public boolean equals(Object obj) {
		  if(this == obj) {
			  return true;
		  }
		  else if(obj == null) {
			  return false;
		  }
		  else if (getClass() == obj.getClass()) {
			Node other = (Node) obj;
			
			if (!fType.comparable(other.fType)) {
				return false;
			}
			
			if (fChildren.length != other.fChildren.length) {
				return false;		
			}
			
			if (fName == other.fName || (fName != null && fName.equals(other.fName))) {
				for (int i = 0; i < fChildren.length; i++) {
					if (!fChildren[i].equals(other.fChildren[i])) {
						return false;
					}
				}
			
				return true;
			}
		}
		
		return false;
	}
	
	public int computeHashCode() {
       int hash = fName != null ? fName.hashCode() : 0;
       
	   for (int i = 0; i < fChildren.length; i++) {
	     hash = (hash << 1) ^ (hash >> 1) ^ fChildren[i].hashCode();
	   }
	   return hash;
	}

	@Override
	public int hashCode() {
		if (fHash == 0) {
			fHash = computeHashCode();
		}
		return fHash;
	}

	public boolean hasAnnotation(String label) {
		return fAnnotations.containsKey(label);
	}

	public INode setAnnotation(String label, IValue value) {
		IValue previous = getAnnotation(label);
		
		if (value == null) {
			throw new NullPointerException();
		}
		
		if (previous != null) {
			Type expected = previous.getType();
	
			if (!expected.comparable(value.getType())) {
				throw new UnexpectedAnnotationTypeException(expected, value.getType());
			}
		}
	
		return new Node(this, label, value);
	}

	public IValue getAnnotation(String label) throws FactTypeUseException {
		return fAnnotations.get(label);
	}

	public Map<String, IValue> getAnnotations() {
		return Collections.unmodifiableMap(fAnnotations);
	}

	public boolean hasAnnotations() {
		return fAnnotations != null && !fAnnotations.isEmpty();
	}

	public INode joinAnnotations(Map<String, IValue> annotations) {
		return new Node(this, annotations);
	}

	public INode setAnnotations(Map<String, IValue> annotations) {
		return removeAnnotations().joinAnnotations(annotations);
	}

	public INode removeAnnotation(String key) {
		return new Node(this, key);
	}

	public INode removeAnnotations() {
		return new Node(this);
	}
	
	public INode replace(int first, int second, int end, IList repl)
			throws FactTypeUseException, IndexOutOfBoundsException {
		ArrayList<IValue> newChildren = new ArrayList<IValue>();
		int rlen = repl.length();
		int increment = Math.abs(second - first);
		if(first < end){
			int childIndex = 0;
			// Before begin
			while(childIndex < first){
				newChildren.add(fChildren[childIndex++]);
			}
			int replIndex = 0;
			boolean wrapped = false;
			// Between begin and end
			while(childIndex < end){
				newChildren.add(repl.get(replIndex++));
				if(replIndex == rlen){
					replIndex = 0;
					wrapped = true;
				}
				childIndex++; //skip the replaced element
				for(int j = 1; j < increment && childIndex < end; j++){
					newChildren.add(fChildren[childIndex++]);
				}
			}
			if(!wrapped){
				while(replIndex < rlen){
					newChildren.add(repl.get(replIndex++));
				}
			}
			// After end
			int dlen = fChildren.length;
			while( childIndex < dlen){
				newChildren.add(fChildren[childIndex++]);
			}
		} else {
			// Before begin (from right to left)
			int childIndex = fChildren.length - 1;
			while(childIndex > first){
				newChildren.add(0, fChildren[childIndex--]);
			}
			// Between begin (right) and end (left)
			int replIndex = 0;
			boolean wrapped = false;
			while(childIndex > end){
				newChildren.add(0, repl.get(replIndex++));
				if(replIndex == repl.length()){
					replIndex = 0;
					wrapped = true;
				}
				childIndex--; //skip the replaced element
				for(int j = 1; j < increment && childIndex > end; j++){
					newChildren.add(0, fChildren[childIndex--]);
				}
			}
			if(!wrapped){
				while(replIndex < rlen){
					newChildren.add(0, repl.get(replIndex++));
				}
			}
			// Left of end
			while(childIndex >= 0){
				newChildren.add(0, fChildren[childIndex--]);
			}
		}

        IValue[] childArray = new IValue[newChildren.size()];
        newChildren.toArray(childArray);	
		return new Node(fName, childArray);
	}

	@Override
	public IValue getKeywordArgumentValue(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasKeywordArguments() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String[] getKeywordArgumentNames() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getKeywordIndex(String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int positionalArity() {
		// TODO Auto-generated method stub
		return 0;
	}
}
