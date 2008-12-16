/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.TupleType;

public interface IRelation extends ISet, IValue {
    public int arity();

    /**
     * Computes the composition of this relation, with type rel[t_1...t_n]
     * with another relation with type rel[u_1...u_m].
     * 
     * @param rel another relation 
     * @return a relation with type rel[t_1,...,t_n-1,u_2...u_m]
     * @throws FactTypeError when t_n is not a sub-type of u_1
     */
    public IRelation compose(IRelation rel) throws FactTypeError;

    /**
     * Computes transitive non-reflexive closure of a relation, but only
     * if this is a binary and symmetric relation (the field types are
     * equivalent)
     * 
     * @return a relation with the same type as the receiver
     * @throws FactTypeError if this relation is not a binary or symmetric.
     */
    public IRelation closure() throws FactTypeError;

    /**
     * Computes transitive and reflexive closure of a relation, but only
     * if this is a binary and symmetric relation (the field types are
     * equivalent)
     * 
     * @return a relation with the same type as the receiver
     * @throws FactTypeError if this relation is not a binary or symmetric.
     */
    public IRelation closureStar() throws FactTypeError;
    
    /**
     * Computes the carrier of the relation, which is the set
     * of all elements of all tuples that it is composed of.
     * 
     * @return a set with as element type the least upperbound
     * of the field types of the relation.
     */
    public ISet carrier();
    
    /**
     * @return the field types represented as a tuple type
     */
    public TupleType getFieldTypes();
    
    /**
     * @return the set of elements in the first field of the relation
     */
    public ISet domain();
    
    /**
     * @return the set of elements in the last field of the relation
     */
    public ISet range();
    
    /**
     * Select from the relation only the following fields. In case a single
     * field is selected, the result is a set, otherwise the result is a relation.
     */
    public ISet select(int ... fields);
    
    /**
     * Select from the relation only the following fields. In case a single
     * field is selected, the result is a set, otherwise the result is a relation.
     */
    public ISet select(String ... fields) throws FactTypeError;
}
