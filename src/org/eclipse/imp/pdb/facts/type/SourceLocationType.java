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



/*package*/ final class SourceLocationType  extends DefaultSubtypeOfValue {
    private static final class InstanceKeeper {
      public final static SourceLocationType sInstance= new SourceLocationType();
    }

    public static SourceLocationType getInstance() {
        return InstanceKeeper.sInstance;
    }

    private SourceLocationType() {
    	super();
    }

    /**
     * Should never need to be called; there should be only one instance of IntegerType
     */
    @Override
    public boolean equals(Object obj) {
        return obj == SourceLocationType.getInstance();
    }

    @Override
    public int hashCode() {
        return 61547;
    }

    @Override
    public String toString() {
        return "loc";
    }
    
    @Override
    public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
    	return visitor.visitSourceLocation(this);
    }

    @Override
    protected boolean isSupertypeOf(Type type) {
      return type.isSubtypeOfSourceLocation(this);
    }
    
    @Override
    public Type lub(Type other) {
      return other.lubWithSourceLocation(this);
    }
    
    @Override
    public Type glb(Type type) {
      return type.glbWithSourceLocation(this);
    }
    
    @Override
    protected boolean isSubtypeOfSourceLocation(Type type) {
      return true;
    }
    
    @Override
    protected Type lubWithSourceLocation(Type type) {
      return this;
    }
    
    @Override
    protected Type glbWithSourceLocation(Type type) {
      return this;
    }
}
