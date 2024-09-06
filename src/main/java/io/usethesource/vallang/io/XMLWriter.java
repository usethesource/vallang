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
package io.usethesource.vallang.io;

import java.io.IOException;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IExternalValue;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IMap;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.exceptions.UnsupportedTypeException;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeStore;

/**
 * This IValueWriter serializes values to XML documents.
 * It will not serialize all IValues, see <code>XMLReader</code> for limitations.
 */
public class XMLWriter implements IValueTextWriter {
    private static final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    public void write(IValue value, java.io.Writer stream) throws IOException {
        try {
            Document doc = dbf.newDocumentBuilder().newDocument();

            Node top = give(value, doc);
            doc.appendChild(top);

            Transformer t = TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(OutputKeys.INDENT, "yes");

            t.transform(new DOMSource(doc), new StreamResult(stream));
        } catch (ParserConfigurationException e) {
            throw new IOException("XML configuration is invalid: " + e.getMessage());
        } catch (TransformerException e) {
            throw new IOException("Exception while serializing XML: " + e.getMessage());
        } catch (DOMException e) {
            throw new UnsupportedTypeException("" + e.getMessage(), value.getType());
        }
    }

    public void write(IValue value, java.io.Writer stream, TypeStore typeStore) throws IOException {
        write(value, stream);
    }

    private Node give(IValue value, Document doc) {
        Type type = value.getType();

        if (type.isAbstractData()) {
            Type node = ((IConstructor) value).getConstructorType();

            if (isListWrapper(node)) {
                return giveList((INode) value,  doc);
            }
            else if (isSetWrapper(node)) {
                return giveSet((INode) value, doc);
            }
            else if (isRelationWrapper(node)) {
                return giveRelation((INode) value, doc);
            }
            else if (isMapWrapper(node)) {
                return giveMap((INode) value, doc);
            }
            else {
              return giveTree((INode) value, doc);
            }
        }
        else if (type.isString()) {
            return giveString((IString) value, doc);
        }
        else if (type.isInteger()) {
            return giveInt((IInteger) value, doc);
        }
        else if (type.isRational()) {
            return giveRational((IRational) value, doc);
        }
        else if (type.isReal()) {
            return giveDouble((IReal) value, doc);
        }
        else if (type.isExternalType()) {
            return giveExternal((IExternalValue) value, doc);
        }

        throw new UnsupportedTypeException(
                "Outermost or nested tuples, lists, sets, relations or maps are not allowed.", type);
    }

    private boolean isListWrapper(Type nodeType) {
            return nodeType.getArity() == 1
                    && nodeType.getFieldTypes().getFieldType(0).isList();
    }

    private boolean isSetWrapper(Type nodeType) {
            return nodeType.getArity() == 1
                    && nodeType.getFieldTypes().getFieldType(0).isSet();
    }

    private boolean isRelationWrapper(Type nodeType) {
            return nodeType.getArity() == 1
                    && nodeType.getFieldTypes().getFieldType(0)
                            .isRelation();
    }

    private boolean isMapWrapper(Type nodeType) {
            return nodeType.getArity() == 1
                    && nodeType.getFieldTypes().getFieldType(0).isMap();
    }


    private Node giveDouble(IReal value, Document doc) {
        return doc.createTextNode(value.toString());
    }

    private Node giveInt(IInteger value, Document doc) {
        return doc.createTextNode(value.toString());
    }

    private Node giveRational(IRational value, Document doc) {
        Element element = doc.createElementNS("values", "rat");
        element.setAttribute("num", value.numerator().toString());
        element.setAttribute("denom", value.denominator().toString());
        return element;
    }

    private Node giveString(IString value, Document doc) {
        return doc.createTextNode(value.getValue());
    }

    private Node giveExternal(IExternalValue value, Document doc) {
        return doc.createTextNode(value.toString());
    }

    private Node giveMap(INode node, Document doc) {
        Element treeNode = doc.createElement(node.getName());
        IMap map = (IMap) node.get(0);

        for (Entry<IValue, IValue> entry : (Iterable<Entry<IValue,IValue>>) () -> map.entryIterator()) {
            IValue key = entry.getKey();
            IValue value = entry.getValue();

            if (key.getType().isTuple()) {
                appendTupleElements(doc, treeNode, key);
            }
            else {
              treeNode.appendChild(give(key, doc));
            }

            if (value.getType().isTuple()) {
                appendTupleElements(doc, treeNode, value);
            }
            else {
                treeNode.appendChild(give(value, doc));
            }
        }

        return treeNode;
    }

    private void appendTupleElements(Document doc, Element treeNode, IValue tupleValue) {
        ITuple tuple = (ITuple) tupleValue;

        for (IValue element : tuple) {
            treeNode.appendChild(give(element, doc));
        }
    }

    private Node giveRelation(INode node, Document doc) {
        Element treeNode = doc.createElement(node.getName());
        ISet relation = (ISet) node.get(0);
        assert (relation.getType().isRelation());

        for (IValue tuple : relation) {
            appendTupleElements(doc, treeNode, tuple);
        }

        return treeNode;
    }

    private Node giveSet(INode node, Document doc) {
        Element treeNode = doc.createElement(node.getName());
        ISet set = (ISet) node.get(0);

        for (IValue elem : set) {
            if (elem.getType().isTuple()) {
              appendTupleElements(doc, treeNode, elem);
            }
            else {
              treeNode.appendChild(give(elem, doc));
            }
        }

        return treeNode;
    }

    private Node giveList(INode node, Document doc) {
        Element treeNode = doc.createElement(node.getName());
        IList list = (IList) node.get(0);

        for (IValue elem : list) {
            if (elem.getType().isTuple()) {
                appendTupleElements(doc, treeNode, elem);
            }
            else {
              treeNode.appendChild(give(elem, doc));
            }
        }

        return treeNode;
    }

    private Node giveTree(INode value,  Document doc) {
        Element treeNode = doc.createElement(value.getName());

        for (IValue child : value) {
            if (child.getType().isTuple()) {
                appendTupleElements(doc, treeNode, child);
            }
            else {
              treeNode.appendChild(give(child, doc));
            }
        }

        return treeNode;
    }
}
