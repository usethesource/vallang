/*******************************************************************************
* Copyright (c) 2008  CWI
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Jurgen Vinju  (jurgen@vinju.org)       
*******************************************************************************/
package org.eclipse.imp.pdb.facts.type;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITree;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.io.IValueReader;
import org.eclipse.imp.pdb.facts.io.IValueWriter;
import org.eclipse.imp.pdb.facts.visitors.NullVisitor;
import org.eclipse.imp.pdb.facts.visitors.VisitorException;

/**
 * This class converts types to representations of types as values and back.  
 * It can be used for (de)serialization of types via {@link IValueReader} and {@link IValueWriter}
 *
 */
public class TypeDescriptorFactory {
	private TypeFactory tf = TypeFactory.getInstance();
	private NamedTreeType typeSort = tf.namedTreeType("Type");
	private TreeNodeType doubleType = tf.treeNodeType(typeSort, "double");
	private TreeNodeType integerType = tf.treeNodeType(typeSort, "int");
	private TreeNodeType treeType = tf.treeNodeType(typeSort, "tree");
	private TreeNodeType listType = tf.treeNodeType(typeSort, "list", typeSort, "element");
	private TreeNodeType mapType = tf.treeNodeType(typeSort, "map", typeSort, "key", typeSort, "value");
	private TreeNodeType namedType = tf.treeNodeType(typeSort, "named", typeSort, "super");
	private TreeNodeType objectType = tf.treeNodeType(typeSort, "object", tf.stringType(), "name");
	private TreeNodeType relationType = tf.treeNodeType(typeSort, "relation", tf.listType(typeSort), "fields");
	private TreeNodeType setType = tf.treeNodeType(typeSort, "set", typeSort, "element");
	private TreeNodeType sourceLocationType = tf.treeNodeType(typeSort, "sourceLocation");
	private TreeNodeType sourceRangeType = tf.treeNodeType(typeSort, "sourceRange");
	private TreeNodeType stringType = tf.treeNodeType(typeSort, "string");
	private TreeNodeType treeNodeType = tf.treeNodeType(typeSort, "tree", typeSort, "sort", tf.stringType(), "name", tf.listType(typeSort), "children");
	private TreeNodeType namedTreeType = tf.treeNodeType(typeSort, "sort", tf.stringType(), "name");
	private TreeNodeType tupleType = tf.treeNodeType(typeSort, "tuple", tf.listType(typeSort), "fields");
	private TreeNodeType valueType = tf.treeNodeType(typeSort, "value");
	private TreeNodeType voidType = tf.treeNodeType(typeSort, "void");

	private static class InstanceHolder {
		public static TypeDescriptorFactory sInstance = new TypeDescriptorFactory();
	}
	
	private TypeDescriptorFactory() {}

	public static TypeDescriptorFactory getInstance() {
		return InstanceHolder.sInstance;
	}
	
	/**
	 * Create a representation of a type for use in (de)serialization or other
	 * computations on types.
	 * 
	 * @param factory the factory to use to construct the values
	 * @param type    the type to convert to a value
	 * @return a value that represents this type and can be convert back to 
	 *         the original type via {@link TypeDescriptorFactory#fromTypeDescriptor(ITree)}
	 */
	public IValue toTypeDescriptor(IValueFactory factory, Type type) {
		return type.accept(new ToTypeVisitor(factory));
	}
	
	/**
	 * Construct a type that is represented by this value. Will only work for values
	 * that have been constructed using {@link TypeDescriptorFactory#toTypeDescriptor(IValueFactory, Type)},
	 * or something that exactly mimicked it.
	 * 
	 * @param descriptor a value that represents a type
	 * @return a type that was represented by the descriptor
	 * @throws TypeDeclarationException if the descriptor is not a valid type descriptor
	 */
	public Type fromTypeDescriptor(IValue descriptor) throws TypeDeclarationException {
		try {
			return descriptor.accept(new FromTypeVisitor());
		} catch (VisitorException e) {
			throw new TypeDeclarationException(e.getMessage());
		}
	}
	
	private class FromTypeVisitor extends NullVisitor<Type> {
		@Override
		public Type visitNode(INode o) throws VisitorException {
			TreeNodeType node = o.getTreeNodeType();
		
			if (node == doubleType) {
				return tf.doubleType();
			}
			else if (node == integerType) {
				return tf.integerType();
			}
			else if (node == treeType) {
				return tf.treeType();
			}
			else if (node == listType) {
				return tf.listType(o.get("element").accept(this));
			}
			else if (node == mapType) {
				return tf.mapType(o.get("key").accept(this), o.get("value").accept(this));
			}
			else if (node == namedType) {
				return tf.namedType(((IString) o.get("name")).getValue(), o.get("super").accept(this));
			}
			else if (node == relationType) {
				IList fieldValues = (IList) o.get("fields");
				List<Type> fieldTypes = new LinkedList<Type>();
				
				for (IValue field : fieldValues) {
					fieldTypes.add(field.accept(this));
				}
				
				return tf.relType(tf.tupleType(fieldTypes));
			}
			else if (node == setType) {
				return tf.setType(o.get("element").accept(this));
			}
			else if (node == sourceLocationType) {
				return tf.sourceLocationType();
			}
			else if (node == sourceRangeType) {
				return tf.sourceRangeType();
			}
			else if (node == stringType) {
				return tf.stringType();
			}
			else if (node == treeNodeType) {
				NamedTreeType sort = (NamedTreeType) o.get("sort").accept(this);
				String name = ((IString) o.get("name")).getValue();
				
				IList childrenValues = (IList) o.get("children");
				List<Type> childrenTypes = new LinkedList<Type>();
				
				for (IValue child : childrenValues) {
					childrenTypes.add(child.accept(this));
				}
				
				return tf.treeNodeType(sort, name, tf.tupleType(childrenTypes));
			}
			else if (node == namedTreeType) {
				return tf.namedTreeType(((IString) o.get("name")).getValue());
			}
			else if (node == tupleType) {
				IList fieldValues = (IList) o.get("fields");
				List<Type> fieldTypes = new LinkedList<Type>();
				
				for (IValue field : fieldValues) {
					fieldTypes.add(field.accept(this));
				}
				
				return tf.tupleType(fieldTypes);	
			}
			else if (node == valueType) {
				return tf.valueType();
			}
			
			
			throw new FactTypeError("Unexpected type representation encountered: " + o);
		}
	}
	
	private class ToTypeVisitor implements ITypeVisitor<ITree> {
		private IValueFactory vf;
		
		public ToTypeVisitor(IValueFactory factory) {
			this.vf = factory;
		}
		
		public ITree visitDouble(DoubleType type) {
			return vf.tree(doubleType);
		}

		public ITree visitInteger(IntegerType type) {
			return vf.tree(integerType);
		}

		public ITree visitList(ListType type) {
			return vf.tree(listType, type.getElementType().accept(this));
		}

		public ITree visitMap(MapType type) {
			return vf.tree(mapType, type.getKeyType().accept(this), type.getValueType().accept(this));
		}

		public ITree visitNamed(NamedType type) {
			return vf.tree(namedType, type.getSuperType().accept(this));
		}

		public <U> ITree visitObject(ObjectType<U> type) {
			return vf.tree(objectType, vf.string(type.getClass().getCanonicalName()));
		}
		
		public ITree visitRelationType(RelationType type) {
			IListWriter w = vf.listWriter(typeSort);
			
			for (Type field : type.getFieldTypes()) {
				w.append(field.accept(this));
			}
			
			return vf.tree(relationType, w.done());
		}

		public ITree visitSet(SetType type) {
			return vf.tree(setType, type.getElementType().accept(this));
		}

		public ITree visitSourceLocation(SourceLocationType type) {
			return vf.tree(sourceLocationType);
		}

		public ITree visitSourceRange(SourceRangeType type) {
			return vf.tree(sourceRangeType);
		}

		public ITree visitString(StringType type) {
			return vf.tree(stringType);
		}

		public ITree visitTreeNode(TreeNodeType type) {
			IListWriter w = vf.listWriter(typeSort);
			
			for (Type field : type.getChildrenTypes()) {
				w.append(field.accept(this));
			}
			
			return vf.tree(treeNodeType, type.getNamedTreeType().accept(this), vf.string(type.getName()), w.done());
		}

		public ITree visitNamedTree(NamedTreeType type) {
			return vf.tree(namedTreeType, vf.string(type.getName()));
		}

		public ITree visitTuple(TupleType type) {
			IListWriter w = vf.listWriter(typeSort);
			
			for (Type field : type) {
				w.append(field.accept(this));
			}
			
			
			return vf.tree(tupleType, w.done());
		}

		public ITree visitValue(ValueType type) {
			return vf.tree(valueType);
		}

		public ITree visitVoid(VoidType type) {
			return vf.tree(voidType);
		}

		public ITree visitTree(TreeType type) {
			return vf.tree(treeType);
		}
	}
}
