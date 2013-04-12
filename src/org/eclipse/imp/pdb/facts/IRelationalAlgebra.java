package org.eclipse.imp.pdb.facts;

public interface IRelationalAlgebra<T extends ISetAlgebra<T>> {

	T compose(T other);
	T closure();
	T closureStar();

	T project(int... fields);
	T projectByFieldNames(String... fields);

	T carrier();
	T domain();
	T range();

}
