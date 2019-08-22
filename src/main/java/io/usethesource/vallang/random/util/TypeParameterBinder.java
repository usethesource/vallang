/** 
 * Copyright (c) 2017, Davy Landman, Centrum Wiskunde & Informatica (CWI) 
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
package io.usethesource.vallang.random.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.usethesource.vallang.random.RandomTypeGenerator;
import io.usethesource.vallang.type.DefaultTypeVisitor;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

public class TypeParameterBinder extends DefaultTypeVisitor<Void,RuntimeException> {

    private final HashMap<Type, Type> typeParameters = new HashMap<>();
    private final RandomTypeGenerator randomType = new RandomTypeGenerator();

    private TypeParameterBinder() {
        super(null);
    }

    public static Map<Type, Type> bind(Type type) { 
        TypeParameterBinder tb = new TypeParameterBinder();
        type.accept(tb);
        return Collections.unmodifiableMap(tb.typeParameters);
    }

    @Override
    public Void visitParameter(Type parameterType) {
        Type type = typeParameters.get(parameterType);
        
        if (type == null){
            Type bound = parameterType.getBound();
            while (bound.isOpen()){
                bound = typeParameters.get(bound);
                if (bound == null) {
                    bound = TypeFactory.getInstance().valueType();
                }
            }

            do {
                type = randomType.next(5);
            } while (bound != null && !type.isSubtypeOf(bound));
            typeParameters.put(parameterType,  type);
        }
        return null;
    }

    @Override
    public Void visitTuple(Type type) {
        for(int i = 0; i < type.getArity(); i++){
            type.getFieldType(i).accept(this);
        }
        return null;
    }

    @Override
    public Void visitList(Type type) {
        type.getElementType().accept(this);
        return null;
    }

    @Override
    public Void visitMap(Type type) {
        type.getKeyType().accept(this);
        type.getValueType().accept(this);
        return null;
    }

    @Override
    public Void visitSet(Type type) {
        type.getElementType().accept(this);
        return null;
    }
    
    
    @Override
    public Void visitAbstractData(Type type) throws RuntimeException {
        type.getTypeParameters().accept(this);
        return null;
    }
}
