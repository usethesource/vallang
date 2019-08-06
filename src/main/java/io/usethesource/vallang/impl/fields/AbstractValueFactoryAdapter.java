package io.usethesource.vallang.impl.fields;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IConstructor;
import io.usethesource.vallang.IDateTime;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IMapWriter;
import io.usethesource.vallang.INode;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ISetWriter;
import io.usethesource.vallang.ISourceLocation;
import io.usethesource.vallang.IString;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.exceptions.FactTypeUseException;
import io.usethesource.vallang.type.Type;

/**
 * This class provides a default way of easily reusing existing implementations
 * of IValueFactory without having to extend them again and again using
 * inheritance. Clients extend this class and override the methods that need
 * special handling.
 * 
 * Note: this class is intended to be sub-classed. It should not be abstract
 * because we want the compiler to check that it provides a facade for the full
 * IValueFactory interface.
 */
@SuppressWarnings("deprecation")
public /* abstract */ class AbstractValueFactoryAdapter implements IValueFactory {
    protected final IValueFactory adapted;

    public AbstractValueFactoryAdapter(IValueFactory adapted) {
        this.adapted = adapted;
    }

    @Override
    public IBool bool(boolean value) {
        return adapted.bool(value);
    }

    @Override
    public IConstructor constructor(Type constructor) {
        return adapted.constructor(constructor);
    }

    @Override
    public IConstructor constructor(Type constructor, IValue... children) {
        return adapted.constructor(constructor, children);
    }

    @Override
    public IDateTime date(int year, int month, int day) {
        return adapted.date(year, month, day);
    }

    @Override
    public IDateTime datetime(int year, int month, int day, int hour, int minute, int second, int millisecond) {
        return adapted.datetime(year, month, day, hour, minute, second, millisecond);
    }

    @Override
    public IDateTime datetime(int year, int month, int day, int hour, int minute, int second, int millisecond,
            int hourOffset, int minuteOffset) {
        return adapted.datetime(year, month, day, hour, minute, second, millisecond, hourOffset, minuteOffset);
    }

    @Override
    public IDateTime datetime(long instant) {
        return adapted.datetime(instant);
    }

    @Override
    public IDateTime datetime(long instant, int timezoneHours, int timezoneMinutes) {
        return adapted.datetime(instant, timezoneHours, timezoneMinutes);
    }

    @Override
    public IInteger integer(String i) {
        return adapted.integer(i);
    }
    
    @Override
    public IInteger integer(int i) {
        return adapted.integer(i);
    }

    @Override
    public IInteger integer(long i) {
        return adapted.integer(i);
    }

    @Override
    public IInteger integer(byte[] a) {
        return adapted.integer(a);
    }

    @Override
    public IList list(IValue... elems) {
        return adapted.list(elems);
    }

    @Override
    public IListWriter listWriter() {
        return adapted.listWriter();
    }

    @Override
    public IMapWriter mapWriter() {
        return adapted.mapWriter();
    }

    @Override
    public INode node(String name) {
        return adapted.node(name);
    }

    @Override
    public INode node(String name, IValue... children) {
        return adapted.node(name, children);
    }

    @Override
    public IReal real(String s) {
        return adapted.real(s);
    }

    @Override
    public IReal real(double d) {
        return adapted.real(d);
    }

    @Override
    public ISet set(IValue... elems) {
        return adapted.set(elems);
    }

    @Override
    public ISetWriter setWriter() {
        return adapted.setWriter();
    }

    @Override
    public ISourceLocation sourceLocation(URI uri, int offset, int length, int beginLine, int endLine, int beginCol,
            int endCol) {
        return adapted.sourceLocation(uri, offset, length, beginLine, endLine, beginCol, endCol);
    }

    @Override
    public ISourceLocation sourceLocation(String path, int offset, int length, int beginLine, int endLine, int beginCol,
            int endCol) {
        return adapted.sourceLocation(path, offset, length, beginLine, endLine, beginCol, endCol);
    }

    @Override
    public ISourceLocation sourceLocation(URI uri) {
        return adapted.sourceLocation(uri);
    }

    @Override
    public ISourceLocation sourceLocation(String path) {
        return adapted.sourceLocation(path);
    }

    @Override
    public ISourceLocation sourceLocation(ISourceLocation loc, int offset, int length, int beginLine, int endLine,
            int beginCol, int endCol) {
        return adapted.sourceLocation(loc, offset, length, beginLine, endLine, beginCol, endCol);
    }

    @Override
    public ISourceLocation sourceLocation(ISourceLocation loc, int offset, int length) {
        return adapted.sourceLocation(loc, offset, length);
    }

    @Override
    public ISourceLocation sourceLocation(String scheme, String authority, String path) throws URISyntaxException {
        return adapted.sourceLocation(scheme, authority, path);
    }

    @Override
    public ISourceLocation sourceLocation(String scheme, String authority, String path, String query, String fragment)
            throws URISyntaxException {
        return adapted.sourceLocation(scheme, authority, path, query, fragment);
    }

    @Override
    public IString string(String s) {
        return adapted.string(s);
    }

    @Override
    public IDateTime time(int hour, int minute, int second, int millisecond) {
        return adapted.time(hour, minute, second, millisecond);
    }

    @Override
    public IDateTime time(int hour, int minute, int second, int millisecond, int hourOffset, int minuteOffset) {
        return adapted.time(hour, minute, second, millisecond, hourOffset, minuteOffset);
    }

    @Override
    public ITuple tuple() {
        return adapted.tuple();
    }

    @Override
    public ITuple tuple(IValue... args) {
        return adapted.tuple(args);
    }

    @Override
    public IRational rational(int a, int b) {
        return adapted.rational(a, b);
    }

    @Override
    public IRational rational(long a, long b) {
        return adapted.rational(a, b);
    }

    @Override
    public IRational rational(IInteger a, IInteger b) {
        return adapted.rational(a, b);
    }

    @Override
    public IRational rational(String rat) {
        return adapted.rational(rat);
    }

    @Override
    public IReal real(String s, int p) {
        return adapted.real(s, p);
    }

    @Override
    public IReal real(double d, int p) {
        return adapted.real(d, p);
    }

    @Override
    public int getPrecision() {
        return adapted.getPrecision();
    }

    @Override
    public int setPrecision(int p) {
        return adapted.setPrecision(p);
    }

    @Override
    public IReal pi(int precision) {
        return adapted.pi(precision);
    }

    @Override
    public IReal e(int precision) {
        return adapted.e(precision);
    }

    @Override
    public IString string(int[] chars) {
        return adapted.string(chars);
    }

    @Override
    public IString string(int ch) {
        return adapted.string(ch);
    }

    @Override
    @Deprecated
    public ISourceLocation sourceLocation(URI uri, int offset, int length) {
        return adapted.sourceLocation(uri, offset, length);
    }

    @Override
    public INode node(String name, Map<String, IValue> annotations, IValue... children) {
        return adapted.node(name, annotations, children);
    }

    @Override
    public INode node(String name, IValue[] children, Map<String, IValue> keyArgValues) {
        return adapted.node(name, children, keyArgValues);
    }

    @Override
    @Deprecated
    public IConstructor constructor(Type constructor, Map<String, IValue> annotations, IValue... children)
            throws FactTypeUseException {
        return adapted.constructor(constructor, annotations, children);
    }

    @Override
    public IConstructor constructor(Type constructor, IValue[] children, Map<String, IValue> kwParams)
            throws FactTypeUseException {
        return adapted.constructor(constructor, children, kwParams);
    }

}
