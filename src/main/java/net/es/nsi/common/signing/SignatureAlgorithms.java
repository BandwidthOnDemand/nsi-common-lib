package net.es.nsi.common.signing;

import java.security.NoSuchAlgorithmException;
import javax.xml.crypto.dsig.SignatureMethod;

/**
 * Maps supported key algorithms to SHA-256 XML signature methods.
 *
 * <p>Signing and key selection share this mapping so they cannot silently disagree.
 */
final class SignatureAlgorithms {
    /**
     * Prevents instantiation of this algorithm mapping utility.
     */
    private SignatureAlgorithms() {
    }

    /**
     * Selects the XML signature method for an RSA or DSA key, ignoring case.
     *
     * @param keyAlgorithm JCA key algorithm name
     * @return the corresponding SHA-256 signature-method URI
     * @throws NoSuchAlgorithmException if the algorithm is unsupported, including a null name
     */
    static String forKey(String keyAlgorithm) throws NoSuchAlgorithmException {
        if ("RSA".equalsIgnoreCase(keyAlgorithm)) {
            return SignatureMethod.RSA_SHA256;
        }
        if ("DSA".equalsIgnoreCase(keyAlgorithm)) {
            return SignatureMethod.DSA_SHA256;
        }
        throw new NoSuchAlgorithmException("Unsupported XML signature key algorithm: " + keyAlgorithm);
    }

    /**
     * Checks whether a key can be used with the requested supported signature method.
     *
     * @param signatureAlgorithm XML signature-method URI
     * @param keyAlgorithm JCA key algorithm name
     * @return {@code true} for a supported, matching pair; {@code false} otherwise
     */
    static boolean matches(String signatureAlgorithm, String keyAlgorithm) {
        try {
            return forKey(keyAlgorithm).equals(signatureAlgorithm);
        } catch (NoSuchAlgorithmException ex) {
            return false;
        }
    }
}
