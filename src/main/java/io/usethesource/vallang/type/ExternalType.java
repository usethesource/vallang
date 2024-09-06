/*******************************************************************************
* Copyright (c) 2007 IBM Corporation, 2009-2015 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju (jurgen@vinju.org) - initial API and implementation

*******************************************************************************/
package io.usethesource.vallang.type;

import java.util.Map;
import java.util.Random;

import io.usethesource.vallang.IExternalValue;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;

/**
 * ExternalType facilitates a limited form of extensibility to the PDB's type system.
 * It can be used for example to add 'function types' to the PDB. Any such extension
 * to PDB must be a subclass of ExternalType and override isSubTypeOf() and lub().
 * <br> 
 * Note that NORMAL USE OF THE VALUES LIBRARY DOES NOT REQUIRE EXTENDING THIS CLASS
 */
public abstract class ExternalType extends DefaultSubtypeOfValue {
    
    /**
     * Provide the type of the values produced by {@link IExternalValue}.encodeAsConstructor()
     */
    public abstract Type asAbstractDataType();
        
    @Override
    public Type getTypeParameters() {
        return TypeFactory.getInstance().voidType();
    }
    
    @Override
    public boolean isExternalType() {
        return true;
    }

    @Override
    public final <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
        return visitor.visitExternal(this);
    }
    
    @Override
    public final Type lub(Type other) {
      return other.lubWithExternal(this);    
    }
    
    @Override
    public final Type glb(Type type) {
      return type.glbWithExternal(this);
    }
    
    @Override
    public boolean intersects(Type other) {
        return other.intersectsWithExternal(this);
    }
    
    @Override
    protected /*final*/ boolean isSupertypeOf(Type type) {
      return type.isSubtypeOfExternal(this);
    }
    
    @Override
    abstract protected Type lubWithExternal(Type type);
    
    @Override
    abstract protected boolean intersectsWithExternal(Type type);
    
    @Override
    abstract protected Type glbWithExternal(Type type);
  
    
    @Override
    abstract protected boolean isSubtypeOfExternal(Type type);
    
    @Override
    abstract public IValue randomValue(Random random, IValueFactory vf, TypeStore store, Map<Type, Type> typeParameters, int maxDepth, int maxBreadth);
}
