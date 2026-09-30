package app.revanced.extension.imgur;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class ProfileLinksTest {
    @Test
    public void profileDetailReturnKeepsFirstImageAfterDatabaseReloadDropsItsFormat() {
        ProfileLinks links = new ProfileLinks();
        String album = "https://imgur.com/a/post";
        assertEquals("https://i.imgur.com/first.png", links.firstImageUrl(new Post("post", "first", "png", album)));
        for (int trip = 0; trip < 3; trip++) {
            String direct = links.firstImageUrl(new Post("post", "first", null, album));
            assertEquals("https://i.imgur.com/first.png", LinkPolicy.selectShareUrl(album, direct, true));
            assertEquals(album, LinkPolicy.selectShareUrl(album, direct, false));
        }
        assertEquals("https://i.imgur.com/first.png", links.firstImageUrl(new Post("post", null, null, album)));
    }

    @Test
    public void recycledRowsAndChangedCoversNeverCopyAnotherPostsCachedImage() {
        ProfileLinks links = new ProfileLinks();
        links.firstImageUrl(new Post("a", "first", "jpeg", "https://imgur.com/a/a"));
        links.firstImageUrl(new Post("b", "second", "png", "https://imgur.com/a/b"));
        assertEquals("https://i.imgur.com/first.jpeg", links.firstImageUrl(new Post("a", "first", "", "https://imgur.com/a/a")));
        assertEquals("https://imgur.com/a/c", links.firstImageUrl(new Post("c", null, null, "https://imgur.com/a/c")));
        assertEquals("https://imgur.com/a/a", links.firstImageUrl(new Post("a", "new-cover", null, "https://imgur.com/a/a")));
        assertEquals("https://imgur.com/a/a", links.firstImageUrl(new Post("a", "first", null, "https://imgur.com/a/a")));
    }

    @Test
    public void cacheIsBoundedAndIncompleteModelsKeepTheirStockLink() {
        ProfileLinks links = new ProfileLinks();
        for (int index = 0; index < 257; index++) {
            links.firstImageUrl(new Post("post" + index, "image" + index, "png", "https://imgur.com/a/" + index));
        }
        assertEquals("https://imgur.com/a/0", links.firstImageUrl(new Post("post0", "image0", null, "https://imgur.com/a/0")));
        assertEquals("https://i.imgur.com/image256.png", links.firstImageUrl(new Post("post256", "image256", null, "https://imgur.com/a/256")));
        assertEquals("https://imgur.com/a/unknown", links.firstImageUrl(new Post(null, null, null, "https://imgur.com/a/unknown")));
    }

    public static final class Post {
        private final String id;
        private final String imageId;
        private final String extension;
        private final String link;

        Post(String id, String imageId, String extension, String link) {
            this.id = id;
            this.imageId = imageId;
            this.extension = extension;
            this.link = link;
        }

        public String getId() { return id; }
        public String getImageId() { return imageId; }
        public String getImageExtension() { return extension; }
        public String getLink() { return link; }
    }
}
