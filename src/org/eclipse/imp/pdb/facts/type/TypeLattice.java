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
  
  public static class Parameter extends Forward {
    public Parameter(Type it) {
      super(it, it.getBound());
    }
  }
  
  public static class Tuple extends Value {
    private final Type it;

    public Tuple(Type it) {
      this.it = it;
    }
    
    @Override
    protected Type getIt() {
      return it;
    }
    
    @Override
    public boolean subTuple(TupleType type) {
      if (it.getArity() == type.getArity()) {
        for (int i = 0; i < it.getArity(); i++) {
          if (!it.getFieldType(i).isSubtypeOf(type.getFieldType(i))) {
            return false;
          }
        }
        
        return true;
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
    private final Type it;

    public Set(Type it) {
      this.it = it;
    }
    
    @Override
    protected Type getIt() {
      return it;
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
    private final Type it;

    public List(Type it) {
      this.it = it;
    }
    
    @Override
    protected Type getIt() {
      return it;
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
    private final Type it;

    public Map(Type it) {
      this.it = it;
    }
    
    @Override
    protected Type getIt() {
      return it;
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
