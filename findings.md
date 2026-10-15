# findings.md

imgur-revanced の実現手段、共通指針の適用条件と調査知見の正本。作業前に全文読む。利用者向けの導入・使用方法はREADME、更新の手順はhow-to-update.md、実施結果と未検証事項はverification.mdを参照する。移した記述の確認日・条件・留保は元のまま保持し、文書移動を再実測と解釈しない。

## 共通指針の適用条件と代替検証

- 対象の共通ルール: 設計と実装方針／検証／ReVanced／ライセンス
- 理由: Imgur APK用のKotlin/Gradleパッチである。JDK/Android SDKとCLI同梱AAPT2を使用し、既存ライセンスはGPL-3.0。
- 適用範囲: パッチの生成・APK再構築・署名を保つ実機更新、リポジトリの利用条件
- 同等の品質を保つ手段・検証: Gradleテスト、RVP内DEX、複数APK世代とManagerを確認する。同一署名・package・versionCodeを照合して許可済みAPKだけ更新し、既存LICENSEとクレジットを保持する。

新たな免除や操作許可を追加するものではない。AGENTS.mdの禁止・承認条件と共通指針は引き続き有効。

<a id="agents-1"></a>

## Environment
<必要に応じて適宜書き足すこと。>

- GitHub Packagesのローカルビルドには `GITHUB_ACTOR` / `GITHUB_TOKEN` に加えて `ORG_GRADLE_PROJECT_githubPackagesUsername` / `ORG_GRADLE_PROJECT_githubPackagesPassword` を同じ値で渡す。トークンをファイルやログへ保存しない。
- CLI 6.0.0のAPK再構築には同梱AAPT2を使う。SDK版AAPT2はstockの `$` 付きdrawable名を拒否する。
- 画像長押し・共有は `ShareUtils$Companion.shareDirectImageLink` が共通経路。旧版の `GalleryDetail2View.shareDirectLink`、新旧Lightbox、現行 `MediaItemsActions` / `MediaViewHolder.getMediaLink` の `ImageItem.getLink` も確認する。
- ポストのCopy Permalinkは現行 `PostViewHolder.copyLink` と旧 `GalleryDetail2View.copyLink` に分かれる。コメントのCopy Permalinkは別のdeep linkなので変更しない。
- `GalleryItemApiModel.getImageItem` は共有されるImageItemを `initFromGalleryItem` で更新する。直リンク復元はモデルを書き換えず、既存CDN URLの形式・queryを保持し、既知MIMEとIDがある場合だけ補完する。
- #4の実測経路はProfileの2列投稿一覧→投稿詳細→一覧へ戻る→一覧項目長押し。投稿詳細内の画像→Lightbox→戻るとは区別する。ProfilePostsViewは再attach時にDBからモデルを再生成する。ProfileLinksは画像形式が欠けた再bindに備え、投稿IDごとに既知の先頭画像URLを最大256件メモリ保持する。別投稿・異なるカバーIDへ流用しない。既知URLがなければstockリンクへfallbackする。
- 既存ImgurがManager署名の場合、PCの一時CLI鍵とは一致しないことがある。秘密鍵を抽出せずManager内でAPKを生成し、公開証明書・package・versionCodeを既存APKと照合する。一致した承認済みAPKだけ `adb install -r` で更新し、認証維持を確認する。

移動元: `AGENTS.md`。

<a id="readme-1"></a>

## ビルド

JDK 17、Android SDK、GitHub Packagesを読み取れるGitHub認証情報が必要です。

```powershell
$env:GITHUB_ACTOR = "<GitHubユーザー名>"
$env:GITHUB_TOKEN = "<read:packages権限を持つトークン>"
$env:ORG_GRADLE_PROJECT_githubPackagesUsername = $env:GITHUB_ACTOR
$env:ORG_GRADLE_PROJECT_githubPackagesPassword = $env:GITHUB_TOKEN
.\gradlew.bat clean build :patches:buildAndroid --no-daemon --no-configuration-cache
```

成果物は `patches/build/libs/patches-<version>.rvp` です。リリース成果物にはGitHub Artifact Attestationを付与します。

```text
gh attestation verify patches-0.2.1.rvp --repo roflsunriz/imgur-revanced
```

移動元: `README.md`。

<a id="readme-2"></a>

## 依存更新の自動処理

Dependabot は対象の依存関係を毎週確認します。patch／minor 更新は PR のチェック（Build）が成功した後に自動で squash merge されます。CI の失敗ジョブは 1 回だけ再実行します。再失敗した PR は残して手動で修正します。major 更新は手動で確認します。

移動元: `README.md`。

<a id="update-1"></a>

## Dependabot PR の更新

前提は `.github/dependabot.yml` と PR 用 CI（Build）です。更新 PR の head SHA と `gh pr checks <PR番号>` の結果を確認してください。patch／minor は全チェック成功後に自動取り込みされます。初回 CI 失敗は failed jobs のみを 1 回再実行し、再失敗した PR は残して手動で修正します。

設定を変えたときは `actionlint .github/workflows/dependabot-automation.yml` と実際の PR の Actions 結果を確認します。問題があれば呼び出し先の共通 workflow SHA を直前の検証済み値へ戻すコミットを push します。取り込まれた依存更新に問題があれば通常の revert コミットで復旧します。

CI 完了より Dependabot の分類が遅れる場合は、`callback_workflow_file` が指す呼び出し側 workflow を `workflow_dispatch` し、同じ PR 番号・head SHA・全チェックを再確認する。呼び出し側のファイル名を変える際はこの入力も一緒に更新する。

移動元: `how-to-update.md`。

<a id="verification-1"></a>

## Dependabot 自動処理（2026-09-23）

`.github/workflows/dependabot-automation.yml` を actionlint で検査し、PR 用 workflow 名（Build）と一致することを確認する。Dependabot の patch／minor かつ全 PR チェック成功の場合だけ取り込み、major・古い SHA・再失敗は残す。

実際の Dependabot PR がまだない場合、動作経路は未検証として扱う。実 PR 発生後に自動化ジョブ、CI の再試行、マージ結果を確認する。

大量の Dependabot PR により CI 完了より分類が遅れる場合でも、分類後の `workflow_dispatch` が現在の PR 番号と head SHA を照合して再評価する。別の作成者、古い SHA、未完了の CI はマージしない。

移動元: `verification.md`。

## 関連文書

- [利用者向けREADME](README.md)
- [更新と復旧](how-to-update.md)
- [検証結果と未検証範囲](verification.md)
- [変更履歴](CHANGELOG.md)
