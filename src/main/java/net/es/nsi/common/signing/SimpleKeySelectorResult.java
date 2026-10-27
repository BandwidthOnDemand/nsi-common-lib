package net.es.nsi.common.signing;

import java.security.Key;
import java.security.PublicKey;
import javax.xml.crypto.KeySelectorResult;

/**
 * Immutable holder that exposes a selected public key to the XML signature API.
 *
 * @author hacksaw
 */
public class SimpleKeySelectorResult implements KeySelectorResult {

    /**
     * Public key selected from signature key information.
     */
    private final PublicKey pk;
    /**
     * Wraps the selected key without copying it.
     *
     * @param pk public key to expose
     */
    SimpleKeySelectorResult(PublicKey pk) {
        this.pk = pk;
    }

    /**
     * Returns the public key supplied at construction.
     *
     * @return the selected public key
     */
    @Override
    public Key getKey() { return pk; }
 }
