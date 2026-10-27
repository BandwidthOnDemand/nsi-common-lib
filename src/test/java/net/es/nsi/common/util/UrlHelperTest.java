package net.es.nsi.common.util;

import java.net.MalformedURLException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests absolute URI recognition and URL path concatenation with preserved components.
 */
class UrlHelperTest {
    /**
     * Accepts scheme-bearing URIs across HTTP, URN, and file forms.
     *
     * @param uri valid absolute URI
     */
    @ParameterizedTest
    @ValueSource(strings = {"https://example.net/path", "urn:ogf:network:example:2026:nsa", "file:/tmp/test.xml"})
    void recognizesAbsoluteUris(String uri) { assertTrue(UrlHelper.isAbsolute(uri)); }

    /**
     * Rejects relative references and invalid syntax.
     *
     * @param uri relative or malformed URI
     */
    @ParameterizedTest
    @ValueSource(strings = {"path", "/path", "//example.net/path", "http://[", "", "bad uri"})
    void rejectsRelativeAndMalformedUris(String uri) { assertFalse(UrlHelper.isAbsolute(uri)); }

    /**
     * Treats absent URI text as non-absolute.
     */
    @Test
    void nullIsNotAnAbsoluteUri() { assertFalse(UrlHelper.isAbsolute(null)); }

    /**
     * Checks slash joining while preserving authority, escapes, query, and fragment.
     *
     * @param base absolute base URL
     * @param postfix path suffix to append
     * @param expected complete expected URL
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @CsvSource({
        "https://example.net/base,child,https://example.net/base/child",
        "https://example.net/base/,/child,https://example.net/base/child",
        "https://example.net,/child,https://example.net/child",
        "https://example.net/base?x=1#part,child,https://example.net/base/child?x=1#part",
        "https://user:pass@[::1]:8443/base,child,https://user:pass@[::1]:8443/base/child",
        "https://example.net/base%20path,child%20path,https://example.net/base%20path/child%20path"
    })
    void appendsToThePathWithoutLosingUrlComponents(String base, String postfix, String expected) throws Exception {
        assertEquals(expected, UrlHelper.append(base, postfix));
    }

    /**
     * Keeps opaque archive locations, entry names, raw escapes, and outer fragments intact.
     *
     * @param base opaque jar URL
     * @param postfix entry-path suffix to append
     * @param expected complete URL after appending
     * @throws Exception if the URL cannot be constructed
     */
    @ParameterizedTest
    @CsvSource({
        "jar:file:/tmp/example.jar!/dir,child,jar:file:/tmp/example.jar!/dir/child",
        "jar:file:/tmp/example.jar!/,child,jar:file:/tmp/example.jar!/child",
        "jar:file:/tmp/example.jar!/dir///,///child,jar:file:/tmp/example.jar!/dir/child",
        "jar:file:/tmp/example%20archive.jar!/dir%20name,child%20name,jar:file:/tmp/example%20archive.jar!/dir%20name/child%20name",
        "jar:file:/tmp/example.jar!/dir#part%20one,child,jar:file:/tmp/example.jar!/dir/child#part%20one",
        "jar:file:/tmp/example.jar!/dir,'',jar:file:/tmp/example.jar!/dir/"
    })
    void appendsToOpaqueUrlsWithoutLosingArchiveComponents(String base, String postfix, String expected) throws Exception {
        assertEquals(expected, UrlHelper.append(base, postfix));
    }

    /**
     * Requires a checked URL error for malformed base text.
     */
    @Test
    void rejectsInvalidBaseUrls() { assertThrows(MalformedURLException.class, () -> UrlHelper.append("not a url", "child")); }
}
