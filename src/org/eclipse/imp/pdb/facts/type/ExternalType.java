/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju (jurgen@vinju.org) - initial API and implementation

*******************************************************************************/
package org.eclipse.imp.pdb.facts.type;




/**
 * ExternalType facilitates a limited form of extensibility to the PDB's type system.
 * It can be used for example to add 'function types' to the PDB. Any such extension
 * to PDB must be a subclass of ExternalType and override isSubTypeOf() and lub().
 * <br>
 * Features such as (de)serialization are NOT supported for values that have an 
 * external type. However, such features will choose a non-failing default behavior,
 * such as silently not printing the value at all.
 * <br> 
 * Note that NORMAL USE OF THE PDB DOES NOT REQUIRE EXTENDING THIS CLASS
 */
public abstract class ExternalType extends DefaultSubtypeOfValue {
	static private final TypeStore store = new TypeStore();
	
	/** 
	 * For encoding an external value as an ADT we need a representative type
	 */
	public Type asAbstractDataType() {
		return TypeFactory.getInstance().abstractDataType(store, getName());
	}
	
	@Override
	public String getName() {
		return getClass().getSimpleName();
	}
	
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
	protected /*final*/ boolean isSupertypeOf(Type type) {
	  return type.isSubtypeOfExternal(this);
	}
	
	@Override
	abstract protected Type lubWithExternal(Type type);
	
	@Override
    abstract protected Type glbWithExternal(Type type);
  
	
	@Override
	abstract protected boolean isSubtypeOfExternal(Type type);
}
