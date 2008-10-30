/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.impl.hash;

import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.WritableValue;
import org.eclipse.imp.pdb.facts.impl.WriterBase;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.ListType;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.RelationType;
import org.eclipse.imp.pdb.facts.type.SetType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

public class List extends WritableValue<IListWriter> implements IList {
	/* package */LinkedList<IValue> fList = new LinkedList<IValue>();

	static private class ListWriter extends WriterBase<IListWriter> implements
			IListWriter {
		private List fValue;

		public ListWriter(List value) {
			super(value);
			fValue = value;
		}

		public IList getList() {
			return fValue;
		}

		public void insert(IValue tuple) throws FactTypeError {
			fValue.checkInsert(tuple);
			fValue.fList.add(0, tuple);
		}

		public void insertAll(IList other) throws FactTypeError {
		    fValue.checkInsert(other.getElementType());
			fValue.fList.addAll(0, ((List) other).fList);
		}

		public void insertAll(IRelation other) throws FactTypeError {
			SetType st = ((RelationType) other.getType().getBaseType()).toSet();
			fValue.checkInsert(st.getElementType());
			for (ITuple t : other) {
				fValue.fList.add(0, t);
			}
		}

		public void insertAll(ISet set) throws FactTypeError {
			fValue.checkInsert(set.getElementType());
			for (IValue t : set) {
				fValue.fList.add(0, t);
			}
		}
		
		public void insertAll(Iterable<? extends IValue> collection) throws FactTypeError {
			for (IValue v : collection) {
				insert(v);
			}
		}

		public void append(IValue e) throws FactTypeError {
			fValue.checkInsert(e);
			fValue.fList.add(e);
		}

	}

	private Type fEltType;

	/* package */List(Type eltType) {
		super(TypeFactory.getInstance().listType(eltType));
		this.fEltType = eltType;
	}
	
	/* package */List(NamedType namedType) throws FactTypeError {
		super(namedType);
		
		if (!namedType.getBaseType().isListType()) {
			throw new FactTypeError("Type " + namedType + " is not a list");
		}
		
		ListType base = (ListType) namedType.getBaseType();
		
		this.fEltType = base.getElementType();
	}

	public Type getElementType() {
		return fEltType;
	}

	@Override
	protected IListWriter createWriter() {
		return new ListWriter(this);
	}

	@SuppressWarnings("unchecked")
	public IList append(IValue e) throws FactTypeError {
		List result = new List(checkInsert(e).getElementType());
		IListWriter w = result.getWriter();
		
		result.fList = (LinkedList<IValue>) fList.clone();
		w.append(e);
		w.done();
		return result;
	}

	

	public IValue get(int i) {
		return fList.get(i);
	}

	@SuppressWarnings("unchecked")
	public IList insert(IValue e) throws FactTypeError {
		List result = new List(checkInsert(e).getElementType());
		IListWriter w = result.getWriter();
		
		result.fList = (LinkedList<IValue>) fList.clone();
		w.insert(e);
		w.done();
		return result;
	}

	public int length() {
		return fList.size();
	}

	public IList reverse() {
		List result = new List(getElementType());
		IListWriter w = result.getWriter();
		try {
			for (IValue v : this) {
				w.insert(v);
			}
		} catch (FactTypeError e) {
			// this will never happen
		} 
		w.done();
		return result;
	}

	public Iterator<IValue> iterator() {
		return fList.iterator();
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[ ");
		int idx = 0;
		for (IValue a : this) {
			if (idx++ > 0)
				sb.append(", ");
			sb.append(a.toString());
		}
		sb.append(" ]");
		return sb.toString();
	}

	private ListType checkInsert(IValue e) throws FactTypeError {
		checkInsert(e.getType());
		return (ListType) getType().getBaseType();
	}
	
	private void checkInsert(Type eltType) throws FactTypeError {
		if (!eltType.isSubtypeOf(fEltType)) {
			throw new FactTypeError("Element type " + eltType + " is not compatible with " + fEltType);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		// TODO: should we allow IList here?
		// TODO: should the types be equal?
		if (!(o instanceof List)) {
			return false;
		}
		List other = (List) o;
		return fList.equals(other.fList);
	}
	
	public IValue accept(IValueVisitor v) throws VisitorException {
		return v.visitList(this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected Object clone() throws CloneNotSupportedException {
		List tmp;
		
		if (getType() instanceof NamedType) {
		    tmp =  new List((NamedType) getType());
		}
		else {
			tmp = new List(getElementType());
		}
	
		// we don't have to clone fList if this instance is not mutable anymore,
		// otherwise we certainly do, to prevent modification of the original list.
		if (isMutable()) {
			tmp.fList = (LinkedList<IValue>) fList.clone();
		}
		else {
			tmp.fList = fList;
			tmp.getWriter().done();
		}
		
		return tmp;
	}

	public boolean isEmpty() {
		return fList.isEmpty();
	}
}
