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


public class RelationType extends Type {
    /*package*/ TupleType fTupleType;

    /**
     * Create a new relation type from a tuple type.
     * @param tupleType
     */
    /*package*/ RelationType(TupleType tupleType) {
        fTupleType= tupleType;
    }
    
    /**
     * Create a new Relation type from a named tupled type
     * @param namedType that must be a tuple
     * @throws FactTypeError
     */
    /*package*/ RelationType(NamedType namedType) throws FactTypeError {
    	Type baseType = namedType.getBaseType();
    	
    	if (!baseType.isTupleType()) {
    		throw new FactTypeError("Type " + namedType + " is not a tuple type");
    	}
    	
    	fTupleType = (TupleType) baseType;
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
    
    @Override
    public boolean isRelationType() {
    	return true;
    }
    
    public boolean isSubtypeOf(Type other) {
        if (other == this || other.isValueType()) {
        	return true;
        }
        else if (other.isSetType()) {
        	SetType o = (SetType) other;
        	Type eltType = o.getElementType();
        	
        	if (fTupleType == eltType) {
        		return true;
        	}
        	else if (eltType.isTupleType()) {
        		return fTupleType.isSubtypeOf(eltType);
        	}
        	else if (eltType.isValueType()) {
        		return true;
        	}
        }
        else if (other.isRelationType()) {
        	RelationType o = (RelationType) other;
        	return fTupleType.isSubtypeOf(o.fTupleType);
        }
        
        return false;
    }

    @Override
    public Type lub(Type other) {
    	if (other == this) {
    		return this;
    	}
    	else if (other.isValueType()) {
    		return other;
    	}
    	else if (other.isSetType()) {
    		SetType o = (SetType) other;
        	Type eltType = o.getElementType();
        	Type lub = fTupleType.lub(eltType);
        	
        	if (lub.isTupleType()) {
        		return  TypeFactory.getInstance().relType((TupleType) lub);
        	}
        	
            // The upper bound on tuples of different arity is just ValueType,
            // so if the set's element type was tuple, but the arity didn't match,
            // just return set[lub].
            // N.B.: fTupleType.lub(eltType) would compute Value, so the below is just an optimization.
        	return TypeFactory.getInstance().setTypeOf(lub);
    	}
    	else if (other.isRelationType()) {
    		RelationType o = (RelationType) other;
    		Type lub = fTupleType.lub(o.fTupleType);
    		
    		if (lub.isTupleType()) {
    			return TypeFactory.getInstance().relType((TupleType) lub);
    		}
    		
    		return TypeFactory.getInstance().setTypeOf(lub);
    	}
    	else if (other.isNamedType()) {
    		return lub(((NamedType) other).getSuperType());
    	}
    	
    	return TypeFactory.getInstance().valueType();
    }

    @Override
    public String getTypeDescriptor() {
        return toString();
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

	public RelationType product(RelationType r) {
		return TypeFactory.getInstance().relationProduct(this, r);
	}
	
	public RelationType product(TupleType r) {
		return TypeFactory.getInstance().relType(TypeFactory.getInstance().tupleProduct(fTupleType, r));
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
		return TypeFactory.getInstance().setTypeOf(fTupleType);
	}
}
