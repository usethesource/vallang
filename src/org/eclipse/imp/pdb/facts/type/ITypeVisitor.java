package org.eclipse.imp.pdb.facts.type;


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
	T visitTreeSort(TreeSortType type);
	T visitTuple(TupleType type);
	T visitValue(ValueType type);
	T visitVoid(VoidType type);
}
