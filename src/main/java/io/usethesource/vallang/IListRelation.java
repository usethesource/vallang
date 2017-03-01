package io.usethesource.vallang;

public interface IListRelation<T extends IListAlgebra<T>> extends IRelationalAlgebra<T, IListRelation<T>> {

	T asList();

}
