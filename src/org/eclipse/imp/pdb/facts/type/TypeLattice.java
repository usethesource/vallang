package org.eclipse.imp.pdb.facts.type;

public class TypeLattice {
  private final static TypeFactory TF = TypeFactory.getInstance();

  public interface IKind {
    boolean subAlias(AliasType type);
    boolean subParameter(ParameterType type);
    boolean subReal(RealType type);
    boolean subInteger(IntegerType type);
    boolean subRational(RationalType type);
    boolean subList(ListType type);
    boolean subMap(MapType type);
    boolean subNumber(NumberType type);
    boolean subRelation(RelationType type);
    boolean subListRelation(ListRelationType type);
    boolean subSet(SetType type);
    boolean subSourceLocation(SourceLocationType type);
    boolean subString(StringType type);
    boolean subNode(NodeType type);
    boolean subConstructor(ConstructorType type);
    boolean subAbstractData(AbstractDataType type);
    boolean subTuple(TupleType type);
    boolean subValue(ValueType type);
    boolean subVoid(VoidType type);
    boolean subBool(BoolType type);
    boolean subExternal(ExternalType type);
    boolean subDateTime(DateTimeType type);
    
    Type lubAlias(AliasType type);
    Type lubParameter(ParameterType type);
  
    Type lubReal(RealType type);
    Type lubInteger(IntegerType type);
    Type lubRational(RationalType type);
    Type lubList(ListType type);
    Type lubMap(MapType type);
    Type lubNumber(NumberType type);
    Type lubRelationType(RelationType type);
    Type lubListRelationType(ListRelationType type);
    Type lubSet(SetType type);
    Type lubSourceLocation(SourceLocationType type);
    Type lubString(StringType type);
    Type lubNode(NodeType type);
    Type lubConstructor(ConstructorType type);
    Type lubAbstractData(AbstractDataType type);
    Type lubTuple(TupleType type);
    Type lubValue(ValueType type);
    Type lubVoid(VoidType type);
    Type lubBool(BoolType type);
    Type lubExternal(ExternalType type);
    Type lubDateTime(DateTimeType type);
  }
  
  protected static abstract class Default implements IKind {
    protected Type it;

    @Override
    public boolean subAlias(AliasType type) {
      return it.isSubtypeOf(type.getAliased());
    }
    
    @Override
    public boolean subParameter(ParameterType type) {
      return it.isSubtypeOf(type.getBound());
    }
    
    @Override
    public boolean subReal(RealType type) {
      return false;
    }

    @Override
    public boolean subInteger(IntegerType type) {
      return false;
    }

    @Override
    public boolean subRational(RationalType type) {
      return false;
    }

    @Override
    public boolean subList(ListType type) {
      return false;
    }

    @Override
    public boolean subMap(MapType type) {
      return false;
    }

    @Override
    public boolean subNumber(NumberType type) {
      return false;
    }

    @Override
    public boolean subRelation(RelationType type) {
      return false;
    }

    @Override
    public boolean subListRelation(ListRelationType type) {
      return false;
    }

    @Override
    public boolean subSet(SetType type) {
      return false;
    }

    @Override
    public boolean subSourceLocation(SourceLocationType type) {
      return false;
    }

    @Override
    public boolean subString(StringType type) {
      return false;
    }

    @Override
    public boolean subNode(NodeType type) {
      return false;
    }

    @Override
    public boolean subConstructor(ConstructorType type) {
      return false;
    }

    @Override
    public boolean subAbstractData(AbstractDataType type) {
      return false;
    }

    @Override
    public boolean subTuple(TupleType type) {
      return false;
    }

    @Override
    public boolean subValue(ValueType type) {
      // all types are subtypes of value
      return true;
    }

    @Override
    public boolean subVoid(VoidType type) {
      return false;
    }

    @Override
    public boolean subBool(BoolType type) {
      return false;
    }

    @Override
    public boolean subExternal(ExternalType type) {
      return false;
    }

    @Override
    public boolean subDateTime(DateTimeType type) {
      return false;
    }

    @Override
    public Type lubAlias(AliasType type) {
      return it == type ? it : it.lub(type.getAliased()); 
    }
    
    @Override
    public Type lubParameter(ParameterType type) {
      return it.lub(type.getBound());
    }
    
    @Override
    public Type lubReal(RealType type) {
      return it;
    }

    @Override
    public Type lubInteger(IntegerType type) {
      return it;
    }

    @Override
    public Type lubRational(RationalType type) {
      return it;
    }

    @Override
    public Type lubList(ListType type) {
      return it;
    }

    @Override
    public Type lubMap(MapType type) {
      return it;
    }

    @Override
    public Type lubNumber(NumberType type) {
      return it;
    }

    @Override
    public Type lubRelationType(RelationType type) {
      return it;
    }

    @Override
    public Type lubListRelationType(ListRelationType type) {
      return it;
    }

    @Override
    public Type lubSet(SetType type) {
      return it;
    }

    @Override
    public Type lubSourceLocation(SourceLocationType type) {
      return it;
    }

    @Override
    public Type lubString(StringType type) {
      return it;
    }

    @Override
    public Type lubNode(NodeType type) {
      return it;
    }

    @Override
    public Type lubConstructor(ConstructorType type) {
      return it;
    }

    @Override
    public Type lubAbstractData(AbstractDataType type) {
      return it;
    }

    @Override
    public Type lubTuple(TupleType type) {
      return it;
    }

    @Override
    public Type lubValue(ValueType type) {
      return it;
    }

    @Override
    public Type lubVoid(VoidType type) {
      return it;
    }

    @Override
    public Type lubBool(BoolType type) {
      return it;
    }

    @Override
    public Type lubExternal(ExternalType type) {
      return it;
    }

    @Override
    public Type lubDateTime(DateTimeType type) {
      return it;
    }
  }  
 
  public abstract static class Forward extends Default {
    private final Type fwd;

    public Forward(Type fwd) {
      this.fwd = fwd;
    }
    
    @Override
    public boolean subReal(RealType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subInteger(IntegerType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subRational(RationalType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subList(ListType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subMap(MapType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subNumber(NumberType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subRelation(RelationType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subListRelation(ListRelationType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subSet(SetType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subSourceLocation(SourceLocationType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subString(StringType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subNode(NodeType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subConstructor(ConstructorType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subAbstractData(AbstractDataType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subTuple(TupleType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subValue(ValueType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subVoid(VoidType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subBool(BoolType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subExternal(ExternalType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public boolean subDateTime(DateTimeType type) {
      return fwd.isSubtypeOf(type);
    }

    @Override
    public Type lubReal(RealType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubInteger(IntegerType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubRational(RationalType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubList(ListType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubMap(MapType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubNumber(NumberType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubRelationType(RelationType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubListRelationType(ListRelationType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubSet(SetType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubSourceLocation(SourceLocationType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubString(StringType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubNode(NodeType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubConstructor(ConstructorType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubAbstractData(AbstractDataType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubTuple(TupleType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubValue(ValueType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubVoid(VoidType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubBool(BoolType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubExternal(ExternalType type) {
      return fwd.lub(type);
    }

    @Override
    public Type lubDateTime(DateTimeType type) {
      return fwd.lub(type);
    }
  }

  public static class Alias extends Forward {
    public Alias(Type it) {
      super(it.getAliased());
    }
  }
  
  public static class Parameter extends Forward {
    public Parameter(Type it) {
      super(it.getBound());
    }
  }
  
  public static class Value extends Default {
    protected Type it;

    public Value() {
      this.it = TF.valueType();
    }
    
    @Override
    public boolean subValue(ValueType type) {
      return true;
    }
  }
  
  public static class Number extends Value {
    public Number() {
      this.it = TF.numberType();
    }
    
    @Override
    public boolean subNumber(NumberType type) {
      return true;
    }
    
    @Override
    public Type lubNumber(NumberType type) {
      return it;
    }
    
    @Override
    public Type lubInteger(IntegerType type) {
      return it;
    }
    
    @Override
    public Type lubReal(RealType type) {
      return it;
    }
    
    @Override
    public Type lubRational(RationalType type) {
      return it;
    }
  }
  
  public static class Integer extends Number {
    public Integer() {
      this.it = TF.integerType();
    }
    
    @Override
    public boolean subNumber(NumberType type) {
      return true;
    }
    
    @Override
    public Type lubInteger(IntegerType type) {
      return it;
    }
  }
  
  public static class Real extends Number {
    public Real() {
      this.it = TF.realType();
    }
    
    @Override
    public boolean subNumber(NumberType type) {
      return true;
    }
    
    @Override
    public Type lubReal(RealType type) {
      return it;
    }
  }
  
  public static class Rational extends Number {
    public Rational() {
      this.it = TF.rationalType();
    }
    
    @Override
    public boolean subNumber(NumberType type) {
      return true;
    }
    
    @Override
    public Type lubRational(RationalType type) {
      return it;
    }
  }
  
  public static class Bool extends Value {
    
    public Bool() {
      it = TF.boolType();
    }
    
    @Override
    public boolean subBool(BoolType type) {
      return true;
    }
    
    @Override
    public Type lubBool(BoolType type) {
      return it;
    }
  }
  
  public static class String extends Value {
    
    public String() {
      it = TF.stringType();
    }
    
    @Override
    public boolean subString(StringType type) {
      return true;
    }
    
    @Override
    public Type lubString(StringType type) {
      return it;
    }
  }

  public static class Void extends Value {
    public Void() {
      it = TF.voidType();
    }
    
    @Override
    public boolean subVoid(VoidType type) {
      return true;
    }
    
    @Override
    public Type lubVoid(VoidType type) {
      return it;
    }

    @Override
    public boolean subReal(RealType type) {
      return true;
    }

    @Override
    public boolean subInteger(IntegerType type) {
      return true;
    }

    @Override
    public boolean subRational(RationalType type) {
      return true;
    }

    @Override
    public boolean subList(ListType type) {
      return true;
    }

    @Override
    public boolean subMap(MapType type) {
      return true;
    }

    @Override
    public boolean subNumber(NumberType type) {
      return true;
    }

    @Override
    public boolean subRelation(RelationType type) {
      return true;
    }

    @Override
    public boolean subListRelation(ListRelationType type) {
      return true;
    }

    @Override
    public boolean subSet(SetType type) {
      return true;
    }

    @Override
    public boolean subSourceLocation(SourceLocationType type) {
      return true;
    }

    @Override
    public boolean subString(StringType type) {
      return true;
    }

    @Override
    public boolean subNode(NodeType type) {
      return true;
    }

    @Override
    public boolean subConstructor(ConstructorType type) {
      return true;
    }

    @Override
    public boolean subAbstractData(AbstractDataType type) {
      return true;
    }

    @Override
    public boolean subTuple(TupleType type) {
      return true;
    }

    @Override
    public boolean subValue(ValueType type) {
      return true;
    }

    @Override
    public boolean subBool(BoolType type) {
      return true;
    }

    @Override
    public boolean subExternal(ExternalType type) {
      return true;
    }

    @Override
    public boolean subDateTime(DateTimeType type) {
      return true;
    }

    @Override
    public Type lubReal(RealType type) {
      return type;
    }

    @Override
    public Type lubInteger(IntegerType type) {
      return type;
    }

    @Override
    public Type lubRational(RationalType type) {
      return type;
    }

    @Override
    public Type lubList(ListType type) {
      return type;
    }

    @Override
    public Type lubMap(MapType type) {
      return type;
    }

    @Override
    public Type lubNumber(NumberType type) {
      return type;
    }

    @Override
    public Type lubRelationType(RelationType type) {
      return type;
    }

    @Override
    public Type lubListRelationType(ListRelationType type) {
      return type;
    }

    @Override
    public Type lubSet(SetType type) {
      return type;
    }

    @Override
    public Type lubSourceLocation(SourceLocationType type) {
      return type;
    }

    @Override
    public Type lubString(StringType type) {
      return type;
    }

    @Override
    public Type lubNode(NodeType type) {
      return type;
    }

    @Override
    public Type lubConstructor(ConstructorType type) {
      return type;
    }

    @Override
    public Type lubAbstractData(AbstractDataType type) {
      return type;
    }

    @Override
    public Type lubTuple(TupleType type) {
      return type;
    }

    @Override
    public Type lubValue(ValueType type) {
      return type;
    }

    @Override
    public Type lubBool(BoolType type) {
      return type;
    }

    @Override
    public Type lubExternal(ExternalType type) {
      return type;
    }

    @Override
    public Type lubDateTime(DateTimeType type) {
      return type;
    }
  }

  public static class Node extends Value {
    public Node() {
      it = TF.nodeType();
    }
    
    @Override
    public boolean subNode(NodeType type) {
      return true;
    }
    
    @Override
    public Type lubNode(NodeType type) {
      return it;
    }
    
    @Override
    public Type lubAbstractData(AbstractDataType type) {
      return it;
    }
    
    @Override
    public Type lubConstructor(ConstructorType type) {
      return it;
    }
  }

  public static class AbstractData extends Node {
    public AbstractData(Type it) {
      this.it = it;
    }
    
    @Override
    public boolean subNode(NodeType type) {
      return true;
    }
    
    @Override
    public boolean subAbstractData(AbstractDataType type) {
      return it == type || (it.getName().equals(type.getName()) && it.getTypeParameters().isSubtypeOf(type.getTypeParameters()));
    }

    @Override
    public Type lubAbstractData(AbstractDataType type) {
      if (it == type) {
        return it;
      }
      
      if (it.getName().equals(type.getName())) {
        return TF.abstractDataTypeFromTuple(new TypeStore(), it.getName(), it.getTypeParameters().lub(type.getTypeParameters()));
      }
      
      return TF.nodeType();
    }
    
    @Override
    public Type lubConstructor(ConstructorType type) {
      return lubAbstractData((AbstractDataType) type.getAbstractDataType());
    }
  }

  public static class Constructor extends AbstractData {
    public Constructor(Type it) {
      super(it);
    }
    
    @Override
    public boolean subAbstractData(AbstractDataType type) {
      return it.getAbstractDataType().isSubtypeOf(type);
    }
    
    @Override
    public boolean subConstructor(ConstructorType type) {
      return it.getAbstractDataType().isSubtypeOf(type.getAbstractDataType())
          && it.getFieldTypes().isSubtypeOf(type.getFieldTypes());
    }
  }

  public static class Datetime extends Value {
    public Datetime() {
      it = TF.dateTimeType();
    }
    
    @Override
    public boolean subDateTime(DateTimeType type) {
      return true;
    }
    
    @Override
    public Type lubDateTime(DateTimeType type) {
      return it;
    }
  }
  
  public static class SourceLocation extends Value {
    public SourceLocation() {
      this.it = TF.sourceLocationType();
    }
    
    @Override
    public boolean subSourceLocation(SourceLocationType type) {
      return true;
    }
    
    @Override
    public Type lubSourceLocation(SourceLocationType type) {
      return it;
    }
  }

  public static class Tuple extends Value {
    public Tuple(Type it) {
      this.it = it;
    }
    
    @Override
    public boolean subTuple(TupleType type) {
      if (it.getArity() == type.getArity()) {
        for (int i = 0; i < it.getArity(); i++) {
          if (!it.getFieldType(i).isSubtypeOf(type.getFieldType(i))) {
            return false;
          }
        }
      }
      
      return false;
    }
    
    @Override
    public Type lubTuple(TupleType type) {
      if (it.getArity() == type.getArity()) {
        return TupleType.lubNamedTupleTypes(it, type);
      }
      
      return TF.valueType();
    }
  }

  public static class Set extends Value {
    public Set(Type it) {
      this.it = it;
    }
    
    @Override
    public boolean subSet(SetType type) {
      return it == type || it.getElementType().isSubtypeOf(type.getElementType());
    }
    
    @Override
    public Type lubSet(SetType type) {
      return it == type ? it : TF.setType(it.getElementType().lub(type.getElementType()));
    }
    
    @Override
    public boolean subRelation(RelationType type) {
      return it == type || it.getElementType().isSubtypeOf(type.getElementType()); 
    }
    
    @Override
    public Type lubRelationType(RelationType type) {
      return it == type ? it : TF.setType(it.getElementType().lub(type.getElementType()));
    }
  }
  
  public static class List extends Value {
    public List(Type it) {
      this.it = it;
    }
    
    @Override
    public boolean subList(ListType type) {
      return it == type || it.getElementType().isSubtypeOf(type.getElementType());
    }
    
    @Override
    public Type lubList(ListType type) {
      return it == type ? it : TF.listType(it.getElementType().lub(type.getElementType()));
    }
    
    @Override
    public boolean subListRelation(ListRelationType type) {
      return it == type || it.getElementType().isSubtypeOf(type.getElementType());
    }

    @Override
    public Type lubListRelationType(ListRelationType type) {
      return it == type ? it : TF.listType(it.getElementType().lub(type.getElementType()));
    }
    
  }
  
  public static class Map extends Value {
    public Map(Type it) {
      this.it = it;
    }
    
    @Override
    public boolean subMap(MapType type) {
      return it == type 
          || (it.getKeyType().isSubtypeOf(type.getKeyType())
              && it.getValueType().isSubtypeOf(type.getValueType()));
    }
    
    @Override
    public Type lubMap(MapType type) {
      // tuple types deal better with labels, so we forward here
      return it == type ? it : TF.mapTypeFromTuple(it.getFieldTypes().lub(type.getFieldTypes()));
    }
  }

  public static class Relation extends Set {
    public Relation(Type it) {
      super(it);
    }
  }
  
  public static class ListRelation extends List {
    public ListRelation(Type it) {
      super(it);
    }
  }
}
