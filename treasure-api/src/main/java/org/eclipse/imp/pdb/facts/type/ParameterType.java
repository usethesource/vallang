/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - initial API and implementation
*******************************************************************************/

package org.eclipse.imp.pdb.facts.type;

import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;



/**
 * A Parameter Type can be used to represent an abstract type,
 * i.e. a type that needs to be instantiated with an actual type
 * later.
 */
/*package*/ final class ParameterType extends Type {
	private final String fName;
	private final Type fBound;
	
	/* package */ ParameterType(String name, Type bound) {
		fName = name.intern();
		fBound = bound;
	}
	
	/* package */ ParameterType(String name) {
		fName = name.intern();
		fBound = TypeFactory.getInstance().valueType();
	}
	
	@Override
	public Type getBound() {
		return fBound;
	}
	
	@Override
	public String getName() {
		return fName;
	}
	
	@Override
	public int getArity(){
		return fBound.getArity();
	}
	
	@Override
	public Type getFieldType(int i){
		return fBound.getFieldType(i);
	}
	
	@Override
	public String[] getFieldNames(){
		return fBound.getFieldNames();
	}
	
	@Override
	public String toString() {
		return fBound.equivalent(ValueType.getInstance()) ? "&" + fName : "&" + fName + "<:" + fBound.toString();
	}
	
	@Override
	public int hashCode() {
		return 49991 + 49831 * fName.hashCode() + 133020331 * fBound.hashCode();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof ParameterType) {
			ParameterType other = (ParameterType) o;
			return fName.equals(other.fName) && fBound == other.fBound;
		}
		return false;
	}
	
	@Override
	public <T,E extends Throwable> T accept(ITypeVisitor<T,E> visitor) throws E {
		return visitor.visitParameter(this);
	}

	@Override
	protected boolean isSupertypeOf(Type type) {
	  return type.isSubtypeOfParameter(this);
	}
	
	@Override
	public Type lub(Type type) {
	  return type.glbWithParameter(this);
	}
	
	@Override
	public Type glb(Type type) {
	  return type.glbWithParameter(this);
	}
	
	@Override
	protected Type lubWithParameter(Type type) {
	  if (type == this) {
	    return this;
	  }
	  
	  return getBound().lub(type.getBound());
	}
	
	@Override
	protected Type glbWithParameter(Type type) {
	  if (type == this) {
	    return this;
	  }
	  
	  return getBound().glb(type.getBound());
	}
	
	@Override
	public boolean match(Type matched, Map<Type, Type> bindings)
			throws FactTypeUseException {
		if (!super.match(matched, bindings)) {
			return false;
		}
		
		Type earlier = bindings.get(this);
		if (earlier != null) {
			Type lub = earlier.lub(matched);
			if (!lub.isSubtypeOf(getBound())) {
				return false;
			}
			
			bindings.put(this, lub);
		}
		else {
			bindings.put(this, matched);
		}
		
		return true;
	}
	
	@Override
	public Type instantiate(Map<Type, Type> bindings) {
		Type result = bindings.get(this);
		return result != null ? result : this;
	}

  @Override
  protected boolean isSubtypeOfReal(Type type) {
    return getBound().isSubtypeOfReal(type);
  }

  @Override
  protected boolean isSubtypeOfInteger(Type type) {
    return getBound().isSubtypeOfInteger(type);
  }

  @Override
  protected boolean isSubtypeOfRational(Type type) {
    return getBound().isSubtypeOfRational(type);
  }

  @Override
  protected boolean isSubtypeOfList(Type type) {
    return getBound().isSubtypeOfList(type);
  }

  @Override
  protected boolean isSubtypeOfMap(Type type) {
    return getBound().isSubtypeOfMap(type);
  }

  @Override
  protected boolean isSubtypeOfNumber(Type type) {
    return getBound().isSubtypeOfNumber(type);
  }

  @Override
  protected boolean isSubtypeOfRelation(Type type) {
    return getBound().isSubtypeOfRelation(type);
  }

  @Override
  protected boolean isSubtypeOfListRelation(Type type) {
    return getBound().isSubtypeOfListRelation(type);
  }

  @Override
  protected boolean isSubtypeOfSet(Type type) {
    return getBound().isSubtypeOfSet(type);
  }

  @Override
  protected boolean isSubtypeOfSourceLocation(Type type) {
    return getBound().isSubtypeOfSourceLocation(type);
  }

  @Override
  protected boolean isSubtypeOfString(Type type) {
    return getBound().isSubtypeOfString(type);
  }

  @Override
  protected boolean isSubtypeOfNode(Type type) {
    return getBound().isSubtypeOfNode(type);
  }

  @Override
  protected boolean isSubtypeOfConstructor(Type type) {
    return getBound().isSubtypeOfConstructor(type);
  }

  @Override
  protected boolean isSubtypeOfAbstractData(Type type) {
    return getBound().isSubtypeOfAbstractData(type);
  }

  @Override
  protected boolean isSubtypeOfTuple(Type type) {
    return getBound().isSubtypeOfTuple(type);
  }

  @Override
  protected boolean isSubtypeOfValue(Type type) {
    return getBound().isSubtypeOfValue(type);
  }

  @Override
  protected boolean isSubtypeOfVoid(Type type) {
    return getBound().isSubtypeOfVoid(type);
  }

  @Override
  protected boolean isSubtypeOfBool(Type type) {
    return getBound().isSubtypeOfBool(type);
  }

  @Override
  protected boolean isSubtypeOfExternal(Type type) {
    return getBound().isSubtypeOfExternal(type);
  }

  @Override
  protected boolean isSubtypeOfDateTime(Type type) {
    return getBound().isSubtypeOfDateTime(type);
  }

  @Override
  protected Type lubWithReal(Type type) {
    return getBound().lubWithReal(type);
  }

  @Override
  protected Type lubWithInteger(Type type) {
    return getBound().lubWithInteger(type);
  }

  @Override
  protected Type lubWithRational(Type type) {
    return getBound().lubWithRational(type);
  }

  @Override
  protected Type lubWithList(Type type) {
    return getBound().lubWithList(type);
  }

  @Override
  protected Type lubWithMap(Type type) {
    return getBound().lubWithMap(type);
  }

  @Override
  protected Type lubWithNumber(Type type) {
    return getBound().lubWithNumber(type);
  }

  @Override
  protected Type lubWithSet(Type type) {
    return getBound().lubWithSet(type);
  }

  @Override
  protected Type lubWithSourceLocation(Type type) {
    return getBound().lubWithSourceLocation(type);
  }

  @Override
  protected Type lubWithString(Type type) {
    return getBound().lubWithString(type);
  }

  @Override
  protected Type lubWithNode(Type type) {
    return getBound().lubWithNode(type);
  }

  @Override
  protected Type lubWithConstructor(Type type) {
    return getBound().lubWithConstructor(type);
  }

  @Override
  protected Type lubWithAbstractData(Type type) {
    return getBound().lubWithAbstractData(type);
  }

  @Override
  protected Type lubWithTuple(Type type) {
    return getBound().lubWithTuple(type);
  }

  @Override
  protected Type lubWithValue(Type type) {
    return getBound().lubWithValue(type);
  }

  @Override
  protected Type lubWithVoid(Type type) {
    return getBound().lubWithVoid(type);
  }

  @Override
  protected Type lubWithBool(Type type) {
    return getBound().lubWithBool(type);
  }

  @Override
  protected Type lubWithExternal(Type type) {
    return getBound().lubWithExternal(type);
  }

  @Override
  protected Type lubWithDateTime(Type type) {
    return getBound().lubWithDateTime(type);
  }
  
  
  @Override
  public boolean isOpen() {
    return true;
  }
  
  @Override
  protected Type glbWithReal(Type type) {
    return getBound().glbWithReal(type);
  }

  @Override
  protected Type glbWithInteger(Type type) {
    return getBound().glbWithInteger(type);
  }

  @Override
  protected Type glbWithRational(Type type) {
    return getBound().glbWithRational(type);
  }

  @Override
  protected Type glbWithList(Type type) {
    return getBound().glbWithList(type);
  }

  @Override
  protected Type glbWithMap(Type type) {
    return getBound().glbWithMap(type);
  }

  @Override
  protected Type glbWithNumber(Type type) {
    return getBound().glbWithNumber(type);
  }

  @Override
  protected Type glbWithSet(Type type) {
    return getBound().glbWithSet(type);
  }

  @Override
  protected Type glbWithSourceLocation(Type type) {
    return getBound().glbWithSourceLocation(type);
  }

  @Override
  protected Type glbWithString(Type type) {
    return getBound().glbWithString(type);
  }

  @Override
  protected Type glbWithNode(Type type) {
    return getBound().glbWithNode(type);
  }

  @Override
  protected Type glbWithConstructor(Type type) {
    return getBound().glbWithConstructor(type);
  }

  @Override
  protected Type glbWithAbstractData(Type type) {
    return getBound().glbWithAbstractData(type);
  }

  @Override
  protected Type glbWithTuple(Type type) {
    return getBound().glbWithTuple(type);
  }

  @Override
  protected Type glbWithValue(Type type) {
    return getBound().glbWithValue(type);
  }

  @Override
  protected Type glbWithVoid(Type type) {
    return getBound().glbWithVoid(type);
  }

  @Override
  protected Type glbWithBool(Type type) {
    return getBound().glbWithBool(type);
  }

  @Override
  protected Type glbWithExternal(Type type) {
    return getBound().glbWithExternal(type);
  }

  @Override
  protected Type glbWithDateTime(Type type) {
    return getBound().glbWithDateTime(type);
  }
}
