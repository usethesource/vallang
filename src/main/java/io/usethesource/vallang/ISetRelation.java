package io.usethesource.vallang;

public interface ISetRelation<T extends ISetAlgebra<T>> extends IRelationalAlgebra<T, ISetRelation<T>> {

	T asSet();

}
