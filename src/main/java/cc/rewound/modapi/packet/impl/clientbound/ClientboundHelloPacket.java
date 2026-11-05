package cc.rewound.modapi.packet.impl.clientbound;

import net.hypixel.data.region.Environment;
import cc.rewound.modapi.packet.ClientboundRewoundPacket;
import cc.rewound.modapi.serializer.PacketSerializer;
import org.jetbrains.annotations.ApiStatus;

/**
 * This packet is automatically sent on every join to a Rewound server, including server switches, to indicate that the client has connected to a Rewound server.
 * Due to the nature of this packet, it is implemented without versioning and designed to discard extra bytes in the case that more data is added in the future.
 */
public class ClientboundHelloPacket implements ClientboundRewoundPacket {
    private final Environment environment;

    @ApiStatus.Internal
    public ClientboundHelloPacket(Environment environment) {
        this.environment = environment;
    }

    @ApiStatus.Internal
    public ClientboundHelloPacket(PacketSerializer serializer) {
        this.environment = Environment.getById(serializer.readVarInt()).orElseThrow(() -> new IllegalArgumentException("Invalid environment ID"));

        // Read any remaining bytes, so that if more data is added in the future, it will be discarded on older clients
        serializer.discardRemaining();
    }

    @Override
    public void write(PacketSerializer serializer) {
        serializer.writeVarInt(environment.getId());
    }

    public Environment getEnvironment() {
        return environment;
    }

    @Override
    public String toString() {
        return "ClientboundHelloPacket{" +
                "environment=" + environment +
                '}';
    }

}
