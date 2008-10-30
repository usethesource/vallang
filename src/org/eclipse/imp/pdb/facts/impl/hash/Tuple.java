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

import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.impl.Value;
import org.eclipse.imp.pdb.facts.type.NamedType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Tuple extends Value implements ITuple {
    protected IValue[] fElements;

    /*package*/ Tuple(Type type) {
    	super(type);
    }

    /*package*/ Tuple(IValue[] elements) {
	super(TypeFactory.getInstance().tupleTypeOf(elements));
		this.fElements= elements;
    }

    Tuple(IValue a) {
    	this(new IValue[] { a });
    }

    Tuple(IValue a, IValue b) {
	this(new IValue[] { a, b });
    }

    Tuple(IValue a, IValue b, IValue c) {
	this(new IValue[] { a, b, c });
    }

    Tuple(IValue a, IValue b, IValue c, IValue d) {
	this(new IValue[] { a, b, c, d });
    }

    Tuple(IValue a, IValue b, IValue c, IValue d, IValue e) {
	this(new IValue[] { a, b, c, d, e });
    }

    Tuple(IValue a, IValue b, IValue c, IValue d, IValue e, IValue f) {
	this(new IValue[] { a, b, c, d, e, f });
    }

    Tuple(IValue a, IValue b, IValue c, IValue d, IValue e, IValue f, IValue g) {
	this(new IValue[] { a, b, c, d, e, f, g });
    }

    public Tuple(NamedType type, IValue[] tmp) {
		super(type);
		this.fElements = tmp;
	}

	public int arity() {
        return fElements.length;
    }

    public IValue get(int i) {
        return fElements[i];
    }
    
    public IValue get(String label) {
		return fElements[((TupleType) fType).getFieldIndex(label)];
	}


    public Iterator<IValue> iterator() {
        return new Iterator<IValue>() {
            private int count= 0;

            public boolean hasNext() {
                return count < arity();
            }

            public IValue next() {
                return get(count++);
            }

            public void remove() {
                throw new UnsupportedOperationException("Can not remove elements from a tuple");
            }
        };
    }

    public String toString() {
        StringBuffer b= new StringBuffer();
        b.append('<');
        for(Object o: this) {
            b.append(o.toString());
            b.append(',');
        }
        b.deleteCharAt(b.length() - 1);
        b.append('>');
        return b.toString();
    }

    @Override
    public boolean equals(Object o) {
        ITuple peer= (ITuple) o;
        int arity= arity();
        if (arity() != peer.arity()) {
            return false;
        }
        for(int i= 0; i < arity; i++) {
            if (!get(i).equals(peer.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        return 1;
    }
    
    public IValue accept(IValueVisitor v) throws VisitorException {
    	return v.visitTuple(this);
    }
    
    @Override
    protected Object clone() throws CloneNotSupportedException {
    	Tuple tmp = new Tuple(getType());
    	
    	// since tuples are immutable, no need to clone the elements
    	tmp.fElements = fElements;
    	
    	return tmp;
    }

	public ITuple set(int i, IValue arg) {
		try {
			Tuple tmp = (Tuple) clone();
			tmp.fElements[i] = arg;
			return tmp;
		} catch (CloneNotSupportedException e) {
			// does not happen
			return null;
		}
	}

	public ITuple set(String label, IValue arg) {
		try {
			Tuple tmp = (Tuple) clone();
			tmp.fElements[((TupleType) fType).getFieldIndex(label)] = arg;
			return tmp;
		} catch (CloneNotSupportedException e) {
			// does not happen
			return null;
		}
	}
}
