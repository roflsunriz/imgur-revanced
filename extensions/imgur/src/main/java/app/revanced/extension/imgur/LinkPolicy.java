package app.revanced.extension.imgur;

import java.net.URI;
import java.net.URISyntaxException;

final class LinkPolicy {
    private LinkPolicy() {
    }

    static String selectShareUrl(String albumUrl, String directUrl, boolean useDirectLinks) {
        if (albumUrl == null || albumUrl.isEmpty()) {
            return directUrl;
        }
        if (!useDirectLinks || directUrl == null || directUrl.isEmpty()) {
            return albumUrl;
        }
        return directUrl;
    }

    static String imageDirectUrl(String imageId, String mimeType, String currentUrl) {
        // Preserve the original extension, video format and query string when available.
        if (isDirectImageUrl(currentUrl)) {
            return currentUrl;
        }
        if (mimeType == null) {
            return currentUrl;
        }
        String extension;
        switch (mimeType) {
            case "image/jpeg": extension = ".jpeg"; break;
            case "image/png": extension = ".png"; break;
            case "image/gif": extension = ".gif"; break;
            case "image/webp": extension = ".webp"; break;
            case "image/bmp": extension = ".bmp"; break;
            case "video/mp4": extension = ".mp4"; break;
            case "video/webm": extension = ".webm"; break;
            default: return currentUrl;
        }
        return firstImageDirectUrl(imageId, extension, currentUrl);
    }

    static boolean isDirectImageUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            URI uri = new URI(url);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && "i.imgur.com".equalsIgnoreCase(uri.getHost())
                    && uri.getPath() != null && uri.getPath().contains(".");
        } catch (URISyntaxException ignored) {
            return false;
        }
    }

    static String firstImageDirectUrl(String imageId, String extension, String fallbackUrl) {
        if (imageId == null || imageId.isEmpty()) {
            return fallbackUrl;
        }
        if (extension == null || extension.isEmpty() || extension.contains("null")) {
            return fallbackUrl;
        }
        String normalizedExtension = extension.startsWith(".") ? extension : "." + extension;
        return "https://i.imgur.com/" + imageId + normalizedExtension;
    }
}
