/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   jurgen@vinju.org

*******************************************************************************/
package org.eclipse.imp.pdb.facts.type;

/**
 * Visitor interface for all kinds of Types 
 *
 * @param <T> the result type of the visit methods
 */
public interface ITypeVisitor<T> {
	T visitReal(Type type);
	T visitInteger(Type type);
	T visitList(Type type);
	T visitMap(Type type);
	T visitAlias(Type type);
	T visitRelationType(Type type);
	T visitSet(Type type);
	T visitSourceLocation(Type type);
	T visitString(Type type);
	T visitNode(Type type);
	T visitConstructor(Type type);
	T visitAbstractData(Type type);
	T visitTuple(Type type);
	T visitValue(Type type);
	T visitVoid(Type type);
	T visitBool(Type boolType);
	T visitParameter(Type parameterType);
	T visitExternal(Type externalType);
}
