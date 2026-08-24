package app.revanced.extension.imgur;

final class StartupPolicy {
    private StartupPolicy() {
    }

    static boolean shouldRedirect(boolean hideDiscover, boolean hasData, boolean hasExtras) {
        return hideDiscover && !hasData && !hasExtras;
    }
}
