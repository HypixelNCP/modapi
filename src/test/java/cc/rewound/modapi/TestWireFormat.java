package cc.rewound.modapi;

import cc.rewound.modapi.error.BuiltinErrorReason;
import cc.rewound.modapi.error.ErrorReason;
import cc.rewound.modapi.packet.ClientboundRewoundPacket;
import cc.rewound.modapi.packet.RewoundPacket;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundHelloPacket;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundPingPacket;
import cc.rewound.modapi.packet.impl.clientbound.ClientboundPlayerInfoPacket;
import cc.rewound.modapi.packet.impl.clientbound.event.ClientboundGameFinishPacket;
import cc.rewound.modapi.packet.impl.clientbound.event.ClientboundGameStartPacket;
import cc.rewound.modapi.packet.impl.clientbound.event.ClientboundLocationPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPartyInfoPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPingPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundPlayerInfoPacket;
import cc.rewound.modapi.packet.impl.serverbound.ServerboundRegisterPacket;
import cc.rewound.modapi.serializer.PacketSerializer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.hypixel.data.rank.MonthlyPackageRank;
import net.hypixel.data.rank.PackageRank;
import net.hypixel.data.rank.PlayerRank;
import net.hypixel.data.region.Environment;
import net.hypixel.data.type.GameType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Holds the library to the bytes the Rewound server sends and expects. The payloads here are built by hand rather than
 * with {@link PacketSerializer}, so a mistake in the library can't cancel itself out.
 */
public class TestWireFormat {
    private static final List<RewoundPacket> SENT = new ArrayList<>();
    private static final RewoundModAPI API = RewoundModAPI.getInstance();

    @BeforeAll
    static void connect() {
        API.setModImplementation(new RewoundModAPIImplementation() {
            @Override
            public void onInit() {
            }

            @Override
            public boolean sendPacket(RewoundPacket packet) {
                return SENT.add(packet);
            }

            @Override
            public boolean isConnectedToRewound() {
                return true;
            }
        });
    }

    @BeforeEach
    void clearSent() {
        SENT.clear();
    }

    private static final class Bytes {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Bytes raw(int... values) {
            for (int value : values) {
                out.write(value);
            }
            return this;
        }

        Bytes string(String value) {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            out.write(bytes.length);
            out.write(bytes, 0, bytes.length);
            return this;
        }

        Bytes uuid(UUID uuid) {
            ByteBuf buf = Unpooled.buffer();
            buf.writeLong(uuid.getMostSignificantBits()).writeLong(uuid.getLeastSignificantBits());
            out.write(buf.array(), 0, 16);
            return this;
        }

        byte[] bytes() {
            return out.toByteArray();
        }
    }

    private static Bytes success(int version) {
        return new Bytes().raw(1, version);
    }

    private static void receive(String channel, byte[] payload) {
        API.handle(channel, new PacketSerializer(Unpooled.wrappedBuffer(payload)));
    }

    private static <T extends ClientboundRewoundPacket> T receive(Class<T> type, String channel, byte[] payload) {
        AtomicReference<T> received = new AtomicReference<>();
        API.createHandler(type, received::set);
        receive(channel, payload);
        assertNotNull(received.get(), "No packet handled from " + channel);
        return received.get();
    }

    private static byte[] written(RewoundPacket packet) {
        ByteBuf buf = Unpooled.buffer();
        packet.write(new PacketSerializer(buf));
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        return bytes;
    }

    @Test
    void channelsAreRewoundOnes() {
        assertEquals("rewound:hello", new ClientboundHelloPacket(Environment.PRODUCTION).getIdentifier());
        assertEquals("rewound:ping", new ServerboundPingPacket().getIdentifier());
        assertEquals("rewound:party_info", new ServerboundPartyInfoPacket().getIdentifier());
        assertEquals("rewound:player_info", new ServerboundPlayerInfoPacket().getIdentifier());
        assertEquals("rewound:location", API.getRegistry().getIdentifier(ClientboundLocationPacket.class));
        assertEquals("rewound:game_start", API.getRegistry().getIdentifier(ClientboundGameStartPacket.class));
        assertEquals("rewound:game_finish", API.getRegistry().getIdentifier(ClientboundGameFinishPacket.class));
        assertTrue(API.getRegistry().getIdentifiers().stream().allMatch(identifier -> identifier.startsWith("rewound:")));
    }

    @Test
    void requestsSendTheirVersion() {
        assertArrayEquals(new byte[]{1}, written(new ServerboundPingPacket()));
        assertArrayEquals(new byte[]{2}, written(new ServerboundPartyInfoPacket()));
        assertArrayEquals(new byte[]{1}, written(new ServerboundPlayerInfoPacket()));
    }

    @Test
    void everyHelloRegistersAgain() {
        API.subscribeToEventPacket(ClientboundLocationPacket.class);
        SENT.clear();

        byte[] hello = new Bytes().raw(1, 0).bytes();
        receive("rewound:hello", hello);
        receive("rewound:hello", hello);

        assertEquals(2, SENT.size());
        assertTrue(SENT.stream().allMatch(packet -> packet instanceof ServerboundRegisterPacket));
    }

    @Test
    void registerLayout() {
        ServerboundRegisterPacket packet = new ServerboundRegisterPacket(API.getRegistry(), java.util.Collections.singleton("rewound:game_finish"));
        assertArrayEquals(new Bytes().raw(1, 1).string("rewound:game_finish").raw(1).bytes(), written(packet));
    }

    @Test
    void helloCarriesTheEnvironment() {
        ClientboundHelloPacket packet = receive(ClientboundHelloPacket.class, "rewound:hello", new Bytes().raw(1, 0).bytes());
        assertEquals(Environment.PRODUCTION, packet.getEnvironment());
    }

    @Test
    void ping() {
        ClientboundPingPacket packet = receive(ClientboundPingPacket.class, "rewound:ping", success(1).string("pong").bytes());
        assertEquals("pong", packet.getResponse());
    }

    @Test
    void partyInfoWithMembers() {
        UUID leader = new UUID(1, 2);
        UUID member = new UUID(3, 4);
        byte[] payload = success(2).raw(1, 2).uuid(leader).raw(0).uuid(member).raw(2).bytes();

        ClientboundPartyInfoPacket packet = receive(ClientboundPartyInfoPacket.class, "rewound:party_info", payload);

        assertTrue(packet.isInParty());
        assertEquals(leader, packet.getLeader().orElse(null));
        assertEquals(ClientboundPartyInfoPacket.PartyRole.MEMBER, packet.getMemberMap().get(member).getRole());
    }

    @Test
    void partyInfoOutOfAParty() {
        ClientboundPartyInfoPacket packet = receive(ClientboundPartyInfoPacket.class, "rewound:party_info", success(2).raw(0).bytes());
        assertFalse(packet.isInParty());
        assertTrue(packet.getMembers().isEmpty());
    }

    @Test
    void playerInfoForMvpPlusPlus() {
        ClientboundPlayerInfoPacket packet = receive(ClientboundPlayerInfoPacket.class, "rewound:player_info", success(1).raw(1, 5, 2, 0).bytes());
        assertEquals(PlayerRank.NORMAL, packet.getPlayerRank());
        assertEquals(PackageRank.MVP_PLUS, packet.getPackageRank());
        assertEquals(MonthlyPackageRank.SUPERSTAR, packet.getMonthlyPackageRank());
        assertFalse(packet.getPrefix().isPresent());
    }

    @Test
    void playerInfoForStaffHasAPrefix() {
        byte[] payload = success(1).raw(3, 1, 1, 1).string("§c[ADMIN]").bytes();
        ClientboundPlayerInfoPacket packet = receive(ClientboundPlayerInfoPacket.class, "rewound:player_info", payload);
        assertEquals(PlayerRank.STAFF, packet.getPlayerRank());
        assertEquals("§c[ADMIN]", packet.getPrefix().orElse(null));
    }

    @Test
    void locationInAGame() {
        byte[] payload = success(1).string("mini400Z")
                .raw(1).string("SKYWARS").raw(0).raw(1).string("solo_normal").raw(1).string("Elven").bytes();

        ClientboundLocationPacket packet = receive(ClientboundLocationPacket.class, "rewound:location", payload);

        assertEquals("mini400Z", packet.getServerName());
        assertEquals(GameType.SKYWARS, packet.getServerType().orElse(null));
        assertFalse(packet.getLobbyName().isPresent());
        assertEquals("solo_normal", packet.getMode().orElse(null));
        assertEquals("Elven", packet.getMap().orElse(null));
    }

    @Test
    void locationInALobby() {
        byte[] payload = success(1).string("dynamiclobby1")
                .raw(1).string("BEDWARS").raw(1).string("bedwarslobby1").raw(0).raw(0).bytes();

        ClientboundLocationPacket packet = receive(ClientboundLocationPacket.class, "rewound:location", payload);

        assertEquals("bedwarslobby1", packet.getLobbyName().orElse(null));
        assertFalse(packet.getMode().isPresent());
        assertFalse(packet.getMap().isPresent());
    }

    @Test
    void gameStart() {
        byte[] payload = success(1).string("mini400Z").string("BEDWARS").string("BEDWARS_EIGHT_ONE").string("Elven").bytes();

        ClientboundGameStartPacket packet = receive(ClientboundGameStartPacket.class, "rewound:game_start", payload);

        assertEquals("mini400Z", packet.getServerName());
        assertEquals("BEDWARS", packet.getServerTypeName());
        assertEquals(GameType.BEDWARS, packet.getServerType().orElse(null));
        assertEquals("BEDWARS_EIGHT_ONE", packet.getMode());
        assertEquals("Elven", packet.getMap());
    }

    @Test
    void gameFinishWithAMultiByteDuration() {
        byte[] payload = success(1).string("mini400Z").string("DUELS").string("DUELS_CLASSIC_DUEL").string("Arena")
                .raw(1).raw(0xAC, 0x02).bytes();

        ClientboundGameFinishPacket packet = receive(ClientboundGameFinishPacket.class, "rewound:game_finish", payload);

        assertEquals("Arena", packet.getMap());
        assertEquals(ClientboundGameFinishPacket.Outcome.LOSS, packet.getOutcome());
        assertEquals(300, packet.getSecondsPlayed());
    }

    @Test
    void gameFinishOutcomeIds() {
        ClientboundGameFinishPacket.Outcome[] outcomes = {
                ClientboundGameFinishPacket.Outcome.WIN,
                ClientboundGameFinishPacket.Outcome.LOSS,
                ClientboundGameFinishPacket.Outcome.DRAW,
                ClientboundGameFinishPacket.Outcome.CANCELLED,
                ClientboundGameFinishPacket.Outcome.UNKNOWN,
        };
        for (int id = 0; id < outcomes.length; id++) {
            byte[] payload = success(1).string("s").string("t").string("m").string("p").raw(id, 0).bytes();
            assertEquals(outcomes[id], receive(ClientboundGameFinishPacket.class, "rewound:game_finish", payload).getOutcome());
        }
    }

    @Test
    void gameEventsSurviveAServerTypeTheLibraryDoesNotKnow() {
        byte[] payload = success(1).string("s").string("NEW_GAME").string("m").string("p").bytes();

        ClientboundGameStartPacket packet = receive(ClientboundGameStartPacket.class, "rewound:game_start", payload);

        assertEquals("NEW_GAME", packet.getServerTypeName());
        assertFalse(packet.getServerType().isPresent());
    }

    @Test
    void gameEventsWriteWhatTheyRead() {
        byte[] start = success(1).string("s").string("DUELS").string("m").string("p").bytes();
        byte[] finish = success(1).string("s").string("DUELS").string("m").string("p").raw(0, 90).bytes();

        assertArrayEquals(java.util.Arrays.copyOfRange(start, 1, start.length),
                written(new ClientboundGameStartPacket("s", "DUELS", "m", "p")));
        assertArrayEquals(java.util.Arrays.copyOfRange(finish, 1, finish.length),
                written(new ClientboundGameFinishPacket("s", "DUELS", "m", "p", ClientboundGameFinishPacket.Outcome.WIN, 90)));
    }

    @Test
    void aNewerEventVersionIsIgnored() {
        AtomicReference<ClientboundGameStartPacket> received = new AtomicReference<>();
        API.createHandler(ClientboundGameStartPacket.class, received::set);

        receive("rewound:game_start", success(2).string("s").bytes());

        assertNull(received.get());
    }

    private static ErrorReason errorFor(int id) {
        AtomicReference<ErrorReason> reason = new AtomicReference<>();
        API.createHandler(ClientboundPingPacket.class, packet -> {
        }).onError(reason::set);
        receive("rewound:ping", new Bytes().raw(0, id).bytes());
        return reason.get();
    }

    @Test
    void errorIds() {
        assertEquals(BuiltinErrorReason.INTERNAL_SERVER_ERROR, errorFor(2));
        assertEquals(BuiltinErrorReason.RATE_LIMITED, errorFor(3));
        assertEquals(BuiltinErrorReason.INVALID_PACKET_VERSION, errorFor(4));
        assertEquals(99, errorFor(99).getId());
    }
}
