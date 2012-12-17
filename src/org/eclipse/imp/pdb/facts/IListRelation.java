/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation
*    Paul Klint (Paul.Klint@cwi.nl) - ListRelation

*******************************************************************************/

package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.Type;

public interface IListRelation extends IList, IValue {
    public int arity();
    
    /**
     * Computes the union of two lists
     * @param <ListOrRel> ISet when the result will be a set, IRelation when it will be a relation.
     * @param element
     * @return a relation if the element type is a tuple type, a set otherwise
     */
    public <ListOrRel extends IList> ListOrRel union(IList list);

    /**
     * Computes the composition of this relation, works only on
     * two binary relations rel[t_1,t_2] and rel[t_3,t_4] where
     * t_2 is comparable to t_3.
     * 
     * @param rel another relation 
     * @return a relation with type rel[t_1,t_4]
     * @throws FactTypeUseException when t_2 is not comparable to t_3
     */
    public IListRelation compose(IListRelation rel) throws FactTypeUseException;

    /**
     * Computes transitive non-reflexive closure of a relation, but only
     * if this is a binary and symmetric relation (the field types are
     * equivalent)
     * 
     * @return a relation with the same type as the receiver
     * @throws FactTypeUseException if this relation is not a binary or symmetric.
     */
    public IListRelation closure() throws FactTypeUseException;

    /**
     * Computes transitive and reflexive closure of a relation, but only
     * if this is a binary and symmetric relation (the field types are
     * equivalent)
     * 
     * @return a relation with the same type as the receiver
     * @throws FactTypeUseException if this relation is not a binary or symmetric.
     */
    public IListRelation closureStar() throws FactTypeUseException;
    
    /**
     * Computes the carrier of the relation, which is the set
     * of all elements of all tuples that it is composed of.
     * 
     * @return a set with as element type the least upperbound
     * of the field types of the relation.
     */
    public IList carrier();
    
    /**
     * @return the field types represented as a tuple type
     */
    public Type getFieldTypes();
    
    /**
     * @return the set of elements in the first field of the relation
     */
    public IList domain();
    
    /**
     * @return the set of elements in the last field of the relation
     */
    public IList range();
    
    /**
     * Select from the relation only the following fields. In case a single
     * field is selected, the result is a set, otherwise the result is a relation.
     */
    public IList select(int ... fields);
    
    /**
     * Select from the relation only the following fields. In case a single
     * field is selected, the result is a set, otherwise the result is a relation.
     */
    public IList selectByFieldNames(String ... fields) throws FactTypeUseException;

}
