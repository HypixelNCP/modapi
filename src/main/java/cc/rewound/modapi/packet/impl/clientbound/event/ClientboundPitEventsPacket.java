package cc.rewound.modapi.packet.impl.clientbound.event;

import cc.rewound.modapi.packet.EventPacket;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundVersionedPacket;
import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The Pit's event timers: the event running on the player's Pit map, if any, and the next minor and major events with
 * their start times, soonest first. Sent in the Pit on subscribing and again whenever the schedule changes, which
 * includes start times moving: an event waits for enough players, and the next ones are timed from the end of the
 * running one. Nothing is sent outside the Pit.
 */
public class ClientboundPitEventsPacket extends ClientboundVersionedPacket implements EventPacket {
    public static final int CURRENT_VERSION = 1;

    @Nullable
    private PitEvent current;
    private List<PitEvent> upcoming;

    @ApiStatus.Internal
    public ClientboundPitEventsPacket(@Nullable PitEvent current, List<PitEvent> upcoming) {
        super(CURRENT_VERSION);
        this.current = current;
        this.upcoming = Collections.unmodifiableList(new ArrayList<>(upcoming));
    }

    @ApiStatus.Internal
    public ClientboundPitEventsPacket(PacketSerializer serializer) {
        super(serializer);
    }

    @Override
    protected boolean read(PacketSerializer serializer) {
        if (!super.read(serializer)) {
            return false;
        }

        this.current = serializer.readOptionally(PitEvent::readRunning);
        int count = serializer.readVarInt();
        List<PitEvent> upcoming = new ArrayList<>(Math.max(0, Math.min(count, 16)));
        for (int i = 0; i < count; i++) {
            upcoming.add(PitEvent.readUpcoming(serializer));
        }
        this.upcoming = Collections.unmodifiableList(upcoming);
        return true;
    }

    @Override
    public void write(PacketSerializer serializer) {
        super.write(serializer);
        serializer.writeOptionally(current, (s, event) -> event.writeRunning(s));
        serializer.writeVarInt(upcoming.size());
        for (PitEvent event : upcoming) {
            event.writeUpcoming(serializer);
        }
    }

    @Override
    protected int getLatestVersion() {
        return CURRENT_VERSION;
    }

    /**
     * @return the event running on the player's map, with its end time
     */
    public Optional<PitEvent> getCurrent() {
        return Optional.ofNullable(current);
    }

    /**
     * @return the next events, soonest first: at most the next minor and the next major
     */
    public List<PitEvent> getUpcoming() {
        return upcoming;
    }

    /**
     * @return the first upcoming event of that kind
     */
    public Optional<PitEvent> getNext(boolean major) {
        for (PitEvent event : upcoming) {
            if (event.isMajor() == major) {
                return Optional.of(event);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "ClientboundPitEventsPacket{" +
                "current=" + current +
                ", upcoming=" + upcoming +
                "} " + super.toString();
    }

    public static final class PitEvent {
        private final String id;
        private final String name;
        private final boolean major;
        private final long startTime;
        private final long endTime;

        /**
         * @param endTime when a running event ends; 0 for one still to come
         */
        public PitEvent(String id, String name, boolean major, long startTime, long endTime) {
            this.id = id;
            this.name = name;
            this.major = major;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        static PitEvent readRunning(PacketSerializer serializer) {
            String id = serializer.readString();
            String name = serializer.readString();
            boolean major = serializer.readBoolean();
            long start = serializer.readLong();
            long end = serializer.readLong();
            return new PitEvent(id, name, major, start, end);
        }

        static PitEvent readUpcoming(PacketSerializer serializer) {
            String id = serializer.readString();
            String name = serializer.readString();
            boolean major = serializer.readBoolean();
            long start = serializer.readLong();
            return new PitEvent(id, name, major, start, 0L);
        }

        void writeRunning(PacketSerializer serializer) {
            writeUpcoming(serializer);
            serializer.writeLong(endTime);
        }

        void writeUpcoming(PacketSerializer serializer) {
            serializer.writeString(id);
            serializer.writeString(name);
            serializer.writeBoolean(major);
            serializer.writeLong(startTime);
        }

        /**
         * @return the catalog id, such as {@code king-of-the-hill}, {@code 2x-rewards} or {@code spire}
         */
        public String getId() {
            return id;
        }

        /**
         * @return the name as announced, without colours, such as {@code KING OF THE HILL}
         */
        public String getName() {
            return name;
        }

        public boolean isMajor() {
            return major;
        }

        /**
         * @return when it starts, or started, in epoch milliseconds
         */
        public long getStartTime() {
            return startTime;
        }

        /**
         * @return when a running event ends, in epoch milliseconds; 0 for an upcoming one
         */
        public long getEndTime() {
            return endTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PitEvent that = (PitEvent) o;
            return major == that.major && startTime == that.startTime && endTime == that.endTime
                    && Objects.equals(id, that.id) && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, major, startTime, endTime);
        }

        @Override
        public String toString() {
            return "PitEvent{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", major=" + major +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    '}';
        }
    }
}
