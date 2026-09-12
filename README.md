# ElyonaRank

Elyona World のランク・称号システムを管理するプラグイン。シーズン制のランキングと、称号の付与・切り替えを提供する。

## 主な機能

- 経験値に応じたランク管理(Bronze〜Legendの5段階)
- シーズン制(開始・終了で全員のランクをリセット)
- 称号の所持・切り替え・付与(管理者)

## コマンド

| コマンド | 説明 | 権限 |
|---|---|---|
| `/rank [player]` | ランク・経験値を確認する | なし |
| `/season <start <name>\|end>` | シーズンを管理する | `elyona.admin` |
| `/title [set <id>\|clear\|grant <player> <id>]` | 称号を管理する | なし(`grant`のみ`elyona.admin`) |

## 依存関係

- Paper 1.21.1
- [LuckPerms](https://luckperms.net/)
- [ElyonaCore](https://github.com/huraru7/ElyonaCore)

## ビルド

```
./gradlew build
```

Java 21 / Paper 1.21.1 (paperweight userdev) を使用。

## 補足

元々ElyonaCoreに含まれていたランク・称号機能をこのプラグインへ分離したもの。
