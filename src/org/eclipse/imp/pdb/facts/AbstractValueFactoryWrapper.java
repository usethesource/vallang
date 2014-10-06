package org.eclipse.imp.pdb.facts;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.eclipse.imp.pdb.facts.exceptions.FactTypeUseException;
import org.eclipse.imp.pdb.facts.type.Type;

@SuppressWarnings("deprecation")
public abstract class AbstractValueFactoryWrapper implements IValueFactory {
	private final IValueFactory vf;

	public AbstractValueFactoryWrapper(IValueFactory wrapped) {
		this.vf = wrapped;
	}
	
	@Override
	public IInteger integer(String i) throws NumberFormatException {
		return vf.integer(i);
	}

	@Override
	public IInteger integer(int i) {
		return vf.integer(i);
	}

	@Override
	public IInteger integer(long i) {
		return vf.integer(i);
	}

	@Override
	public IInteger integer(byte[] a) {
		return vf.integer(a);
	}

	@Override
	public IRational rational(int a, int b) {
		return vf.rational(a, b);
	}

	@Override
	public IRational rational(long a, long b) {
		return vf.rational(a, b);
	}

	@Override
	public IRational rational(IInteger a, IInteger b) {
		return vf.rational(a, b);
	}

	@Override
	public IRational rational(String rat) throws NumberFormatException {
		return vf.rational(rat);
	}

	@Override
	public IReal real(String s) throws NumberFormatException {
		return vf.real(s);
	}

	@Override
	public IReal real(String s, int p) throws NumberFormatException {
		return vf.real(s, p);
	}

	@Override
	public IReal real(double d) {
		return vf.real(d);
	}

	@Override
	public IReal real(double d, int p) {
		return vf.real(d,p);
	}

	@Override
	public int getPrecision() {
		return vf.getPrecision();
	}

	@Override
	public int setPrecision(int p) {
		return vf.setPrecision(p);
	}

	@Override
	public IReal pi(int precision) {
		return vf.pi(precision);
	}

	@Override
	public IReal e(int precision) {
		return vf.e(precision);
	}

	@Override
	public IString string(String s) {
		return vf.string(s);
	}

	@Override
	public IString string(int[] chars) throws IllegalArgumentException {
		return vf.string(chars);
	}

	@Override
	public IString string(int ch) throws IllegalArgumentException {
		return vf.string(ch);
	}

	
	@Override
	public ISourceLocation sourceLocation(URI uri, int offset, int length,
			int beginLine, int endLine, int beginCol, int endCol) {
		return vf.sourceLocation(uri, offset, length, beginLine, endLine, beginCol, endCol);
	}

	@Override
	public ISourceLocation sourceLocation(ISourceLocation loc, int offset,
			int length, int beginLine, int endLine, int beginCol, int endCol) {
		return vf.sourceLocation(loc, offset, length, beginLine, endLine, beginCol, endCol);
	}

	@Override
	public ISourceLocation sourceLocation(URI uri, int offset, int length) {
		return vf.sourceLocation(uri, offset, length);
	}

	@Override
	public ISourceLocation sourceLocation(ISourceLocation loc, int offset, int length) {
		return vf.sourceLocation(loc, offset, length);
	}

	@Override
	public ISourceLocation sourceLocation(String path, int offset, int length,
			int beginLine, int endLine, int beginCol, int endCol) {
		return vf.sourceLocation(path, offset, length, beginLine, endLine, beginCol, endCol);
	}

	@Override
	public ISourceLocation sourceLocation(URI uri) {
		return vf.sourceLocation(uri);
	}

	@Override
	public ISourceLocation sourceLocation(String scheme, String authority,
			String path) throws URISyntaxException {
		return vf.sourceLocation(scheme, authority, path);
	}

	@Override
	public ISourceLocation sourceLocation(String scheme, String authority,
			String path, String query, String fragment)
			throws URISyntaxException {
		return vf.sourceLocation(scheme, authority, path, query, fragment);
	}

	@Override
	public ISourceLocation sourceLocation(String path) {
		return vf.sourceLocation(path);
	}

	@Override
	public ITuple tuple() {
		return vf.tuple();
	}

	@Override
	public ITuple tuple(IValue... args) {
		return vf.tuple(args);
	}

	@Override
	public ITuple tuple(Type type, IValue... args) {
		return vf.tuple(type, args);
	}

	@Override
	public INode node(String name) {
		return vf.node(name);
	}

	@Override
	public INode node(String name, IValue... children) {
		return vf.node(name, children);
	}

	@Override
	public INode node(String name, Map<String, IValue> annotations,
			IValue... children) throws FactTypeUseException {
		return vf.node(name, annotations, children);
	}

	@Override
	public INode node(String name, IValue[] children,
			Map<String, IValue> keyArgValues) throws FactTypeUseException {
		return vf.node(name, children, keyArgValues);
	}

	@Override
	public IConstructor constructor(Type constructor) {
		return vf.constructor(constructor);
	}

	@Override
	public IConstructor constructor(Type constructor, IValue... children)
			throws FactTypeUseException {
		return vf.constructor(constructor, children);
	}

	@Override
	public IConstructor constructor(Type constructor,
			Map<String, IValue> annotations, IValue... children)
			throws FactTypeUseException {
		return vf.constructor(constructor, annotations, children);
	}

	@Override
	public IConstructor constructor(Type constructor, IValue[] children,
			Map<String, IValue> kwParams) throws FactTypeUseException {
		return vf.constructor(constructor, children, kwParams);
	}

	@Override
	public ISet set(Type eltType) {
		return vf.set(eltType);
	}

	@Override
	public ISetWriter setWriter(Type eltType) {
		return vf.setWriter(eltType);
	}

	@Override
	public ISetWriter setWriter() {
		return vf.setWriter();
	}

	@Override
	public ISet set(IValue... elems) {
		return vf.set(elems);
	}

	@Override
	public IList list(Type eltType) {
		return vf.list(eltType);
	}

	@Override
	public IListWriter listWriter(Type eltType) {
		return vf.listWriter(eltType);
	}

	@Override
	public IListWriter listWriter() {
		return vf.listWriter();
	}

	@Override
	public IList list(IValue... elems) {
		return vf.list(elems);
	}

	@Override
	public IList listRelation(Type tupleType) {
		return vf.listRelation(tupleType);
	}

	@Override
	public IList listRelation(IValue... elems) {
		return vf.listRelation(elems);
	}

	@Override
	public IListWriter listRelationWriter(Type type) {
		return vf.listRelationWriter(type);
	}

	@Override
	public IListWriter listRelationWriter() {
		return vf.listRelationWriter();
	}

	@Override
	public ISet relation(Type tupleType) {
		return vf.relation(tupleType);
	}

	@Override
	public ISetWriter relationWriter(Type type) {
		return vf.relationWriter(type);
	}

	@Override
	public ISetWriter relationWriter() {
		return vf.relationWriter();
	}

	@Override
	public ISet relation(IValue... elems) {
		return vf.relation(elems);
	}

	@Override
	public IMap map(Type key, Type value) {
		return vf.map(key, value);
	}

	@Override
	public IMap map(Type mapType) {
		return vf.map(mapType);
	}

	@Override
	public IMapWriter mapWriter(Type mapType) {
		return vf.mapWriter(mapType);
	}

	@Override
	public IMapWriter mapWriter(Type key, Type value) {
		return vf.mapWriter(key, value);
	}

	@Override
	public IMapWriter mapWriter() {
		return vf.mapWriter();
	}

	@Override
	public IBool bool(boolean value) {
		return vf.bool(value);
	}

	@Override
	public IDateTime date(int year, int month, int day) {
		return vf.date(year, month, day);
	}

	@Override
	public IDateTime time(int hour, int minute, int second, int millisecond) {
		return vf.time(hour, minute, second, millisecond);
	}

	@Override
	public IDateTime time(int hour, int minute, int second, int millisecond,
			int hourOffset, int minuteOffset) {
		return vf.time(hour, minute, second, millisecond, hourOffset, minuteOffset);
	}

	@Override
	public IDateTime datetime(int year, int month, int day, int hour,
			int minute, int second, int millisecond) {
		return vf.datetime(year, month, day, hour, minute, second, millisecond);
	}

	@Override
	public IDateTime datetime(int year, int month, int day, int hour,
			int minute, int second, int millisecond, int hourOffset,
			int minuteOffset) {
		return vf.datetime(year, month, day, hourOffset, minuteOffset, second, millisecond);
	}

	@Override
	public IDateTime datetime(long instant) {
		return vf.datetime(instant);
	}
}
