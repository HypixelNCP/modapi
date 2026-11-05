package cc.rewound.modapi.packet.handler;

import net.hypixel.data.rank.MonthlyPackageRank;
import net.hypixel.data.rank.PackageRank;
import net.hypixel.data.rank.PlayerRank;
import cc.rewound.modapi.packet.ClientboundRewoundPacket;
import cc.rewound.modapi.packet.PacketHandler;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;

public class PlayerInfoHandler implements PacketHandler<ServerboundPlayerInfoPacket> {
    @Override
    public ClientboundRewoundPacket handle(String identifier, ServerboundPlayerInfoPacket packet) {
        return new ClientboundPlayerInfoPacket(
                PlayerRank.NORMAL,
                PackageRank.MVP_PLUS,
                MonthlyPackageRank.SUPERSTAR,
                null
        );
    }
}
