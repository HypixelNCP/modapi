package cc.rewound.modapi.packet.impl.clientbound.event;

import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Sent when a match ends, to the players still in it.
 */
public class ClientboundGameFinishPacket extends ClientboundGameEventPacket {
    public static final int CURRENT_VERSION = 1;

    private Outcome outcome;
    private int secondsPlayed;

    @ApiStatus.Internal
    public ClientboundGameFinishPacket(String serverName, String serverTypeName, String mode, String map, Outcome outcome, int secondsPlayed) {
        super(CURRENT_VERSION, serverName, serverTypeName, mode, map);
        this.outcome = outcome;
        this.secondsPlayed = secondsPlayed;
    }

    @ApiStatus.Internal
    public ClientboundGameFinishPacket(PacketSerializer serializer) {
        super(serializer);
    }

    @Override
    protected boolean read(PacketSerializer serializer) {
        if (!super.read(serializer)) {
            return false;
        }

        this.outcome = Outcome.getById(serializer.readVarInt());
        this.secondsPlayed = serializer.readVarInt();
        return true;
    }

    @Override
    public void write(PacketSerializer serializer) {
        super.write(serializer);
        serializer.writeVarInt(outcome.getId());
        serializer.writeVarInt(secondsPlayed);
    }

    @Override
    protected int getLatestVersion() {
        return CURRENT_VERSION;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public int getSecondsPlayed() {
        return secondsPlayed;
    }

    @Override
    public String toString() {
        return "ClientboundGameFinishPacket{" +
                "outcome=" + outcome +
                ", secondsPlayed=" + secondsPlayed +
                "} " + super.toString();
    }

    public enum Outcome {
        WIN(0),
        LOSS(1),
        DRAW(2),
        /**
         * The match was stopped without a result.
         */
        CANCELLED(3),
        /**
         * An id this version of the library doesn't know.
         */
        UNKNOWN(-1),
        ;

        private final int id;

        Outcome(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        static Outcome getById(int id) {
            for (Outcome outcome : values()) {
                if (outcome.id == id) {
                    return outcome;
                }
            }
            return UNKNOWN;
        }
    }
}
