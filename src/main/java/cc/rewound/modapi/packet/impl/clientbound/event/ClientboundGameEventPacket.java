package cc.rewound.modapi.packet.impl.clientbound.event;

import cc.rewound.modapi.packet.EventPacket;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundVersionedPacket;
import cc.rewound.modapi.serializer.PacketSerializer;
import net.hypixel.data.type.ServerType;
import org.jetbrains.annotations.ApiStatus;

import java.util.Optional;

/**
 * The fields shared by the match events. Unlike the location event, all of them are always present.
 */
public abstract class ClientboundGameEventPacket extends ClientboundVersionedPacket implements EventPacket {
    private String serverName;
    private String serverTypeName;
    private String mode;
    private String map;

    @ApiStatus.Internal
    protected ClientboundGameEventPacket(int version, String serverName, String serverTypeName, String mode, String map) {
        super(version);
        this.serverName = serverName;
        this.serverTypeName = serverTypeName;
        this.mode = mode;
        this.map = map;
    }

    @ApiStatus.Internal
    protected ClientboundGameEventPacket(PacketSerializer serializer) {
        super(serializer);
    }

    @Override
    protected boolean read(PacketSerializer serializer) {
        if (!super.read(serializer)) {
            return false;
        }

        this.serverName = serializer.readString();
        this.serverTypeName = serializer.readString();
        this.mode = serializer.readString();
        this.map = serializer.readString();
        return true;
    }

    @Override
    public void write(PacketSerializer serializer) {
        super.write(serializer);
        serializer.writeString(serverName);
        serializer.writeString(serverTypeName);
        serializer.writeString(mode);
        serializer.writeString(map);
    }

    public String getServerName() {
        return serverName;
    }

    /**
     * @return the server type as sent, for a type {@link #getServerType()} doesn't know
     */
    public String getServerTypeName() {
        return serverTypeName;
    }

    public Optional<ServerType> getServerType() {
        return ServerType.valueOf(serverTypeName);
    }

    public String getMode() {
        return mode;
    }

    public String getMap() {
        return map;
    }

    @Override
    public String toString() {
        return "ClientboundGameEventPacket{" +
                "serverName='" + serverName + '\'' +
                ", serverTypeName='" + serverTypeName + '\'' +
                ", mode='" + mode + '\'' +
                ", map='" + map + '\'' +
                "} " + super.toString();
    }
}
