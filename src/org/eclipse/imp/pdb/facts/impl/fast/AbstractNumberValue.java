package org.eclipse.imp.pdb.facts.impl.fast;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedTypeException;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

public abstract class AbstractNumberValue extends Value implements INumber{
	private final static TypeFactory typeFactory = TypeFactory.getInstance();
	
	public AbstractNumberValue(){
		super();
	}

	public INumber add(INumber other){
		Type otherType = other.getType();
		if(otherType.isIntegerType()){
			return add(other.toInteger());
		}
		if(otherType.isRealType()){
			return add(other.toReal());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), otherType);
	}
	
	public INumber divide(INumber other, int precision){
		Type otherType = other.getType();
		if(otherType.isIntegerType()){
			return divide(other.toInteger(), precision);
		}
		if(otherType.isRealType()){
			return divide(other.toReal(), precision);
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), otherType);
	}

	public IBool greater(INumber other){
		Type otherType = other.getType();
		if(otherType.isIntegerType()){
			return greater(other.toInteger());
		}
		if(otherType.isRealType()){
			return greater(other.toReal());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), otherType);
	}
	
	public IBool greaterEqual(INumber other){
		Type otherType = other.getType();
		if(otherType.isIntegerType()){
			return greaterEqual(other.toInteger());
		}
		if(otherType.isRealType()){
			return greaterEqual(other.toReal());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), otherType);
	}
	
	public IBool less(INumber other){
		Type otherType = other.getType();
		if(otherType.isIntegerType()){
			return less(other.toInteger());
		}
		if(otherType.isRealType()){
			return less(other.toReal());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), otherType);
	}
	
	public IBool lessEqual(INumber other){
		Type otherType = other.getType();
		if(otherType.isIntegerType()){
			return lessEqual(other.toInteger());
		}
		if(otherType.isRealType()){
			return lessEqual(other.toReal());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), otherType);
	}

	public INumber multiply(INumber other){
		Type otherType = other.getType();
		if(otherType.isIntegerType()){
			return multiply(other.toInteger());
		}
		if(otherType.isRealType()){
			return multiply(other.toReal());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), otherType);
	}

	public INumber subtract(INumber other){
		Type otherType = other.getType();
		if(otherType.isIntegerType()){
			return subtract(other.toInteger());
		}
		if(otherType.isRealType()){
			return subtract(other.toReal());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), otherType);
	}
}
