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
}
