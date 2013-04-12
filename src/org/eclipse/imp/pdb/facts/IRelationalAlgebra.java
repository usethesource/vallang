package org.eclipse.imp.pdb.facts;

public interface IRelationalAlgebra<T> extends ISetAlgebra<T> {

	T compose(T rel1, T rel2);
	T closure();
	T closureStar();
	
    T project(int ... fields);
    T projectByFieldNames(String ... fields);	
	    
	ISet carrier();
	ISet domain();
	ISet range();
	
}
