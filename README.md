# Fabricord

Fabricord is a server-side Fabric mod that connects Minecraft server chat and events to Discord.
It is intended as a Fabric alternative to DiscordSRV and does not need to be installed on clients.

> Fabricord is not affiliated with the mod of the same name published on CurseForge.

## Features

- Bidirectional chat between Minecraft and Discord
- Join, leave, death, and advancement notifications
- Minecraft and Discord account linking, lookup, and unlinking
- Discord mentions associated with linked accounts
- Discord slash commands for server information and administration
- Per-command Discord user and role permissions
- Discord role to Minecraft operator synchronization
- Optional Minecraft console relay
- Configurable message templates with server and event placeholders
- English, Japanese, German, and French messages

## Supported versions

The repository currently contains compatibility modules for:

- 26.1-26.1.2
- 1.21.9-1.21.11, 1.21.6-1.21.8, and 1.20.5-1.21.5
- 1.20.3-1.20.4, 1.20.2, and 1.20-1.20.1
- 1.19.3-1.19.4, 1.19.1-1.19.2, and 1.19
- 1.18-1.18.2
- 1.17-1.17.1
- 1.16.4-1.16.5
- 1.16.1-1.16.3
- 1.16
- 1.15-1.15.2
- 1.14.4, 1.14.3, and 1.14-1.14.2

Modules are organized around Minecraft API compatibility boundaries. Each module combines its version-specific sources with the shared sources in `fabricord/common`.

Fabricord bundles SQLite native libraries for 64-bit Windows, Linux (glibc and
musl), and macOS on x86_64 and ARM64. Other operating systems and architectures
are not supported.

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

Release JARs are collected in `dist` and named `Fabricord-<version>.jar`.

Public versions use `YYYY.Minor.Patch`. The year is taken from the release
commit, while each compatibility module's `Minor.Patch` value is configured as
`version.<Minecraft version range>` in `version.properties`. The public version
is written to `fabric.mod.json`. Release JAR filenames use the full
GenCal-SemVer deep version, including the product generation, Git revision,
and supported Minecraft range. Modrinth version numbers use the public version,
while the uploaded filename retains the complete deep version.

## Publishing

Provide a Modrinth personal access token with the `VERSION_CREATE` scope using
the `MODRINTH_TOKEN` environment variable or the user Gradle properties file:

```properties
modrinthToken=mrp_your_token
```

The user Gradle properties file is `~/.gradle/gradle.properties` and must not be
committed to the repository. Then publish every compatibility module:

```shell
./gradlew publishModrinth
```

All compatibility modules are built and tested first. Their Modrinth versions
are then published serially, starting with the oldest supported Minecraft
version.

Publish a single compatibility module with its `modrinth` task, for example:

```shell
./gradlew :1.20.3-1.20.4:modrinth
```

Every Modrinth version links to the GitHub Releases page for its changelog.

## Contributing

Bug reports and feature requests can be submitted through [GitHub Issues](https://github.com/K-Lqrs/Fabricord/issues). Pull requests are also accepted.

## License

Fabricord is licensed under the [Apache License 2.0](LICENSE).

## Status

![CodeFactor](https://www.codefactor.io/repository/github/ririf4/fabricord/badge)
![GitHub License](https://img.shields.io/github/license/ririf4/Fabricord?style=flat)
![GitHub commit activity](https://img.shields.io/github/commit-activity/t/ririf4/Fabricord?style=flat)
