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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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
    protected static final HashMap<String, IValue> EMPTY_ANNOTATIONS = new HashMap<String,IValue>();
	protected final IValue[] fChildren;
    protected final String fName;
	protected final HashMap<String, IValue> fAnnotations;
	
	/*package*/ Node(String name, IValue[] children) {
		super(TypeFactory.getInstance().nodeType());
		fName = name;
		fChildren = new IValue[children.length];
		System.arraycopy(children, 0, fChildren, 0, children.length);
		fAnnotations = EMPTY_ANNOTATIONS;
	}
	
	protected Node(String name, Type type, IValue[] children) {
		super(type);
		fName = name;
		fChildren = new IValue[children.length];
		System.arraycopy(children, 0, fChildren, 0, children.length);
		fAnnotations = EMPTY_ANNOTATIONS;
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
		fChildren = new IValue[other.fChildren.length];
		System.arraycopy(other.fChildren, 0, fChildren, 0, other.fChildren.length);
		fAnnotations = (HashMap<String, IValue>) other.fAnnotations.clone();
		fAnnotations.put(label, anno);
	}
	
	/**
	 * Removes one annotation
	 */
	@SuppressWarnings("unchecked")
	protected Node(Node other, String label) {
		super(other.fType);
		fName = other.fName;
		fChildren = new IValue[other.fChildren.length];
		System.arraycopy(other.fChildren, 0, fChildren, 0, other.fChildren.length);
		fAnnotations = (HashMap<String, IValue>) other.fAnnotations.clone();
		fAnnotations.remove(label);
	}
	
	/**
	 * Removes all annotations
	 * @param other
	 */
	protected Node(Node other) {
		super(other.fType);
		fName = other.fName;
		fChildren = new IValue[other.fChildren.length];
		System.arraycopy(other.fChildren, 0, fChildren, 0, other.fChildren.length);
		fAnnotations = EMPTY_ANNOTATIONS;
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
		super(other);
		fName = other.fName;
		fChildren = other.fChildren.clone();
		fChildren[index] = newChild;
		fAnnotations = (HashMap<String, IValue>) other.fAnnotations.clone();
	}
	
	/**
	 * Adds all annotations to the annotations of the other
	 * @param other
	 * @param annotations
	 */
	@SuppressWarnings("unchecked")
	public Node(Node other, Map<String, IValue> annotations) {
		super(other);
		fName = other.fName;
		fChildren = other.fChildren.clone();
		fAnnotations = (HashMap<String, IValue>) other.fAnnotations.clone();
		fAnnotations.putAll(annotations);
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
		if (getClass() == obj.getClass()) {
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
	
	@Override
	public int hashCode() {
       int hash = fName != null ? fName.hashCode() : 0;
       
	   for (int i = 0; i < fChildren.length; i++) {
	     hash = (hash << 1) ^ (hash >> 1) ^ fChildren[i].hashCode();
	   }
	   return hash;
	}

	public boolean hasAnnotation(String label) {
		return fAnnotations.containsKey(label);
	}

	public INode setAnnotation(String label, IValue value) {
		IValue previous = getAnnotation(label);
		
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
}
