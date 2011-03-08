package org.eclipse.imp.pdb.facts.impl;

import java.net.URI;

import org.eclipse.imp.pdb.facts.IBool;
import org.eclipse.imp.pdb.facts.IConstructor;
import org.eclipse.imp.pdb.facts.IDateTime;
import org.eclipse.imp.pdb.facts.IInteger;
import org.eclipse.imp.pdb.facts.IList;
import org.eclipse.imp.pdb.facts.IListWriter;
import org.eclipse.imp.pdb.facts.IMap;
import org.eclipse.imp.pdb.facts.IMapWriter;
import org.eclipse.imp.pdb.facts.INode;
import org.eclipse.imp.pdb.facts.IReal;
import org.eclipse.imp.pdb.facts.IRelation;
import org.eclipse.imp.pdb.facts.IRelationWriter;
import org.eclipse.imp.pdb.facts.ISet;
import org.eclipse.imp.pdb.facts.ISetWriter;
import org.eclipse.imp.pdb.facts.ISourceLocation;
import org.eclipse.imp.pdb.facts.IString;
import org.eclipse.imp.pdb.facts.ITuple;
import org.eclipse.imp.pdb.facts.IValue;
import org.eclipse.imp.pdb.facts.IValueFactory;
import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.Type;

/**
 * This class provides a default way of easily reusing existing implementations of IValueFactory without having
 * to extend them again and again using inheritance. Clients extend this class and override the methods that need 
 * special handling.
 */
public abstract class AbstractValueFactoryAdapter implements IValueFactory {
	private final IValueFactory adapted;
	
	public AbstractValueFactoryAdapter(IValueFactory adapted) {
		this.adapted = adapted;
	}
	
	public IBool bool(boolean value) {
		return adapted.bool(value);
	}
	

	public IConstructor constructor(Type constructor) {
		return adapted.constructor(constructor);
	}

	public IConstructor constructor(Type constructor, IValue... children)
			throws FactTypeUseException {
		return adapted.constructor(constructor, children);
	}

	public IDateTime date(int year, int month, int day) {
		return adapted.date(year, month, day);
	}

	public IDateTime datetime(int year, int month, int day, int hour,
			int minute, int second, int millisecond) {
		return adapted.datetime(year, month, day, hour, minute, second, millisecond);
	}

	public IDateTime datetime(int year, int month, int day, int hour,
			int minute, int second, int millisecond, int hourOffset,
			int minuteOffset) {
		return adapted.datetime(year, month, day, hour, minute, second, millisecond, hourOffset, minuteOffset);
	}

	public IDateTime datetime(long instant) {
		return adapted.datetime(instant);
	}

	public IInteger integer(String i) throws NumberFormatException {
		return adapted.integer(i);
	}

	public IInteger integer(int i) {
		return adapted.integer(i);
	}

	public IInteger integer(long i) {
		return adapted.integer(i);
	}

	public IInteger integer(byte[] a) {
		return adapted.integer(a);
	}

	public IList list(Type eltType) {
		return adapted.list(eltType);
	}

	public IList list(IValue... elems) {
		return adapted.list(elems);
	}

	public IListWriter listWriter(Type eltType) {
		return adapted.listWriter(eltType);
	}

	public IListWriter listWriter() {
		return adapted.listWriter();
	}

	public IMap map(Type key, Type value) {
		return adapted.map(key, value);
	}

	public IMapWriter mapWriter(Type key, Type value) {
		return adapted.mapWriter(key, value);
	}

	public IMapWriter mapWriter() {
		return adapted.mapWriter();
	}

	public INode node(String name) {
		return adapted.node(name);
	}

	public INode node(String name, IValue... children) {
		return adapted.node(name, children);
	}

	public IReal real(String s) throws NumberFormatException {
		return adapted.real(s);
	}

	public IReal real(double d) {
		return adapted.real(d);
	}

	public IRelation relation(Type tupleType) {
		return adapted.relation(tupleType);
	}

	public IRelation relation(IValue... elems) {
		return adapted.relation(elems);
	}

	public IRelationWriter relationWriter(Type type) {
		return adapted.relationWriter(type);
	}

	public IRelationWriter relationWriter() {
		return adapted.relationWriter();
	}

	public ISet set(Type eltType) {
		return adapted.set(eltType);
	}

	public ISet set(IValue... elems) {
		return adapted.set(elems);
	}

	public ISetWriter setWriter(Type eltType) {
		return adapted.setWriter(eltType);
	}

	public ISetWriter setWriter() {
		return adapted.setWriter();
	}

	public ISourceLocation sourceLocation(URI uri, int offset, int length,
			int beginLine, int endLine, int beginCol, int endCol) {
		return adapted.sourceLocation(uri, offset, length, beginLine, endLine, beginCol, endCol);
	}

	public ISourceLocation sourceLocation(String path, int offset, int length,
			int beginLine, int endLine, int beginCol, int endCol) {
		return adapted.sourceLocation(path, offset, length, beginLine, endLine, beginCol, endCol);
	}

	public ISourceLocation sourceLocation(URI uri) {
		return adapted.sourceLocation(uri);
	}

	public ISourceLocation sourceLocation(String path) {
		return adapted.sourceLocation(path);
	}

	public IString string(String s) {
		return adapted.string(s);
	}

	public IDateTime time(int hour, int minute, int second, int millisecond) {
		return adapted.time(hour, minute, second, millisecond);
	}

	public IDateTime time(int hour, int minute, int second, int millisecond,
			int hourOffset, int minuteOffset) {
		return adapted.time(hour, minute, second, millisecond, hourOffset, minuteOffset);
	}

	public ITuple tuple() {
		return adapted.tuple();
	}

	public ITuple tuple(IValue... args) {
		return adapted.tuple(args);
	}
}
