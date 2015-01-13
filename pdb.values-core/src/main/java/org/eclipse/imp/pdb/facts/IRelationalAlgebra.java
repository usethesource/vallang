package org.eclipse.imp.pdb.facts;

/*
 * Desired: 
 * public interface IRelationalAlgebra<T extends ISetAlgebra<T>>
 */
public interface IRelationalAlgebra<R, A1 extends IRelationalAlgebra<R, A1>> {	
	
	R compose(A1 other);
	R closure();
	R closureStar();

	int arity();
	R project(int... fields);
	
	@Deprecated
	R projectByFieldNames(String... fields);

	R carrier();
	R domain();
	R range();

}
