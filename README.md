# PocketTable

PocketTable is a local multiplayer platform for tabletop games, played over Wi-Fi with no internet connection or backend server required.

One device hosts the game and acts as the game server; other players join as clients using a room code. All game logic and state are managed directly between devices over a local WebSocket connection.

## Tech Stack

- Java 17
- Android (native, Java)
- Java-WebSocket (peer-to-peer WebSocket client/server, no external backend)
- Gson (JSON serialization)
- JUnit (unit testing)
- Git

## How It Works

- The host creates a room, configures game settings (blinds, starting chips), and starts a local WebSocket server on their device.
- Other players discover the room via UDP broadcast on the local network and join using a room code.
- The host device runs the authoritative game engine; all game state updates are broadcast to connected clients in real time.
- No data leaves the local network — the app does not require or use an internet connection to play.

## Games

| Game    | Status      |
|---------|-------------|
| Poker   | ✅ Playable |
| Uno     | Planned     |
| Makao   | Planned     |
| War     | Planned     |
| Blackjack | Planned   |
| Mafia   | Considering |

## Poker — Current Features

- Texas Hold'em rules engine (betting rounds, side pots, hand evaluation)
- Configurable small blind, big blind, and starting chip count
- Player elimination when chips fall below the small blind
- Lobby with live player list and room code / QR-free local discovery
- In-game player list showing chip counts, dealer position, and status (folded / all-in)
- Slider-based bet/raise input with quick pot-sized bet shortcuts

## Installation

1. Download the APK from the [Releases page](https://github.com/ApictoSole34/PocketTable/releases).
2. Transfer it to your Android device.
3. Open the file and install (enable "Install from unknown sources" if prompted).

> [!WARNING]
> **Some devices aggressively restrict background apps**
>
> Xiaomi, Oppo, Realme, Huawei, Vivo, and other brands may kill PocketTable in the background, causing disconnections during gameplay.
>
> **Fix:**
> 1. Go to **Settings → Battery → App battery management**
> 2. Find **PocketTable**
> 3. Select **Don't optimize** or **Allow background activity**
>
> For detailed instructions per device: [Don't Kill My App](https://dontkillmyapp.com/)

## Status

- [x] Poker game engine
- [x] Lobby & local discovery
- [x] Android client/host UI
- [x] Unit tests (game engine)
- [ ] Additional games

## Testing

The core game engine (hand evaluation, betting logic, side pot calculation, blind/elimination rules) is covered by JUnit unit tests, located under `app/src/test/java`. These run on the JVM without requiring an emulator or device:

```bash
./gradlew test
```

## License

This project is licensed under the [PolyForm Noncommercial License 1.0.0](https://polyformproject.org/licenses/noncommercial/1.0.0/).

You are free to use, copy, and modify this code for personal, educational, or other non-commercial purposes. Commercial use is not permitted without explicit permission from the author.