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

import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.visitors.IValueVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;


public interface IValue  {
	/** 
	 * @return the Type of a value
	 */
    Type getType();
    
    /**
     * @return the smallest super type of getType() that is not a named type.
     */
    Type getBaseType();
    
    /**
     * get the value of an annotation on this value
     * @param label
     * @return the value of the annotation labeled 'label' of this value, or null
     *         if it does not exist
     */
    IValue getAnnotation(String label);

    /**
     * set an annotation on this value
     * @param label
     * @param value
     * @return a new value with the annotation set
     */
    IValue setAnnotation(String label, IValue value);
    
    /**
     * determine whether a certain annotation is set on this value
     * @param label
     * @return true iff this value has an annotation with that label
     */
    boolean hasAnnotation(String label);
    
    /**
     * Execute the @see IValueVisitor on the current node
     * 
     * @param
     */
    IValue accept(IValueVisitor v) throws VisitorException;
}
