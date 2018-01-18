package io.vos.stun.attribute;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;
import javax.annotation.Nullable;

/**
 * A sorted collection of {@code Attribute} objects. This object provides
 * ordered iteration and map like lookup for {@code Attribute} objects. It is
 * immutable, however via the {@link Builder} supports
 * adding and removing attributes.
 * <p>
 * To build a new {@code AttributeCollection}
 * see the {@link AttributesCollection#replyBuilder} and {@link
 * AttributesCollection.builder} methods.
 */
public final class AttributesCollection implements Iterable<Attribute> {

  public static AttributesCollection EMPTY_COLLECTION = builder().build();

  private static final Function<AttributeEntry, Attribute> ENTRY_TO_ATTRIBUTE_TRANSFORM =
      new Function<AttributeEntry, Attribute>() {
    @Override
    public Attribute apply(AttributeEntry attrEntry) {
      return attrEntry.getAttribute();
    }
  };

  // Using a TreeMultimap as the internal data structure because it organizes
  // the attributes exactly as the spec defines. Using the attribute type (an
  // int) as the key allows fast'ish (log N?) lookups for all attributes of a
  // given type. The multimap feature means we can store multiple attributes at
  // a given type, like the spec allows for. The tree keeps the map values in
  // sorted order by natural ordering of the AttributeEntry.position, aka the
  // order it was added in. This is the most important aspect because if
  // multiple attributes are in the message, then only the first one matters.
  private final TreeMultimap<Integer, AttributeEntry> attributeMap;

  // The total size of all attributes in bytes, helps when writing the
  // attributes back out to a byte array.
  private final int totalByteSize;

  /**
   * Creates a new AttributesCollection. Use
   * {@code AttributesCollection.builder} to use this.
   */
  private AttributesCollection(Builder builder) {
    attributeMap = TreeMultimap.create(builder.builderMap);
    totalByteSize = builder.totalByteSize;
  }

  /**
   * Returns true if this {@code AttributeCollection} contains no type/Attribute
   * pairs.
   */
  public boolean isEmpty() {
    return attributeMap.isEmpty();
  }

  /** Returns the number of type/Attribute pairs in the collection. */
  public int size() {
    return attributeMap.size();
  }

  @Override
  public Iterator<Attribute> iterator() {
    // Using this backing iterator is probably slower, but not worried about
    // that now...
    final Iterator<AttributeEntry> backingIterator = attributeMap.values().iterator();
    return new Iterator<Attribute>() {
      @Override
      public boolean hasNext() {
        return backingIterator.hasNext();
      }

      @Override
      public Attribute next() {
        return backingIterator.next().getAttribute();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }

  public boolean hasAttributeType(int type) {
    return attributeMap.containsKey(type);
  }

  /**
   * Returns an {@link Iterable} of all attributes in the collection of
   * {@code type}. Returns an empty {@link Iterable} if there are none.
   */
  public Iterable<Attribute> getAttributesOfType(int type) {
    return Iterables.transform(attributeMap.get(type), ENTRY_TO_ATTRIBUTE_TRANSFORM);
  }

  /**
   * Returns the first attribute of the the given {@code type} or null. This is
   * generally the only attribute that matters in a message with multiple
   * duplicate attributes, unless otherwise stated in the definition of the
   * attribute.
   */
  @Nullable
  public Attribute getFirstAttributeOfType(int type) {
    NavigableSet<AttributeEntry> entrySet = attributeMap.get(type);
    return entrySet.isEmpty()
        ? null
        : entrySet.first().getAttribute();
  }

  /**
   * Returns an in order byte array representation of all {@code Attribute}
   * instances in the collection.
   */
  public byte[] toByteArray() {
    byte[] byteOutput = new byte[totalByteSize];
    int currentIndex = 0;
    for (Attribute attr : this) {
      int attrByteSize = attr.getTotalLength();
      System.arraycopy(attr.toByteArray(), 0, byteOutput, currentIndex, attrByteSize);
      currentIndex += attrByteSize;
    }
    return byteOutput;
  }

  /**
   * Returns a new {@link Builder} preloaded with the
   * {@link Attribute} instances stored in this collection. Useful for modifying
   * the attributes for a reply.
   */
  public Builder replyBuilder() {
    Builder builder = new Builder();
    for (Attribute attr : this) {
      builder.addAttribute(attr);
    }
    return builder;
  }

  /**
   * Returns a new {@link Builder}, useful for building an
   * {@code AttributeCollection} for a newly received message.
   */
  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {

    private int position;
    private Multimap<Integer, AttributeEntry> builderMap;
    private int totalByteSize;

    Builder() {
      position = 0;
      totalByteSize = 0;
      // Using a hashmultimap because I have hunch (no proof) that's its
      // fast to build.
      builderMap = HashMultimap.create();
    }

    /**
     * Adds a new {@code Attribute} to the multimap, capable of being looked up
     * by its type and sorted by its position in the message.
     */
    public Builder addAttribute(Attribute attr) {
      builderMap.put(attr.getType(), new AttributeEntry(attr, position++));
      totalByteSize += attr.getTotalLength();
      return this;
    }

    public Builder addAllAttributes(Iterable<Attribute> attrs) {
      for (Attribute attr : attrs) {
        addAttribute(attr);
      }
      return this;
    }

    /**
     * Removes all attributes of the given {@code type}.
     */
    public Builder removeAllAttributesByType(int type) {
      builderMap.removeAll(type);
      return this;
    }

    public AttributesCollection build() {
      return new AttributesCollection(this);
    }
  }

  /**
   * An entry in the internal multimap used to keep the sorted order that the
   * entry is added in.
   */
  private static class AttributeEntry implements Comparable<AttributeEntry> {

    private final int position;
    private final Attribute attribute;

    AttributeEntry(Attribute attribute, int position) {
      Preconditions.checkArgument(position >= 0);
      this.position = position;
      this.attribute = Preconditions.checkNotNull(attribute);
    }

    Attribute getAttribute() {
      return attribute;
    }

    @Override
    public int compareTo(AttributeEntry other) {
      return position - other.position;
    }

    /** The comparators used in a TreeMap must be consistent with equals. */
    @Override
    public boolean equals(Object other) {
      if (other == null || !(other instanceof AttributeEntry)) {
        return false;
      } else if (this == other) {
        return true;
      }

      AttributeEntry otherAttrEntry = (AttributeEntry)other;
      return position == otherAttrEntry.position && attribute.equals(otherAttrEntry.attribute);
    }

    @Override
    public int hashCode() {
      return Objects.hash(position, attribute);
    }
  }
}
