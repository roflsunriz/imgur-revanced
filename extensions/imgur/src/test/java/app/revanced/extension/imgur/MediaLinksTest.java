package app.revanced.extension.imgur;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public final class MediaLinksTest {
    private static final String ALBUM = "https://imgur.com/a/post";

    @Test
    public void selectedImageSurvivesRepeatedLightboxRoundTripsAndModeChanges() {
        LegacyImageItem image = new LegacyImageItem("second", "image/png", "https://i.imgur.com/second.png");
        for (int trip = 0; trip < 3; trip++) {
            assertEquals("https://i.imgur.com/second.png", MediaLinks.imageUrl(image));
            // GalleryItemApiModel.getImageItem() calls initFromGalleryItem on a shared ImageItem.
            image.link = ALBUM;
            for (boolean direct : new boolean[]{true, false, true}) {
                assertEquals(direct ? "https://i.imgur.com/second.png" : ALBUM,
                        LinkPolicy.selectShareUrl(ALBUM, MediaLinks.imageUrl(image), direct));
            }
            assertEquals(ALBUM, image.getLink()); // Resolving a copy never mutates the app model.
        }
    }

    @Test
    public void permalinkUsesFirstImageWhileImageSharingUsesTheSelectedImage() {
        LegacyImageItem first = new LegacyImageItem("first", "image/jpeg", "https://i.imgur.com/first.jpg");
        LegacyImageItem second = new LegacyImageItem("second", "image/png", "https://i.imgur.com/second.png");
        Post post = new Post(Arrays.asList(new LegacyImage(first), new LegacyImage(second)));
        assertEquals("https://i.imgur.com/first.jpg", MediaLinks.permalinkImageUrl(new Holder(post)));
        assertEquals("https://i.imgur.com/second.png", MediaLinks.imageUrl(second));
        first.link = ALBUM;
        assertEquals("https://i.imgur.com/first.jpeg", MediaLinks.firstPostImageUrl(post));
        assertEquals(ALBUM, LinkPolicy.selectShareUrl(ALBUM, MediaLinks.firstPostImageUrl(post), false));
    }

    @Test
    public void modernImageAndVideoContentUseTheirMediaUrl() {
        for (Object content : new Object[]{
                new Image(new MediaModel("https://i.imgur.com/image.webp")),
                new Video(new MediaModel("https://i.imgur.com/video.mp4")),
                new LegacyVideo(new LegacyImageItem("clip", "video/webm", "https://i.imgur.com/clip.webm"))}) {
            String result = MediaLinks.firstPostImageUrl(new Post(Collections.singletonList(content)));
            assertEquals(true, LinkPolicy.isDirectImageUrl(result));
        }
    }

    @Test
    public void legacyPermalinkUsesPresenterPostAndFallsBackWhenAbsent() {
        LegacyImageItem image = new LegacyImageItem("first", "image/png", ALBUM);
        LegacyHolder holder = new LegacyHolder(new Presenter(new GalleryItem(Collections.singletonList(image))));
        assertEquals("https://i.imgur.com/first.png", MediaLinks.permalinkImageUrl(holder));
        assertEquals(ALBUM, LinkPolicy.selectShareUrl(ALBUM, MediaLinks.permalinkImageUrl(holder), false));
        assertNull(MediaLinks.permalinkImageUrl(new LegacyHolder(null)));
    }

    @Test
    public void missingLinkCanBeReconstructedFromKnownImageMetadata() {
        assertEquals("https://i.imgur.com/first.png",
                MediaLinks.imageUrl(new LegacyImageItem("first", "image/png", null)));
    }

    @Test
    public void missingUnsupportedAndThrowingModelsRetainStockPermalink() {
        for (Object post : new Object[]{null, new Object(), new Post(Collections.emptyList()),
                new Post(Collections.singletonList(new Object())),
                new Post(Collections.singletonList(new LegacyImage(
                        new LegacyImageItem("unknown", "image/unknown", ALBUM)))), new ThrowingPost()}) {
            assertNull(MediaLinks.firstPostImageUrl(post));
            assertEquals(ALBUM, LinkPolicy.selectShareUrl(ALBUM, MediaLinks.firstPostImageUrl(post), true));
        }
        assertNull(MediaLinks.permalinkImageUrl(new Object()));
        assertNull(MediaLinks.imageUrl(null));
    }

    public static final class LegacyImageItem {
        private final String id;
        private final String type;
        private String link;
        public LegacyImageItem(String id, String type, String link) { this.id = id; this.type = type; this.link = link; }
        public String getId() { return id; }
        public String getType() { return type; }
        public String getLink() { return link; }
    }
    public static final class LegacyImage {
        private final LegacyImageItem image;
        public LegacyImage(LegacyImageItem image) { this.image = image; }
        public LegacyImageItem getLegacyImage() { return image; }
    }
    public static final class LegacyVideo {
        private final LegacyImageItem image;
        public LegacyVideo(LegacyImageItem image) { this.image = image; }
        public LegacyImageItem getLegacyVideo() { return image; }
    }
    public static final class MediaModel {
        private final String url;
        public MediaModel(String url) { this.url = url; }
        public String getId() { return "media"; }
        public String getMimeType() { return "image/webp"; }
        public String getUrl() { return url; }
    }
    public static final class Image {
        private final MediaModel image;
        public Image(MediaModel image) { this.image = image; }
        public MediaModel getImageData() { return image; }
    }
    public static final class Video {
        private final MediaModel image;
        public Video(MediaModel image) { this.image = image; }
        public MediaModel getVideoData() { return image; }
    }
    public static final class Post {
        private final List<?> items;
        public Post(List<?> items) { this.items = items; }
        public List<?> getPostMediaContentItems() { return items; }
    }
    public static final class Holder {
        private final Post boundItem;
        public Holder(Post post) { boundItem = post; }
    }
    public static final class ThrowingPost {
        public List<?> getPostMediaContentItems() { throw new IllegalStateException("model unavailable"); }
    }
    public static final class GalleryItem {
        private final List<LegacyImageItem> images;
        public GalleryItem(List<LegacyImageItem> images) { this.images = images; }
        public List<LegacyImageItem> getImages() { return images; }
    }
    public static final class Presenter {
        private final GalleryItem post;
        public Presenter(GalleryItem post) { this.post = post; }
        public GalleryItem getPost() { return post; }
    }
    public static final class LegacyHolder {
        private final Presenter presenter;
        public LegacyHolder(Presenter presenter) { this.presenter = presenter; }
    }
}
