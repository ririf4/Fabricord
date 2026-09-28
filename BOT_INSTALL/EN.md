# Discord bot setup

This guide creates a private Discord application for Fabricord, installs it in one server, and connects it to the Minecraft server.

## 1. Create the application

1. Open the [Discord Developer Portal](https://discord.com/developers/applications).
2. Select **New Application**, enter a name, and create it.
3. Open **Bot** in the left sidebar. Discord normally creates the bot user with the application.
4. Disable **Public Bot** unless other server owners should be able to install this bot.

## 2. Enable the required Gateway Intents

Under **Bot > Privileged Gateway Intents**, configure:

- **Message Content Intent: ON**
  Fabricord needs message content to relay Discord chat and execute linked console commands.
- **Server Members Intent: ON only when `opSyncRoleIDs` is configured**
  Fabricord uses member role updates for OP synchronization.
- **Presence Intent: OFF**
  Fabricord does not read member presence information.

Discord treats Message Content and Server Members as privileged intents. An application that requests one without enabling it in the portal can have its Gateway connection rejected. See Discord's [Gateway Intents documentation](https://docs.discord.com/developers/events/gateway#privileged-intents).

## 3. Configure installation

Open **Installation** in the Developer Portal.

1. Enable the **Guild Install** installation context.
2. Under **Default Install Settings**, add these scopes:
   - `bot`
   - `applications.commands`
3. Grant the bot these permissions:
   - **View Channels**
   - **Send Messages**
   - **Embed Links**
   - **Manage Webhooks**
4. Save the changes.
5. Use the Discord-provided **Install Link** to add the app to the target server.

The `applications.commands` scope allows Fabricord to register its slash commands. Discord also includes this scope automatically with `bot` authorization, but selecting both in the portal makes the intended installation explicit. See Discord's [application command authorization documentation](https://docs.discord.com/developers/interactions/application-commands#authorizing-your-application).

### Why Administrator is not required

Fabricord does not moderate Discord members. Its `/kick`, `/ban`, and `/pardon` commands act on the Minecraft server, so the Discord permissions **Kick Members** and **Ban Members** are not needed. Avoid granting **Administrator** unless you deliberately want the bot to have unrelated server-wide permissions.

`Manage Webhooks` is required for the modern Minecraft chat style. Fabricord creates or reuses a webhook named `Fabricord` in configured text channels. If you do not grant it, Fabricord falls back to ordinary bot messages when webhook setup fails.

## 4. Copy the bot token

1. Return to **Bot**.
2. Select **Reset Token** if Discord is not currently showing a token.
3. Copy the token and store it securely.
4. Set it as `botToken` in `<server>/fabricord/config.yml`.

The bot token provides complete access to the bot account. Never commit it, post it in chat, or include it in logs. If it is exposed, reset it immediately in the Developer Portal and update the Fabricord configuration.

## 5. Configure Discord channels

Enable **Developer Mode** in Discord under **User Settings > Advanced**, then right-click a channel and select **Copy Channel ID**.

Add the IDs to `logChannels` in `fabricord/config.yml`. Fabricord supports separate channels for chat, advancements, deaths, joins, leaves, and server start/stop events. A `Default` entry is used when an event-specific channel is not configured.

If console relay is needed, set `consoleLogChannelID`. Messages posted in that channel can execute Minecraft server commands only for Discord users linked to a Minecraft operator account. Keep this channel private and restrict who can post in it.

Restart the Minecraft server after editing the configuration. Fabricord registers its global slash commands when the bot connects; Discord may take a short time to display newly registered global commands.

## Troubleshooting

- **The bot cannot connect:** verify `botToken` and confirm every requested privileged intent is enabled.
- **Discord messages do not reach Minecraft:** enable **Message Content Intent** and confirm the channel ID is listed under the Chat channel configuration.
- **The bot cannot send notifications:** check **View Channels**, **Send Messages**, and **Embed Links** in the target channel.
- **Modern chat uses ordinary bot messages:** grant **Manage Webhooks** in the configured chat channel.
- **OP synchronization does not react to role changes:** enable **Server Members Intent**, configure `opSyncRoleIDs`, and restart the server.
- **Slash commands are missing:** reinstall the app with the `applications.commands` scope and allow time for global command propagation.
