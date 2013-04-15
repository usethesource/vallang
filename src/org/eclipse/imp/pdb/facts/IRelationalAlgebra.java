package org.eclipse.imp.pdb.facts;

/*
 * Desired: 
 * public interface IRelationalAlgebra<T extends ISetAlgebra<T>>
 */
public interface IRelationalAlgebra<T> {	
	
	T compose(T other);
	T closure();
	T closureStar();

	int arity();
	T project(int... fields);
	T projectByFieldNames(String... fields);

	T carrier();
	T domain();
	T range();

}
