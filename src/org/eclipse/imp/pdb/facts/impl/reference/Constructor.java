package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.HashMap;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * Implementation of a typed tree node with access to children via labels
 */
public class Constructor extends Node implements IConstructor {
	protected final static HashMap<String, IValue> EMPTY_ANNOTATIONS = new HashMap<String,IValue>();
    protected final HashMap<String, IValue> fAnnotations;
    
	/*package*/ Constructor(Type type, IValue[] children) {
		super(type.getName(), type, children);
		fAnnotations = EMPTY_ANNOTATIONS;
	}
	
	/*package*/ Constructor(Type type) {
		this(type, new IValue[0]);
	}
	
	@SuppressWarnings("unchecked")
	private Constructor(Constructor constructor, String label, IValue anno) {
		super(constructor.fType.getName(), constructor.fType, constructor.fChildren);
		fAnnotations = (HashMap<String, IValue>) constructor.fAnnotations.clone();
		fAnnotations.put(label, anno);
	}

	private Constructor(Constructor other, int childIndex, IValue newChild) {
		super(other, childIndex, newChild);
		fAnnotations = other.fAnnotations;
	}
	
	@Override
	public Type getType() {
		return fType.getAbstractDataType();
	}
	
	public Type getConstructorType() {
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

	
	public IConstructor set(String label, IValue newChild) throws FactTypeError {
		int childIndex = fType.getFieldIndex(label);
		checkChildType(childIndex, newChild);
		return new Constructor(this, childIndex, newChild);
	}
	
	private void checkChildType(int i, IValue newChild) {
		Type type = newChild.getType();
		Type expectedType = getType().getFieldType(i);
		if (!type.isSubtypeOf(expectedType)) {
			throw new FactTypeError("New child type " + type + " is not a subtype of " + expectedType);
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (getClass() == obj.getClass()) {
		  Constructor other = (Constructor) obj;
		  return fType.comparable(other.fType) && super.equals(obj) && fAnnotations.equals(other.fAnnotations);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		 return 17 + ~super.hashCode();
	}
	
	@Override
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitConstructor(this);
	}
	
	public boolean hasAnnotation(String label) {
		boolean result = fAnnotations.containsKey(label);
		if (!result && !declaresAnnotation(label)) {
			throw new FactTypeError("This type " + getType() + " has no annotation named " + label + " declared for it.");
		}
		return result;
	}

	public boolean declaresAnnotation(String label) {
		return TypeFactory.getInstance().getAnnotationType(getType(), label) != null;
	}
	
	public IConstructor setAnnotation(String label, IValue value) {
		Type expected = TypeFactory.getInstance().getAnnotationType(getType(), label);

		if (expected == null) {
			throw new FactTypeError("This annotation was not declared for this type: " + label + " for " + getType());
		}

		if (!value.getType().isSubtypeOf(expected)) {
			throw new FactTypeError("The type of this annotation should be a subtype of " + expected + " and not " + value.getType());
		}

		return new Constructor(this, label, value);
	}

	public IValue getAnnotation(String label) throws FactTypeError {
		if (!declaresAnnotation(label)) {
			throw new FactTypeError("This type " + getType() + " has no annotation named " + label + " declared for it.");
		}
		return fAnnotations.get(label);
	}
}
