package net.es.nsi.common.signing;

import java.security.Key;
import java.security.PublicKey;
import javax.xml.crypto.KeySelectorResult;

/**
 *
 * @author hacksaw
 */
public class SimpleKeySelectorResult implements KeySelectorResult {

    private final PublicKey pk;
    SimpleKeySelectorResult(PublicKey pk) {
        this.pk = pk;
    }

    @Override
    public Key getKey() { return pk; }
 }
