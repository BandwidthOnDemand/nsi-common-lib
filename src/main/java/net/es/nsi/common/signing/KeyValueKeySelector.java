package net.es.nsi.common.signing;

import java.security.KeyException;
import java.security.PublicKey;
import java.util.List;
import javax.xml.crypto.AlgorithmMethod;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.KeySelectorResult;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyValue;

/**
 * Selects the first compatible raw public key from embedded {@code KeyValue} data.
 *
 * <p>Only RSA-SHA256 and DSA-SHA256 signatures are supported. Selection checks
 * algorithm compatibility; it does not authenticate the key owner or establish trust.
 *
 * @author hacksaw
 */
public class KeyValueKeySelector extends KeySelector {
    /**
     * Returns the first embedded raw public key compatible with the signature method.
     *
     * @param keyInfo embedded key information to inspect
     * @param purpose requested key purpose; not used by this selector
     * @param method signature method whose algorithm must match the key
     * @param context calling XML context; not used by this selector
     * @return the first matching key
     * @throws KeySelectorException if key information is absent, unreadable, or contains no compatible key
     */
    @Override
    public KeySelectorResult select(KeyInfo keyInfo,
                                    KeySelector.Purpose purpose,
                                    AlgorithmMethod method,
                                    XMLCryptoContext context)
        throws KeySelectorException {
        if (keyInfo == null) {
            throw new KeySelectorException("Null KeyInfo object!");
        }
        SignatureMethod sm = (SignatureMethod) method;
        List list = keyInfo.getContent();

        for (int i = 0; i < list.size(); i++) {
            XMLStructure xmlStructure = (XMLStructure) list.get(i);
            if (xmlStructure instanceof KeyValue) {
                PublicKey pk = null;
                try {
                    pk = ((KeyValue)xmlStructure).getPublicKey();
                } catch (KeyException ke) {
                    throw new KeySelectorException(ke);
                }
                if (algEquals(sm.getAlgorithm(), pk.getAlgorithm())) {
                    return new SimpleKeySelectorResult(pk);
                }
            }
        }
        throw new KeySelectorException("No KeyValue element found!");
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
