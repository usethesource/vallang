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


/*package*/ final class StringType extends DefaultSubtypeOfValue {
    private static final class InstanceKeeper {
      private final static StringType sInstance= new StringType();
    }

    public static StringType getInstance() {
        return InstanceKeeper.sInstance;
    }

    private StringType() {
      super();
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
}
