package app.revanced.extension.imgur;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class LinkPolicyTest {
    @Test
    public void directLinkIsTheDefaultPolicyResult() {
        assertEquals("https://i.imgur.com/image.jpg", LinkPolicy.selectShareUrl(
                "https://imgur.com/a/album",
                "https://i.imgur.com/image.jpg",
                true
        ));
    }

    @Test
    public void albumModeKeepsTheAlbumUrl() {
        assertEquals("https://imgur.com/a/album", LinkPolicy.selectShareUrl(
                "https://imgur.com/a/album",
                "https://i.imgur.com/image.jpg",
                false
        ));
    }

    @Test
    public void missingDirectLinkFallsBackToAlbumUrl() {
        assertEquals("https://imgur.com/a/album", LinkPolicy.selectShareUrl(
                "https://imgur.com/a/album",
                "",
                true
        ));
        assertEquals("https://imgur.com/a/album", LinkPolicy.selectShareUrl(
                "https://imgur.com/a/album",
                null,
                true
        ));
    }

    @Test
    public void firstImageUrlUsesTheImageIdAndMimeExtension() {
        assertEquals("https://i.imgur.com/abc123.webp", LinkPolicy.firstImageDirectUrl(
                "abc123",
                ".webp",
                "https://imgur.com/a/album"
        ));
        assertEquals("https://i.imgur.com/abc123.mp4", LinkPolicy.firstImageDirectUrl(
                "abc123",
                "mp4",
                "https://imgur.com/a/album"
        ));
    }

    @Test
    public void incompleteImageMetadataFallsBackWithoutInventingAnExtension() {
        assertEquals("https://imgur.com/a/album", LinkPolicy.firstImageDirectUrl(
                "abc123",
                null,
                "https://imgur.com/a/album"
        ));
    }

    @Test
    public void lightboxWithoutParentUrlKeepsAUsableLinkInBothModes() {
        for (boolean direct : new boolean[]{true, false}) {
            assertEquals("https://i.imgur.com/image.jpg", LinkPolicy.selectShareUrl(
                    null, "https://i.imgur.com/image.jpg", direct));
            assertEquals("https://i.imgur.com/image.jpg", LinkPolicy.selectShareUrl(
                    "", "https://i.imgur.com/image.jpg", direct));
        }
    }

    @Test
    public void postUrlCannotBeMistakenForSelectedImageUrlAfterNavigation() {
        assertEquals("https://i.imgur.com/selected.png", LinkPolicy.imageDirectUrl(
                "selected", "image/png", "https://imgur.com/gallery/post"));
        assertEquals("https://i.imgur.com/selected.jpeg", LinkPolicy.imageDirectUrl(
                "selected", "image/jpeg", "https://imgur.com/a/album#selected"));
    }

    @Test
    public void originalDirectUrlAndVideoFormatArePreserved() {
        for (String extension : new String[]{"jpg", "jpeg", "png", "gif", "webp", "mp4", "webm"}) {
            String original = "https://i.imgur.com/selected." + extension + "?download=1";
            assertEquals(original, LinkPolicy.imageDirectUrl("selected", "image/jpeg", original));
        }
    }

    @Test
    public void unknownMetadataDoesNotInventAFileFormat() {
        String album = "https://imgur.com/a/album";
        assertEquals(album, LinkPolicy.imageDirectUrl("selected", "image/unknown", album));
        assertEquals(album, LinkPolicy.imageDirectUrl(null, "image/png", album));
        assertEquals(album, LinkPolicy.imageDirectUrl("selected", null, album));
    }
}
