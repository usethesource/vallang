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

/*package*/ final class BoolType extends DefaultSubtypeOfValue {
  static final Type CONSTRUCTOR = declareTypeSymbol("bool");

  private final static class InstanceKeeper {
    public final static BoolType sInstance = new BoolType();
  }

  public static BoolType getInstance() {
	  return InstanceKeeper.sInstance;
  }

  public static Type fromSymbol(IConstructor symbol, TypeStore store, Function<IConstructor,Set<IConstructor>> grammar) {
	  return TF.boolType();
  }
  
  @Override
  public void asProductions(IValueFactory vf, TypeStore store, Map<IConstructor, Set<IConstructor>> grammar) {
	  // empty TODO pull up empty asProduction
  }
  
	@Override
	protected Type getReifiedConstructorType() {
		return CONSTRUCTOR;
	}

	/**
	 * Should never need to be called; there should be only one instance of
	 * IntegerType
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == BoolType.getInstance();
	}

	@Override
	public int hashCode() {
		return 84121;
	}

	@Override
	public String toString() {
		return "bool";
	}

	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitBool(this);
	}
	
	@Override
  protected boolean isSupertypeOf(Type type) {
    return type.isSubtypeOfBool(this);
  }
  
  @Override
  public Type lub(Type other) {
    return other.lubWithBool(this);
  }
  
  @Override
  protected boolean isSubtypeOfBool(Type type) {
    return true;
  }
  
  @Override
  protected Type lubWithBool(Type type) {
    return this;
  }
  
  @Override
  public Type glb(Type type) {
    return type.glbWithBool(this);
  }
  
  @Override
  protected Type glbWithBool(Type type) {
    return this;
  }
}
