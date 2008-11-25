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


public interface IInteger extends IValue {
    int getValue();
    IInteger add(IInteger other);
    IInteger subtract(IInteger other);
    IInteger multiply(IInteger other);
    IInteger divide(IInteger other);
    IInteger remainder(IInteger other);
    IDouble  toDouble();
    IBool less(IInteger other);
    IBool greater(IInteger other);
    IBool lessEqual(IInteger other);
    IBool greaterEqual(IInteger other);
    
    /**
     * Compares two integers
     * @param other
     * @return -1 if receiver is less than other, 0 is receiver is equal, 1 if receiver is larger
     */
    int compare(IInteger other);
}
