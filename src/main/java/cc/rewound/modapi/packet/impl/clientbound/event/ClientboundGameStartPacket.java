package cc.rewound.modapi.packet.impl.clientbound.event;

import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;

/**
 * Sent when a match starts playing, after the countdown and any cage phase.
 */
public class ClientboundGameStartPacket extends ClientboundGameEventPacket {
    public static final int CURRENT_VERSION = 1;

    @ApiStatus.Internal
    public ClientboundGameStartPacket(String serverName, String serverTypeName, String mode, String map) {
        super(CURRENT_VERSION, serverName, serverTypeName, mode, map);
    }

    @ApiStatus.Internal
    public ClientboundGameStartPacket(PacketSerializer serializer) {
        super(serializer);
    }

    @Override
    protected int getLatestVersion() {
        return CURRENT_VERSION;
    }
}
