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

package org.rascalmpl.value.type;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.rascalmpl.value.IConstructor;
import org.rascalmpl.value.IValueFactory;

/*package*/ final class StringType extends DefaultSubtypeOfValue {
    static final Type CONSTRUCTOR = declareTypeSymbol("str");

	private static final class InstanceKeeper {
      private final static StringType sInstance= new StringType();
    }

    public static StringType getInstance() {
        return InstanceKeeper.sInstance;
    }

    @Override
	public IConstructor asSymbol(IValueFactory vf) {
      return vf.constructor(CONSTRUCTOR);
    }

    /**
     * Should never need to be called; there should be only one instance of IntegerType
     */
    @Override
    public boolean equals(Object obj) {
        return obj == StringType.getInstance();
    }

    @Override
    public int hashCode() {
        return 94903;
    }

    @Override
    public String toString() {
        return "str";
    }
    
    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
    	return visitor.visitString(this);
    }
    
    @Override
    protected boolean isSupertypeOf(Type type) {
      return type.isSubtypeOfString(this);
    }
    
    @Override
    public Type lub(Type other) {
      return other.lubWithString(this);
    }
    
    @Override
    public Type glb(Type type) {
      return type.glbWithString(this);
    }
    
    @Override
    protected boolean isSubtypeOfString(Type type) {
      return true;
    }
    
    @Override
    protected Type lubWithString(Type type) {
      return this;
    }
    
    @Override
    protected Type glbWithString(Type type) {
      return this;
    }
    
    public static Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar) {
  	  return TF.rationalType();
    }
    
    @Override
    public void asProductions(IValueFactory vf, TypeStore store, Map<IConstructor, Set<IConstructor>> grammar) {
    	// TODO empty on purpose, pull up
    }
}
