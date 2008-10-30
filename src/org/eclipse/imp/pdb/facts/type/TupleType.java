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

import java.util.Iterator;

public class TupleType extends Type implements Iterable<Type> {
    protected Type[] fFieldTypes;
    protected String[] fFieldNames;
    protected int fHashcode= -1;

    /**
     * Creates a tuple type with the given field types. Copies the array.
     */
    /*package*/ TupleType(int len, int start, Type[] fieldTypes) {
        if (fieldTypes != null && len >= 0) {
            fFieldTypes= new Type[len];
            System.arraycopy(fieldTypes, start, fFieldTypes, 0, len);
        } else {
            throw new IllegalArgumentException("Null array of field types or non-positive length passed to TupleType ctor!");
        }
    }

    /**
     * Creates a tuple type with the given field types and names. Copies the arrays.
     */
    /*package*/ TupleType(int len, int start, Type[] fieldTypes,String[] fieldNames) {
       this(len, start, fieldTypes);
       if (fieldNames != null && len >= 0 && fieldTypes.length == fieldNames.length) {
    	   fFieldNames = new String[len];
    	   System.arraycopy(fieldNames, start, fFieldNames, 0, len);
       }
       else {
    	   throw new IllegalArgumentException("Unequal amounts of field names and field types");
       }
    }
    
    @Override
    public boolean isTupleType() {
    	return true;
    }
    
    public Type getFieldType(int i) {
        return fFieldTypes[i];
    }

    public Type getFieldType(String fieldName) {
    	return getFieldType(getFieldIndex(fieldName));
    }
    
    public int getFieldIndex(String fieldName) {
    	if (fFieldNames != null) {
    		for (int i = 0; i < fFieldNames.length; i++) {
    			if (fFieldNames[i].equals(fieldName)) {
    				return i;
    			}
    		}
    		
    		throw new FactTypeError("no field exists with this name: " + fieldName);
    	}
    	
    	throw new FactTypeError("tuple type has no labels");
    }
    
    public int getArity() {
        return fFieldTypes.length;
    }
    
    public TupleType product(TupleType other) {
    	return TypeFactory.getInstance().tupleProduct(this, other);
    }
    
    public TupleType compose(TupleType other) {
    	return TypeFactory.getInstance().tupleCompose(this, other);
    }

    @Override
    public boolean isSubtypeOf(Type other) {
        if (other == this || other.isValueType()) {
        	return true;
        }
        else if (other.isTupleType()) {
        	TupleType o = (TupleType) other;
        	if (getArity() == o.getArity()) {
        		for (int i = 0; i < getArity(); i++) {
        			if (!getFieldType(i).isSubtypeOf(o.getFieldType(i))) {
        				return false;
        			}
        		}
        		return true;
        	}
        }
        	
        return false;
    }

    @Override
    public Type lub(Type other) {
    	if (other.isSubtypeOf(this)) {
    		return this;
    	}
    	else if (other.isTupleType()) {
    		TupleType o = (TupleType) other;
    		if (getArity() == o.getArity()) {
    	      return TypeFactory.getInstance().lubTupleTypes(this, o);
    		}
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
        if (fHashcode == -1) {
            fHashcode= 55501;
            for(Type elemType: fFieldTypes) {
                fHashcode= fHashcode * 44927 + elemType.hashCode();
            }
        }
        return fHashcode;
    }

    /**
     * Compute tuple type equality. Note that field labels are insignificant for
     * the identity of a tuple type.
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TupleType)) {
            return false;
        }
        
        TupleType other= (TupleType) obj;
        if (fFieldTypes.length != other.fFieldTypes.length) {
            return false;
        }
        
        for(int i=0; i < fFieldTypes.length; i++) {
            // N.B.: The field types must have been created and canonicalized before any
            // attempt to manipulate the outer type (i.e. TupleType), so we can use object
            // identity here for the fFieldTypes.
            if (fFieldTypes[i] != other.fFieldTypes[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb= new StringBuilder();
        sb.append("<");
        int idx= 0;
        for(Type elemType: fFieldTypes) {
            if (idx++ > 0)
                sb.append(", ");
            sb.append(elemType.toString());
        }
        sb.append(">");
        return sb.toString();
    }

	public Iterator<Type> iterator() {
		return new Iterator<Type>() {
			private int cursor = 0;

			public boolean hasNext() {
				return cursor < fFieldTypes.length;
			}

			public Type next() {
				return fFieldTypes[cursor++];
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}
}
