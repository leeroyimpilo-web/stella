package za.co.mp3dork;

import org.junit.Test;
import static org.junit.Assert.*;

public class CoreTest {
    @Test public void directQueryHasFiltersAndExactPhrase() {
        assertEquals("\"ocean piano\" filetype:mp3 site:example.org -remix -live", QueryBuilder.build(" ocean  piano ",0,true,"https://example.org/music","remix, live"));
    }
    @Test public void folderQuerySearchesPagesRatherThanFiles() {
        assertEquals("jazz intitle:\"index of\" mp3",QueryBuilder.build("jazz",1,false,"",""));
    }
    @Test public void archiveQueryAvoidsConflictingSites() {
        assertEquals("ambient site:archive.org (mp3 OR audio)",QueryBuilder.build("ambient",3,false,"archive.org",""));
        try { QueryBuilder.build("ambient",3,false,"other.org",""); fail("Conflicting domain accepted"); } catch(IllegalArgumentException expected) {}
    }
    @Test public void rejectsUnsafeSchemesAndCredentials() {
        for(String url:new String[]{"javascript:alert(1)","file:///etc/passwd","content://test/file","https://user:pass@example.org/t.mp3","https://"}) {
            try { MediaTools.webUrl(url);fail("Accepted "+url); } catch(IllegalArgumentException expected) {}
        }
    }
    @Test public void recognisesGoogleRedirectAndSignedMp3Links() {
        String url="https://example.org/artist/track.mp3?token=abc";
        assertEquals(url,MediaTools.unwrapGoogle("https://www.google.com/url?q=https%3A%2F%2Fexample.org%2Fartist%2Ftrack.mp3%3Ftoken%3Dabc&sa=U"));
        assertTrue(MediaTools.isMp3Link(url));
        assertFalse(MediaTools.isMp3Link("https://example.org/track.mp3.html"));
    }
    @Test public void filenameCannotEscapeDownloadFolder() {
        String name=MediaTools.cleanFileName("../../bad\\track\n.mp3");
        assertFalse(name.contains(".."));assertFalse(name.contains("/"));assertFalse(name.contains("\\"));assertFalse(name.contains("\n"));assertTrue(name.endsWith(".mp3"));
        assertEquals("Song + One.mp3",MediaTools.fileName("https://example.org/Song%20+%20One.mp3",null));
        assertEquals("café.mp3",MediaTools.fileName("https://example.org/download","attachment; filename*=UTF-8''caf%C3%A9.mp3"));
    }
    @Test public void rejectsHtmlDisguisedAsAudio() {
        byte[] html="<!doctype html><title>Sign in</title>".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertFalse(MediaTools.hasMp3Signature(html,html.length));
        assertTrue(MediaTools.hasMp3Signature(new byte[]{'I','D','3',4,0,0},6));
        assertTrue(MediaTools.hasMp3Signature(new byte[]{(byte)255,(byte)251,(byte)144,0},4));
    }
}
