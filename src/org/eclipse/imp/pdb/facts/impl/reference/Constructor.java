package org.eclipse.imp.pdb.facts.impl.reference;

import java.util.Map;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedAnnotationTypeException;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedChildTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeStore;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

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
	
	private Constructor(Constructor constructor, String label, IValue value) {
		super(constructor, label, value);
	}

	private Constructor(Constructor constructor, Map<String, IValue> annotations) {
		super(constructor, annotations);
	}

	private Constructor(Constructor constructor) {
		super(constructor);
	}

	private Constructor(Constructor constructor, String key) {
		super(constructor, key);
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
		if (getClass() == obj.getClass()) {
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
	public <T> T accept(IValueVisitor<T> v) throws VisitorException {
		return v.visitConstructor(this);
	}
	
	public boolean declaresAnnotation(TypeStore store, String label) {
		return store.getAnnotationType(getType(), label) != null;
	}
	
	@Override
	public IConstructor setAnnotation(String label, IValue value) {
		IValue previous = getAnnotation(label);
		
		if (previous != null) {
			Type expected = previous.getType();
	
			if (!expected.comparable(value.getType())) {
				throw new UnexpectedAnnotationTypeException(expected, value.getType());
			}
		}
	
		return new Constructor(this, label, value);
	}
	
	@Override
	public IConstructor joinAnnotations(Map<String, IValue> annotations) {
		return new Constructor(this, annotations);
	}
	
	@Override
	public IConstructor setAnnotations(Map<String, IValue> annotations) {
		return removeAnnotations().joinAnnotations(annotations);
	}
	
	@Override
	public IConstructor removeAnnotations() {
		return new Constructor(this);
	}
	
	@Override
	public IConstructor removeAnnotation(String key) {
		return new Constructor(this, key);
	}
}
