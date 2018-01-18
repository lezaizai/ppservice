package io.vos.stun.ice;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.Objects;

/**
 * Created by wuh56 on 4/25/2017.
 */
public final class PeerCollection implements Iterable<Peer> {
    public static PeerCollection EMPTY_COLLECTION = builder().build();

    private static final Function<PeerEntry, Peer> ENTRY_TO_NODE_TRANSFORM =
            new Function<PeerEntry, Peer>() {
                @Override
                public Peer apply(PeerEntry peerEntry) {
                    return peerEntry.getPeer();
                }
            };

    // Using a TreeMultimap as the internal data structure because it organizes
    // the Peer exactly as the spec defines. Using the Peer type (an
    // int) as the key allows fast'ish (log N?) lookups for all Peer of a
    // given type. The multimap feature means we can store multiple Peer at
    // a given type, like the spec allows for. The tree keeps the map values in
    // sorted order by natural ordering of the PeerEntry.position, aka the
    // order it was added in. This is the most important aspect because if
    // multiple Peer are in the message, then only the first one matters.
    private final TreeMultimap<String, PeerEntry> PeerMap;

    // The total size of all Peer in bytes, helps when writing the
    // Peer back out to a byte array.

    /**
     * Creates a new PeerCollection. Use
     * {@code PeerCollection.builder} to use this.
     */
    private PeerCollection(Builder builder) {
        PeerMap = TreeMultimap.create(builder.builderMap);
    }

    /**
     * Returns true if this {@code PeerCollection} contains no type/Peer
     * pairs.
     */
    public boolean isEmpty() {
        return PeerMap.isEmpty();
    }

    /** Returns the number of type/Peer pairs in the collection. */
    public int size() {
        return PeerMap.size();
    }

    @Override
    public Iterator<Peer> iterator() {
        // Using this backing iterator is probably slower, but not worried about
        // that now...
        final Iterator<PeerEntry> backingIterator = PeerMap.values().iterator();
        return new Iterator<Peer>() {
            @Override
            public boolean hasNext() {
                return backingIterator.hasNext();
            }

            @Override
            public Peer next() {
                return backingIterator.next().getPeer();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public boolean addPeer(InetAddress address, int port) {
        String id = String.format("%s:%d", address.getHostAddress(), port);
        return PeerMap.put(id, new PeerEntry(new Peer(address, port, false), 0));
    }
    public boolean addPeer(String address, int port) {
        String id = String.format("%s:%d", address, port);
        return PeerMap.put(id, new PeerEntry(new Peer(address, port, false), 0));
    }

    public boolean addPeer(Peer peer) {
        return PeerMap.put(peer.getId(), new PeerEntry(peer, 0));
    }

    public boolean hasPeer(String address, int port) {
        String id = String.format("%s:%d", address, port);
        return PeerMap.containsKey(id);
    }

    public boolean hasPeer(InetAddress address, int port) {
        String id = String.format("%s:%d", address.getHostAddress(), port);
        return PeerMap.containsKey(id);
    }

    /**
     * Returns an {@link Iterable} of all Peer in the collection of
     * {@code type}. Returns an empty {@link Iterable} if there are none.
     */
    public Iterable<Peer> getPeer(String address, int port) {
        String id = String.format("%s:%d", address, port);
        return Iterables.transform(PeerMap.get(id), ENTRY_TO_NODE_TRANSFORM);
    }
    public Iterable<Peer> getPeer(InetAddress address, int port) {
        String id = String.format("%s:%d", address.getHostAddress(), port);
        return Iterables.transform(PeerMap.get(id), ENTRY_TO_NODE_TRANSFORM);
    }

    /**
     * Returns the first Peer of the the given {@code type} or null. This is
     * generally the only Peer that matters in a message with multiple
     * duplicate Peer, unless otherwise stated in the definition of the
     * Peer.
     */
    @Nullable
    public Peer getFirstPeer(String address, int port) {
        String id = String.format("%s:%d", address, port);
        NavigableSet<PeerEntry> entrySet = PeerMap.get(id);
        if (entrySet.isEmpty()) {
            addPeer(address, port);
            return PeerMap.get(id).first().getPeer();
        }
        return entrySet.first().getPeer();
    }

    @Nullable
    public Peer getFirstPeer(InetAddress address, int port) {
        String id = String.format("%s:%d", address.getHostAddress(), port);
        NavigableSet<PeerEntry> entrySet = PeerMap.get(id);
        if (entrySet.isEmpty()) {
            addPeer(address, port);
            return PeerMap.get(id).first().getPeer();
        }
        return entrySet.first().getPeer();
    }

    /**
     * Returns a new {@link Builder} preloaded with the
     * {@link Peer} instances stored in this collection. Useful for modifying
     * the Peer for a reply.
     */
    public Builder replyBuilder() {
        Builder builder = new Builder();
        for (Peer attr : this) {
            builder.addPeer(attr);
        }
        return builder;
    }

    /**
     * Returns a new {@link Builder}, useful for building an
     * {@code PeerCollection} for a newly received message.
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private int position;
        private Multimap<String, PeerEntry> builderMap;
        private int totalByteSize;

        Builder() {
            position = 0;
            totalByteSize = 0;
            // Using a hashmultimap because I have hunch (no proof) that's its
            // fast to build.
            builderMap = HashMultimap.create();
        }

        /**
         * Adds a new {@code Peer} to the multimap, capable of being looked up
         * by its type and sorted by its position in the message.
         */
        public Builder addPeer(Peer peer) {
            builderMap.put(peer.getId(), new PeerEntry(peer, position++));
            return this;
        }

        public Builder addAllPeer(Iterable<Peer> peers) {
            for (Peer peer : peers) {
                addPeer(peer);
            }
            return this;
        }

        /**
         * Removes all Peer of the given {@code type}.
         */
        public Builder removeAllPeerByAddress(String address, int port) {
            String id = String.format("%s:%d", address, port);
            builderMap.removeAll(id);
            return this;
        }
        public Builder removeAllPeerByAddress(InetAddress address, int port) {
            String id = String.format("%s:%d", address.getHostAddress(), port);
            builderMap.removeAll(id);
            return this;
        }

        public PeerCollection build() {
            return new PeerCollection(this);
        }
    }

    /**
     * An entry in the internal multimap used to keep the sorted order that the
     * entry is added in.
     */
    private static class PeerEntry implements Comparable<PeerEntry> {

        private final int position;
        private final Peer Peer;

        PeerEntry(Peer Peer, int position) {
            Preconditions.checkArgument(position >= 0);
            this.position = position;
            this.Peer = Preconditions.checkNotNull(Peer);
        }

        Peer getPeer() {
            return Peer;
        }

        @Override
        public int compareTo(PeerEntry other) {
            return position - other.position;
        }

        /** The comparators used in a TreeMap must be consistent with equals. */
        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof PeerEntry)) {
                return false;
            } else if (this == other) {
                return true;
            }

            PeerEntry otherAttrEntry = (PeerEntry)other;
            return position == otherAttrEntry.position && Peer.equals(otherAttrEntry.Peer);
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, Peer);
        }
    }

}
