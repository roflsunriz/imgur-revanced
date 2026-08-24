# Imgur ReVanced

Imgur Androidアプリ向けのReVancedパッチです。ImgurのAPK自体は配布せず、利用者が用意したstock APKへ適用します。

## 機能

- ProfileのPostsで、初期フィルターをPublicではなくAllにする。
- 下部の広告枠を0dpにし、同梱広告SDKのManifest自動初期化、広告ID取得、初期化・読み込み経路を停止する。
- 一覧で画像を長押しすると、その画像の直リンクをクリップボードへコピーする。
- 詳細画面の「Imgur Copy link」と「テキストの共有」で、アルバムリンクを長押しした画像の直リンクへ置き換える。
- 複数画像ポストでも、選択した画像の直リンクを使用する。
- Imgur内の設定に「Imgur ReVanced」を追加し、直リンク／アルバムリンクを切り替える。初期値は直リンク。
- Discover、Search、Notificationsを個別に非表示にする。初期値はすべて非表示で、残ったタブは均等配置する。
- Discover非表示時はアプリ起動先をProfileのPosts（All）にし、DiscoverのFragment・ViewModelを生成せず起動時通信を遮断する。Discover表示時は従来の起動画面を維持する。

設定はImgurの `Profile > Settings > Imgur ReVanced` にあります。

## ReVanced Managerで使う

ReVanced Manager 2.6.0以降のPatches画面で、次のURLをパッチソースとして追加します。

```text
https://raw.githubusercontent.com/roflsunriz/imgur-revanced/main/patches.json
```

その後、Apps画面の「ストレージから選択」で単体のImgur APKを選び、「Imgur ReVanced」を適用します。APKM、XAPK、APKSなどの分割APKバンドルは使えません。

## 検証済み環境

- ReVanced CLI 6.0.0: Imgur 4.22.1、6.3.12、7.34.0へ適用成功
- ReVanced Manager 2.6.0: Imgur 7.34.0のパッチ済みAPK生成に成功
- Android 16（API 36、arm64）実機: 起動、設定保存、タブ再配置、広告枠、共有リンク切替を確認

詳しい結果と未検証範囲は [実機・互換性検証記録](docs/verification.md) を参照してください。APK更新時の手順は [更新手順](how-to-update.md) にあります。

## ビルド

JDK 17、Android SDK、GitHub Packagesを読み取れるGitHub認証情報が必要です。

```powershell
$env:GITHUB_ACTOR = "<GitHubユーザー名>"
$env:GITHUB_TOKEN = "<read:packages権限を持つトークン>"
.\gradlew.bat clean build :patches:buildAndroid --no-daemon --no-configuration-cache
```

成果物は `patches/build/libs/patches-<version>.rvp` です。リリース成果物にはGitHub Artifact Attestationを付与します。

```text
gh attestation verify patches-0.2.0.rvp --repo roflsunriz/imgur-revanced
```

## 注意

- パッチ適用前のAPKは自分で正規に入手してください。このリポジトリとリリースにはImgurのAPKや逆コンパイル成果物を含めません。
- インストール済みPlay版とは署名が異なるため、通常は上書きできません。必要なデータを保護したうえで利用者自身がインストール方法を判断してください。
- フィード内にImgurのコンテンツAPIから通常ポストとして返るPromoted投稿がある場合、本パッチの「広告SDK通信停止」の対象外です。

## ライセンス

GNU General Public License v3.0です。詳細は [LICENSE](LICENSE) を参照してください。
