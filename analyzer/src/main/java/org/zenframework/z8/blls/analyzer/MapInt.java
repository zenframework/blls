package org.zenframework.z8.blls.analyzer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static java.util.Arrays.copyOf;

// Memory efficient implementation based on Robin-Hood hashing
@NullUnmarked
public final class MapInt<V> implements Map<Integer, V>, Cloneable {
  private static final short[] PRIMES = new short[]{
    29,
    37,
    47,
    59,
    73,
    97,
    127,
    151,
    197,
    251,
    313,
    397,
    499,
    631,
    797,
    1009,
    1259,
    1597,
    2011,
    2539,
    3203,
    4027,
    5087,
    6421,
    8089,
    10193,
    12853,
    16193,
    20399,
    25717,
    32401,
  };

  private int[] keys;
  private byte[] offsets;
  private V[] values;
  private int lookups;
  private int capacityIndex;
  private int size;
  private int modifications = 0;

  public MapInt() {
    clear();
  }

  @SuppressWarnings("unchecked")
  private void grow() {
    int capacityOld = PRIMES[capacityIndex] + lookups;
    int[] keysOld = keys;
    V[] valuesOld = values;

    // PRIMES[i] * 2 ~= PRIMES[i + 3]
    capacityIndex += 3;
    lookups = (int) (Math.log(PRIMES[capacityIndex]) / Math.log(2));
    int capacityNew = PRIMES[capacityIndex] + lookups;
    keys = new int[capacityNew];
    offsets = new byte[capacityNew];
    values = (V[]) new Object[capacityNew];
    size = 0;

    for (int i = 0; i < capacityOld; i++) {
      if (keysOld[i] > 0) {
        put(keysOld[i], valuesOld[i]);
      }
    }
  }

  private int indexOf(int key) {
    int index = key % PRIMES[capacityIndex];

    // skip all slots that occupy our position
    int offset = 1;
    while (offsets[index] > offset) {
      index++;
      offset++;
    }

    // test key equality
    while (offsets[index] == offset) {
      if (keys[index] == key) return index;
      index++;
      offset++;
    }

    return -1;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public boolean containsKey(Object key) {
    return key instanceof Integer k && containsKey(k.intValue());
  }

  public boolean containsKey(int key) {
    return key > 0 && indexOf(key) != -1;
  }

  @Override
  public boolean containsValue(Object value) {
    return values().contains(value);
  }

  @Override
  public @Nullable V get(Object key) {
    if (!(key instanceof Integer k)) return null;

    return get(k.intValue());
  }

  public @Nullable V get(int key) {
    if (key <= 0) return null;

    int i = indexOf(key);
    return i == -1 ? null : values[i];
  }

  @Override
  public V put(Integer key, V value) {
    return put(key.intValue(), value);
  }

  public @Nullable V put(int key, V value) {
    if (key <= 0) throw new IllegalArgumentException("key <= 0");
    if (size + 1 > PRIMES[capacityIndex] >> 1) {
      grow();
    }

    int index = key % PRIMES[capacityIndex];
    // skip all slots that occupy our position
    byte offset = 1;
    while (offsets[index] > offset) {
      index++;
      offset++;
    }

    // test key equality
    for (; offsets[index] == offset; index++, offset++) {
      if (keys[index] == key) {
        V result = values[index];
        values[index] = value;
        modifications++;
        return result;
      }
    }

    // no duplicate key, insert
    for (; ; index++, offset++) {
      if (offset == lookups + 1) {
        // reached collisions limit - grow
        grow();
        return put(key, value);
      } else if (keys[index] == 0) {
        // free position - insert
        keys[index] = key;
        offsets[index] = offset;
        V result = values[index];
        values[index] = value;
        size++;
        modifications++;
        return result;
      } else if (offsets[index] < offset) {
        assert offsets[index] > 0;
        // Robin-Hood hashing
        // old value is closer to desired position than new
        // swap places to keep equal distance
        int keyTemp = keys[index];
        byte offsetTemp = offsets[index];
        V valueTemp = values[index];
        keys[index] = key;
        offsets[index] = offset;
        values[index] = value;
        key = keyTemp;
        offset = offsetTemp;
        value = valueTemp;
      }
    }
  }

  @Override
  public @Nullable V remove(Object key) {
    return key instanceof Integer v ? remove(v.intValue()) : null;
  }

  public @Nullable V remove(int key) {
    if (key <= 0) return null;

    int i = indexOf(key);
    return i == -1 ? null : removeAtIndex(i);
  }

  private V removeAtIndex(int index) {
    size--;
    modifications++;
    V result = values[index];
    for (; offsets[index + 1] > 1; index++) {
      // shift following values closer to their desired position
      keys[index] = keys[index + 1];
      offsets[index] = offsets[index + 1];
      offsets[index]--;
      values[index] = values[index + 1];
    }
    keys[index] = 0;
    offsets[index] = 0;
    return result;
  }

  @Override
  public void putAll(@NonNull Map<? extends Integer, ? extends V> m) {
    for (var e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void clear() {
    capacityIndex = 0;
    lookups = 4; // (int) log2(29) == 4;
    int capacityNew = PRIMES[capacityIndex] + lookups;
    keys = new int[capacityNew];
    offsets = new byte[capacityNew];
    values = (V[]) new Object[capacityNew];
    size = 0;
    modifications++;
  }

  @Override
  public Keys keySet() {
    return new Keys();
  }

  @Override
  public Collection<V> values() {
    return new AbstractCollection<>() {
      @Override
      public java.util.Iterator<V> iterator() {
        return Spliterators.iterator(spliterator());
      }

      @Override
      public Spliterator<V> spliterator() {
        return new Spliterator<V>() {
          @Override
          public boolean tryAdvance(Consumer<? super V> action) {
            int i = next();
            if (i == -1) return false;
            action.accept(values[i]);
            return true;
          }

          @Override
          public java.util.Spliterator<V> trySplit() {
            return null;
          }

          @Override
          public int characteristics() {
            return SIZED;
          }
        };
      }

      @Override
      public int size() {
        return size;
      }
    };
  }

  @Override
  public java.util.Set<Entry<Integer, V>> entrySet() {
    return new AbstractSet<>() {
      @Override
      public java.util.Iterator<Entry<Integer, V>> iterator() {
        return Spliterators.iterator(spliterator());
      }

      @Override
      public java.util.Spliterator<Entry<Integer, V>> spliterator() {
        return new Spliterator<Entry<Integer, V>>() {
          @Override
          public boolean tryAdvance(Consumer<? super Entry<Integer, V>> action) {
            int i = next();
            if (i == -1) return false;
            action.accept(Map.entry(keys[i], values[i]));
            return true;
          }

          @Override
          public java.util.Spliterator<Entry<Integer, V>> trySplit() {
            return null;
          }

          @Override
          public int characteristics() {
            return DISTINCT | NONNULL | SIZED;
          }
        };
      }

      @Override
      public int size() {
        return size;
      }
    };
  }

  @Override
  public String toString() {
    var sb = new StringBuilder("{");
    var iter = entrySet().iterator();
    if (iter.hasNext()) while (true) {
      var e = iter.next();
      sb.append(e.getKey())
        .append('=')
        .append(e.getValue() == this ? "(this Map)" : e.getValue());
      if (iter.hasNext()) sb.append(", ");
      else break;
    }
    sb.append('}');
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  public MapInt<V> clone() {
    try {
      MapInt<V> result = (MapInt<V>) super.clone();
      result.keys = copyOf(keys, keys.length);
      result.offsets = copyOf(offsets, offsets.length);
      result.values = copyOf(values, values.length);
      result.lookups = lookups;
      result.capacityIndex = capacityIndex;
      result.size = size;
      return result;
    } catch (CloneNotSupportedException _) {
      throw new AssertionError();
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == this)
      return true;

    if (!(o instanceof Map<?, ?> m))
      return false;
    if (m.size() != size())
      return false;

    try {
      for (var e : entrySet()) {
        int key = e.getKey();
        V value = e.getValue();
        if (!value.equals(m.get(key)))
          return false;
      }
    } catch (ClassCastException | NullPointerException _) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int h = 0;
    for (var e : entrySet())
      h += e.hashCode();
    return h;
  }

  public final class Keys extends AbstractSet<Integer> {
    @Override
    public PrimitiveIterator.OfInt iterator() {
      return Spliterators.iterator(spliterator());
    }

    @Override
    public java.util.Spliterator.OfInt spliterator() {
      return new Spliterator();
    }

    @Override
    public int size() {
      return size;
    }

    @Override
    public boolean add(Integer key) {
      return add(key.intValue());
    }

    public boolean add(int key) {
      int sizeOld = size;
      put(key, null);
      return sizeOld < size;
    }

    @Override
    public boolean contains(Object key) {
      return key instanceof Integer k && contains(k.intValue());
    }

    public boolean contains(int key) {
      return containsKey(key);
    }

    @Override
    public boolean remove(Object key) {
      return key instanceof Integer k && remove(k.intValue());
    }

    public boolean remove(int key) {
      if (key <= 0) return false;

      int i = indexOf(key);
      if (i == -1) return false;
      else {
        removeAtIndex(i);
        return true;
      }
    }

    @Override
    public void clear() {
      MapInt.this.clear();
    }

    private final class Spliterator extends MapInt<?>.Spliterator<Integer> implements java.util.Spliterator.OfInt {
      @Override
      public java.util.Spliterator.OfInt trySplit() {
        return null;
      }

      @Override
      public int characteristics() {
        return DISTINCT | SIZED | NONNULL;
      }

      @Override
      public boolean tryAdvance(IntConsumer action) {
        int i = next();
        if (i == -1) return false;
        action.accept(keys[i]);
        return true;
      }
    }
  }

  private abstract class Spliterator<I> implements java.util.Spliterator<I> {
    private final int modifications = MapInt.this.modifications;
    private int index = 0;

    protected final int next() {
      if (modifications != MapInt.this.modifications)
        throw new ConcurrentModificationException();

      for (int i = index; i < keys.length; i++) {
        if (keys[i] > 0) {
          index = i + 1;
          return i;
        }
      }
      index = keys.length;
      return -1;
    }

    @Override
    public final long estimateSize() {
      return size;
    }
  }
}
