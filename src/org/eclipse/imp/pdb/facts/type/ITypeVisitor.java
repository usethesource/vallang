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
 * Visitor interface for all kinds of IValues 
 *
 * @param <T> the result type of the visit methods
 */
public interface ITypeVisitor<T> {
	T visitDouble(DoubleType type);
	T visitInteger(IntegerType type);
	T visitList(ListType type);
	T visitMap(MapType type);
	T visitNamed(NamedType type);
	<U> T visitObject(ObjectType<U> type);
	T visitRelationType(RelationType type);
	T visitSet(SetType type);
	T visitSourceLocation(SourceLocationType type);
	T visitSourceRange(SourceRangeType type);
	T visitString(StringType type);
	T visitTreeNode(TreeNodeType type);
	T visitNamedTree(NamedTreeType type);
	T visitTuple(TupleType type);
	T visitValue(ValueType type);
	T visitVoid(VoidType type);
}
