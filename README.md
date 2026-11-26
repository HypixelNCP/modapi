# Rewound Mod API

A fork of [Hypixel's Mod API](https://github.com/HypixelDev/ModAPI) for the Rewound server. It speaks the same payloads
over plugin messages, but on `rewound:` channels, and adds match start, finish and rejoin events and the Pit's event timers. Hypixel's stock mod doesn't work
on Rewound, because it only listens on Hypixel's channels.

This is the library only. Players need a mod that implements `RewoundModAPIImplementation`: use
[FabricModAPI](https://github.com/HypixelNCP/FabricModAPI) or [ForgeModAPI](https://github.com/HypixelNCP/ForgeModAPI).

## Protocol

Numbers are VarInts. Strings are a VarInt byte length, then UTF-8. Optional fields are a boolean, then the value if it's
true. Longs are eight bytes, big-endian. UUIDs are two longs.

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
| `rewound:game_rejoin` | event | `1` | server name, server type, mode, map |
| `rewound:pit_events` | event | `2` | map id, map name, population, min players, running (boolean); if running: id, name, major (boolean), start, end, description, rules, optional destination, status lines; then count, and each upcoming event's id, name, major (boolean), start, duration seconds, description, rules |

Outcome is `0` win, `1` loss, `2` draw or `3` cancelled. Send each request type at most about once a second.

`rewound:game_rejoin` goes to a player who returns to a running match that held their place, instead of a second
`rewound:game_start`. If they reconnected, it arrives once the client subscribes, unless they've left that match or it has
finished by then.

`rewound:pit_events` is sent only in the Pit: on subscribing, and again whenever the schedule, map population, or the
viewer's live event status changes. Times are epoch milliseconds (longs). The id is the event's catalog id, such as
`king-of-the-hill` or `spire`, and the name is how it's announced, such as `KING OF THE HILL`. Upcoming holds up to 6
projected upcoming events, soonest first, and the named events are the ones that will start in that order. Start times
can move: an event waits a minute at a time for enough players, and while one runs the next ones are timed from its
scheduled end. `ClientboundPitEventsPacket` reads version `2` (the server still serves version `1` for older clients);
`getNext(true)` is the next major.

## Developer Usage

The library is available through [JitPack](https://jitpack.io/#HypixelNCP/modapi). Rank, server type and environment
types come from `net.hypixel:hypixel-data`, which is bundled into the jar.

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.HypixelNCP:modapi:1.0.5'
}
```

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.HypixelNCP</groupId>
    <artifactId>modapi</artifactId>
    <version>1.0.5</version>
</dependency>
```

To build it yourself, run `mvn install` with JDK 8 or newer.

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
