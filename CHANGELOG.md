# 変更履歴

このプロジェクトは [Keep a Changelog](https://keepachangelog.com/ja/1.1.0/) に従います。

## [Unreleased]

## [0.1.0] - 2026-08-24

### Added

- Imgurアプリ内でリンク形式と下部タブ表示を変更できるように、主要11言語へ対応した「Imgur ReVanced」設定画面を追加した。
- 一覧と詳細の画像長押しで対象画像を正しく扱えるように、直リンクのクリップボードコピーと共有文のリンク置換を追加した。
- Profile Postsを公開範囲にかかわらず確認しやすくするため、初期フィルターをAllへ変更した。
- Imgur 4.22.1、6.3.12、7.34.0で適用できることを確認するため、バージョン差を許容するフィンガープリントと任意フックを追加した。
- Managerから継続更新できるように、固定URLのパッチソースJSONとタグ駆動のリリースworkflowを追加した。

### Changed

- Discover、Search、Notificationsを初期状態で非表示にし、残った下部タブを空白なしで均等配置するようにした。
- コピーとテキスト共有の初期値をアルバムリンクから選択画像の直リンクへ変更した。

### Security

- 下部広告枠と広告通信を抑止するため、広告用Manifest componentと権限の除去、広告ID取得・SDK初期化・読み込み処理の停止を追加した。
- ビルド時の既知DoS脆弱性を避けるため、Patcherの推移依存Commons Langを修正版3.20.0へ固定した。
- 配布RVPの由来と改ざんを検証できるように、release workflowへSHA-256とGitHub Artifact Attestationを追加した。

[Unreleased]: https://github.com/roflsunriz/imgur-revanced/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/roflsunriz/imgur-revanced/releases/tag/v0.1.0
