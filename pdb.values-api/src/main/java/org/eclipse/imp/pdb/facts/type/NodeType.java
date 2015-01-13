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

/**
 * A type for values that are nodes. All INode have the type NodeType, and all
 * IConstructors have NodeType as a supertype.
 */
class NodeType extends DefaultSubtypeOfValue {
  protected static class InstanceKeeper {
    public final static NodeType sInstance = new NodeType();
  }

  public static NodeType getInstance() {
    return InstanceKeeper.sInstance;
  }

  protected NodeType() {
    super();
  }

  @Override
  public String toString() {
    return "node";
  }

  /**
   * Should never be called, NodeType is a singleton
   */
  @Override
  public boolean equals(Object o) {
    return o == NodeType.getInstance();
  }

  @Override
  public int hashCode() {
    return 20102;
  }

  @Override
  protected boolean isSupertypeOf(Type type) {
    return type.isSubtypeOfNode(this);
  }

  @Override
  public Type lub(Type other) {
    return other.lubWithNode(this);
  }

  @Override
  protected boolean isSubtypeOfNode(Type type) {
    return true;
  }

  @Override
  protected Type lubWithAbstractData(Type type) {
    return this;
  }

  @Override
  protected Type lubWithConstructor(Type type) {
    return this;
  }

  @Override
  protected Type lubWithNode(Type type) {
    return type;
  }
  
  @Override
  public Type glb(Type type) {
    return type.glbWithNode(this);
  }
  
  @Override
  protected Type glbWithNode(Type type) {
    return this;
  }
  
  @Override
  protected Type glbWithConstructor(Type type) {
    return type;
  }
  
  @Override
  protected Type glbWithAbstractData(Type type) {
    return type;
  }
  
  @Override
  public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
    return visitor.visitNode(this);
  }
}
