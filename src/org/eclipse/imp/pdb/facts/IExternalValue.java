/*******************************************************************************
* Copyright (c) 2009 CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju - initial API and implementation

*******************************************************************************/
package org.eclipse.imp.pdb.facts;

import org.eclipse.imp.pdb.facts.type.ExternalType;

/**
 * IExternalValue, together with {@link ExternalType} offer a limited form of extensibility
 * to the PDB's value and type system. The IExternalValue interface is used to tag extensions,
 * such as 'function values' that are not part of the PDB fact exchange and manipulation
 * interfaces but do need to integrate with them.
 * <br>
 * Note that features such as (de)serialization are not supported for external values. However,
 * such generic features will provide a non-failing default such as (quietly) not serializing
 * the value at all. This is to obtain robustness.
 * <br>
 * Note that implementations of IExternalValues are obliged to have a type that subclasses
 * ExternalType.
 * <br>
 * Note that NORMAL USE OF THE PDB DOES NOT REQUIRE IMPLEMENTING THIS INTERFACE
 */
public interface IExternalValue extends IValue {
   // this is a marker interface
}
