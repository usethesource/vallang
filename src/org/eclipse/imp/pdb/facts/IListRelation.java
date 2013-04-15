package org.eclipse.imp.pdb.facts;

public interface IListRelation<T extends IListAlgebra<T>> extends IRelationalAlgebra<T, IListRelation<T>> {

	T asList();

}
