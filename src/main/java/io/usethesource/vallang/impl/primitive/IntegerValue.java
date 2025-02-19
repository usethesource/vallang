/*******************************************************************************
 * Copyright (c) 2009-2013 CWI
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *
 *   * Arnold Lankamp - interfaces and implementation
 *   * Michael Steindorfer - Michael.Steindorfer@cwi.nl - CWI
 *******************************************************************************/
package io.usethesource.vallang.impl.primitive;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.checkerframework.checker.nullness.qual.Nullable;

import io.usethesource.vallang.IBool;
import io.usethesource.vallang.IInteger;
import io.usethesource.vallang.INumber;
import io.usethesource.vallang.IRational;
import io.usethesource.vallang.IReal;
import io.usethesource.vallang.type.Type;
import io.usethesource.vallang.type.TypeFactory;

/**
 * Implementation for IInteger.
 * <br /><br />
 * Integer values that fall outside the 32-bit range will be store in BigIntegerValue instead.
 *
 * @author Arnold Lankamp
 */
/*package*/ class IntegerValue extends AbstractNumberValue implements IInteger, ICanBecomeABigInteger{
    private static final Type INTEGER_TYPE = TypeFactory.getInstance().integerType();

    private static final String INTEGER_MAX_STRING = "2147483647";
    private static final String NEGATIVE_INTEGER_MAX_STRING = "-2147483648";

    private static final int SEVEN_BITS_MASK = 0x0000007f;
    private static final int FIFTEEN_BITS_MASK = 0x00007fff;
    private static final int TWENTYTHREE_BITS_MASK = 0x007fffff;

    private static final IInteger[] smallValues;
    private static final BigInteger[] smallBigIntegerValues;
    private static final int minSmallValue = -100;
    private static final int maxSmallValue = 100;
    public static final IInteger INTEGER_ONE;
    public static final IInteger INTEGER_ZERO;
    static {
        smallValues = new IInteger[(maxSmallValue - minSmallValue) + 1];
        smallBigIntegerValues = new BigInteger[(maxSmallValue - minSmallValue) + 1];
        for (int i = minSmallValue; i <= maxSmallValue; i++) {
            smallValues[i - minSmallValue] = new IntegerValue(i);
            smallBigIntegerValues[i - minSmallValue] = new BigInteger("" + i);
        }
        INTEGER_ONE = smallValues[1 - minSmallValue];
        INTEGER_ZERO = smallValues[0 - minSmallValue];
    }

    protected final int value;

    /*
     * TODO: Unify IntegerValue and BigIntegerValue in same java class file.
     */
    /*package*/ static IInteger newInteger(BigInteger value) {
        if (value.bitLength() > 31) {
            return new BigIntegerValue(value);
        }
        return newInteger(value.intValue());
    }


    /*package*/ static IInteger newInteger(int value) {
        if (minSmallValue <= value && value <= maxSmallValue) {
            return smallValues[value - minSmallValue];
        }
        return new IntegerValue(value);
    }

    /*package*/ static IInteger newInteger(String integerValue) {
        if (integerValue.startsWith("-")) {
            if (integerValue.length() < 11 || (integerValue.length() == 11 && integerValue.compareTo(NEGATIVE_INTEGER_MAX_STRING) <= 0)) {
                return newInteger(Integer.parseInt(integerValue));
            }
            return new BigIntegerValue(new BigInteger(integerValue));
        }

        if (integerValue.length() < 10 || (integerValue.length() == 10 && integerValue.compareTo(INTEGER_MAX_STRING) <= 0)) {
            return newInteger(Integer.parseInt(integerValue));
        }
        return new BigIntegerValue(new BigInteger(integerValue));
    }

    /*package*/ static IInteger newInteger(long value) {
        if (((value & 0x000000007fffffffL) == value) || ((value & 0xffffffff80000000L) == 0xffffffff80000000L)) {
            return newInteger((int) value);
        } else {
            byte[] valueData = new byte[8];
            valueData[0] = (byte) ((value >>> 56) & 0xff);
            valueData[1] = (byte) ((value >>> 48) & 0xff);
            valueData[2] = (byte) ((value >>> 40) & 0xff);
            valueData[3] = (byte) ((value >>> 32) & 0xff);
            valueData[4] = (byte) ((value >>> 24) & 0xff);
            valueData[5] = (byte) ((value >>> 16) & 0xff);
            valueData[6] = (byte) ((value >>> 8) & 0xff);
            valueData[7] = (byte) (value & 0xff);
            return newInteger(valueData);
        }
    }

    /*package*/ static IInteger newInteger(byte[] integerData) {
        if (integerData.length <= 4) {
            int value = 0;
            for (int i = integerData.length - 1, j = 0; i >= 0; i--, j++) {
                value |= ((integerData[i] & 0xff) << (j * 8));
            }

            return newInteger(value);
        }
        return new BigIntegerValue(new BigInteger(integerData));
    }

    private IntegerValue(int value){
        super();
        this.value = value;
    }

    @Override
    public IInteger toInteger() {
        return this;
    }

    @Override
    public Type getType(){
        return INTEGER_TYPE;
    }

    @Override
    public int intValue(){
        return value;
    }

    @Override
    public long longValue(){
        return value;
    }

    @Override
    public double doubleValue(){
        return value;
    }

    @Override
    public IReal toReal(int precision){
        return BigDecimalValue.newReal(BigDecimal.valueOf(value));
    }

    @Override
    public byte[] getTwosComplementRepresentation(){
        if((value & SEVEN_BITS_MASK) == value){
            byte[] data = new byte[1];
            data[0] = (byte) (value & 0x7f);
            return data;
        }else if((value & FIFTEEN_BITS_MASK) == value){
            byte[] data = new byte[2];
            data[0] = (byte) ((value >> 8) & 0x7f);
            data[1] = (byte) (value & 0xff);
            return data;
        }else if((value & TWENTYTHREE_BITS_MASK) == value){
            byte[] data = new byte[3];
            data[0] = (byte) ((value >> 16) & 0x7f);
            data[1] = (byte) ((value >> 8) & 0xff);
            data[2] = (byte) (value & 0xff);
            return data;
        }

        byte[] data = new byte[4];
        data[0] = (byte) ((value >> 24) & 0xff);
        data[1] = (byte) ((value >> 16) & 0xff);
        data[2] = (byte) ((value >> 8) & 0xff);
        data[3] = (byte) (value & 0xff);
        return data;
    }

    @Override
    public BigInteger toBigInteger(){
        if (minSmallValue <= value && value <= maxSmallValue) {
            return smallBigIntegerValues[value - minSmallValue];
        }
        return new BigInteger(getTwosComplementRepresentation());
    }

    @Override
    public IInteger add(IInteger other){
        if(value == 0) {
            return other;
        }

        if(other instanceof BigIntegerValue){
            return other.add(this);
        }

        int otherIntValue = other.intValue();

        if(otherIntValue == 0) {
            return this;
        }

        int result = value + otherIntValue;
        if((value < 0) && (otherIntValue < 0) && (result >= 0)){// Overflow -> positive.
            byte[] intValueData = new byte[5];
            intValueData[0] = (byte) 0xff;
            intValueData[1] = (byte)((result >>> 24) & 0xff);
            intValueData[2] = (byte)((result >>> 16) & 0xff);
            intValueData[3] = (byte)((result >>> 8) & 0xff);
            intValueData[4] = (byte)(result & 0xff);

            return IntegerValue.newInteger(new BigInteger(intValueData));
        } else if((value > 0) && (otherIntValue > 0) && (result < 0)){// Overflow -> negative.
            byte[] intValueData = new byte[5];
            intValueData[0] = 0;
            intValueData[1] = (byte)((result >>> 24) & 0xff);
            intValueData[2] = (byte)((result >>> 16) & 0xff);
            intValueData[3] = (byte)((result >>> 8) & 0xff);
            intValueData[4] = (byte)(result & 0xff);

            return IntegerValue.newInteger(new BigInteger(intValueData));
        }

        return IntegerValue.newInteger(result);
    }

    @Override
    public IRational add(IRational other) {
        return (IRational ) other.add(this);
    }

    @Override
    public IReal add(IReal other) {
        return (IReal) other.add(this);
    }

    @Override
    public INumber subtract(IReal other) {
        return toReal(other.precision()).subtract(other);
    }

    @Override
    public IInteger subtract(IInteger other){
        if(value == 0) {
            return other.negate();
        }

        if(other instanceof BigIntegerValue){
            return other.negate().subtract(this.negate());
        }

        int otherIntValue = other.intValue();

        if(otherIntValue == 0) {
            return this;
        }

        int result = value - otherIntValue;
        if((value < 0) && (otherIntValue > 0) && (result > 0)){// Overflow -> positive.
            byte[] intValueData = new byte[5];
            intValueData[0] = (byte) 0xff;
            intValueData[1] = (byte)((result >>> 24) & 0xff);
            intValueData[2] = (byte)((result >>> 16) & 0xff);
            intValueData[3] = (byte)((result >>> 8) & 0xff);
            intValueData[4] = (byte)(result & 0xff);

            return IntegerValue.newInteger(new BigInteger(intValueData));
        } else if((value > 0) && (otherIntValue < 0) && (result < 0)){// Overflow -> negative.
            byte[] intValueData = new byte[5];
            intValueData[0] = 0;
            intValueData[1] = (byte)((result >>> 24) & 0xff);
            intValueData[2] = (byte)((result >>> 16) & 0xff);
            intValueData[3] = (byte)((result >>> 8) & 0xff);
            intValueData[4] = (byte)(result & 0xff);

            return IntegerValue.newInteger(new BigInteger(intValueData));
        }

        return IntegerValue.newInteger(result);
    }

    @Override
    public IRational subtract(IRational other) {
        return toRational().subtract(other);
    }

    @Override
    public IInteger multiply(IInteger other){
        if(value == 0) {
            return this;
        }
        if(value == 1) {
            return other;
        }

        if(other instanceof BigIntegerValue){
            return other.multiply(this);
        }

        int otherIntValue = other.intValue();
        if(otherIntValue == 0) { return other; }
        if(otherIntValue == 1) { return this; }

        boolean resultIsPositive = ((((value ^ otherIntValue) ^ 0x80000000) & 0x80000000) == 0x80000000);
        if(resultIsPositive){
            int div = Integer.MAX_VALUE / otherIntValue;
            if((value > 0)){
                if(value <= div){
                    return IntegerValue.newInteger(value * other.intValue());
                }
            }else{
                if(value >= div){
                    return IntegerValue.newInteger(value * other.intValue());
                }
            }
        } else{
            int div = Integer.MIN_VALUE / otherIntValue;
            if((value > 0)){
                if(value <= div){
                    return IntegerValue.newInteger(value * other.intValue());
                }
            } else{
                if(value >= div){
                    return IntegerValue.newInteger(value * other.intValue());
                }
            }
        }

        return IntegerValue.newInteger(toBigInteger().multiply(((ICanBecomeABigInteger) other).toBigInteger()));
    }

    @Override
    public IRational multiply(IRational other) {
        return (IRational) other.multiply(this);
    }

    @Override
    public IReal multiply(IReal other) {
        return (IReal) other.multiply(this);
    }

    @Override
    public IInteger divide(IInteger other) {
        if (other.equals(INTEGER_ZERO)) {
            throw new ArithmeticException("/ by zero");
        }

        if (value == 0) {
            return this;
        }

        if (other instanceof BigIntegerValue){
            return IntegerValue.newInteger(toBigInteger().divide(((ICanBecomeABigInteger) other).toBigInteger()));
        }

        int otherIntValue = other.intValue();
        if (otherIntValue == 1) {
            return this;
        }

        return IntegerValue.newInteger(value / otherIntValue);
    }


    @Override
    public IRational divide(IRational other) {
        return toRational().divide(other);
    }

    @Override
    public INumber divide(IInteger other, int precision) {
        return toReal(precision).divide(other, precision);
    }

    @Override
    public INumber divide(IRational other, int precision) {
        return toReal(precision).divide(other, precision);
    }

    @Override
    public IReal divide(IReal other, int precision) {
        return toReal(precision).divide(other, precision);
    }

    @Override
    public IInteger mod(IInteger other){
        if(other instanceof BigIntegerValue){
            if(value < 0){
                BigInteger m = ((BigIntegerValue)other).toBigInteger();
                // i.e. -1 % m = m + (-1)
                BigInteger res = m.add(toBigInteger());
                return IntegerValue.newInteger(res);
            }
            return this;
        }
        int otherVal = other.intValue();
        int newValue = value % other.intValue();
        newValue = newValue >= 0 ? newValue : newValue + otherVal;
        return IntegerValue.newInteger(newValue);
    }

    @Override
    public IInteger remainder(IInteger other){
        if(other instanceof BigIntegerValue){
            return this;
        }

        return IntegerValue.newInteger(value % other.intValue());
    }

    @Override
    public IInteger negate(){
        if(value == 0) {
            return this;
        } else {
            return IntegerValue.newInteger((~((long) value)) + 1);
        }
    }

    @Override
    public IBool equal(IInteger other) {
        return BoolValue.getBoolValue(compare(other) == 0);
    }

    @Override
    public IBool equal(IRational other) {
        return other.equal(this);
    }

    @Override
    public IBool equal(IReal other) {
        return other.equal(this);
    }

    @Override
    public IBool greater(IInteger other){
        return BoolValue.getBoolValue(compare(other) > 0);
    }

    @Override
    public IBool greater(IRational other) {
        return other.less(this);
    }

    @Override
    public IBool greater(IReal other) {
        return other.less(this);
    }

    @Override
    public IBool greaterEqual(IInteger other){
        return BoolValue.getBoolValue(compare(other) >= 0);
    }

    @Override
    public IBool greaterEqual(IRational other) {
        return other.lessEqual(this);
    }

    @Override
    public IBool greaterEqual(IReal other) {
        return BoolValue.getBoolValue(compare(other) >= 0);
    }

    @Override
    public IBool less(IInteger other){
        return BoolValue.getBoolValue(compare(other) < 0);
    }

    @Override
    public IBool less(IRational other) {
        return other.greater(this);
    }

    @Override
    public IBool less(IReal other) {
        return other.greater(this);
    }

    @Override
    public IBool lessEqual(IInteger other){
        return BoolValue.getBoolValue(compare(other) <= 0);
    }

    @Override
    public IBool lessEqual(IRational other) {
        return other.greaterEqual(this);
    }

    @Override
    public IBool lessEqual(IReal other) {
        return other.greaterEqual(this);
    }

    @Override
    public int compare(IInteger other){
        if(other instanceof BigIntegerValue){
            return ((~other.compare(this)) + 1);
        }

        if(value > other.intValue()) { return 1; }
        if(value < other.intValue()) { return -1; }

        return 0;
    }

    @Override
    public int compare(INumber other) {
        if (isIntegerType(other)) {
            return compare(other.toInteger());
        }
        else if (isRationalType(other)) {
            return toRational().compare(other);
        }
        else {
            assert other instanceof IReal;
            return toReal(((IReal) other).precision()).compare(other);
        }
    }

    @Override
    public int hashCode(){
        int h = value ^ 0x85ebca6b;
        // based on the final Avalanching phase of MurmurHash2
        // providing a nice mix of bits even for small numbers.
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        h ^= h >>> 15;

        return h;
    }

    public boolean equals(@Nullable Object o){
        if(o == null) {
            return false;
        }
        else if(o == this) {
            return true;
        }

        if (o.getClass() == getClass()) {
            IntegerValue otherInteger = (IntegerValue) o;
            return (value == otherInteger.value);
        }

        return false;
    }

    @Override
    public String getStringRepresentation(){
        return Integer.toString(value);
    }

    @Override
    public int signum() {
        return value < 0 ? -1 : (value == 0 ? 0 : 1);
    }

    @Override
    public IInteger abs() {
        return newInteger(Math.abs(value));
    }

    @Override
    public IRational toRational() {
        return RationalValue.newRational(this, INTEGER_ONE);
    }

}
