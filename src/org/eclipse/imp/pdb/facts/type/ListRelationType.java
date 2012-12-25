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

import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;


/*package*/ final class ListRelationType extends ListType {
	private final Type fTupleType;

    /**
     * Create a new list relation type from a tuple type.
     * @param tupleType
     */
    /*package*/ ListRelationType(Type tupleType) {
        super(tupleType);
        
        fTupleType = tupleType;
    }
    
    @Override
    public int getArity() {
    	return fTupleType.getArity();
    }
    
    @Override
    public Type getFieldType(int i) {
    	return fTupleType.getFieldType(i);
    }
    
    @Override
    public Type getFieldType(String label) {
    	return fTupleType.getFieldType(label);
    }
    
    @Override
    public int getFieldIndex(String fieldName) {
    	return fTupleType.getFieldIndex(fieldName);
    }
    
    @Override
    public boolean hasField(String fieldName) {
    	return fTupleType.hasField(fieldName);
    }
    
    @Override
    public Type getFieldTypes() {
    	return fTupleType;
    }
    
    @Override
    public String getFieldName(int i) {
		return fTupleType.getFieldName(i);
    }
    
    @Override
    public boolean isListRelationType() {
    	return true;
    }
    
    
    @Override
    public Type select(int... fields) {
    	return TypeFactory.getInstance().listType(fTupleType.select(fields));
    }
    
    @Override
    public Type select(String... names) {
    	return TypeFactory.getInstance().listType(fTupleType.select(names));
    }
    
    @Override
    public boolean isSubtypeOf(Type o) {
        if (o.isListRelationType() && !o.isVoidType()) {
        	return fTupleType.isSubtypeOf(o.getFieldTypes());
        }
        
        return super.isSubtypeOf(o);
    }

    @Override
    public Type lub(Type o) {
    	if(o == this)
    		return TypeFactory.getInstance().listType(fTupleType.lub(o.getFieldTypes()));
    	if (o.isListRelationType()) {
    		return TypeFactory.getInstance().listType(fTupleType.lub(o.getFieldTypes()));
    	}
    	
    	return super.lub(o);
    }

    @Override
    public int hashCode() {
        return 290253 + 269479 * fTupleType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ListRelationType))
            return false;
        ListRelationType other= (ListRelationType) obj;
        // N.B.: The element type must have been created and canonicalized before any
        // attempt to manipulate the outer type (i.e. ListType), so we can use object
        // identity here for the fEltType.
        return fTupleType == other.fTupleType;
    }

    @Override
    public String toString() {
    	if (fTupleType.isVoidType() || (fTupleType.getArity() == 1 && fTupleType.getFieldType(0).isVoidType())) { 
    		return "list[void]";
    	}
    	
    	StringBuffer b = new StringBuffer();

    	b.append("lrel[");
    	int idx = 0;
    	for (Type t : fTupleType) {
    		if (idx++ > 0) {
    			b.append(", ");
    		}
    		b.append(t.toString());
    		if (fTupleType.hasFieldNames()) {
    		  b.append(" " + fTupleType.getFieldName(idx-1));
    		}
    	}
    	b.append("]");
    	return b.toString();
    }

	@Override
	public boolean hasFieldNames() {
		return fTupleType.hasFieldNames();
	}
	
	@Override
	public Type compose(Type other) throws FactTypeUseException {
		return TypeFactory.getInstance().lrelTypeFromTuple(getFieldTypes().compose(other.getFieldTypes()));
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
		
		if (getArity() != 2 || !getFieldType(0).comparable(getFieldType(1))) {
			throw new IllegalOperationException("closure", this);
		}
		Type lub = getFieldType(0).lub(getFieldType(1));
		
		TypeFactory tf = TypeFactory.getInstance();
		if (hasFieldNames()) {
			return tf.lrelType(lub, getFieldName(0), lub, getFieldName(1));
		}
		
		return tf.lrelType(lub, lub);
	}
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitListRelationType(this);
	}

	@Override
	public IValue make(IValueFactory f) {
		return f.relation(fTupleType);
	}
	
	@Override
	public IValue make(IValueFactory f, IValue...elems) {
		return f.relation(elems);
	}
	
	@Override
	public IListWriter writer(IValueFactory f) {
		return (IListWriter) f.listRelationWriter(fTupleType);
	}
}
