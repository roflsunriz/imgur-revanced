package app.revanced.extension.imgur;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class ImgurExtension {
    static final String KEY_DIRECT_LINKS = "direct_links";
    static final String KEY_HIDE_DISCOVER = "hide_discover";
    static final String KEY_HIDE_SEARCH = "hide_search";
    static final String KEY_HIDE_NOTIFICATIONS = "hide_notifications";

    private static volatile Context applicationContext;

    private ImgurExtension() {
    }

    public static void initialize(Context context) {
        if (context != null) {
            applicationContext = context.getApplicationContext();
        }
    }

    static SharedPreferences preferences(Context context) {
        Context safeContext = context == null ? applicationContext : context.getApplicationContext();
        if (safeContext == null) {
            throw new IllegalStateException("Imgur ReVanced has not been initialized");
        }
        return safeContext.getSharedPreferences(
                safeContext.getPackageName() + "_preferences",
                Context.MODE_PRIVATE
        );
    }

    public static String selectShareUrl(String albumUrl, String directUrl) {
        Context context = applicationContext;
        boolean useDirectLinks = context != null && preferences(context).getBoolean(KEY_DIRECT_LINKS, true);
        return LinkPolicy.selectShareUrl(albumUrl, directUrl, useDirectLinks);
    }

    public static void copyFeedImageLink(String directUrl) {
        Context context = applicationContext;
        if (context == null || directUrl == null || directUrl.isEmpty()) {
            return;
        }

        ClipboardManager clipboard =
                (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null) {
            return;
        }

        clipboard.setPrimaryClip(ClipData.newPlainText("Imgur direct link", directUrl));
        Toast.makeText(context, LocalizedStrings.linkCopied(context), Toast.LENGTH_SHORT).show();
    }

    public static void bindProfilePostLongPress(View view, Object postViewModel) {
        if (view == null || postViewModel == null) {
            return;
        }
        try {
            Class<?> modelClass = postViewModel.getClass();
            Method imageIdMethod = modelClass.getMethod("getImageId");
            Method extensionMethod = modelClass.getMethod("getImageExtension");
            Method linkMethod = modelClass.getMethod("getLink");
            String directUrl = LinkPolicy.firstImageDirectUrl(
                    (String) imageIdMethod.invoke(postViewModel),
                    (String) extensionMethod.invoke(postViewModel),
                    (String) linkMethod.invoke(postViewModel)
            );
            view.setOnLongClickListener(ignored -> {
                copyFeedImageLink(directUrl);
                return true;
            });
        } catch (ReflectiveOperationException ignored) {
            // A future Imgur version with a changed model will fail safely without breaking item clicks.
        }
    }

    public static void applyNavigationVisibility(ViewGroup bottomBar) {
        if (bottomBar == null) {
            return;
        }

        Context context = bottomBar.getContext();
        SharedPreferences preferences = preferences(context);
        setHidden(bottomBar, "item_home", preferences.getBoolean(KEY_HIDE_DISCOVER, true));
        setHidden(bottomBar, "item_search", preferences.getBoolean(KEY_HIDE_SEARCH, true));
        setHidden(bottomBar, "item_notifs", preferences.getBoolean(KEY_HIDE_NOTIFICATIONS, true));
    }

    public static boolean isDiscoverHidden() {
        Context context = applicationContext;
        return context == null || preferences(context).getBoolean(KEY_HIDE_DISCOVER, true);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void configureStartupDestination(Object activity) {
        if (!isDiscoverHidden()) {
            return;
        }
        try {
            Class<?> activityClass = activity.getClass();
            Field homeDestination = activityClass.getDeclaredField("homeDestination");
            Class<? extends Enum> destinationClass = Class.forName(
                    "com.imgur.mobile.common.navigation.NavDestination",
                    false,
                    activityClass.getClassLoader()
            ).asSubclass(Enum.class);
            homeDestination.setAccessible(true);
            homeDestination.set(activity, Enum.valueOf(destinationClass, "PROFILE"));
        } catch (ReflectiveOperationException | SecurityException exception) {
            throw new IllegalStateException("Unable to disable Discover startup", exception);
        }
    }

    public static boolean redirectLegacyStartupToProfile(Activity activity) {
        if (activity == null) {
            return false;
        }

        Intent sourceIntent = activity.getIntent();
        Bundle extras = sourceIntent == null ? null : sourceIntent.getExtras();
        if (!StartupPolicy.shouldRedirect(
                isDiscoverHidden(),
                sourceIntent != null && sourceIntent.getData() != null,
                extras != null && !extras.isEmpty()
        )) {
            return false;
        }

        Intent profileIntent = new Intent()
                .setClassName(activity, "com.imgur.mobile.profile.ProfileActivity")
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                .putExtra("com.imgur.mobile.EXTRA_NAV_METHOD", "revanced_startup");
        activity.startActivity(profileIntent);
        activity.finish();
        return true;
    }

    @SuppressLint("DiscouragedApi")
    private static void setHidden(ViewGroup parent, String idName, boolean hidden) {
        // Resource IDs change between Imgur releases, while these entry names have remained stable.
        int id = parent.getResources().getIdentifier(idName, "id", parent.getContext().getPackageName());
        if (id == 0) {
            return;
        }

        View view = parent.findViewById(id);
        if (view != null) {
            view.setVisibility(hidden ? View.GONE : View.VISIBLE);
        }
    }
}
