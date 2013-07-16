package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.imp.pdb.facts.IAnnotatable;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedChildTypeException;
import org.eclipse.imp.pdb.facts.impl.AbstractDefaultAnnotatable;
import org.eclipse.imp.pdb.facts.impl.AnnotatedConstructorFacade;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.util.ShareableHashMap;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;

/**
 * Implementation of a typed tree node with access to children via labels
 */
public class Constructor extends Node implements IConstructor {
	/*package*/ Constructor(Type type, IValue[] children) {
		super(type.getName(), type, children);
		
	}
	
	/*package*/ Constructor(Type type) {
		this(type, new IValue[0]);
	}

	private Constructor(Constructor other, int childIndex, IValue newChild) {
		super(other, childIndex, newChild);
	}

	@Override
	public Type getType() {
		return getConstructorType().getAbstractDataType();
	}
	
	@Override
	public Type getUninstantiatedConstructorType() {
	  return fType;
	}
	
	public Type getConstructorType() {
	  if (fType.getAbstractDataType().isParameterized()) {
      assert fType.getAbstractDataType().isOpen();
    
      // this assures we always have the most concrete type for constructors.
      Type[] actualTypes = new Type[fChildren.length];
      for (int i = 0; i < fChildren.length; i++) {
        actualTypes[i] = fChildren[i].getType();
      }
    
      Map<Type,Type> bindings = new HashMap<Type,Type>();
      fType.getFieldTypes().match(TypeFactory.getInstance().tupleType(actualTypes), bindings);
      
      for (Type field : fType.getAbstractDataType().getTypeParameters()) {
        if (!bindings.containsKey(field)) {
          bindings.put(field, TypeFactory.getInstance().voidType());
        }
      }
      
      return fType.instantiate(bindings);
    }
    
    return fType;
	}

	public IValue get(String label) {
		return super.get(fType.getFieldIndex(label));
	}

	public Type getChildrenTypes() {
		return fType.getFieldTypes();
	}

	@Override
	public IConstructor set(int i, IValue newChild) throws IndexOutOfBoundsException {
		checkChildType(i, newChild);
		return new Constructor(this, i, newChild);
	}

	
	public IConstructor set(String label, IValue newChild) throws FactTypeUseException {
		int childIndex = fType.getFieldIndex(label);
		checkChildType(childIndex, newChild);
		return new Constructor(this, childIndex, newChild);
	}
	
	private void checkChildType(int i, IValue newChild) {
		Type type = newChild.getType();
		Type expectedType = getConstructorType().getFieldType(i);
		if (!type.isSubtypeOf(expectedType)) {
			throw new UnexpectedChildTypeException(expectedType, type);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		else if(obj == null) {
			return false;
		}
		else if (getClass() == obj.getClass()) {
			Constructor other = (Constructor) obj;
			return fType.comparable(other.fType) && super.equals(obj);
		}
		return false;
	}

	@Override
	public int hashCode() {
		 return 17 + ~super.hashCode();
	}
	
	@Override
	public <T, E extends Throwable> T accept(IValueVisitor<T,E> v) throws E {
		return v.visitConstructor(this);
	}
	
	public boolean declaresAnnotation(TypeStore store, String label) {
		return store.getAnnotationType(getType(), label) != null;
	}

	public boolean has(String label) {
		return getConstructorType().hasField(label);
	}
	
	/**
	 * TODO: Create and move to {@link AbstractConstructor}.
	 */
	@Override
	public boolean isAnnotatable() {
		return true;
	}
	
	/**
	 * TODO: Create and move to {@link AbstractConstructor}.
	 */
	@Override
	public IAnnotatable<? extends IConstructor> asAnnotatable() {
		return new AbstractDefaultAnnotatable<IConstructor>(this) {

			@Override
			protected IConstructor wrap(IConstructor content,
					ShareableHashMap<String, IValue> annotations) {
				return new AnnotatedConstructorFacade(content, annotations);
			}
		};
	}
	
}
