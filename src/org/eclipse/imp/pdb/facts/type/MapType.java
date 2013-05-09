/*******************************************************************************
* Copyright (c) 2008, 2012 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Anya Helene Bagge - labels
*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UndeclaredFieldException;

/*package*/ final class MapType extends DefaultSubtypeOfValue {
    private final Type fKeyType;
    private final Type fValueType;
    private final String fKeyLabel;
    private final String fValueLabel;
    
    /*package*/ MapType(Type keyType, Type valueType) {
    	fKeyType= keyType;
    	fValueType = valueType;
    	fKeyLabel = null;
    	fValueLabel = null;
    }
    
    /*package*/ MapType(Type keyType, String keyLabel, Type valueType, String valueLabel) {
    	fKeyType= keyType;
    	fValueType = valueType;
    	fKeyLabel = keyLabel;
    	fValueLabel = valueLabel;
    }
    
	@Override
    public Type getKeyType() {
    	return fKeyType;
    }
    
	@Override
	public String getKeyLabel() {
		return fKeyLabel;
	}
	
	@Override
	public int getArity() {
		return 2;
	}
	
	@Override
	public String getValueLabel() {
		return fValueLabel;
	}
	
    @Override
    public Type getValueType() {
    	return fValueType;
    }

  
    @Override
    public boolean hasFieldNames() {
    	return fKeyLabel != null && fValueLabel != null;
    }
    
    @Override
    public Type getFieldType(String fieldName) throws FactTypeUseException {
    	if (fKeyLabel.equals(fieldName)) {
    		return fKeyType;
    	}
    	if (fValueLabel.equals(fieldName)) {
    		return fValueType;
    	}
    	throw new UndeclaredFieldException(this, fieldName);
    }
    
    @Override
    public boolean hasField(String fieldName) {
    	if (fieldName.equals(fKeyLabel)) {
    		return true;
    	}
    	else if (fieldName.equals(fValueLabel)) {
    		return true;
    	}
    	
    	return false;
    }
    
    @Override
    public Type getFieldType(int i) {
    	switch (i) {
    	case 0: return fKeyType;
    	case 1: return fValueType;
    	default:
    		throw new IndexOutOfBoundsException();
    	}
    }
    
    @Override
    public String getFieldName(int i) {
    	switch (i) {
    	case 0: return fKeyLabel;
    	case 1: return fValueLabel;
    	default:
    		throw new IndexOutOfBoundsException();
    	}

    }
    
    @Override
    public Type select(int... fields) {
    	return TypeFactory.getInstance().setType(getFieldTypes().select(fields));
    }
    
    @Override
    public Type select(String... names) {
    	return TypeFactory.getInstance().setType(getFieldTypes().select(names));
    }
    
    @Override
    public int getFieldIndex(String fieldName) {
    	if (fKeyLabel.equals(fieldName)) {
    		return 0;
    	}
    	if (fValueLabel.equals(fieldName)) {
    		return 1;
    	}
    	throw new UndeclaredFieldException(this, fieldName);
    }
    
    @Override
    public Type getFieldTypes() {
    	if (hasFieldNames()) {
    		return TypeFactory.getInstance().tupleType(fKeyType, fKeyLabel, fValueType, fValueLabel);
    	}
    	else {
    		return TypeFactory.getInstance().tupleType(fKeyType, fValueType);
    	}
    }
    
    @Override
    public Type carrier() {
      TypeFactory tf = TypeFactory.getInstance();
      return tf.setType(fKeyType.lub(fValueType));
    }

    @Override
    public int hashCode() {
      return 56509 + 3511 * fKeyType.hashCode() + 1171 * fValueType.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof MapType)) {
            return false;
        }
        MapType other= (MapType) obj;
        
        if (fKeyLabel != null) {
        	if (!fKeyLabel.equals(other.fKeyLabel)) {
        		return false;
        	}
        }
        else if(other.fKeyLabel != null)
        	return false;
        
        if (fValueLabel != null) {
        	if (!fValueLabel.equals(other.fValueLabel)) {
        		return false;
        	}
        }
        else if(other.fValueLabel != null)
        	return false;
        
        // N.B.: The element type must have been created and canonicalized before any
        // attempt to manipulate the outer type (i.e. SetType), so we can use object
        // identity here for the fEltType.
        return fKeyType == other.fKeyType && fValueType == other.fValueType;
    }

    @Override
    public String toString() {
    	return "map[" + 
    	fKeyType + (fKeyLabel != null ? " " + fKeyLabel : "") + ", " 
    	+ fValueType + (fValueLabel != null ? " " + fValueLabel : "")  + 
    	"]";
    }
    
    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
      return visitor.visitMap(this);
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
      return type.isSubtypeOfMap(this);
    }
    
    @Override
    public Type lub(Type other) {
      return other.lubWithMap(this);
    }
    
    @Override
    public Type glb(Type type) {
      return type.glbWithMap(this);
    }
    
    @Override
    protected boolean isSubtypeOfMap(Type type) {
      return fKeyType.isSubtypeOf(type.getKeyType())
          && fValueType.isSubtypeOf(type.getValueType());
    }
    
    @Override
    protected Type lubWithMap(Type type) {
      return this == type ? this : TF.mapTypeFromTuple(getFieldTypes().lub(type.getFieldTypes()));
    }
    
    @Override
    protected Type glbWithMap(Type type) {
      return this == type ? this : TF.mapTypeFromTuple(getFieldTypes().glb(type.getFieldTypes()));
    }
    
    @Override
    public boolean isOpen() {
      return fKeyType.isOpen() || fValueType.isOpen();
    }
    
	@Override
	public boolean match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		return super.match(matched, bindings)
				&& getKeyType().match(matched.getKeyType(), bindings)
				&&getValueType().match(matched.getValueType(), bindings);
	}
	
	@Override
	public Type instantiate(Map<Type, Type> bindings) {
	  if (fKeyLabel != null) {
	    return TypeFactory.getInstance().mapType(getKeyType().instantiate(bindings), fKeyLabel, getValueType().instantiate(bindings), fValueLabel);
	  } 
	  else {
	    return TypeFactory.getInstance().mapType(getKeyType().instantiate(bindings), getValueType().instantiate(bindings));
	  }
	}
}
