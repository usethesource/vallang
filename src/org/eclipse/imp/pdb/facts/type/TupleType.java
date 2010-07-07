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
import java.util.Map;

import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.IllegalOperationException;
import org.eclipse.imp.pdb.facts.exceptions.UndeclaredFieldException;

/*package*/ final class TupleType extends Type {
	private final Type[] fFieldTypes;
	private final String[] fFieldNames;
	private int fHashcode= -1;

    /**
     * Creates a tuple type with the given field types. Copies the array.
     */
    /*package*/ TupleType(Type[] fieldTypes){
    	super();
    	
        fFieldTypes = fieldTypes.clone();
        fFieldNames = null;
    }

    /**
     * Creates a tuple type with the given field types and names. Copies the arrays.
     */
    /*package*/ TupleType(Type[] fieldTypes, String[] fieldNames){
    	super();
    	
    	fFieldTypes = fieldTypes.clone();
		if(fieldNames.length != 0) fFieldNames = fieldNames.clone();
		else fFieldNames = null;
    }
    
    @Override
    public boolean hasFieldNames() {
    	return fFieldNames != null;
    }
    
    @Override
    public boolean isTupleType() {
    	return true;
    }
    
    @Override
    public Type getFieldType(int i) {
        return fFieldTypes[i];
    }

    @Override
    public Type getFieldType(String fieldName) {
    	return getFieldType(getFieldIndex(fieldName));
    }
    
    @Override
    public int getFieldIndex(String fieldName) throws FactTypeUseException {
    	if (fFieldNames != null) {
    		for (int i = fFieldNames.length - 1; i >= 0; i--) {
    			if (fFieldNames[i].equals(fieldName)) {
    				return i;
    			}
    		}
    	}

		throw new UndeclaredFieldException(this, fieldName);
    }
    
    @Override
    public boolean hasField(String fieldName) {
    	try {
    		return getFieldIndex(fieldName) != -1;
    	}
    	catch (FactTypeUseException e) {
    		return false;
    	}
    }
    
    @Override
    public int getArity() {
        return fFieldTypes.length;
    }
    
    @Override
    public Type compose(Type other) {
    	if (other.isVoidType()) {
    		return other;
    	}
    	
    	if (this.getArity() != 2 || other.getArity() != 2) {
			throw new IllegalOperationException("compose", this, other);
		}
		
		if (!getFieldType(1).comparable(other.getFieldType(0))) {
			throw new IllegalOperationException("compose", this, other);
		}
		
		TypeFactory tf = TypeFactory.getInstance();
		
		if (hasFieldNames() && other.hasFieldNames()) {
			String fieldNameLeft = this.getFieldName(0);
			String fieldNameRight = other.getFieldName(1);
			
			if (!fieldNameLeft.equals(fieldNameRight)) {
				return tf.tupleType(
						this.getFieldType(0), 
						fieldNameLeft,
						other.getFieldType(1), 
						fieldNameRight
				);
			}
		}
		
		return tf.tupleType(this.getFieldType(0), other.getFieldType(1));
    }
    
    @Override
    public Type carrier() {
    	Type lub = TypeFactory.getInstance().voidType();
    	
    	for (Type field : this) {
    		lub = lub.lub(field);
    	}
    	
    	return TypeFactory.getInstance().setType(lub);
    }

    @Override
    public boolean isSubtypeOf(Type o) {
    	if (o == this) {
			return true; // optimize to prevent loop
		} else if (o.isTupleType()) {
			if (getArity() == o.getArity()) {
				for (int i = getArity() - 1; i >= 0; i--) {
					if (!getFieldType(i).isSubtypeOf(o.getFieldType(i))) {
						return false;
					}
				}
				return true;
			}
		}

		return super.isSubtypeOf(o);
    }

    /**
     * Compute a new tupletype that is the lub of t1 and t2. 
     * Precondition: t1 and t2 have the same arity.
     * @param t1
     * @param t2
     * @return a TupleType which is the lub of t1 and t2
     */
    private Type lubTupleTypes(Type t1, Type t2) {
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
    	
    	return TypeFactory.getInstance().tupleType(fieldTypes);
    }
    
    /**
     * Compute a new tupletype that is the lub of t1 and t2. 
     * Precondition: t1 and t2 have the same arity.
     * @param t1
     * @param t2
     * @return a TupleType which is the lub of t1 and t2, if all the names are equal at every 
     *         position, they remain, otherwise we get an unlabeled tuple.
     */
    private Type lubNamedTupleTypes(Type t1, Type t2) {
    	int N = t1.getArity();
    	Object[] fieldTypes = new Object[N*2];
    	Type[] types = new Type[N];
    	boolean first = t1.hasFieldNames();
    	boolean second = t2.hasFieldNames();
    	boolean consistent = true;
    	
    	for (int i = 0, j = 0; i < N; i++, j++) {
    		Type lub = t1.getFieldType(i).lub(t2.getFieldType(i));
    		types[i] = lub;
			fieldTypes[j++] = lub;
    		
			if (first && second) {
				String fieldName1 = t1.getFieldName(i);
				String fieldName2 = t2.getFieldName(i);
				
				if (fieldName1.equals(fieldName2)) {
					fieldTypes[j] = fieldName1;
				}
				else {
					consistent = false;
				}
    		}
    		else if (first) {
    			fieldTypes[j] = t1.getFieldName(i);
    		}
    		else if (second) {
    			fieldTypes[i] = t2.getFieldName(i);
    		}
    	}
    	
    	if (consistent && first && second) {
    		return TypeFactory.getInstance().tupleType(fieldTypes);
    	}
    	else {
    		return TypeFactory.getInstance().tupleType(types);
    	}
    }
    
    @Override
    public Type lub(Type o) {
    	if (o.isTupleType()) {
    		if (getArity() == o.getArity()) {
    			if (hasFieldNames() || o.hasFieldNames()) {
    				return lubNamedTupleTypes(this, o);
    			}
    			
    			return lubTupleTypes(this, o);
    		}
    	}
    	return super.lub(o);
    }

    @Override
    public int hashCode() {
    	int h = fHashcode;
        if (h == -1) {
            h = 55501;
            for(Type elemType: fFieldTypes) {
                h = h * 44927 + elemType.hashCode();
            }
            fHashcode = h;
        }
        return h;
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
        
        for(int i = fFieldTypes.length - 1; i >= 0; i--) {
            // N.B.: The field types must have been created and canonicalized before any
            // attempt to manipulate the outer type (i.e. TupleType), so we can use object
            // identity here for the fFieldTypes.
            if (fFieldTypes[i] != other.fFieldTypes[i]) {
                return false;
            }
        }
        
        if (fFieldNames != null) {
        	if (other.fFieldNames == null) {
        		return false;
        	}
        	for (int i = fFieldNames.length - 1; i >= 0; i--) {
              if (!fFieldNames[i].equals(other.fFieldNames[i])) {
            	  return false;
              }
        	}
        }
        else if (other.fFieldNames != null) {
        	return false;
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
            if (hasFieldNames()) {
            	sb.append(" " + fFieldNames[idx-1]);
            }
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
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

	 @Override
	public IValue make(IValueFactory f) {
		return f.tuple();
	}

	 @Override
	public IValue make(IValueFactory f, IValue... elems) {
		return f.tuple(elems);
	}

	 @Override
	public String getFieldName(int i) {
		return fFieldNames != null ? fFieldNames[i] : null;
	}
	
	@Override
	public void match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		super.match(matched, bindings);
		
		for (int i = getArity() - 1; i >= 0; i--) {
			getFieldType(i).match(matched.getFieldType(i), bindings);
		}
	}
	
	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		if (hasFieldNames()) {
			Type[] fTypes = new Type[getArity()];
			String[] fLabels = new String[getArity()];

			for (int i = fTypes.length - 1; i >= 0; i--) {
				fTypes[i] = getFieldType(i).instantiate(bindings);
				fLabels[i] = getFieldName(i);
			}

			return TypeFactory.getInstance().tupleType(fTypes, fLabels);
		}
		
		Type[] fChildren = new Type[getArity()];
		for (int i = fChildren.length - 1; i >= 0; i--) {
			fChildren[i] = getFieldType(i).instantiate(bindings);
		}

		return TypeFactory.getInstance().tupleType(fChildren);
	}

	 @Override
	public Type select(int... fields) {
        int width = fields.length;
        
        if (width == 0) {
        	return TypeFactory.getInstance().voidType();
        }
        else if (width == 1) {
        	return getFieldType(fields[0]);
        }
        else {
        	if (!hasFieldNames()) {
        		Type[] fieldTypes = new Type[width];
    		    for (int i = width - 1; i >= 0; i--) {
    		    	fieldTypes[i] = getFieldType(fields[i]);
    		    }
    		    
    		    return TypeFactory.getInstance().tupleType(fieldTypes);
        	}
        	
    		Type[] fieldTypes = new Type[width];
    		String[] fieldNames = new String[width];
    		boolean seenDuplicate = false;
    		
		    for (int i = width - 1; i >= 0; i--) {
		    	fieldTypes[i] = getFieldType(fields[i]);
		    	fieldNames[i] = getFieldName(fields[i]);
		    	
		    	for (int j = width - 1; j > i; j--) {
		    		if (fieldNames[j].equals(fieldNames[i])) {
		    			seenDuplicate = true;
		    		}
		    	}
		    }
		    
		    if (!seenDuplicate) {
		    	return TypeFactory.getInstance().tupleType(fieldTypes, fieldNames);
		    }
		    else {
		    	return TypeFactory.getInstance().tupleType(fieldTypes);
		    }
        }
	}
	
	 @Override
	public Type select(String... names) {
		int[] indexes = new int[names.length];
		int i = 0;
		for (String name : names) {
			indexes[i] = getFieldIndex(name);
		}
		
		return select(indexes);
	}
}
