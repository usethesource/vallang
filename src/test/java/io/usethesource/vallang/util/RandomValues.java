/** 
 * Copyright (c) 2016, Davy Landman, Centrum Wiskunde & Informatica (CWI) 
 * All rights reserved. 
 *  
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 
 *  
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 
 *  
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 
 *  
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */ 
package io.usethesource.vallang.util;

import java.util.HashMap;
import java.util.Random;

import io.usethesource.vallang.IValue;
import io.usethesource.vallang.IValueFactory;
import io.usethesource.vallang.random.RandomValueGenerator;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;
import io.usethesource.vallang.type.TypeStore;

public class RandomValues {
	private static TypeStore ts = new TypeStore();
	private static TypeFactory tf = TypeFactory.getInstance();
	private static Type Boolean = tf.abstractDataType(ts,"Boolean");

	private static Type Name = tf.abstractDataType(ts,"Name");
    private static Type True = tf.constructor(ts,Boolean, "true");
    private static Type False= tf.constructor(ts,Boolean, "false");
	private static Type And= tf.constructor(ts,Boolean, "and", Boolean, Boolean);
	private static Type Or= tf.constructor(ts,Boolean, "or", tf.listType(Boolean));
	private static Type Not= tf.constructor(ts,Boolean, "not", Boolean);
	private static Type TwoTups = tf.constructor(ts,Boolean, "twotups", tf.tupleType(Boolean, Boolean), tf.tupleType(Boolean, Boolean));
	private static Type NameNode  = tf.constructor(ts,Name, "name", tf.stringType());
	private static Type Friends = tf.constructor(ts,Boolean, "friends", tf.listType(Name));
	private static Type Couples = tf.constructor(ts,Boolean, "couples", tf.listType(tf.tupleType(Name, Name)));
	static {
	    ts.declareKeywordParameter(Name, "moreName", Name);
	    ts.declareKeywordParameter(Name, "listName", tf.listType(Name));
	    ts.declareKeywordParameter(Name, "anyValue", tf.valueType());
	    ts.declareAnnotation(Boolean, "boolAnno", tf.boolType());
	}

	private static IValue name(IValueFactory vf, String n){
		return vf.constructor(NameNode, vf.string(n));
	}
	
	@SuppressWarnings("deprecation")
    public static IValue[] getTestValues(IValueFactory vf) {
	    return new IValue[] {
			vf.constructor(True),
			vf.constructor(And, vf.constructor(True), vf.constructor(False)),
			vf.constructor(Not, vf.constructor(And, vf.constructor(True), vf.constructor(False))),
			vf.constructor(TwoTups, vf.tuple(vf.constructor(True), vf.constructor(False)),vf.tuple(vf.constructor(True), vf.constructor(False))),
			vf.constructor(Or, vf.list(vf.constructor(True), vf.constructor(False), vf.constructor(True))),
			vf.constructor(Friends, vf.list(name(vf, "Hans").asWithKeywordParameters().setParameter("listName", vf.list(name(vf,"Hansie"))), name(vf, "Bob"))),
			vf.constructor(Or, vf.list()).asAnnotatable().setAnnotation("boolAnno", vf.bool(true)),
			vf.constructor(Couples, vf.list(vf.tuple(name(vf, "A"), name(vf, "B")), vf.tuple(name(vf, "C"), name(vf, "D")))),
			vf.integer(0),
			vf.integer(1),
			vf.integer(-1),
			vf.string("🍝"),
			vf.integer(Integer.MAX_VALUE),
			vf.integer(Integer.MIN_VALUE),
			vf.integer(new byte[]{(byte)0xfe, (byte)0xdc, (byte)0xba, (byte)0x98, (byte)0x76, (byte)0x54}),
			vf.constructor(True).asAnnotatable().setAnnotation("test", vf.integer(1))
	    };
	};
	
	public static Type addNameType(TypeStore t) {
	    t.extendStore(ts);
	    return Name;
	}

    public static IValue generate(Type tp, TypeStore ts, IValueFactory vf, Random rand, int maxDepth, boolean generateAnnotations) {
        return new RandomValueGenerator(vf, rand, maxDepth, maxDepth * 2, generateAnnotations).generate(tp, ts, new HashMap<>());
    }
}
