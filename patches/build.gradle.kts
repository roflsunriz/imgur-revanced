group = "io.github.roflsunriz"

patches {
    about {
        name = "Imgur ReVanced"
        description = "Imgur向けのプライバシー・共有・ナビゲーション改善パッチ"
        source = "https://github.com/roflsunriz/imgur-revanced"
        author = "roflsunriz"
        contact = "https://github.com/roflsunriz/imgur-revanced/issues"
        website = "https://github.com/roflsunriz/imgur-revanced"
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    constraints {
        implementation("org.apache.commons:commons-lang3:3.20.0") {
            because("3.18.0より前の再帰的なClassUtils.getClass処理にDoS脆弱性があるため")
        }
    }
    testImplementation(kotlin("test-junit"))
}
