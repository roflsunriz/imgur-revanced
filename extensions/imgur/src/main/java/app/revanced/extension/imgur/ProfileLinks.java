package app.revanced.extension.imgur;

import java.util.LinkedHashMap;
import java.util.Map;

/** Retains first-image metadata while Profile reloads reduced post models from its database. */
final class ProfileLinks {
    private static final int CAPACITY = 256;
    private final Map<String, ImageLink> images = new LinkedHashMap<String, ImageLink>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, ImageLink> eldest) {
            return size() > CAPACITY;
        }
    };

    synchronized String firstImageUrl(Object post) {
        String postId = MediaLinks.stringValue(post, "getId");
        String imageId = MediaLinks.stringValue(post, "getImageId");
        String albumUrl = MediaLinks.stringValue(post, "getLink");
        String directUrl = LinkPolicy.firstImageDirectUrl(
                imageId, MediaLinks.stringValue(post, "getImageExtension"), albumUrl);
        if (LinkPolicy.isDirectImageUrl(directUrl)) {
            if (postId != null && !postId.isEmpty()) {
                images.put(postId, new ImageLink(imageId, directUrl));
            }
            return directUrl;
        }
        ImageLink previous = images.get(postId);
        if (previous != null) {
            if (imageId == null || imageId.isEmpty() || imageId.equals(previous.imageId)) {
                return previous.url;
            }
            // An edited post with a different cover must not copy the previous image.
            images.remove(postId);
        }
        return albumUrl;
    }

    private static final class ImageLink {
        final String imageId;
        final String url;

        ImageLink(String imageId, String url) {
            this.imageId = imageId;
            this.url = url;
        }
    }
}
