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


/*package*/ final class RealType extends NumberType {
  private final static class InstanceKeeper {
    public final static RealType sInstance = new RealType();
  }

	public static RealType getInstance() {
		return InstanceKeeper.sInstance;
	}

	private RealType() {
		super();
	}

	/**
	 * Should never need to be called; there should be only one instance of
	 * IntegerType
	 */
	@Override
	public boolean equals(Object obj) {
		return (obj instanceof RealType);
	}

	@Override
	public int hashCode() {
		return 84121;
	}

	@Override
	public String toString() {
		return "real";
	}

	@Override
	public <T> T accept(ITypeVisitor<T> visitor) {
		return visitor.visitReal(this);
	}
	
	@Override
	protected boolean isSupertypeOf(Type type) {
	  return type.isSubtypeOfReal(this);
	}
	
	@Override
	protected Type lubWithReal(Type type) {
	  return this;
	}
	 
	@Override
	protected boolean isSubtypeOfReal(Type type) {
	  return true;
	}
}
