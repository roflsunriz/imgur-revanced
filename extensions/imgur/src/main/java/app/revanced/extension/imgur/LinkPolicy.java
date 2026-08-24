package app.revanced.extension.imgur;

final class LinkPolicy {
    private LinkPolicy() {
    }

    static String selectShareUrl(String albumUrl, String directUrl, boolean useDirectLinks) {
        if (!useDirectLinks || directUrl == null || directUrl.isEmpty()) {
            return albumUrl;
        }
        return directUrl;
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
