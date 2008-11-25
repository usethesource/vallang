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

public interface IDouble extends IValue {
    double getValue();
    IDouble add(IDouble other);
    IDouble subtract(IDouble other);
    IDouble multiply(IDouble other);
    IDouble divide(IDouble other);
    IInteger floor();
    IInteger round();
    IBool less(IDouble other);
    IBool greater(IDouble other);
    IBool lessEqual(IDouble other);
    IBool greaterEqual(IDouble other);
}
