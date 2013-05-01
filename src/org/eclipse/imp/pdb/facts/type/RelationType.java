/*******************************************************************************
* Copyright (c) 2007-2013 IBM Corporation and CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Anya Helene Bagge (anya@ii.uib.no) - simplify code  
*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;


/*package*/ final class RelationType extends SetType {

    /**
     * Create a new relation type from a tuple type.
     * @param tupleType
     */
    /*package*/ RelationType(Type tupleType) {
        super(tupleType);
        if(!tupleType.isTupleType()) {
        	throw new IllegalArgumentException("Argument should be a tupletype: " + tupleType);
        }
    }
    
    @Override
    public int getArity() {
    	return fEltType.getArity();
    }
    
    @Override
    public Type getFieldType(int i) {
    	return fEltType.getFieldType(i);
    }
    
    @Override
    public Type getFieldType(String label) {
    	return fEltType.getFieldType(label);
    }
    
    @Override
    public int getFieldIndex(String fieldName) {
    	return fEltType.getFieldIndex(fieldName);
    }
    
    @Override
    public boolean hasField(String fieldName) {
    	return fEltType.hasField(fieldName);
    }
    
    @Override
    public Type getFieldTypes() {
    	return fEltType;
    }
    
    @Override
    public String getFieldName(int i) {
		return fEltType.getFieldName(i);
    }
    
    @Override
    public boolean isRelationType() {
    	return true;
    }
    
    
    @Override
    public Type select(int... fields) {
    	return TypeFactory.getInstance().setType(fEltType.select(fields));
    }
    
    @Override
    public Type select(String... names) {
    	return TypeFactory.getInstance().setType(fEltType.select(names));
    }
    

    @Override
    public String toString() {
    	if (fEltType.isVoidType() || (fEltType.getArity() == 1 && fEltType.getFieldType(0).isVoidType())) { 
    		return "set[void]";
    	}
    	
    	StringBuilder b = new StringBuilder();

    	b.append("rel[");
    	int idx = 0;
    	for (Type t : fEltType) {
    		if (idx++ > 0) {
    			b.append(", ");
    		}
    		b.append(t.toString());
    		if (fEltType.hasFieldNames()) {
    		  b.append(" " + fEltType.getFieldName(idx-1));
    		}
    	}
    	b.append("]");
    	return b.toString();
    }

	@Override
	public boolean hasFieldNames() {
		return fEltType.hasFieldNames();
	}
	
	@Override
	public Type compose(Type other) throws FactTypeUseException {
		return TypeFactory.getInstance().relTypeFromTuple(getFieldTypes().compose(other.getFieldTypes()));
	}
	
	@Override
	public Type carrier() {
		return getFieldTypes().carrier();
	}
	
	@Override
	public Type closure() {
		if (getElementType().isVoidType()) {
			return this;
		}
		
		if (getArity() != 2) {
			throw new IllegalOperationException("closure", this);
		}
		Type lub = getFieldType(0).lub(getFieldType(1));
		
		TypeFactory tf = TypeFactory.getInstance();
		if (hasFieldNames()) {
			return tf.relType(lub, getFieldName(0), lub, getFieldName(1));
		}
		
		return tf.relType(lub, lub);
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitRelationType(this);
	}

	@Override
	public IValue make(IValueFactory f) {
		return f.relation(fEltType);
	}
	
	@Override
	public IValue make(IValueFactory f, IValue...elems) {
		return f.relation(elems);
	}
	
	@Override
	public ISetWriter writer(IValueFactory f) {
		return f.relationWriter(fEltType);
	}
}
