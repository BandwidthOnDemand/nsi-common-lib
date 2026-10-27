package net.es.nsi.common.util;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Checks URI syntax and appends paths to absolute URLs without accessing the network.
 *
 * @author hacksaw
 */
public class UrlHelper {
  /**
   * Logger for malformed URI diagnostics.
   */
  private final static Logger LOG = LogManager.getLogger(UrlHelper.class);

  /**
   * Checks whether syntactically valid URI text includes a scheme.
   * This accepts non-HTTP schemes and does not verify reachability.
   *
   * @param uri URI text, or null
   * @return {@code false} for null, malformed, or relative URIs; {@code true} otherwise
   */
  public static boolean isAbsolute(String uri) {
    if (uri == null) {
      return false;
    }
    try {
      final URI u = new URI(uri);
      if (u.isAbsolute()) {
        return true;
      }
    } catch (URISyntaxException ex) {
      LOG.debug("isAbsolute: invalid URI " + uri);
    }

    return false;
  }

    /**
     * Appends a path with one slash at the join, retaining the base authority, raw
     * escapes, query, and fragment. This is path concatenation rather than relative-URI
     * resolution; dot segments are not resolved. Opaque URLs retain their raw
     * scheme-specific component, such as the archive location and entry in a jar URL.
     *
     * @param base non-null absolute URL with a supported scheme
     * @param postfix non-null path suffix, with required URL escaping already applied
     * @return the combined URL
     * @throws MalformedURLException if the base or result is not a valid supported URL
     */
    public static String append(String base, String postfix) throws MalformedURLException {
        try {
            URI parsed = new URI(base);
            parsed.toURL(); // Check that the base uses a supported, absolute URL scheme.
            String path = parsed.isOpaque() ? parsed.getRawSchemeSpecificPart()
                    : (parsed.getRawPath() == null ? "" : parsed.getRawPath());
            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            while (postfix.startsWith("/")) {
                postfix = postfix.substring(1);
            }
            String result;
            if (parsed.isOpaque()) {
                // An opaque URI has no hierarchical path; its raw component includes the archive URL.
                result = parsed.getScheme() + ":" + path + "/" + postfix;
            } else {
                result = parsed.getScheme() + "://" + (parsed.getRawAuthority() == null ? "" : parsed.getRawAuthority())
                        + path + "/" + postfix
                        + (parsed.getRawQuery() == null ? "" : "?" + parsed.getRawQuery());
            }
            result += parsed.getRawFragment() == null ? "" : "#" + parsed.getRawFragment();
            return new URI(result).toURL().toExternalForm();
        } catch (URISyntaxException | IllegalArgumentException ex) {
            MalformedURLException error = new MalformedURLException("Invalid URL: " + base);
            error.initCause(ex);
            throw error;
        }
    }
}
