package org.eclipse.imp.pdb.facts;

public interface ISetRelation<T extends ISetAlgebra<T>> extends IRelationalAlgebra<T, ISetRelation<T>> {

	T asSet();

}
