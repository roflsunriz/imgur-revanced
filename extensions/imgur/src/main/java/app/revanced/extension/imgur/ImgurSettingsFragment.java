package app.revanced.extension.imgur;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.preference.PreferenceFragmentCompat;

public final class ImgurSettingsFragment extends PreferenceFragmentCompat {
    @Override
    @SuppressLint("DiscouragedApi")
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        int screenResource = getResources().getIdentifier(
                "imgur_revanced_preferences",
                "xml",
                requireContext().getPackageName()
        );
        if (screenResource == 0) {
            throw new IllegalStateException("Imgur ReVanced preference screen is missing");
        }
        setPreferencesFromResource(screenResource, rootKey);
    }
}
