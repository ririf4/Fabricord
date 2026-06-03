### Discord Bot の作り方（かんたんガイド）

もし Discord Bot を作ったことがない場合は、次の手順に沿って作成できます。

1. Discord Developer Portal を開き、右上の **New Application** をクリックします。
2. 好きな名前を付けてアプリケーションを作成します。
3. 左側のメニューから **Bot** タブへ移動します。
4. **Reset Token** を押してトークンを再発行し、その後 **Copy** を押してコピーします。  
   このトークンを Fabricord の `BotToken` に設定します。
5. 少し下にスクロールし、次の設定を行います。
    - **Public Bot** → OFF
    - **Presence Intent** → ON
    - **Message Content Intent** → ON
6. さらに下にある Bot Permissions を設定します。  
   以下の権限を個別に ON にするか、
    - View Channels
    - Manage Webhooks
    - Kick / Ban Members
    - Send Messages
    - Read Message History  
      もしくは、**Administrator** を付与するのが一番確実です  
      （実際に必要な権限は今後増える可能性があるため、Admin のほうが安定します）。
7. 右側のメニューから **Installation** を開きます。
8. **Install Link** をコピーし、ブラウザや Discord に貼り付けて移動し、Bot をサーバーに追加します。

Bot を追加できたら、Fabricord の設定ファイルに BotToken を貼り付けて完了です。
