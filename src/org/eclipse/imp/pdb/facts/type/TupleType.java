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

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;

public class TupleType extends Type implements Iterable<Type> {
    protected final Type[] fFieldTypes;
    protected final String[] fFieldNames;
    protected int fHashcode= -1;

    /**
     * Creates a tuple type with the given field types. Copies the array.
     */
    /*package*/ TupleType(int len, int start, Type[] fieldTypes) {
        if (fieldTypes != null && len >= 0) {
            fFieldTypes= new Type[len];
            fFieldNames= null;
            System.arraycopy(fieldTypes, start, fFieldTypes, 0, len);
        } else {
            throw new IllegalArgumentException("Null array of field types or non-positive length passed to TupleType ctor!");
        }
    }

    /**
     * Creates a tuple type with the given field types and names. Copies the arrays.
     */
    /*package*/ TupleType(int len, int start, Type[] fieldTypes,String[] fieldNames) {
    	if (fieldTypes != null && len >= 0) {
    		fFieldTypes= new Type[len];
    		System.arraycopy(fieldTypes, start, fFieldTypes, 0, len);
    	} else {
    		throw new IllegalArgumentException("Null array of field types or non-positive length passed to TupleType ctor!");
    	}
    	if (fieldNames != null && len >= 0 && fieldTypes.length == fieldNames.length) {
    		fFieldNames = new String[len];
    		System.arraycopy(fieldNames, start, fFieldNames, 0, len);
    	}
    	else {
    		throw new IllegalArgumentException("Unequal amounts of field names and field types");
    	}
    }
    
    public boolean hasFieldNames() {
    	return fFieldNames != null;
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
    
    private TupleType tupleCompose(TupleType type, TupleType other) {
		int N = type.getArity() + other.getArity() - 2;
		Type[] fieldTypes = new Type[N];
		
		for (int i = 0; i < type.getArity() - 1; i++) {
			fieldTypes[i] = type.getFieldType(i);
		}
		
		for (int i = type.getArity() - 1, j = 1; i < N; i++, j++) {
			fieldTypes[i] = other.getFieldType(j);
		}
		
		return TypeFactory.getInstance().tupleType(fieldTypes);
	}
    
    public TupleType compose(TupleType other) {
    	return tupleCompose(this, other);
    }

    @Override
    public boolean isSubtypeOf(Type other) {
    	if (other == this) {
			return true; // optimize to prevent loop
		} else if (other.isTupleType()) {
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

		return super.isSubtypeOf(other);
    }

    /**
     * Compute a new tupletype that is the lub of t1 and t2. 
     * Precondition: t1 and t2 have the same arity.
     * @param t1
     * @param t2
     * @return a TupleType which is the lub of t1 and t2
     */
    private TupleType lubTupleTypes(TupleType t1, TupleType t2) {
    	int N = t1.getArity();
    	Type[] fieldTypes = new Type[N];
    	String[] fieldNames = new String[N];
    	
    	for (int i = 0; i < N; i++) {
    		fieldTypes[i] = t1.getFieldType(i).lub(t2.getFieldType(i));
    		
    		if (t1.hasFieldNames()) {
    			fieldNames[i] = t1.getFieldName(i);
    		}
    		else if (t1.hasFieldNames()) {
    			fieldNames[i] = t2.getFieldName(i);
    		}
    	}
    	
    	TupleType result = TypeFactory.getInstance().tupleType(fieldTypes);
    	
    	return result;
    }
    
    /**
     * Compute a new tupletype that is the lub of t1 and t2. 
     * Precondition: t1 and t2 have the same arity.
     * @param t1
     * @param t2
     * @return a TupleType which is the lub of t1 and t2
     */
    private TupleType lubNamedTupleTypes(TupleType t1, TupleType t2) {
    	int N = t1.getArity();
    	Object[] fieldTypes = new Object[N*2];
    	boolean first = t1.hasFieldNames();
    	
    	for (int i = 0, j = 0; i < N; i++, j++) {
    		fieldTypes[j++] = t1.getFieldType(i).lub(t2.getFieldType(i));
    		
			if (first) {
    			fieldTypes[j] = t1.getFieldName(i);
    		}
    		else {
    			fieldTypes[j] = t2.getFieldName(i);
    		}
    	}
    	
    	TupleType result = TypeFactory.getInstance().tupleType(fieldTypes);
    	
    	return result;
    }
    
    @Override
    public Type lub(Type other) {
    	if (other.isTupleType()) {
    		TupleType o = (TupleType) other;
    		if (getArity() == o.getArity()) {
    			if (hasFieldNames() || ((TupleType) other).hasFieldNames()) {
    				return lubNamedTupleTypes(this, o);
    			}
    			else {
    	          return lubTupleTypes(this, o);
    			}
    		}
    	}
    	return super.lub(other);
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
     * Compute tuple type equality. Note that field labels are significant here
     * for equality while they do not count for isSubtypeOf and lub.
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
        
        if (fFieldNames != null) {
        	for (int i = 0; i < fFieldNames.length; i++) {
              if (!fFieldNames[i].equals(other.fFieldNames[i])) {
            	  return false;
              }
        	}
        }
        
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb= new StringBuilder();
        sb.append("tuple[");
        int idx= 0;
        for(Type elemType: fFieldTypes) {
            if (idx++ > 0)
                sb.append(",");
            sb.append(elemType.toString());
        }
        sb.append("]");
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
	
	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitTuple(this);
	}

	public IValue make(IValueFactory f) {
		return f.tuple();
	}

	public IValue make(IValueFactory f, IValue... elems) {
		return f.tuple(elems);
	}

	public String getFieldName(int i) {
		return fFieldNames != null ? fFieldNames[i] : null;
	}
}
