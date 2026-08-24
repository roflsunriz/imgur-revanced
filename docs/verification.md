# 実機・互換性検証記録

## 0.2.0（2026-08-24）

- 7.34.0の通常起動が `MainActivity` から `GridAndFeedNavActivity` のhome destination `SPACES` へ進み、Spaces生成後にDiscover feedを取得することを逆コンパイル結果で確認した。
- Discover非表示ONではNavControllerの初期化を維持したままhome destinationを `PROFILE` へ変更し、Postsを先頭タブ、既存のPostFilter patchでAllを初期値にした。
- Discover非表示OFF、data付きdeep link、extra付き通知・shortcutでは従来経路を維持することをStartupPolicy unit testで確認した。
- CLI 6.0.0で4.22.1、6.3.12、7.34.0へ適用し、旧 `GridAndFeedActivity` と新 `GridAndFeedNavActivity` の両起動経路でDEX・resourcesの再構築、整列、署名が成功した。
- 生成DEXで、7.34.0と6.3.12は設定ON時だけhome destinationが `PROFILE`、4.22.1の旧経路は `super.onCreate` 後にProfileへ転送され、設定OFFでは元のonCreateへ進むことを確認した。
- Android 16 / API 36のheadless emulatorで、設定ONのコールド起動時にPostsが選択され、設定OFFではMost Viral、User Sub、Featured、Arcade、For Youを含むDiscover画面が表示された。両分岐でFATAL例外はなかった。
- Android 16 / API 36のUSB実機でも、元のPlay版を残した検証専用packageで同じON/OFF挙動と設定保持を確認した。ONの起動ログにはSpacesDestinationFragment、SpacesViewModel、Most Viral、User Sub、FATAL例外の痕跡がなかった。
- 設定ONではnavigation graphがProfileを直接生成するため、Discover通信を行うSpacesDestinationFragment、SpacesViewModel、ContentAreaManagerは起動時に生成されない。アプリ全体のFirebase、認証、Profile等の通信は本要件の遮断対象外。
- 公開RVPのSHA-256 `6dd9a607eff1a624a7bf2f0630886f1bd58ada5f26843ab4cf2f8a8c648f0a64` がrelease metadataと `SHA256SUMS` に一致した。
- `gh attestation verify` で、公開RVPが `refs/tags/v0.2.0` のrelease workflowとGitHub-hosted runnerから生成されたことを確認した。
- 実機Managerの固定URLを `v0.2.0 / 1個のパッチ` として再取得し、公開RVPで7.34.0の16 DEXとresourcesの再構築、APK整列、`result.apk` 保存まで完走した。

## 0.1.1（2026-08-24）

- 0.1.0の公開URLをManagerへ追加したところ、`created_at` 末尾の `Z` をLocalDateTimeとして解析できず、パッチを取得できないことを実機ログで確認した。
- `created_at` をoffsetなし形式へ修正し、タグ、RVP名、固定URLを0.1.1へ揃えて修正版を公開した。
- 固定URLをManager 2.6.0へ追加し、「Imgur ReVanced v0.1.1 / 1個のパッチ」として自動ダウンロードされたことを確認した。
- 固定URLから取得した公開RVPで7.34.0を処理し、16 DEXとresourcesの再構築、APK整列、`result.apk` 保存まで完走した。
- 公開RVPのSHA-256 `01223e5feb892702a21ad8a9c92b79ee1d511af4e8d648d45b2e61e1fa1a7fd3` がrelease metadataと `SHA256SUMS` に一致した。
- `gh attestation verify` で、公開RVPが `refs/tags/v0.1.1` のrelease workflowとGitHub-hosted runnerから生成されたことを確認した。
- ローカルRVPとrelease runnerのRVPを展開比較すると、class、DEX、RVEは同一で、公式Gradle pluginが生成時刻を格納するManifestの `Timestamp` だけが異なった。公開RVPそのものは前項のManager適用で機能確認した。

## 0.1.0（2026-08-24）

### 検証環境

- ReVanced Patcher 22.0.1
- ReVanced CLI 6.0.0
- ReVanced Manager 2.6.0
- Android 16 / API 36 / arm64のUSB接続実機
- Imgur 4.22.1、6.3.12、7.34.0の単体APK

### 自動・静的検証

- Gradleのunit test、lint、RVE/RVP buildが成功した。
- LinkPolicyについて、直リンク選択、アルバムリンク選択、null/空値fallback、画像IDと拡張子からのURL生成をunit testで確認した。
- Manifest変換について、広告componentと広告ID権限の除去、他componentの保持、Facebook追跡metadataの無効化、広告layout高さの0dp化をunit testで確認した。
- CLI 6.0.0で4.22.1、6.3.12、7.34.0へ同じRVPを適用し、いずれも警告・エラーなしでpatched APKを生成した。
- 7.34.0の生成物で、Application初期化、PostsのAll初期値、一覧長押し、Profile Posts長押し、共有URL、下部タブ、広告停止の各注入箇所と追加resourcesを逆コンパイル結果で確認した。

### 実機検証

- 検証専用package IDでPlay版Imgurを残したまま7.34.0を導入し、起動と画面遷移でFATAL例外がないことを確認した。
- 下部ナビゲーションは初期状態でCreate/Profileの2項目が各630pxになった。Searchを表示へ変更するとSearch/Create/Profileの3項目が各420pxになり、再起動後も設定が保持された。
- SettingsのSign out直前にImgur ReVancedが表示され、日本語の4スイッチを操作できた。
- 直リンク設定ONで、詳細画面の共有文がタイトルと `https://i.imgur.com/ajwALwB.jpeg` になった。
- 直リンク設定OFFで、同じ共有文がタイトルと `https://imgur.com/gallery/kenya-believe-UoIrI8c` に戻った。
- 下部広告枠が表示されず、実行中processのログにGoogle Mobile Ads、AppLovin、SafeDK、Facebook Audience Network、MediaLab、Moloco、MobileFuse、MBridge、comScore、AdvertisingIdClientの初期化・通信痕跡がないことを確認した。
- ManagerへローカルRVPを追加すると「Imgur ReVanced 0.1.0 / 1個のパッチ」として認識された。7.34.0の単体APKをストレージから選択し、16 DEXとresourcesの再構築、APK整列、署名、`result.apk` 保存まで完走した。

### 検証中に見つけて修正した問題

- AdsModule constructorをsuper constructorより前でreturnしていたためAndroidの検証エラーになった。`Object.<init>` の直後で停止するよう修正した。
- StickyAdViewを通常Viewへ置換するとViewBindingのcastが壊れたため、型は保持してMediaLabの初期化・読み込み処理をno-op化した。
- 旧版のPreference APIに存在しない動的screen生成を使っていたため、XML resourceを読み込む方式へ変更した。
- `copyImageUrl` が画像直リンクではなくgallery URLだったため、実機の引数実測に基づき `downloadImageUrl` を選択画像の直リンクとして使うよう修正した。

### 未検証範囲と残るリスク

- 検証専用packageはログアウト状態だったため、ログイン必須のProfile Posts一覧で長押しコピーを最後まで操作できていない。7.34.0の対象bind methodへの注入、URL生成unit test、生成DEXは確認済み。
- 複数画像ポストは共有処理が選択画像の `downloadImageUrl` を使うことを生成DEXで確認したが、異なる拡張子を含む全形式の実機操作までは網羅していない。
- ImgurのコンテンツAPIが通常ポストとして返すPromoted投稿は広告SDK経路ではないため、フィード内に残る場合がある。
- 「バージョン非依存」は将来版を無条件に保証する意味ではない。4.22.1から7.34.0までの構造差を許容することを確認しており、Imgur更新時は本記録の手順で再検証する。
