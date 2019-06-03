package io.usethesource.vallang.impl.persistent;

import static io.usethesource.capsule.util.stream.CapsuleCollectors.UNORDERED;
import static io.usethesource.vallang.impl.persistent.SetWriter.equivalenceEqualityComparator;

import java.util.Collections;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collector;

import io.usethesource.capsule.Set;
import io.usethesource.capsule.SetMultimap;
import io.usethesource.capsule.util.stream.DefaultCollector;
import io.usethesource.vallang.IList;
import io.usethesource.vallang.ISet;
import io.usethesource.vallang.ITuple;
import io.usethesource.vallang.IValue;
import io.usethesource.vallang.util.AbstractTypeBag;

public class ValueCollectors {

  public static <T extends IValue> Collector<T, ?, ISet> toSet() {

    class SetStruct {
      AbstractTypeBag elementTypeBag = AbstractTypeBag.of();
      Set.Transient<T> set = Set.Transient.of();
    }

    /** extract key/value from type {@code T} and insert into multimap */
    final BiConsumer<SetStruct, T> accumulator = (struct, element) -> {
      if (struct.set.__insert(element)) {
        struct.elementTypeBag = struct.elementTypeBag.increase(element.getType());
      }
    };

    return new DefaultCollector<>(SetStruct::new, accumulator, unsupportedCombiner(),
        struct -> PersistentSetFactory.from(struct.elementTypeBag,
            (Set.Immutable<IValue>) struct.set.freeze()),
        UNORDERED);
  }
  
  public static <T extends IValue> Collector<T, ?, IList> toList() {
    return new DefaultCollector<>(ValueFactory.getInstance()::listWriter, (w,e) -> { w.append(e); }, 
    		unsupportedCombiner(), w -> w.done(), Collections.emptySet()); 
  }
  

  /**
   * @param keyLabel optional label of first column
   * @param valueLabel optional label of second column
   */
  public static <T extends ITuple, K extends IValue, V extends IValue> Collector<T, ?, ISet> toSetMultimap(
      Optional<String> keyLabel, Function<? super T, ? extends K> keyMapper,
      Optional<String> valueLabel, Function<? super T, ? extends V> valueMapper) {

    class SetMultimapStruct {
      AbstractTypeBag keyTypeBag = AbstractTypeBag.of(keyLabel.orElse(null));
      AbstractTypeBag valTypeBag = AbstractTypeBag.of(valueLabel.orElse(null));
      SetMultimap.Transient<K, V> map =
          SetMultimap.Transient.of(equivalenceEqualityComparator);
    }

    /** extract key/value from type {@code T} and insert into multimap */
    final BiConsumer<SetMultimapStruct, T> accumulator = (struct, element) -> {
      final K key = keyMapper.apply(element);
      final V val = valueMapper.apply(element);

      if (struct.map.__insert(key, val)) {
        struct.keyTypeBag = struct.keyTypeBag.increase(key.getType());
        struct.valTypeBag = struct.valTypeBag.increase(val.getType());
      }
    };

    return new DefaultCollector<>(SetMultimapStruct::new, accumulator,
        unsupportedCombiner(), struct -> PersistentSetFactory.from(struct.keyTypeBag,
            struct.valTypeBag, (SetMultimap.Immutable<IValue, IValue>) struct.map.freeze()),
        UNORDERED);
  }

  private static <T> BinaryOperator<T> unsupportedCombiner() {
    return (u, v) -> {
      throw new UnsupportedOperationException("Merging is not yet supported.");
    };
  }

}
