# Fabricord

Fabricord is a server-side Fabric mod that connects Minecraft server chat and events to Discord.
It is intended as a Fabric alternative to DiscordSRV and does not need to be installed on clients.

> Fabricord is not affiliated with the mod of the same name published on CurseForge.

## Features

- Bidirectional chat between Minecraft and Discord
- Join, leave, death, and advancement notifications
- Minecraft and Discord account linking
- Discord mentions associated with linked accounts
- Discord slash commands for server information and administration
- Discord role to Minecraft operator synchronization
- Optional Minecraft console relay
- English, Japanese, German, and French messages

## Supported versions

The repository currently contains compatibility modules for:

- 26.1.2
- 1.21.11, 1.21.8, and 1.21.5
- 1.20.4, 1.20.2, and 1.20.1
- 1.19.4, 1.19.2, and 1.19
- 1.18.2
- 1.17.1
- 1.16.5
- 1.16.3
- 1.16
- 1.15.2
- 1.14.4, 1.14.3, and 1.14.2

Modules are organized around Minecraft API compatibility boundaries. Each module combines its version-specific sources with the shared sources in `fabricord/common`.

## Installation

1. Install Fabric Loader and Fabric API for the target Minecraft version.
2. Place the matching Fabricord JAR in the server's `mods` directory.
3. Start the server once to generate `fabricord/config.yml`.
4. Configure `botToken`, `logChannels`, and any optional integrations.
5. Restart the server.

Discord application setup:

- [English guide](BOT_INSTALL/EN.md)
- [Japanese guide](BOT_INSTALL/JA.md)

## Building

Build and test every compatibility module with:

```shell
./gradlew build
```

Generated JARs are written to `fabricord/versions/<version>/build/libs`.

## Contributing

Bug reports and feature requests can be submitted through [GitHub Issues](https://github.com/K-Lqrs/Fabricord/issues). Pull requests are also accepted.

## License

Fabricord is licensed under the [Apache License 2.0](LICENSE).

## Status

![CodeFactor](https://www.codefactor.io/repository/github/ririf4/fabricord/badge)
![GitHub License](https://img.shields.io/github/license/ririf4/Fabricord?style=flat)
![GitHub commit activity](https://img.shields.io/github/commit-activity/t/ririf4/Fabricord?style=flat)
