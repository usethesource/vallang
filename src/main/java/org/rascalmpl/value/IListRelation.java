package org.rascalmpl.value;

public interface IListRelation<T extends IListAlgebra<T>> extends IRelationalAlgebra<T, IListRelation<T>> {

	T asList();

}
