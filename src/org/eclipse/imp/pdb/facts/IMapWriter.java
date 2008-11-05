/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts;

import java.util.Map;

import org.eclipse.imp.pdb.facts.type.FactTypeError;


public interface IMapWriter {
    void put(IValue key, IValue value) throws FactTypeError ;
    void putAll(IMap map)  throws FactTypeError;
    void putAll(Map<? extends IValue, ? extends IValue> map) throws FactTypeError;
    IMap done();
}
