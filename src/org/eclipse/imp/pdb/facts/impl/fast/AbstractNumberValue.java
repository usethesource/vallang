package org.eclipse.imp.pdb.facts.impl.fast;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.INumber;
import org.eclipse.imp.pdb.facts.exceptions.UnexpectedTypeException;
import org.eclipse.imp.pdb.facts.type.TypeFactory;

/*package*/ abstract class AbstractNumberValue extends Value implements INumber{
	private final static TypeFactory typeFactory = TypeFactory.getInstance();
	
	/*package*/ AbstractNumberValue(){
		super();
	}

	public INumber add(INumber other){
		if(isIntegerType(other)){
			return add(other.toInteger());
		}
		if(isRealType(other)){
			return add(other.toReal());
		}
		if(isRationalType(other)){
			return add(other.toRational());
		}
		
		throw new UnexpectedTypeException(typeFactory.numberType(), other.getType());
	}
	
	public INumber divide(INumber other, int precision){
		if(isIntegerType(other)){
			return divide(other.toInteger(), precision);
		}
		if(isRealType(other)){
			return divide(other.toReal(), precision);
		}
		if(isRationalType(other)){
			return divide(other.toRational(), precision);
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), other.getType());
	}

	public IBool greater(INumber other){
		if(isIntegerType(other)){
			return greater(other.toInteger());
		}
		if(isRealType(other)){
			return greater(other.toReal());
		}
		if(isRationalType(other)){
			return greater(other.toRational());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), other.getType());
	}
	

  public IBool equal(INumber other){
    if(isIntegerType(other)){
      return equal(other.toInteger());
    }
    if(isRealType(other)){
      return equal(other.toReal());
    }
    if(isRationalType(other)){
      return equal(other.toRational());
    }
    throw new UnexpectedTypeException(typeFactory.numberType(), other.getType());
  }
  
	public IBool greaterEqual(INumber other){
		if(isIntegerType(other)){
			return greaterEqual(other.toInteger());
		}
		if(isRealType(other)){
			return greaterEqual(other.toReal());
		}
		if(isRationalType(other)){
			return greaterEqual(other.toRational());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), other.getType());
	}
	
	public IBool less(INumber other){
		if(isIntegerType(other)){
			return less(other.toInteger());
		}
		if(isRealType(other)){
			return less(other.toReal());
		}
		if(isRationalType(other)){
			return less(other.toRational());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), other.getType());
	}
	
	public IBool lessEqual(INumber other){
		if(isIntegerType(other)){
			return lessEqual(other.toInteger());
		}
		if(isRealType(other)){
			return lessEqual(other.toReal());
		}
		if(isRationalType(other)){
			return lessEqual(other.toRational());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), other.getType());
	}

	public INumber multiply(INumber other){
		if(isIntegerType(other)){
			return multiply(other.toInteger());
		}
		if(isRealType(other)){
			return multiply(other.toReal());
		}
		if(isRationalType(other)){
			return multiply(other.toRational());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), other.getType());
	}

	public INumber subtract(INumber other){
		if(isIntegerType(other)){
			return subtract(other.toInteger());
		}
		if(isRealType(other)){
			return subtract(other.toReal());
		}
		if(isRationalType(other)){
			return subtract(other.toRational());
		}
		throw new UnexpectedTypeException(typeFactory.numberType(), other.getType());
	}

  protected boolean isRationalType(INumber other) {
    return other.getType().equivalent(TypeFactory.getInstance().rationalType());
  }

  protected boolean isRealType(INumber other) {
    return other.getType().equivalent(TypeFactory.getInstance().realType());
  }

  protected boolean isIntegerType(INumber other) {
    return other.getType().equivalent(TypeFactory.getInstance().integerType());
  }
}
