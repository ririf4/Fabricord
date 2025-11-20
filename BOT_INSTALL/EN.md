### How to create your Discord bot (easy setup guide)

If you have never created a Discord bot before, follow these simple steps:

1. Open the Discord Developer Portal and click **New Application** in the top right.
2. Give your application any name you like, then create it.
3. In the left sidebar, go to the **Bot** tab.
4. Click **Reset Token**, then click **Copy** to copy your bot token.
   You will paste this token into `BotToken` in the Fabricord config.
5. Scroll down a bit and:
    - Turn **Public Bot** OFF
    - Turn **Presence Intent** ON
    - Turn **Message Content Intent** ON
6. Below that, set the bot permissions.  
   You can manually enable:
    - View Channels
    - Manage Webhooks
    - Kick / Ban Members
    - Send Messages
    - Read Message History  
      or simply grant **Administrator** for the most reliable setup  
      (it is recommended because not all required permissions are fully listed yet).
7. In the right sidebar, go to **Installation**.
8. Copy the **Install Link**, paste it into your browser (or Discord), and run it to add the bot to your server.

Once your bot is added, return to the Fabricord config and insert your bot token.
