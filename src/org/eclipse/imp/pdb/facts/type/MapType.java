/*******************************************************************************
* Copyright (c) 2008 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import java.util.Map;

import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;

/*package*/ final class MapType extends Type {
    private final Type fKeyType;
    private final Type fValueType;
    
    /*package*/ MapType(Type keyType, Type valueType) {
    	fKeyType= keyType;
    	fValueType = valueType;
    }
    
    @Override
    public Type getKeyType() {
    	return fKeyType;
    }
    
    @Override
    public Type getValueType() {
    	return fValueType;
    }

    @Override
    public boolean isMapType() {
    	return true;
    }
  
    @Override
    public boolean isSubtypeOf(Type o) {
        if (o.isMapType()) {
        	return fKeyType.isSubtypeOf(o.getKeyType()) && fValueType.isSubtypeOf(o.getValueType());
        }
        
        return super.isSubtypeOf(o);
    }

    @Override
    public Type lub(Type o) {
    	if (o.isMapType()) {
    		return TypeFactory.getInstance().mapType(fKeyType.lub(o.getKeyType()),
    				                                 fValueType.lub(o.getValueType()));
    	}
    	
    	return super.lub(o);
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
        // N.B.: The element type must have been created and canonicalized before any
        // attempt to manipulate the outer type (i.e. SetType), so we can use object
        // identity here for the fEltType.
        return fKeyType == other.fKeyType && fValueType == other.fValueType;
    }

    @Override
    public String toString() {
        return "map[" + fKeyType + "," + fValueType + "]";
    }
    
    @Override
    public <T> T accept(ITypeVisitor<T> visitor) {
    	return visitor.visitMap(this);
    }
    
    @Override
    public IMap make(IValueFactory f) {
    	return f.map(fKeyType, fValueType);
    }
    
	@SuppressWarnings("unchecked")
	@Override
    public IMapWriter writer(IValueFactory f) {
    	return f.mapWriter(fKeyType, fValueType);
    }
	
	@Override
	public void match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		super.match(matched, bindings);
		getKeyType().match(matched.getKeyType(), bindings);
		getValueType().match(matched.getValueType(), bindings);
	}
	
	@Override
	public Type instantiate(TypeStore store, Map<Type, Type> bindings) {
		return TypeFactory.getInstance().mapType(getKeyType().instantiate(store, bindings), getValueType().instantiate(store, bindings));
	}
}
