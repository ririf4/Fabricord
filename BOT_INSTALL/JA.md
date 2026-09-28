# Discord Botのセットアップ

このガイドでは、Fabricord専用の非公開Discordアプリケーションを作成し、利用するサーバーへ導入してMinecraftサーバーと接続します。

## 1. アプリケーションを作成する

1. [Discord Developer Portal](https://discord.com/developers/applications)を開きます。
2. **New Application**を選択し、名前を入力して作成します。
3. 左側の**Bot**を開きます。通常、アプリケーションと同時にBotユーザーも作成されます。
4. 他のサーバー管理者にこのBotを導入させる必要がなければ、**Public Bot**を無効にします。

## 2. Gateway Intentsを設定する

**Bot > Privileged Gateway Intents**で、次のように設定します。

- **Message Content Intent: ON（必須）**
  Discordチャットの転送と、リンク済みユーザーによるコンソールコマンドの受信に使用します。
- **Server Members Intent: `opSyncRoleIDs`を使用する場合のみON**
  Discordロールの変更をMinecraftのOP状態へ同期するために使用します。
- **Presence Intent: OFF**
  Fabricordはメンバーのプレゼンス情報を読み取りません。

Message ContentとServer MembersはDiscordの特権Intentです。ポータルで有効にせずBotから要求すると、Gateway接続が拒否される場合があります。詳細はDiscord公式の[Gateway Intentsドキュメント](https://docs.discord.com/developers/events/gateway#privileged-intents)を参照してください。

## 3. インストール設定を行う

Developer Portalで**Installation**を開きます。

1. インストール先として**Guild Install**を有効にします。
2. **Default Install Settings**で次のScopeを追加します。
   - `bot`
   - `applications.commands`
3. Botへ次の権限を付与します。
   - **View Channels**
   - **Send Messages**
   - **Embed Links**
   - **Manage Webhooks**
4. 設定を保存します。
5. Discordが生成した**Install Link**を開き、利用するDiscordサーバーへ追加します。

`applications.commands`はFabricordがスラッシュコマンドを登録するために必要です。Discordでは`bot`による認可にも自動的に含まれますが、ポータル上で両方を選択しておくと用途が明確になります。詳細はDiscord公式の[Application Commands認可ドキュメント](https://docs.discord.com/developers/interactions/application-commands#authorizing-your-application)を参照してください。

### Administratorを推奨しない理由

FabricordはDiscordメンバーをKickまたはBanしません。`/kick`、`/ban`、`/pardon`が操作する対象はMinecraftサーバーです。そのため、Discordの**Kick Members**と**Ban Members**は不要です。無関係なサーバー全体権限まで与えることになるため、特別な理由がない限り**Administrator**も付与しないでください。

**Manage Webhooks**はMinecraftチャットのModernスタイルで使用します。Fabricordは設定されたテキストチャンネルで`Fabricord`というWebhookを作成または再利用します。この権限がない場合、Webhookの準備に失敗したチャンネルでは通常のBotメッセージへフォールバックします。

## 4. Botトークンを設定する

1. **Bot**へ戻ります。
2. トークンが表示されていなければ**Reset Token**を選択します。
3. 表示されたトークンをコピーし、安全な場所で管理します。
4. `<サーバー>/fabricord/config.yml`の`botToken`へ設定します。

Botトークンが漏れると、第三者がBotアカウントを完全に操作できます。Gitへコミットしたり、チャットやログへ貼り付けたりしないでください。漏えいした場合はDeveloper Portalですぐに再発行し、Fabricordの設定も更新してください。

## 5. チャンネルIDを設定する

Discordの**ユーザー設定 > 詳細設定 > 開発者モード**を有効にし、対象チャンネルを右クリックして**チャンネルIDをコピー**を選択します。

コピーしたIDを`fabricord/config.yml`の`logChannels`へ設定します。チャット、進捗、死亡、参加、退出、サーバー起動・停止イベントには個別のチャンネルを指定できます。イベント別の指定がない場合は`Default`が使用されます。

コンソール転送を使用する場合は`consoleLogChannelID`も設定します。このチャンネルからMinecraftサーバーコマンドを実行できるのは、MinecraftのOPアカウントとリンク済みのDiscordユーザーだけです。それでも、チャンネルは非公開にし、投稿できるユーザーを厳しく制限してください。

設定後にMinecraftサーバーを再起動します。FabricordはBot接続時にグローバルスラッシュコマンドを登録するため、Discord上へ表示されるまで少し時間がかかる場合があります。

## トラブルシューティング

- **Botが接続できない:** `botToken`が正しいか、Botが要求する特権Intentをすべて有効にしたか確認します。
- **DiscordのメッセージがMinecraftへ届かない:** **Message Content Intent**と、Chat用チャンネルIDの設定を確認します。
- **Botが通知を送信できない:** 対象チャンネルの**View Channels**、**Send Messages**、**Embed Links**を確認します。
- **Modernチャットが通常メッセージになる:** Chat用チャンネルで**Manage Webhooks**を付与します。
- **ロール変更がOPへ同期されない:** **Server Members Intent**を有効にし、`opSyncRoleIDs`を設定してサーバーを再起動します。
- **スラッシュコマンドが表示されない:** `applications.commands` Scopeを含めて再インストールし、グローバルコマンドが反映されるまで待ちます。
