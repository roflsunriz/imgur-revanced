package app.revanced.extension.imgur;

import android.content.Context;
final class LocalizedStrings {
    private LocalizedStrings() {
    }

    @SuppressWarnings("DiscouragedApi")
    static String linkCopied(Context context) {
        int resource = context.getResources().getIdentifier(
                "imgur_revanced_link_copied",
                "string",
                context.getPackageName()
        );
        return resource == 0 ? "Direct link copied" : context.getString(resource);
    }
}
