package org.rascalmpl.value;

public interface IListAlgebra<T extends IListAlgebra<T>> {
    
    T concat    (T collection);
    T intersect (T collection);
    T subtract  (T collection);

}
