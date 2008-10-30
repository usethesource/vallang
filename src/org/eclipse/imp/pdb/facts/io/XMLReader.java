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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.impl.hash.ValueFactory;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.ListType;
import org.eclipse.imp.pdb.facts.type.MapType;
import org.eclipse.imp.pdb.facts.type.RelationType;
import org.eclipse.imp.pdb.facts.type.SetType;
import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TreeSortType;
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
			throw new IOException("Parsing of value failed because XML was invalid", se);
		} catch (ParserConfigurationException pce) {
			throw new IOException("Parsing of value failed because XML configuration is wrong", pce);
		} catch (DOMException de) {
			throw new IOException("Parsing of value failed because of a XML document failure", de);
		} catch (NumberFormatException nfe) {
			throw new FactTypeError("Expected a number, got something different: " + nfe.getMessage(), nfe);
		}
	}
	
	private IValue parse(Node node, Type expected) {
		if (expected.isTreeSortType()) {
			TreeSortType sort = (TreeSortType) expected;
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

	private IValue parseMap(Node node, TreeSortType expected) {
		TreeNodeType nodeType = tf.signatureGet(expected, node.getNodeName());
		MapType mapType = (MapType) nodeType.getChildType(0);
		Type keyType = mapType.getKeyType();
		Type valueType = mapType.getValueType();
		NodeList children = node.getChildNodes();
		IMap map = vf.map(keyType, valueType);
		IMapWriter writer = map.getWriter();
		
		for (int i = 0; i + 1 < children.getLength(); ) {
			IValue key, value;
			
			if (keyType.isTupleType()) {
				TupleType tuple = (TupleType) keyType;
				IValue [] elements = new IValue[tuple.getArity()];
				for (int j = 0; j < tuple.getArity(); j++) {
					elements[i] = parse(children.item(i++), tuple.getFieldType(j));
				}
				
				key = vf.tuple(elements, tuple.getArity());
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
				
				value = vf.tuple(elements, tuple.getArity());
			}
			else {
			  value = parse(children.item(i++), valueType);
			}
			
			writer.put(key, value);
		}
		
		writer.done();
		return vf.tree(nodeType, map);
	}

	private IValue parseRelation(Node node, TreeSortType expected) {
		TreeNodeType nodeType = tf.signatureGet(expected, node.getNodeName());
		TupleType fields = ((RelationType) nodeType.getChildType(0)).getFieldTypes();
		NodeList children = node.getChildNodes();
		IRelation relation = vf.relation(fields);
		IRelationWriter writer = relation.getWriter();
		
		for (int i = 0; i < children.getLength(); ) {
			IValue[] elements = new IValue[fields.getArity()];
				
			for (int j = 0; i < children.getLength() && j < fields.getArity(); j++) {
			   elements[j] = parse(children.item(i++), fields.getFieldType(j));	
			}
				
				writer.insert(vf.tuple(elements, fields.getArity()));
		}
		
		writer.done();
		return vf.tree(nodeType, relation);
	}

	private IValue parseSet(Node node, TreeSortType expected) {
		TreeNodeType nodeType = tf.signatureGet(expected, node.getNodeName());
		Type elementType = ((SetType) nodeType.getChildType(0)).getElementType();
		NodeList children = node.getChildNodes();
		ISet set = vf.set(elementType);
		ISetWriter writer = set.getWriter();
		
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
				
				writer.insert(vf.tuple(elements, tuple.getArity()));
			}
		}
		
		writer.done();
		return vf.tree(nodeType, set);
	}

	private IValue parseList(Node node, TreeSortType expected) {
		TreeNodeType nodeType = tf.signatureGet(expected, node.getNodeName());
		Type elementType = ((ListType) nodeType.getChildType(0)).getElementType();
		NodeList children = node.getChildNodes();
		IList list = vf.list(elementType);
		IListWriter writer = list.getWriter();
		
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
				
				writer.append(vf.tuple(elements, tuple.getArity()));
			}
		}
		
		writer.done();
		return vf.tree(nodeType, list);
	}

    /*package*/ static boolean isListWrapper(String name, TreeSortType expected) {
		TreeNodeType nodeType = tf.signatureGet(expected, name);
		
		return nodeType.getArity() == 1 && 
		nodeType.getChildrenTypes().getFieldType(0).isListType();
	}
	
	/*package*/ static boolean isSetWrapper(String name, TreeSortType expected) {
		TreeNodeType nodeType = tf.signatureGet(expected, name);
		
		return nodeType.getArity() == 1 && 
		nodeType.getChildrenTypes().getFieldType(0).isSetType();
	}
	
	/*package*/ static boolean isRelationWrapper(String name, TreeSortType expected) {
		TreeNodeType nodeType = tf.signatureGet(expected, name);
		
		return nodeType.getArity() == 1 && 
		nodeType.getChildrenTypes().getFieldType(0).isRelationType();
	}
	
	/*package*/ static boolean isMapWrapper(String name, TreeSortType expected) {
		TreeNodeType nodeType = tf.signatureGet(expected, name);
		
		return nodeType.getArity() == 1 && 
		nodeType.getChildrenTypes().getFieldType(0).isMapType();
	}

	private IValue parseTreeSort(Node node, TreeSortType expected) {
		TreeNodeType nodeType = tf.signatureGet(expected, node.getNodeName());
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
				
				values[targetIndex++] = vf.tuple(elements, nodeType.getArity());
			}
			else {
			  values[targetIndex++] = parse(children.item(sourceIndex++), childType);
			}
		}
		
		return vf.tree(nodeType, values);
	}



	public static void main(String[] args) {
		IValueFactory vf = ValueFactory.getInstance();
		TypeFactory tf = TypeFactory.getInstance();
		XMLReader testReader = new XMLReader();
		TreeSortType Boolean = tf.treeSortType("Boolean");
		tf.treeType(Boolean, "true");
		tf.treeType(Boolean, "false");
		tf.treeType(Boolean, "and", Boolean, Boolean);
		tf.treeType(Boolean, "or", tf.listType(Boolean));
		tf.treeType(Boolean, "not", Boolean);
		tf.treeType(Boolean, "twotups", tf.tupleTypeOf(Boolean, Boolean), tf.tupleTypeOf(Boolean, Boolean));
		
		TreeSortType Name = tf.treeSortType("Name");
		tf.treeType(Name, "name", tf.stringType());
		tf.treeType(Boolean, "friends", tf.listType(Name));
		tf.treeType(Boolean, "couples", tf.listType(tf.tupleTypeOf(Name, Name)));
		String[] tests = {
			"<true/>",
			"<and><true/><false/></and>",
		    "<not><and><true/><false/></and></not>",
		    "<twotups><true/><false/><true/><false/></twotups>",
	        "<or><true/><false/><true/></or>",
	        "<friends><name>Hans</name><name>Bob</name></friends>",
	        "<or/>",
	        "<couples><name>A</name><name>B</name><name>C</name><name>D</name></couples>"
	        };
		
		try {
			for (String test : tests) {
				IValue result = testReader.read(vf, Boolean, new ByteArrayInputStream(test.getBytes()));
				System.err.println(test + " -> " + result);
			}
		} catch (FactTypeError e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
