package org.eclipse.imp.pdb.facts.type;

import org.eclipse.imp.pdb.facts.IValue;

public interface ITypeVisitor {
	IValue visitDouble(DoubleType type);
	IValue visitInteger(IntegerType type);
	IValue visitList(ListType type);
	IValue visitMap(MapType type);
	IValue visitNamed(NamedType type);
	<U> IValue visitObject(ObjectType<U> type);
	IValue visitRelationType(RelationType type);
	IValue visitSet(SetType type);
	IValue visitSourceLocation(SourceLocationType type);
	IValue visitSourceRange(SourceRangeType type);
	IValue visitString(StringType type);
	IValue visitTreeNode(TreeNodeType type);
	IValue visitTreeSort(TreeSortType type);
	IValue visitTuple(TupleType type);
	IValue visitValue(ValueType type);
	IValue visitVoid(VoidType type);
}
