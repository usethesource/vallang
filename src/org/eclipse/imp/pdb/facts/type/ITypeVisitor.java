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
public interface ITypeVisitor<T,E extends Throwable> {
	T visitReal(Type type) throws E;
	T visitInteger(Type type) throws E;
	T visitRational(Type type) throws E;
	T visitList(Type type) throws E;
	T visitMap(Type type) throws E;
	T visitNumber(Type type) throws E;
	T visitAlias(Type type) throws E;
	T visitSet(Type type) throws E;
	T visitSourceLocation(Type type) throws E;
	T visitString(Type type) throws E;
	T visitNode(Type type) throws E;
	T visitConstructor(Type type) throws E;
	T visitAbstractData(Type type) throws E;
	T visitTuple(Type type) throws E;
	T visitValue(Type type) throws E;
	T visitVoid(Type type) throws E;
	T visitBool(Type type) throws E;
	T visitParameter(Type type) throws E;
	T visitExternal(Type type) throws E;
	T visitDateTime(Type type) throws E;
}
