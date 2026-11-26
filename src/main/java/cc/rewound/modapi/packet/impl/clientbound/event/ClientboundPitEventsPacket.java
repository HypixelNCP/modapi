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
 * The Pit's event schedule and live event data: the player's Pit map id/name and population, the event running on that
 * map (if any) with its description, rules, optional destination and viewer live status lines, and upcoming events
 * soonest first with their start times, nominal durations, descriptions and rules. Sent in the Pit on subscribing and
 * again whenever the schedule, population, or live status changes. Nothing is sent outside the Pit.
 */
public class ClientboundPitEventsPacket extends ClientboundVersionedPacket implements EventPacket {
    public static final int CURRENT_VERSION = 2;

    private String mapId;
    private String mapName;
    private int population;
    private int minPlayers;
    @Nullable
    private PitEvent current;
    private List<PitEvent> upcoming;

    @ApiStatus.Internal
    public ClientboundPitEventsPacket(@Nullable PitEvent current, List<PitEvent> upcoming) {
        this("", "", 0, 0, current, upcoming);
    }

    @ApiStatus.Internal
    public ClientboundPitEventsPacket(String mapId, String mapName, int population, int minPlayers,
                                      @Nullable PitEvent current, List<PitEvent> upcoming) {
        super(CURRENT_VERSION);
        this.mapId = Objects.requireNonNull(mapId, "mapId");
        this.mapName = Objects.requireNonNull(mapName, "mapName");
        this.population = population;
        this.minPlayers = minPlayers;
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

        this.mapId = serializer.readString();
        this.mapName = serializer.readString();
        this.population = serializer.readVarInt();
        this.minPlayers = serializer.readVarInt();
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
        serializer.writeString(mapId);
        serializer.writeString(mapName);
        serializer.writeVarInt(population);
        serializer.writeVarInt(minPlayers);
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
     * @return the Pit map id, such as {@code castle}
     */
    public String getMapId() {
        return mapId;
    }

    /**
     * @return the Pit map display name, such as {@code Castle}
     */
    public String getMapName() {
        return mapName;
    }

    /**
     * @return current Pit players on this map
     */
    public int getPopulation() {
        return population;
    }

    /**
     * @return minimum players required for scheduled events to start
     */
    public int getMinPlayers() {
        return minPlayers;
    }

    /**
     * @return the event running on the player's map, with its end time and live status
     */
    public Optional<PitEvent> getCurrent() {
        return Optional.ofNullable(current);
    }

    /**
     * @return the upcoming events, soonest first
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
                "mapId='" + mapId + '\'' +
                ", mapName='" + mapName + '\'' +
                ", population=" + population +
                ", minPlayers=" + minPlayers +
                ", current=" + current +
                ", upcoming=" + upcoming +
                "} " + super.toString();
    }

    public static final class PitEvent {
        private final String id;
        private final String name;
        private final boolean major;
        private final long startTime;
        private final long endTime;
        private final int durationSeconds;
        private final String description;
        private final String rules;
        @Nullable
        private final String destination;
        private final List<String> statusLines;

        /**
         * @param endTime when a running event ends; 0 for one still to come
         */
        public PitEvent(String id, String name, boolean major, long startTime, long endTime) {
            this(id, name, major, startTime, endTime,
                    endTime > startTime ? (int) ((endTime - startTime) / 1000L) : 0,
                    "", "", null, Collections.emptyList());
        }

        public PitEvent(String id, String name, boolean major, long startTime, long endTime,
                        int durationSeconds, String description, String rules,
                        @Nullable String destination, List<String> statusLines) {
            this.id = Objects.requireNonNull(id, "id");
            this.name = Objects.requireNonNull(name, "name");
            this.major = major;
            this.startTime = startTime;
            this.endTime = endTime;
            this.durationSeconds = durationSeconds;
            this.description = Objects.requireNonNull(description, "description");
            this.rules = Objects.requireNonNull(rules, "rules");
            this.destination = destination;
            this.statusLines = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(statusLines, "statusLines")));
        }

        static PitEvent readRunning(PacketSerializer serializer) {
            String id = serializer.readString();
            String name = serializer.readString();
            boolean major = serializer.readBoolean();
            long start = serializer.readLong();
            long end = serializer.readLong();
            String description = serializer.readString();
            String rules = serializer.readString();
            String destination = serializer.readOptionally(PacketSerializer::readString);
            int statusCount = serializer.readVarInt();
            List<String> statusLines = new ArrayList<>(Math.max(0, Math.min(statusCount, 16)));
            for (int i = 0; i < statusCount; i++) {
                statusLines.add(serializer.readString());
            }
            int duration = end > start ? (int) ((end - start) / 1000L) : 0;
            return new PitEvent(id, name, major, start, end, duration, description, rules, destination, statusLines);
        }

        static PitEvent readUpcoming(PacketSerializer serializer) {
            String id = serializer.readString();
            String name = serializer.readString();
            boolean major = serializer.readBoolean();
            long start = serializer.readLong();
            int duration = serializer.readVarInt();
            String description = serializer.readString();
            String rules = serializer.readString();
            return new PitEvent(id, name, major, start, 0L, duration, description, rules, null, Collections.emptyList());
        }

        void writeRunning(PacketSerializer serializer) {
            serializer.writeString(id);
            serializer.writeString(name);
            serializer.writeBoolean(major);
            serializer.writeLong(startTime);
            serializer.writeLong(endTime);
            serializer.writeString(description);
            serializer.writeString(rules);
            serializer.writeOptionally(destination, PacketSerializer::writeString);
            serializer.writeVarInt(statusLines.size());
            for (String line : statusLines) {
                serializer.writeString(line);
            }
        }

        void writeUpcoming(PacketSerializer serializer) {
            serializer.writeString(id);
            serializer.writeString(name);
            serializer.writeBoolean(major);
            serializer.writeLong(startTime);
            serializer.writeVarInt(durationSeconds);
            serializer.writeString(description);
            serializer.writeString(rules);
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

        /**
         * @return nominal event duration in seconds
         */
        public int getDurationSeconds() {
            return durationSeconds;
        }

        /**
         * @return concise plain-text event description
         */
        public String getDescription() {
            return description;
        }

        /**
         * @return plain-text rules, bonus and rewards summary
         */
        public String getRules() {
            return rules;
        }

        /**
         * @return optional destination label for a running event (e.g. {@code Go to GARDEN!})
         */
        public Optional<String> getDestination() {
            return Optional.ofNullable(destination);
        }

        /**
         * @return plain-text live status lines for the viewer while an event is running
         */
        public List<String> getStatusLines() {
            return statusLines;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PitEvent that = (PitEvent) o;
            return major == that.major && startTime == that.startTime && endTime == that.endTime
                    && durationSeconds == that.durationSeconds
                    && Objects.equals(id, that.id) && Objects.equals(name, that.name)
                    && Objects.equals(description, that.description) && Objects.equals(rules, that.rules)
                    && Objects.equals(destination, that.destination) && Objects.equals(statusLines, that.statusLines);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, major, startTime, endTime, durationSeconds, description, rules, destination, statusLines);
        }

        @Override
        public String toString() {
            return "PitEvent{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", major=" + major +
                    ", startTime=" + startTime +
                    ", endTime=" + endTime +
                    ", durationSeconds=" + durationSeconds +
                    ", description='" + description + '\'' +
                    ", rules='" + rules + '\'' +
                    ", destination='" + destination + '\'' +
                    ", statusLines=" + statusLines +
                    '}';
        }
    }
}
