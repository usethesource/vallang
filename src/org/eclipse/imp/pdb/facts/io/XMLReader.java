/*******************************************************************************
* Copyright (c) 2008 CWI.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    jurgen@vinju.org - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.pdb.facts.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.ListType;
import org.eclipse.imp.pdb.facts.type.MapType;
import org.eclipse.imp.pdb.facts.type.NamedTreeType;
import org.eclipse.imp.pdb.facts.type.RelationType;
import org.eclipse.imp.pdb.facts.type.SetType;
import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TupleType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.eclipse.imp.pdb.facts.type.TypeFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This IValueReader parses and validates certain forms of XML and deserializes
 * it as IValues. The forms of XML allowed are limited by a number of different
 * value types. In particular, it allows: <ul>
 *   <li> TreeSortTypes and TreeNodeTypes </li>
 *   <li> lists, sets, relations and maps, but not unless they are wrapped by a single
 *     TreeNodeType. I.o.w. a container must be the only child of a tree node.
 *     Elements of containers are juxtapositioned as children of this node.</li>
 *   <li> tuples, but not nested ones. And tuples of containers are not allowed.
 *     elements of tuples are juxtapositioned in the xml files.</li>
 *   <li> basic types, such as str, int, double; with the same restriction as for
 *     container types, they must be the only child of a tree node.</li>
 *   <li> lists of tuples, sets of tuples and maps of tuples are allowed, but not
 *     lists of lists, tuples of tuples, lists in tuples, sets in tuples, etc.
 *     If such nesting is needed, it is required to use a wrapping tree node.</li>
 * </ul>
 * There is no support for NamedTypes yet, only TreeSortType and TreeNodeType are 
 * allowed.
 *     
 * The limitations of this class are governed by wanting to avoid ambiguity
 * while validating XML using the pdb's type system and the inherent impedance
 * mismatch between the type system of pdb and the structure of XML.
 * 
 * Use this class to import many forms of XML data into PDB.
 * 
 */
public class XMLReader implements IValueReader {
	private DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
	private static TypeFactory tf = TypeFactory.getInstance();
	private IValueFactory vf;
	
	
	public IValue read(IValueFactory factory, Type type, InputStream stream)
			throws FactTypeError, IOException {
		this.vf = factory;
		
		try {
			Document doc = domFactory.newDocumentBuilder().parse(stream);
			return parse(doc.getDocumentElement(), type);
		} catch (SAXException se) {
			throw new IOException("Parsing of value failed because XML was invalid: " + se.getMessage());
		} catch (ParserConfigurationException pce) {
			throw new IOException("Parsing of value failed because XML configuration is wrong: " + pce.getMessage());
		} catch (DOMException de) {
			throw new IOException("Parsing of value failed because of a XML document failure: " + de.getMessage());
		} catch (NumberFormatException nfe) {
			throw new FactTypeError("Expected a number, got something different: " + nfe.getMessage(), nfe);
		}
	}
	
	private IValue parse(Node node, Type expected) {
		if (expected.isNamedTreeType()) {
			NamedTreeType sort = (NamedTreeType) expected;
			String name = node.getNodeName();
			
			if (isListWrapper(name,  sort)) {
				return parseList(node,  sort);
			}
			else if (isSetWrapper(name, sort)) {
				return parseSet(node, sort);
			}
			else if (isRelationWrapper(name, sort)) {
				return parseRelation(node, sort);
			}
			else if (isMapWrapper(name, sort)) {
				return parseMap(node, sort);
			}
			else {
			  return parseTreeSort(node, sort);
			}
		}
		else if (expected.isStringType()) {
			return parseString(node);
		}
		else if (expected.isIntegerType()) {
			return parseInt(node);
		}
		else if (expected.isDoubleType()) {
			return parseDouble(node);
		}

		throw new FactTypeError(
				"Outermost or nested tuples, lists, sets, relations or maps are not allowed: " + expected);
	}


	private IValue parseDouble(Node node) {
		return vf.dubble(Double.parseDouble(node.getNodeValue().trim()));
	}

	private IValue parseInt(Node node) {
		return vf.integer(Integer.parseInt(node.getNodeValue().trim()));
	}

	private IValue parseString(Node node) {
		return vf.string(node.getNodeValue());
	}

	private IValue parseMap(Node node, NamedTreeType expected) {
		List<TreeNodeType> nodeTypes = tf.lookupTreeNodeType(expected, node.getNodeName());
		// TODO: implement overloading
		TreeNodeType nodeType = nodeTypes.get(0);
		MapType mapType = (MapType) nodeType.getChildType(0);
		Type keyType = mapType.getKeyType();
		Type valueType = mapType.getValueType();
		NodeList children = node.getChildNodes();
		IMapWriter writer = mapType.writer(vf);
		
		for (int i = 0; i + 1 < children.getLength(); ) {
			IValue key, value;
			
			if (keyType.isTupleType()) {
				TupleType tuple = (TupleType) keyType;
				IValue [] elements = new IValue[tuple.getArity()];
				for (int j = 0; j < tuple.getArity(); j++) {
					elements[i] = parse(children.item(i++), tuple.getFieldType(j));
				}
				
				key = vf.tuple(elements);
			}
			else {
			  key = parse(children.item(i++), keyType);
			}
			
			if (valueType.isTupleType()) {
				TupleType tuple = (TupleType) keyType;
				IValue [] elements = new IValue[tuple.getArity()];
				for (int j = 0; j < tuple.getArity(); j++) {
					elements[i] = parse(children.item(i++), tuple.getFieldType(j));
				}
				
				value = vf.tuple(elements);
			}
			else {
			  value = parse(children.item(i++), valueType);
			}
			
			writer.put(key, value);
		}
		
		
		return vf.tree(nodeType, writer.done());
	}

	private IValue parseRelation(Node node, NamedTreeType expected) {
		List<TreeNodeType> nodeTypes = tf.lookupTreeNodeType(expected, node.getNodeName());
		// TODO implement overloading
		TreeNodeType nodeType = nodeTypes.get(0);
		RelationType relType = (RelationType) nodeType.getChildType(0);
		TupleType fields = relType.getFieldTypes();
		NodeList children = node.getChildNodes();
		ISetWriter writer = relType.writer(vf);
		
		for (int i = 0; i < children.getLength(); ) {
			IValue[] elements = new IValue[fields.getArity()];
				
			for (int j = 0; i < children.getLength() && j < fields.getArity(); j++) {
			   elements[j] = parse(children.item(i++), fields.getFieldType(j));	
			}
				
				writer.insert(vf.tuple(elements));
		}
		
		return vf.tree(nodeType, writer.done());
	}

	private IValue parseSet(Node node, NamedTreeType expected) {
		List<TreeNodeType> nodeTypes = tf.lookupTreeNodeType(expected, node.getNodeName());
		// TODO implement overloading
		TreeNodeType nodeType = nodeTypes.get(0);
		SetType setType = (SetType) nodeType.getChildType(0);
		Type elementType = setType.getElementType();
		NodeList children = node.getChildNodes();
		ISetWriter writer = setType.writer(vf);
		
		if (!elementType.isTupleType()) {
			for (int i = 0; i < children.getLength(); i++) {
				writer.insert(parse(children.item(i), elementType));
			}
		} else {
			TupleType tuple = (TupleType) elementType;
			for (int i = 0; i < children.getLength(); ) {
				IValue[] elements = new IValue[tuple.getArity()];
				
				for (int j = 0; i < children.getLength() && j < tuple.getArity(); j++) {
				  elements[j] = parse(children.item(i++), tuple.getFieldType(j));	
				}
				
				writer.insert(vf.tuple(elements));
			}
		}
		
		return vf.tree(nodeType, writer.done());
	}

	private IValue parseList(Node node, NamedTreeType expected) {
		List<TreeNodeType> nodeTypes = tf.lookupTreeNodeType(expected, node.getNodeName());
		// TODO implement overloading
		TreeNodeType nodeType = nodeTypes.get(0);
		ListType listType = (ListType) nodeType.getChildType(0);
		Type elementType = listType.getElementType();
		NodeList children = node.getChildNodes();
		IListWriter writer = listType.writer(vf);
		
		if (!elementType.isTupleType()) {
			for (int i = 0; i < children.getLength(); i++) {
				writer.append(parse(children.item(i), elementType));
			}
		} else {
			TupleType tuple = (TupleType) elementType;
			for (int i = 0; i < children.getLength(); ) {
				IValue[] elements = new IValue[tuple.getArity()];
				
				for (int j = 0; i < children.getLength() && j < tuple.getArity(); j++) {
				  elements[j] = parse(children.item(i++), tuple.getFieldType(j));	
				}
				
				writer.append(vf.tuple(elements));
			}
		}
		
		return vf.tree(nodeType, writer.done());
	}

    /*package*/ static boolean isListWrapper(String name, NamedTreeType expected) {
		TreeNodeType nodeType = tf.lookupTreeNodeType(expected, name).get(0);
		
		return nodeType.getArity() == 1 && 
		nodeType.getChildrenTypes().getFieldType(0).isListType();
	}
	
	/*package*/ static boolean isSetWrapper(String name, NamedTreeType expected) {
		TreeNodeType nodeType = tf.lookupTreeNodeType(expected, name).get(0);
		
		return nodeType.getArity() == 1 && 
		nodeType.getChildrenTypes().getFieldType(0).isSetType();
	}
	
	/*package*/ static boolean isRelationWrapper(String name, NamedTreeType expected) {
		TreeNodeType nodeType = tf.lookupTreeNodeType(expected, name).get(0);
		
		return nodeType.getArity() == 1 && 
		nodeType.getChildrenTypes().getFieldType(0).isRelationType();
	}
	
	/*package*/ static boolean isMapWrapper(String name, NamedTreeType expected) {
		TreeNodeType nodeType = tf.lookupTreeNodeType(expected, name).get(0);
		
		return nodeType.getArity() == 1 && 
		nodeType.getChildrenTypes().getFieldType(0).isMapType();
	}

	private IValue parseTreeSort(Node node, NamedTreeType expected) {
		TreeNodeType nodeType = tf.lookupTreeNodeType(expected, node.getNodeName()).get(0);
		TupleType childrenTypes = nodeType.getChildrenTypes();
		NodeList children = node.getChildNodes();
		
	    IValue[] values = new IValue[nodeType.getArity()];
	    
	    int sourceIndex = 0;
	    int targetIndex = 0;
	    
		while(sourceIndex < children.getLength() && targetIndex < nodeType.getArity()) {
			Type childType = childrenTypes.getFieldType(targetIndex);
			
			if (childType.isTupleType()) {
				TupleType tuple = (TupleType) childType;
				IValue[] elements = new IValue[tuple.getArity()];
			  
				for (int tupleIndex = 0; tupleIndex < tuple.getArity() && sourceIndex < children.getLength(); tupleIndex++, sourceIndex++) {
				  elements[tupleIndex] = parse(children.item(sourceIndex), tuple.getFieldType(tupleIndex));
			    }
				
				values[targetIndex++] = vf.tuple(elements);
			}
			else {
			  values[targetIndex++] = parse(children.item(sourceIndex++), childType);
			}
		}
		
		return vf.tree(nodeType, values);
	}
}
