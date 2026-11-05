# Rewound Mod API

A fork of [Hypixel's Mod API](https://github.com/HypixelDev/ModAPI) for the Rewound server. It speaks the same payloads
over plugin messages, but on `rewound:` channels, and adds match start and finish events. Hypixel's stock mod doesn't work
on Rewound, because it only listens on Hypixel's channels.

This is the library only. It needs a mod that implements `RewoundModAPIImplementation` for your loader, as
[ForgeModAPI](https://github.com/HypixelDev/ForgeModAPI) and [FabricModAPI](https://github.com/HypixelDev/FabricModAPI) do
for Hypixel's.

## Protocol

Numbers are VarInts. Strings are a VarInt byte length, then UTF-8. Optional fields are a boolean, then the value if it's
true. UUIDs are two big-endian longs.

Every clientbound message starts with a boolean: `true` means the packet follows, starting with its version, and `false`
means an error id follows (`2` internal, `3` rate limited, `4` wrong version).

| Channel | Direction | Version | Payload |
| --- | --- | --- | --- |
| `rewound:hello` | clientbound, on every join to a server | none | environment |
| `rewound:register` | serverbound | `1` | number of events (max 5), then each event's name and version |
| `rewound:ping` | request | `1` | answers `"pong"` |
| `rewound:party_info` | request | `2` | answers your party's members and roles |
| `rewound:player_info` | request | `1` | answers your rank |
| `rewound:location` | event | `1` | server name, then optional server type, lobby name, mode and map |
| `rewound:game_start` | event | `1` | server name, server type, mode, map |
| `rewound:game_finish` | event | `1` | server name, server type, mode, map, outcome, seconds played |

Outcome is `0` win, `1` loss, `2` draw or `3` cancelled. Send each request type at most about once a second.

## Building

The library isn't published to a repository yet, so install it locally with Maven. Rank, server type and environment types
come from `net.hypixel:hypixel-data`, which resolves from the Hypixel repository and is bundled into the jar.

```sh
mvn install
```

```gradle
repositories {
    mavenLocal()
}

dependencies {
    implementation 'cc.rewound:mod-api:dev-SNAPSHOT'
}
```

## Example Usage

### Sending a packet

```java
public class Example {
    public void sendPacket() {
        RewoundModAPI.getInstance().sendPacket(new ServerboundPartyInfoPacket());
    }
}
```

### Registering a packet handler

```java
public class Example {
    public void registerPacketHandler() {
        RewoundModAPI.getInstance().createHandler(ClientboundPartyInfoPacket.class, packet -> {
            packet.getMembers();
        }).onError(reason -> {
            // Rate limited, for example
        });
    }
}
```

### Subscribing to an event

To receive an event packet, subscribe to it, then register a handler as above.

```java
public class Example {
    public void subscribeToGameFinish() {
        RewoundModAPI api = RewoundModAPI.getInstance();
        api.createHandler(ClientboundGameFinishPacket.class, packet -> {
            packet.getOutcome();
            packet.getSecondsPlayed();
        });
        api.subscribeToEventPacket(ClientboundGameFinishPacket.class);
    }
}
```

Subscribe once per client session, at any time, including in your mod's initialization before the player connects.
Subscriptions don't carry between servers, so the library sends them again on every `ClientboundHelloPacket`.

## License

MIT, as upstream. See [LICENSE](LICENSE).
