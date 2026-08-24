package app.revanced.extension.imgur;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class StartupPolicyTest {
    @Test
    public void redirectsPlainLaunchWhenDiscoverIsHidden() {
        assertTrue(StartupPolicy.shouldRedirect(true, false, false));
    }

    @Test
    public void keepsDiscoverLaunchWhenDiscoverIsVisible() {
        assertFalse(StartupPolicy.shouldRedirect(false, false, false));
    }

    @Test
    public void preservesDeepLinks() {
        assertFalse(StartupPolicy.shouldRedirect(true, true, false));
    }

    @Test
    public void preservesNotificationAndShortcutDestinations() {
        assertFalse(StartupPolicy.shouldRedirect(true, false, true));
    }
}
