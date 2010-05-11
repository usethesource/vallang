package org.eclipse.imp.pdb.facts.impl.fast;

import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.Type;

public abstract class AnnotatedConstructorBase extends Constructor {

	protected AnnotatedConstructorBase(Type constructorType, IValue[] children){
		super(constructorType, children);
	}
	
	public boolean hasAnnotations(){
		return true;
	}

	public IConstructor removeAnnotations(){
		return new Constructor(constructorType, children);
	}
	
	public boolean equals(Object o){
		if(o == this) return true;
		if(o == null) return false;
		
		if(o.getClass() == getClass()){
			AnnotatedConstructorBase other = (AnnotatedConstructorBase) o;
			
			if(constructorType != other.constructorType) return false;
			
			IValue[] otherChildren = other.children;
			int nrOfChildren = children.length;
			if(otherChildren.length == nrOfChildren){
				for(int i = nrOfChildren - 1; i >= 0; i--){
					if(!otherChildren[i].equals(children[i])) return false;
				}
				return true;
			}
		}
		
		return false;
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		
		sb.append(getName());
		sb.append("(");
		
		int size = children.length;
		if(size > 0){
			int i = 0;
			sb.append(children[i]);
			
			for(i = 1; i < size; i++){
				sb.append(",");
				sb.append(children[i]);
			}
		}
		
		sb.append(")");
		
		return sb.toString();
	}
}