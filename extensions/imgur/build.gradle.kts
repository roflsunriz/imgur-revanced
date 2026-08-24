extension {
    name = "extensions/imgur.rve"
}

dependencies {
    // Imgur本体に同梱されているPreference APIを型検査にだけ利用する。
    compileOnly("androidx.preference:preference:1.2.1")
    testImplementation("junit:junit:4.13.2")
}

android {
    defaultConfig {
        minSdk = 21
    }
}
