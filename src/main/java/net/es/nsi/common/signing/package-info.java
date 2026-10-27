/**
 * DOM XML signatures backed by Java keystores and the standard JCA provider APIs.
 *
 * <p>Signing supports RSA-SHA256 and DSA-SHA256 with SHA-512 payload digests.
 * Detached signatures reference a fixed URN resolved exclusively from the supplied
 * payload DOM, including comments. Enveloped signing modifies the payload DOM;
 * detached signing returns a separate signature document.
 *
 * <p>Validation checks cryptographic integrity with the embedded public key.
 * Applications must separately establish certificate trust and signer authorization.
 */
package net.es.nsi.common.signing;
