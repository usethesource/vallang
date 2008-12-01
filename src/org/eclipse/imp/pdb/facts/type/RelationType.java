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

package org.eclipse.imp.pdb.facts.type;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;


public final class RelationType extends SetType {
    /*package*/ TupleType fTupleType;

    /**
     * Create a new relation type from a tuple type.
     * @param tupleType
     */
    /*package*/ RelationType(TupleType tupleType) {
        super(tupleType);
        fTupleType = tupleType;
    }
    
    public int getArity() {
    	return fTupleType.getArity();
    }
    
    public Type getFieldType(int i) {
    	return fTupleType.getFieldType(i);
    }
    
    public TupleType getFieldTypes() {
    	return fTupleType;
    }
    
    public String getFieldName(int i) {
		return fTupleType.getFieldName(i);
    }
    
    @Override
    public boolean isRelationType() {
    	return true;
    }
    
    public boolean isSubtypeOf(Type other) {
        if (other.isRelationType()) {
        	RelationType o = (RelationType) other;
        	return fTupleType.isSubtypeOf(o.fTupleType);
        }
        else {
        	return super.isSubtypeOf(other);
        }
    }

    @Override
    public Type lub(Type other) {
    	if (other.isRelationType()) {
    		RelationType o = (RelationType) other;
    		return TypeFactory.getInstance().setType(fTupleType.lub(o.fTupleType));
    	}
    	else {
    		return super.lub(other);
    	}
    }

    @Override
    public int hashCode() {
        return 58453 + 14323 * fTupleType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof RelationType))
            return false;
        RelationType other= (RelationType) obj;
        // N.B.: The element type must have been created and canonicalized before any
        // attempt to manipulate the outer type (i.e. SetType), so we can use object
        // identity here for the fEltType.
        return fTupleType == other.fTupleType;
    }

    @Override
    public String toString() {
    	StringBuffer b = new StringBuffer();

    	b.append("rel[");
    	int idx = 0;
    	for (Type t : fTupleType.fFieldTypes) {
    		if (idx++ > 0) {
    			b.append(", ");
    		}
    		b.append(t.toString());
    	}
    	b.append("]");
    	return b.toString();
    }

	public boolean isReflexive() throws FactTypeError {
		if (getArity() == 2) {
			Type t1 = getFieldType(0);
			Type t2 = getFieldType(1);

			return t1.isSubtypeOf(t2) || t1.isSubtypeOf(t2);
		}
		
		return false;
	}
	
	public boolean acceptsElementOf(Type eltType) {
		return eltType.isSubtypeOf(fTupleType);
	}

	public RelationType compose(RelationType other) throws FactTypeError {
		if (this == other && isReflexive()) {
			return this;
		}
		
		TupleType t1 = fTupleType;
		TupleType t2 = other.fTupleType;

		Type last = t1.getFieldType(t1.getArity() - 1);
		Type first = t2.getFieldType(0);
		
		if (!(last.isSubtypeOf(first) || first.isSubtypeOf(last))) {
			throw new FactTypeError("composition works only when the last type of the first relation is compatible with the first type of the second relation");
		}
		
		TupleType resultType = t1.compose(t2);
		return TypeFactory.getInstance().relType(resultType);
	}
	
	public SetType toSet() {
		return TypeFactory.getInstance().setType(fTupleType);
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitRelationType(this);
	}

	public IValue make(IValueFactory f) {
		return f.relation(fTupleType);
	}
	
	public IValue make(IValueFactory f, IValue...elems) {
		return f.relation(elems);
	}

	
}
