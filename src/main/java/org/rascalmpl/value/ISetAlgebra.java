package org.rascalmpl.value;

public interface ISetAlgebra<T extends ISetAlgebra<T>> {
    
    T union     (T collection);
    T intersect (T collection);
    T subtract  (T collection);

}
