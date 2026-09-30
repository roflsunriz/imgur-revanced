# 実機・互換性検証記録

## 0.2.1（2026-09-30）

### 既存アプリでの再現

- 更新前のSHARP SH-R80P / Android 16の既存Imgur 7.34.0.0で再現調査した。この段階では更新・アンインストール・データ消去・ログアウトを行っていない。
- #4は直リンク設定ONで再現した。Profileの2列投稿一覧の項目長押しでは画像直リンク、同じ項目を開いて投稿詳細から一覧へ戻った後の項目長押しではアルバムリンクになった。各段階のUI dumpでProfile一覧と投稿詳細を区別し、コピー結果はFirefoxのアドレス欄へ貼り付けて確認した。URLは送信していない。
- 最初に行った投稿詳細内の画像→Lightbox→戻る→画像長押しの2投稿では#4を再現しなかった。この操作は報告された一覧の往復経路とは異なる。
- #5は直リンク設定ONで、ポストのCopy PermalinkがアルバムURLをコピーすることを同じ貼り付け方法で確認した。設定OFFもアルバムURLだった。
- 既存アプリの投稿詳細内の画像長押しは、設定OFFでLightbox往復前後ともアルバムURLだった。検証前の直リンク設定ONへ戻した。

### 修正と自動検証

- ProfilePostsViewの再attachはDBの投稿モデルを再生成し、Profileの長押しリスナーを再bindする。画像形式が欠けたモデルの再bindを想定し、取得済み先頭画像URLを投稿IDごとに最大256件メモリ保持する。異なる投稿や変更されたカバーIDへ流用せず、設定はコピー時に読む。
- Copy Permalinkは新旧ポストViewの既存URL生成・Clipboard helperを維持して共通ポリシーへ接続した。ONは先頭画像、OFFは元のポストURL。コメントのPermalinkは変更していない。
- 画像長押し・共有は選択画像を使い、既存CDN URLの形式・queryを保持する。ポストURLに置換されたImageItemは既知MIMEとIDで直リンクを復元し、親ポストURLがない場合は元のコピーURLを保持する。
- JDK 17で `clean build :patches:buildAndroid --no-daemon --no-configuration-cache` が成功した。unit test計29件（LinkPolicy 9、MediaLinks 6、ProfileLinks 3、StartupPolicy 4、LinkHooks 4、XmlTransforms 3）が失敗なし、Android lintは `No issues found.`。
- 回帰テストはProfile一覧の同じ投稿IDで完全モデル→画像形式欠落モデルの再bindを3回繰り返し、ON/OFF、行再利用、変更されたカバー、キャッシュ上限を確認する。画像詳細の別経路、選択した2枚目と先頭画像の区別、旧新モデル、null/未知モデル、DEXのレジスターと既存コピー処理の保持も検証した。
- 最終RVPをCLI 6.0.0で4.22.1、6.3.12、7.34.0へ適用し、DEX・resources再構築、整列、署名まで成功した。SDK版AAPT2の `$` 付きdrawable名エラーはCLI同梱版を使って解消した。
- 生成DEXで、3版とも一覧長押し・Profile再bind・共有・Copy Permalinkがextensionへ接続され、既存のPermalink生成・Clipboard helperが残ることを検証した。6.3.12のMediaViewHolder、7.34.0のMediaViewHolder/MediaItemsActions、存在する旧詳細とLightboxの画像URL読み取りも復元処理を通る。コメントのPermalinkにフックが入っていないことも確認した。
- ローカル0.2.1 RVPのSHA-256は `92cd053704d6a4b5b21f60bc47854786b0938b03492b90484a10f68a445c19c3`。release runnerの公開RVPは生成時刻等でhashが異なり得るため、公開物は同梱SHA256SUMSとattestationで別途検証する。

### 修正版の実機検証

- PCの8月24日の一時CLI署名鍵は所在不明で、当時の検証APK証明書も既存Imgurとは異なった。端末のManager内で修正版APKを生成し、秘密鍵を抽出せず署名した。
- Manager生成物と既存APKの証明書SHA-256はともに `cddbc1c44abb5b0bc4efab2b9e82291758db40409082c7dda55cd1d771459ab0`。package `com.imgur.mobile` とversionCode `73400` の一致も確認し、承認された `adb install -r` で更新した。firstInstallTimeは変更されず、ログイン済みProfileの投稿を保持した。認証情報の読み取り・変更、アンインストール、データ消去、ログアウトは行っていない。
- 実測に使ったRVPは版数変更前のビルドだが、パッチの適用ソースは最終0.2.1と同じで、同梱 `extensions/imgur.rve` のbyte列も一致する。Manager生成APKでも今回の各DEXフックを検証した。

| 操作 | 直リンクON | アルバムリンクOFF |
| --- | --- | --- |
| Profile一覧の項目長押し | 先頭画像直リンク | アルバムURL |
| 一覧→投稿詳細→一覧→項目長押し | 3往復とも同じ画像URL | 往復前後で同じアルバムURL |
| ポストのCopy Permalink | 一覧の先頭画像と同じ直リンク | アルバムURL |
| 投稿詳細のImgur Copy link | 画像直リンクをコピー | アルバムURLをコピー |
| 投稿詳細→Lightbox→戻る→画像長押し | 直リンクの共有文 | アルバムURLの共有文 |

- Clipboardの結果はFirefoxのアドレス欄へ貼り付けて確認し、URLは送信していない。LightboxActivityへ入ったこととProfile一覧へ戻ったこともUI/Activityで確認した。リンク設定は検証前のONへ復元した。

### 未検証範囲

- Profileの既知URLはプロセス内の限定キャッシュである。最初から画像メタデータが欠ける投稿、プロセス再起動・キャッシュ退避後にメタデータがない投稿はstockリンクへfallbackする。DBの内容・認証データは取得していないため、実機で欠けたフィールドそのものは未測定。
- 選択した2枚目、異なる画像形式・動画形式の網羅はhostの回帰テストとDEX検証で確認した。すべての形式・複数画像での実機操作までは網羅していない。

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
