package org.eclipse.imp.pdb.facts.type;

import java.util.HashMap;
import java.util.Map;

/**
 * Use this class to substitute type parameters for other types.
 */
public class TypeInstantiator implements ITypeVisitor<Type> {
	private final Type[] fActuals;
	private int fActualIndex;
	private final Map<String, Type> fBindings;
	private final TypeFactory tf = TypeFactory.getInstance();

	/**
	 * When this constructor is used the parameter types will
	 * be bound in order of appearance to the actual types.
	 * 
	 * @param actuals an array of actual types, with equal length to the number of
	 *        type parameters embedded in the type to be instantiated.
	 */
	private TypeInstantiator(Type... actuals) {
		fActuals = actuals;
		fBindings = new HashMap<String,Type>();
		fActualIndex = 0;
	}
	
	private TypeInstantiator(Map<String, Type> bindings) {
		fActuals = null;
		fActualIndex = -1;
		fBindings = bindings;
	}
	
	/**
	 * Instantiate a parameterized type with actual types
	 * @param abstractType the type to find embedded parameterized types in
	 * @param actuals      the actual types to replace the parameterized types with
	 * @return a new type with the parameter types replaced by the given actual types.
	 */
	public static Type instantiate(Type abstractType, Type... actuals) {
		return abstractType.accept(new TypeInstantiator(actuals));
	}
	
	public static Type instantiate(Type abstractType, Map<String, Type> bindings) {
		return abstractType.accept(new TypeInstantiator(bindings));
	}
	
	public Type visitBool(BoolType boolType) {
		return boolType;
	}

	public Type visitDouble(DoubleType type) {
		return type;
	}

	public Type visitInteger(IntegerType type) {
		return type;
	}

	public Type visitList(ListType type) {
		return tf.listType(type.getElementType().accept(this));
	}

	public Type visitMap(MapType type) {
		return tf.mapType(type.getKeyType().accept(this), type.getValueType().accept(this));
	}

	public Type visitNamed(NamedType type) {
		return type;
	}

	public Type visitNamedTree(NamedTreeType type) {
		return type;
	}

	public Type visitParameter(ParameterType parameterType) {
		Type boundTo = fBindings.get(parameterType.getName().toString());
		if (boundTo == null) {
			if (fActuals == null) {
				return parameterType;
			}
			else if (fActualIndex >= fActuals.length) {
				return parameterType;
			}
			boundTo = fActuals[fActualIndex++];
			fBindings.put(parameterType.getName(), boundTo);
		}
		
		if (!boundTo.isSubtypeOf(parameterType.getBound())) {
			throw new FactTypeError("Actual type " + boundTo + " is not a subtype of the bound " + parameterType.getBound() + " of the parameter type " + parameterType);
		}
		
		return boundTo;
	}

	public Type visitRelationType(RelationType type) {
		return tf.relType(type.getFieldTypes().accept(this));
	}

	public Type visitSet(SetType type) {
		return tf.setType(type.getElementType().accept(this));
	}

	public Type visitSourceLocation(SourceLocationType type) {
		return type;
	}

	public Type visitSourceRange(SourceRangeType type) {
		return type;
	}

	public Type visitString(StringType type) {
		return type;
	}

	public Type visitTree(TreeType type) {
		return type;
	}

	public Type visitTreeNode(TreeNodeType type) {
		return tf.treeNodeType((NamedTreeType) type.getSuperType().accept(this), type.getName(), type.getChildrenTypes().accept(this));
	}

	public Type visitTuple(TupleType type) {
		if (type.hasFieldNames()) {
			Type[] fTypes = new Type[type.getArity()];
			String[] fLabels = new String[type.getArity()];

			for (int i = 0; i < fTypes.length; i++) {
				fTypes[i] = type.getFieldType(i).accept(this);
				fLabels[i] = type.getFieldName(i);
			}

			return tf.tupleType(fTypes, fLabels);
		}
		else {
			Type[] fChildren = new Type[type.getArity()];
			for (int i = 0; i < fChildren.length; i++) {
				fChildren[i] = type.getFieldType(i).accept(this);
			}

			return tf.tupleType(fChildren);
		}
	}

	public Type visitValue(ValueType type) {
		return type;
	}

	public Type visitVoid(VoidType type) {
		return type;
	}
}
