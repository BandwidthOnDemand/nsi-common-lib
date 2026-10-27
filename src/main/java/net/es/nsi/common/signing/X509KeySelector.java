package net.es.nsi.common.signing;

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;

/**
 * Selects the first compatible certificate public key from embedded {@code X509Data} data.
 *
 * <p>Only RSA-SHA256 and DSA-SHA256 signatures are supported. Selection checks
 * algorithm compatibility; it does not authenticate the key owner or establish trust.
 *
 * @author hacksaw
 */
public class X509KeySelector extends KeySelector {
    /**
     * Returns the first embedded certificate public key compatible with the signature method.
     * Certificate chains, validity dates, and trust anchors are not evaluated.
     *
     * @param keyInfo embedded key information to inspect
     * @param purpose requested key purpose; not used by this selector
     * @param method signature method whose algorithm must match the key
     * @param context calling XML context; not used by this selector
     * @return the first matching key
     * @throws KeySelectorException if key information is absent, unreadable, or contains no compatible key
     */
    @Override
    public KeySelectorResult select(KeyInfo keyInfo,KeySelector.Purpose purpose,
            AlgorithmMethod method, XMLCryptoContext context)
            throws KeySelectorException {

        if (keyInfo == null) {
            throw new KeySelectorException("Null KeyInfo object!");
        }

        Iterator ki = keyInfo.getContent().iterator();
        while (ki.hasNext()) {
            XMLStructure info = (XMLStructure) ki.next();
            if (!(info instanceof X509Data)) {
                continue;
            }

            X509Data x509Data = (X509Data) info;

            Iterator xi = x509Data.getContent().iterator();
            while (xi.hasNext()) {
                Object o = xi.next();
                if (!(o instanceof X509Certificate)) {
                    continue;
                }

                final PublicKey key = ((X509Certificate)o).getPublicKey();

                if (algEquals(method.getAlgorithm(), key.getAlgorithm())) {
                    return () -> key;
                }
            }
        }
        throw new KeySelectorException("No key found!");
    }

    /**
     * Tests the shared signing algorithm policy.
     *
     * @param algURI XML signature-method URI
     * @param algName JCA key algorithm name
     * @return {@code true} if the pair is supported and compatible
     */
    static boolean algEquals(String algURI, String algName) {
        return SignatureAlgorithms.matches(algURI, algName);
    }
}
