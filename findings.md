# 調査知見

- GitHub Packages認証は `GITHUB_ACTOR/TOKEN` と `ORG_GRADLE_PROJECT_githubPackagesUsername/Password` の両組へ同じ値を渡す。値をファイルやログへ残さない。
- APK再構築はReVanced CLI同梱AAPT2を使う。SDK版はstockの `$` 付きdrawable名を拒否する。
- 画像長押し・共有の共通経路は `ShareUtils$Companion.shareDirectImageLink`。新旧Lightbox、`MediaItemsActions`、`MediaViewHolder.getMediaLink` も確認する。投稿PermalinkとコメントPermalinkは別経路で、コメントは変更しない。
- `GalleryItemApiModel.getImageItem` は共有 `ImageItem` を再利用する。直リンク復元でモデルを書換えず、既存CDN URLのqueryを保ち、既知MIMEとIDがある場合だけ補完する。
- Profile一覧は戻り時にDBからモデルを再生成する。形式欠落へ備えた先頭画像URLキャッシュは投稿ID単位・最大256件とし、別投稿や異なるcover IDへ流用しない。未知ならstockリンクへ戻す。
- 既存APKがManager署名ならPCの一時CLI鍵と一致しない。秘密鍵を抽出せずManagerで生成し、公開証明書・package・versionCodeが既存APKと一致する承認済みAPKだけ更新する。
