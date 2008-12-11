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
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

class Tuple extends Value implements ITuple {
    protected final IValue[] fElements;

    /*package*/ Tuple(IValue... elements) {
	super(TypeFactory.getInstance().tupleType(elements));
		this.fElements= elements;
    }
    
    private Tuple(Tuple other, String label, IValue anno) {
    	super(other, label, anno);
    	fElements = other.fElements.clone();
    }

    private Tuple(Tuple other, int i, IValue elem) {
    	super(other);
    	fElements = other.fElements.clone();
    	fElements[i] = elem;
    }
    
	private Tuple(TupleType type, IValue[] elems) {
		super(type);
		fElements = elems;
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
    	if (o instanceof ITuple) {
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
    	return false;
    }

    @Override
    public int hashCode() {
       int hash = 0;
 	   for (int i = 0; i < fElements.length; i++) {
 		 hash = (hash << 1) ^ (hash >> 1) ^ fElements[i].hashCode();
 	   }
 	   return hash;
    }
    
    public <T> T accept(IValueVisitor<T> v) throws VisitorException {
    	return v.visitTuple(this);
    }
    
    @Override
    protected IValue clone(String label, IValue anno) {
    	return new Tuple(this, label, anno);
    }

	public ITuple set(int i, IValue arg) {
		return new Tuple(this, i, arg);
	}

	public ITuple set(String label, IValue arg) {
		int i = ((TupleType) fType).getFieldIndex(label);
		return new Tuple(this, i, arg);
	}

	public IValue select(int... fields) {
		Type type = ((TupleType) fType).select(fields);
		
		if (type.isTupleType()) {
			return doSelect(type, fields);
		}
		else {
			return get(fields[0]);
		}
	}

	private IValue doSelect(Type type, int... fields) {
		IValue[] elems = new IValue[fields.length];
		
		for (int i = 0; i < fields.length; i++) {
			elems[i] = get(fields[i]);
		}
		
		return new Tuple((TupleType) type, elems);
	}

	public IValue select(String... fields) {
		Type type = ((TupleType) fType).select(fields);
		
		if (type.isTupleType()) {
			int[] indexes = new int[fields.length];
			int i = 0;
			for (String name : fields) {
				indexes[i] = ((TupleType) type).getFieldIndex(name);
			}
			
			return doSelect(type, indexes);
		}
		else {
			return get(fields[0]);
		}
	}
}
