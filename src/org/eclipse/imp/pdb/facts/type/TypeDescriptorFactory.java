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
	private Type typeSort = tf.namedTreeType("Type");
	private Type boolType = tf.treeNodeType(typeSort, "bool");
	private Type doubleType = tf.treeNodeType(typeSort, "double");
	private Type integerType = tf.treeNodeType(typeSort, "int");
	private Type treeType = tf.treeNodeType(typeSort, "tree");
	private Type listType = tf.treeNodeType(typeSort, "list", typeSort, "element");
	private Type mapType = tf.treeNodeType(typeSort, "map", typeSort, "key", typeSort, "value");
	private Type namedType = tf.treeNodeType(typeSort, "named", typeSort, "super");
	private Type relationType = tf.treeNodeType(typeSort, "relation", tf.listType(typeSort), "fields");
	private Type setType = tf.treeNodeType(typeSort, "set", typeSort, "element");
	private Type sourceLocationType = tf.treeNodeType(typeSort, "sourceLocation");
	private Type sourceRangeType = tf.treeNodeType(typeSort, "sourceRange");
	private Type stringType = tf.treeNodeType(typeSort, "string");
	private Type treeNodeType = tf.treeNodeType(typeSort, "tree", typeSort, "sort", tf.stringType(), "name", tf.listType(typeSort), "children");
	private Type namedTreeType = tf.treeNodeType(typeSort, "sort", tf.stringType(), "name");
	private Type parameterType = tf.treeNodeType(typeSort, "parameter", tf.stringType(), "name", typeSort, "bound");
	private Type tupleType = tf.treeNodeType(typeSort, "tuple", tf.listType(typeSort), "fields");
	private Type valueType = tf.treeNodeType(typeSort, "value");
	private Type voidType = tf.treeNodeType(typeSort, "void");

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
			Type node = o.getType();
		
			if (node == boolType) {
				return tf.boolType();
			}
			else if (node == doubleType) {
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
				
				return tf.relTypeFromTuple(tf.tupleType(fieldTypes));
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
			else if (node == parameterType) {
				return tf.parameterType(((IString) o.get("name")).getValue(), o.get("bound").accept(this));
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
		
		public ITree visitDouble(Type type) {
			return vf.tree(doubleType);
		}

		public ITree visitInteger(Type type) {
			return vf.tree(integerType);
		}

		public ITree visitList(Type type) {
			return vf.tree(listType, type.getElementType().accept(this));
		}

		public ITree visitMap(Type type) {
			return vf.tree(mapType, type.getKeyType().accept(this), type.getValueType().accept(this));
		}

		public ITree visitNamed(Type type) {
			return vf.tree(namedType, type.getSuperType().accept(this));
		}

		public ITree visitRelationType(Type type) {
			IListWriter w = vf.listWriter(typeSort);
			
			for (Type field : type.getFieldTypes()) {
				w.append(field.accept(this));
			}
			
			return vf.tree(relationType, w.done());
		}

		public ITree visitSet(Type type) {
			return vf.tree(setType, type.getElementType().accept(this));
		}

		public ITree visitSourceLocation(Type type) {
			return vf.tree(sourceLocationType);
		}

		public ITree visitSourceRange(Type type) {
			return vf.tree(sourceRangeType);
		}

		public ITree visitString(Type type) {
			return vf.tree(stringType);
		}

		public ITree visitTreeNode(Type type) {
			IListWriter w = vf.listWriter(typeSort);
			
			for (Type field : type.getFieldTypes()) {
				w.append(field.accept(this));
			}
			
			return vf.tree(treeNodeType, type.getSuperType().accept(this), vf.string(type.getName()), w.done());
		}

		public ITree visitNamedTree(Type type) {
			return vf.tree(namedTreeType, vf.string(type.getName()));
		}

		public ITree visitTuple(Type type) {
			IListWriter w = vf.listWriter(typeSort);
			
			for (Type field : type) {
				w.append(field.accept(this));
			}
			
			
			return vf.tree(tupleType, w.done());
		}

		public ITree visitValue(Type type) {
			return vf.tree(valueType);
		}

		public ITree visitVoid(Type type) {
			return vf.tree(voidType);
		}

		public ITree visitTree(Type type) {
			return vf.tree(treeType);
		}

		public ITree visitBool(Type type) {
			return vf.tree(boolType);
		}

		public ITree visitParameter(Type type) {
			return vf.tree(parameterType, vf.string(type.getName()), type.getBound().accept(this));
		}
	}
}
