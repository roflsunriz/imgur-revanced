# 更新手順

## 前提

- JDK 17
- Android SDK（compile SDK 34を含む）
- GitHub Packagesを読み取れる認証情報
- 実機検証時はADBとUSBデバッグ
- ReVanced Manager 2.6.0以降

ImgurのAPK、逆コンパイル結果、パッチ済みAPKは `temp/` などのGit管理外一時領域だけで扱い、公開しない。

## 依存関係を更新する

1. ReVanced Patcher、Patches Gradle plugin、CLI、Managerの現行リリースをそれぞれ公式リポジトリで確認する。
2. Patcherを更新する場合は `gradle/libs.versions.toml`、Gradle pluginを更新する場合は `settings.gradle.kts` を変更する。
3. 変更理由、代替候補、影響範囲をCHANGELOGへ記録する。

## ビルドと自動テスト

PowerShellでは認証情報を環境変数へ設定して実行する。値をファイルやログへ保存しない。

```powershell
$env:GITHUB_ACTOR = "<GitHubユーザー名>"
$env:GITHUB_TOKEN = "<read:packages権限を持つトークン>"
.\gradlew.bat clean build :patches:buildAndroid --no-daemon --no-configuration-cache
```

次をすべて確認する。

- Java/Kotlinのコンパイル、lint、単体テストが成功する。
- `.rvp` にpatch classes、metadata、`extensions/imgur.rve` が含まれる。
- ReVanced CLIのpatch一覧に `Imgur ReVanced` だけが公開される。
- 依存関係の脆弱性検査が警告なしで完了する。

## APK互換性を確認する

最新、1世代前、構造が大きく異なる旧版の少なくとも3版へ、現行ReVanced CLIで適用する。現在の基準は4.22.1、6.3.12、7.34.0。

フィンガープリントが見つからない場合、クラス名だけを追加して強引に通さず、呼び出し元、引数、戻り値、対象命令列が同じ責務であることを逆コンパイル結果から確認する。任意対応にしたフックは、該当機能が存在しない版だけで省略されていることを確認する。

## 実機とManagerを確認する

1. 元のPlay版とデータを保護し、必要なら検証専用の別package IDを一時的に使う。検証専用patchを配布RVPへ含めない。
2. 起動直後と主要画面遷移後のFATAL例外を確認する。
3. Discover非表示ONでコールド起動するとPosts（All）が選択され、Spaces/DiscoverのFragment・ViewModel・feed取得が発生しないことを確認する。
4. Discover非表示OFFでは従来のDiscover画面が起動し、設定の両分岐が再起動後も保持されることを確認する。
5. Profile Postsの初期値、一覧・詳細の画像長押し、複数画像、共有文、設定保存、タブの均等配置を確認する。
6. 広告枠が0dpであること、広告ID・各広告SDKの初期化やリクエストが発生しないことをログと通信で確認する。
7. Managerへ `patches.json` のURLを追加し、単体APKを「ストレージから選択」して、署名済み `result.apk` の生成まで完走させる。
8. 検証結果と未確認項目を `docs/verification.md` へ追記する。

## リリースする

1. `gradle.properties`、タグ、`patches.json`、CHANGELOGのバージョンが一致することを確認する。
2. `## [Unreleased]` の内容を `## [x.y.z] - YYYY-MM-DD` へ移す。
3. mainへpushし、Build workflowが成功するまで修正する。
4. `vx.y.z` タグをpushする。Release workflowがCHANGELOGから該当バージョンの節だけをリリース本文へ抽出し、RVP、SHA-256、Manager用JSONを公開してprovenance attestationを生成する。該当節がなければworkflowは失敗する。
5. `gh attestation verify`、release assetのSHA-256、ManagerのURL追加とAPK生成を公開物で再確認する。

workflowは最初に短寿命の `GITHUB_TOKEN` で公式GitHub Packagesを読み取る。Packages側のrepository access制限で401/403になる場合だけ、`read:packages` に限定した専用PATをrepository secret `GPR_KEY` として設定する。通常のCLI用トークンや広い権限のPATを流用しない。

## 後片付け

検証終了後、明示的な対象を確認してから次を削除する。

- 検証専用アプリpackage
- 端末へ置いたstock APK、RVP、Manager用JSON、スクリーンショット、UI dump
- ローカルの逆コンパイル結果、パッチ済みAPK、ログ、クラッシュダンプ、依存ソースclone、ビルドディレクトリ

元のPlay版Imgur、ユーザーデータ、他アプリのファイルは削除しない。

## ロールバック

不具合がある場合は直前のGitタグのRVPへ戻し、同じ元APKへ再適用する。リリースやタグを付け替えず、修正版を新しいバージョンとして公開する。
