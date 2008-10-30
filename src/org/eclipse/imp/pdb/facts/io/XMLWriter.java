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
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.imp.pdb.facts.IDouble;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITree;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.type.FactTypeError;
import org.eclipse.imp.pdb.facts.type.TreeNodeType;
import org.eclipse.imp.pdb.facts.type.TreeSortType;
import org.eclipse.imp.pdb.facts.type.Type;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This IValueWriter serializes values to XML documents.  
 * It will not serialize all IValues, see <code>XMLReader</code> for limitations. 
 */
public class XMLWriter implements IValueWriter {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private DocumentBuilder fBuilder;
    
	public void write(IValue value, OutputStream stream) throws IOException {
		try {
			fBuilder = dbf.newDocumentBuilder();
			Document doc = fBuilder.newDocument();
			
			Node top = yield(value, doc);
			doc.appendChild(top);
			
			Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");  
            
			t.transform(new DOMSource(doc), new StreamResult(stream));
		} catch (ParserConfigurationException e) {
			throw new IOException("XML configuration is invalid", e);
		} catch (TransformerException e) {
			throw new IOException("Exception while serializing XML", e);
		}
	}

	private Node yield(IValue value, Document doc) {
		Type type = value.getType();
		
		if (type.isTreeSortType()) {
			TreeSortType sort = (TreeSortType) type;
			TreeNodeType node = ((ITree) value).getTreeNodeType();
			String name = node.getName();
			
			if (XMLReader.isListWrapper(name,  sort)) {
				return yieldList((ITree) value,  doc);
			}
			else if (XMLReader.isSetWrapper(name, sort)) {
				return yieldSet((ITree) value, doc);
			}
			else if (XMLReader.isRelationWrapper(name, sort)) {
				return yieldRelation((ITree) value, doc);
			}
			else if (XMLReader.isMapWrapper(name, sort)) {
				return yieldMap((ITree) value, doc);
			}
			else {
			  return yieldTree((ITree) value, doc);
			}
		}
		else if (type.isStringType()) {
			return yieldString((IString) value, doc);
		}
		else if (type.isIntegerType()) {
			return yieldInt((IInteger) value, doc);
		}
		else if (type.isDoubleType()) {
			return yieldDouble((IDouble) value, doc);
		}

		throw new FactTypeError(
				"Outermost or nested tuples, lists, sets, relations or maps are not allowed: " + type);
	}
	
	private Node yieldDouble(IDouble value, Document doc) {
		return doc.createTextNode("" + value.getValue());
	}

	private Node yieldInt(IInteger value, Document doc) {
		return doc.createTextNode("" + value.getValue());
	}

	private Node yieldString(IString value, Document doc) {
		return doc.createTextNode(value.getValue());
	}

	private Node yieldMap(ITree tree, Document doc) {
		Element treeNode = doc.createElement(tree.getName());
		IMap map = (IMap) tree.get(0);
		
		for (IValue key : map) {
			IValue value = map.get(key);
			
			if (key.getBaseType().isTupleType()) {
				appendTupleElements(doc, treeNode, key);
			}
			else {
			  treeNode.appendChild(yield(key, doc));
			}
			
			if (value.getBaseType().isTupleType()) {
                appendTupleElements(doc, treeNode, value);
			}
			else {
			  treeNode.appendChild(yield(value, doc));
			}
		}

		return treeNode;
	}

	private void appendTupleElements(Document doc, Element treeNode, IValue tupleValue) {
		ITuple tuple = (ITuple) tupleValue;
		
		for (IValue element : tuple) {
			treeNode.appendChild(yield(element, doc));
		}
	}

	private Node yieldRelation(ITree tree, Document doc) {
		Element treeNode = doc.createElement(tree.getName());
		IRelation relation = (IRelation) tree.get(0);
		
		for (ITuple tuple : relation) {
			appendTupleElements(doc, treeNode, tuple);
		}

		return treeNode;
	}
	
	private Node yieldSet(ITree tree, Document doc) {
		Element treeNode = doc.createElement(tree.getName());
		ISet set = (ISet) tree.get(0);
		
		for (IValue elem : set) {
			if (elem.getBaseType().isTupleType()) {
			  appendTupleElements(doc, treeNode, elem);
			}
			else {
			  treeNode.appendChild(yield(elem, doc));
			}
		}

		return treeNode;
	}

	private Node yieldList(ITree tree, Document doc) {
		Element treeNode = doc.createElement(tree.getName());
		IList list = (IList) tree.get(0);
		
		for (IValue elem : list) {
			if (elem.getBaseType().isTupleType()) {
				appendTupleElements(doc, treeNode, elem);
			}
			else {
			  treeNode.appendChild(yield(elem, doc));
			}
		}

		return treeNode;
	}

	private Node yieldTree(ITree value,  Document doc) {
		Element treeNode = doc.createElement(value.getName());
		
		for (IValue child : value) {
			if (child.getBaseType().isTupleType()) {
				appendTupleElements(doc, treeNode, child);
			}
			else {
			  treeNode.appendChild(yield(child, doc));
			}
		}
		
		return treeNode;
	}
}
