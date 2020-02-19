/*******************************************************************************
 * Copyright (c) 2009 Centrum Wiskunde en Informatica (CWI)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Arnold Lankamp - interfaces and implementation
 *    Paul Klint - added new methods and SubList
 *******************************************************************************/
package io.usethesource.vallang.impl.persistent;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Random;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IList;
import io.usethesource.vallang.IListWriter;
import io.usethesource.vallang.IRelation;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.impl.util.collections.ShareableValuesList;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

/*package*/ class List implements IList {
    protected static final TypeFactory typeFactory = TypeFactory.getInstance();
    protected static final Type voidType = typeFactory.voidType();

    protected final Type listType;

    protected final ShareableValuesList data;

    protected int hashCode = -1;

    /*package*/ static IList newList(Type elementType, ShareableValuesList data) {
        return new List(elementType, data);
    }

    private List(Type elementType, ShareableValuesList data){
        super();

        this.listType = typeFactory.listType(elementType);
        this.data = data;
    }

    @Override
    public IRelation<IList> asRelation() {
        return new ListRelation(this);
    }

    @Override
    public IListWriter writer() {
        return new ListWriter();
    }

    @Override
    public int size() {
        return length();
    }

    @Override
    public Type getType(){
        return listType;
    }

    @Override
    public int length(){
        return data.size();
    }

    @Override
    public boolean isEmpty(){
        return length() == 0;
    }

    @Override
    public IValue get(int index){
        return data.get(index);
    }

    @Override
    public boolean contains(IValue element){
        return data.contains(element);
    }

    @Override
    public Iterator<IValue> iterator(){
        return data.iterator();
    }

    @Override
    public IList append(IValue element){
        ShareableValuesList newData = new ShareableValuesList(data);
        newData.append(element);

        Type newElementType = getType().getElementType().lub(element.getType());
        return new ListWriter(newElementType, newData).done();
    }

    @Override
    public IList concat(IList other){
        ShareableValuesList newData = new ShareableValuesList(data);
        Iterator<IValue> otherIterator = other.iterator();
        while(otherIterator.hasNext()){
            newData.append(otherIterator.next());
        }

        Type newElementType = getType().getElementType().lub(other.getElementType());
        return new ListWriter(newElementType, newData).done();
    }

    @Override
    public IList insert(IValue element){
        ShareableValuesList newData = new ShareableValuesList(data);
        newData.insert(element);

        Type newElementType = getType().getElementType().lub(element.getType());
        return new ListWriter(newElementType, newData).done();
    }

    @Override
    public IList put(int index, IValue element) {
        ShareableValuesList newData = new ShareableValuesList(data);
        newData.set(index, element);

        Type newElementType = getType().getElementType().lub(element.getType());
        return new ListWriter(newElementType, newData).done();
    }

    @Override
    public IList delete(int index){
        ShareableValuesList newData = new ShareableValuesList(data);
        newData.remove(index);

        Type newElementType = TypeFactory.getInstance().voidType();
        for(IValue el : newData)
            newElementType = newElementType.lub(el.getType());

        return new ListWriter(newElementType, newData).done();
    }

    @Override
    public IList delete(IValue element){
        ShareableValuesList newData = new ShareableValuesList(data);

        if (newData.remove(element)) {
            Type newElementType = TypeFactory.getInstance().voidType();

            for (IValue el : newData) {
                newElementType = newElementType.lub(el.getType());
            }

            return new ListWriter(newElementType, newData).done();
        }

        return this;
    }

    @Override
    public IList reverse(){
        ShareableValuesList newData = new ShareableValuesList(data);
        newData.reverse();

        return new ListWriter(getType().getElementType(), newData).done();
    }

    @Override
    public IList shuffle(Random rand) {
        ShareableValuesList newData = new ShareableValuesList(data);
        // we use Fisher–Yates shuffle (or Knuth shuffle)
        // unbiased and linear time, since set and get are O(1)
        for (int i= newData.size() - 1; i >= 1; i--) {
            // we use the stack as tmp variable :)
            newData.set(i, newData.set(rand.nextInt(i + 1), newData.get(i)));
        }
        return new ListWriter(getType().getElementType(), newData).done();
    }

    @Override
    public  IList sublist(int offset, int length){
        if(length < 4){
            return materializedSublist(offset, length);
        }
        return new SubList(this, offset, length);
    }

    IList materializedSublist(int offset, int length){
        ShareableValuesList newData = data.subList(offset, length);

        Type oldElementType = getType().getElementType();
        Type newElementType = TypeFactory.getInstance().voidType();
        
        for(IValue el : newData) {
            if (newElementType == oldElementType) {
                // the type can only get more specific
                // once we've reached the type of the whole list, we can stop lubbing.
                break;
            }
            newElementType = newElementType.lub(el.getType());
        }

        return new ListWriter(newElementType, newData).done();
    }

    @Override
    public int hashCode(){
        if (hashCode == -1) {
            hashCode = data.hashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return defaultToString();
    }

    @Override
    public boolean equals(@Nullable Object o){
        if (o == this) {
            return true;
        }
        
        if (o == null) {
            return false;
        }

        if (o instanceof IList) {
            IList otherList = (IList) o;

            if (getType() != otherList.getType()) {
                return false;
            }

            if (otherList instanceof List) {
                List oList = (List) o;

                if (hashCode() != oList.hashCode()) {
                    return false;
                }

                return data.equals(oList.data);
            }
            else {
                return defaultEquals(otherList);
            }
        }

        return false;
    }
}

class SubList implements IList {

    private final IList base;
    private final int offset;
    private final int length;
    private int hashCode;
    private final Type elementType;

    SubList(IList base, int offset, int length){
        this.base = base;
        this.offset = offset;
        this.length = length;

        int end = offset + length;

        if(offset < 0) throw new IndexOutOfBoundsException("Offset may not be smaller than 0.");
        if(length < 0) throw new IndexOutOfBoundsException("Length may not be smaller than 0.");
        if(end > base.length()) throw new IndexOutOfBoundsException("'offset + length' may not be larger than 'list.size()'");

        Type newElementType = TypeFactory.getInstance().voidType();
        Type baseElementType = base.getElementType();

        for(int i = offset; i < end; i++){
            IValue el = base.get(i);
            if (newElementType.equals(baseElementType)) {
                // the type can only get more specific
                // once we've reached the type of the whole list, we can stop lubbing.
                break;
            }
            newElementType = newElementType.lub(el.getType());
        }
        elementType = newElementType;
    }


    IList materialize(){
        ListWriter w = new ListWriter();
        int end = offset + length;
        for(int i = offset; i < end; i++){
            w.append(base.get(i));
        }
        return w.done();
    }

    IList maybeMaterialize(IList lst){
        if(lst instanceof SubList){
            return ((SubList) lst).materialize();
        }
        return lst;
    }

    @Override
    public Type getType() {
        return TF.listType(elementType);
    }

    @Override
    public Type getElementType(){
        return elementType;
    }

    public int hashCode(){
        if (hashCode == 0) {
            hashCode = defaultHashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(@Nullable Object o){
        if (o == this) {
            return true;
        }
        
        if (o == null) {
            return false;
        }

        if (o instanceof SubList){
            SubList otherList = (SubList) o;

            return base.equals(otherList.base) && offset == otherList.offset && length == otherList.length;
        }

        if (o instanceof IList) {
            return defaultEquals(o);
        }
        
        return false;
    }

    @Override
    public String toString() {
        return defaultToString();
    }

    @Override
    public Iterator<IValue> iterator() {
        return new SubListIterator(base, offset, length);
    }

    @Override
    public IList append(IValue val) {
        ListWriter w = new ListWriter();
        int end = offset + length;
        for(int i = offset; i < end; i++){
            w.append(base.get(i));
        }
        w.append(val);
        return w.done();
    }

    @Override
    public IRelation<IList> asRelation() {
        return materialize().asRelation();
    }

    @Override
    public IList concat(IList lst) {
        ListWriter w = new ListWriter();
        int end = offset + length;
        for(int i = offset; i < end; i++){
            w.append(base.get(i));
        }
        for(IValue v : lst){
            w.append(v);
        }

        return w.done();
    }

    @Override
    public boolean contains(IValue val) {
        int end = offset + length;
        for(int i = offset; i < end; i++){
            if(base.get(i).equals(val)){
                return true;
            }
        }
        return false;
    }

    @Override
    public IList delete(IValue val) {
        ListWriter w = new ListWriter();
        int end = offset + length;
        for(int i = offset; i < end; i++){
            IValue elm = base.get(i);
            if(!elm.equals(val)){
                w.append(elm);
            }
        }
        return w.done();
    }

    @Override
    public IList delete(int n) {
        ListWriter w = new ListWriter();
        int end = offset + length;
        for(int i = offset; i < end; i++){
            if(i != n){
                w.append(base.get(i));
            }
        }
        return w.done();
    }

    @Override
    public IValue get(int n) {
        return base.get(offset + n);
    }

    @Override
    public IList insert(IValue val) {
        ListWriter w = new ListWriter();
        int end = offset + length;
        w.insert(val);
        for(int i = offset; i < end; i++){
            w.append(base.get(i));
        }
        return w.done();
    }

    @Override
    public IList intersect(IList arg0) {
        return materialize().intersect(maybeMaterialize(arg0));
    }

    @Override
    public boolean isEmpty() {
        return length == 0;
    }

    @Override
    public boolean isRelation() {
        return getType().isListRelation();
    }

    @Override
    public boolean isSubListOf(IList lst) {
        int end = offset + length;
        int j = 0;
        nextchar:
            for(int i = offset; i < end; i++){
                IValue elm = base.get(i);
                while(j < lst.length()){
                    if (elm.equals(lst.get(j))) {
                        j++;
                        continue nextchar;
                    } else {
                        j++;
                    }
                }
                return false;
            }
        return true;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public IList product(IList arg0) {
        return materialize().product(maybeMaterialize(arg0));
    }

    @Override
    public IList put(int n, IValue val) {
        ListWriter w = new ListWriter();
        int end = offset + length;
        for(int i = offset; i < end; i++){
            if(i == n){
                w.append(val);
            } else {
                w.append(base.get(i));
            }
        }
        return w.done();
    }

    @Override
    public IList replace(int arg0, int arg1, int arg2, IList arg3) {
        return materialize().replace(arg0,  arg1,  arg2, arg3);
    }

    @Override
    public IList reverse() {
        ListWriter w = new ListWriter();
        for(int i = offset + length - 1; i >= offset; i--){	
            w.append(base.get(i));
        }
        return w.done();
    }

    @Override
    public IList shuffle(Random arg0) {
        return materialize().shuffle(arg0);
    }

    @Override
    public IList sublist(int offset, int length) {
        return new SubList(base, this.offset + offset, length);
    }

    @Override
    public IList subtract(IList lst) {
        IListWriter w = writer();
        int end = offset + length;
        for (int i = offset; i < end; i++) {
            IValue v = base.get(i);
            if (lst.contains(v)) {
                lst = lst.delete(v);
            } else {
                w.append(v);
            }
        }
        return w.done();
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public IListWriter writer() {
        return new ListWriter();
    }
}

final class SubListIterator implements Iterator<IValue> {
    private int cursor;
    private final int end;
    private final IList base;

    public SubListIterator(IList base, int offset, int length) {
        this.base = base;
        this.cursor = offset;
        this.end = offset + length;
    }

    public boolean hasNext() {
        return this.cursor < end;
    }

    public IValue next() {
        if(this.hasNext()) {
            return base.get(cursor++);
        }
        throw new NoSuchElementException();
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}

