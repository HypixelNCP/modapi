package cc.rewound.modapi.packet.impl.clientbound.event;

import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Sent to a player who goes back into a running match that held their place, such as Bed Wars with their bed alive.
 * They get this instead of a second {@link ClientboundGameStartPacket}.
 * <p>
 * If they reconnected, the server holds this until the client subscribes, and drops it if they've left the match or it
 * has finished by then.
 */
public class ClientboundGameRejoinPacket extends ClientboundGameEventPacket {
    public static final int CURRENT_VERSION = 1;

    @ApiStatus.Internal
    public ClientboundGameRejoinPacket(String serverName, String serverTypeName, String mode, String map) {
        super(CURRENT_VERSION, serverName, serverTypeName, mode, map);
    }

    @ApiStatus.Internal
    public ClientboundGameRejoinPacket(PacketSerializer serializer) {
        super(serializer);
    }

    @Override
    protected int getLatestVersion() {
        return CURRENT_VERSION;
    }
}
