package net.es.nsi.common.signing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Objects;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;

/**
 * Creates and verifies DOM XML signatures using RSA-SHA256 or DSA-SHA256.
 *
 * <p>Payload digests use SHA-512. SignedInfo uses inclusive canonicalization with
 * comments. Instances hold provider state and a keystore handler; callers should
 * serialize access when sharing an instance or a DOM between threads.
 *
 * <p>Verification checks signature integrity using the embedded certificate. Callers
 * must apply their own certificate trust policy before accepting the signer.
 */
public class SignatureFactory {
    /**
     * DOM signature factory supplied by the selected JCA provider.
     */
    private final XMLSignatureFactory fac;
    /**
     * Loaded store used to recover signing keys and embed certificates.
     */
    private final KeyStoreHandler keyStoreHandler;

    /**
     * Uses the configured DOM signature provider and the default JSSE signing keystore.
     *
     * @throws KeyStoreException if the default signing keystore cannot be loaded
     * @throws NoSuchMethodException if a configured external provider has no no-argument constructor
     * @throws InvocationTargetException if a configured provider constructor throws
     * @throws IllegalArgumentException if the configured provider class cannot be loaded or instantiated
     */
    public SignatureFactory() throws KeyStoreException, RuntimeException, NoSuchMethodException,
            IllegalArgumentException, InvocationTargetException {
        fac = signatureFactory();
        try {
            keyStoreHandler = new KeyStoreHandler();
        } catch (IOException | NoSuchAlgorithmException | CertificateException ex) {
            throw new KeyStoreException("Could not load the default signing keystore", ex);
        }
    }

    /**
     * Uses the configured DOM signature provider with an already loaded keystore.
     *
     * @param keyStoreHandler non-null source of signing keys and certificates
     * @throws NoSuchMethodException if a configured external provider has no no-argument constructor
     * @throws InvocationTargetException if a configured provider constructor throws
     * @throws IllegalArgumentException if the configured provider class cannot be loaded or instantiated
     * @throws NullPointerException if the handler is null
     */
    public SignatureFactory(KeyStoreHandler keyStoreHandler) throws RuntimeException,
            NoSuchMethodException, IllegalArgumentException, InvocationTargetException {
        fac = signatureFactory();
        this.keyStoreHandler = Objects.requireNonNull(keyStoreHandler, "keyStoreHandler");
    }

    /**
     * Resolves {@code jsr105Provider} by registered provider name or class name, then
     * by an externally accessible no-argument constructor. An absent or blank property
     * uses the default DOM provider.
     *
     * @return the selected DOM signature factory
     * @throws NoSuchMethodException if a configured external provider has no no-argument constructor
     * @throws InvocationTargetException if a configured provider constructor throws
     * @throws IllegalArgumentException if the configured provider class cannot be loaded or instantiated
     */
    private static XMLSignatureFactory signatureFactory()
            throws NoSuchMethodException, InvocationTargetException {
        String configured = System.getProperty("jsr105Provider");
        if (configured == null || configured.isBlank()) {
            return XMLSignatureFactory.getInstance("DOM");
        }

        // Match registered names or classes, including the former JDK-internal default,
        // without reflectively constructing an encapsulated JDK class.
        for (Provider provider : Security.getProviders()) {
            if (provider.getName().equals(configured) || provider.getClass().getName().equals(configured)) {
                return XMLSignatureFactory.getInstance("DOM", provider);
            }
        }

        // Preserve explicitly configured, publicly constructible third-party provider classes.
        try {
            Provider provider = Class.forName(configured).asSubclass(Provider.class)
                    .getDeclaredConstructor().newInstance();
            return XMLSignatureFactory.getInstance("DOM", provider);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ClassCastException ex) {
            throw new IllegalArgumentException("Could not load XML signature provider: " + configured, ex);
        }
    }

    /**
     * Appends an enveloped signature to the document element and verifies the result.
     * The same mutable document is returned. A failure after insertion can leave the
     * document partially modified.
     *
     * @param doc namespace-aware document to modify
     * @param alias private-key alias in the configured keystore
     * @return the supplied document, now containing a signature
     * @throws KeyStoreException if the signing private key or certificate cannot be obtained
     * @throws XMLSignatureException if signing, algorithm selection, or verification fails
     * @throws NullPointerException if the document is null
     * @throws IllegalArgumentException if the document has no document element
     */
    public Document generateEnvelopedSignature(Document doc, String alias)
            throws KeyStoreException, XMLSignatureException, RuntimeException {
        Objects.requireNonNull(doc, "doc");
        if (doc.getDocumentElement() == null) {
            throw new IllegalArgumentException("The document must have a document element");
        }
        try {
            Reference reference = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA512, null),
                    List.of(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);
            KeyStore.PrivateKeyEntry keyEntry = privateKey(alias);
            XMLSignature signature = signature(keyEntry, alias, reference);
            DOMSignContext context = new DOMSignContext(keyEntry.getPrivateKey(), doc.getDocumentElement());
            signature.sign(context);
            if (!Validate.validateEnveloped(doc)) {
                throw new XMLSignatureException("The generated enveloped signature failed validation");
            }
            return doc;
        } catch (XMLSignatureException | KeyStoreException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new XMLSignatureException("Could not generate the enveloped signature", ex);
        }
    }

    /**
     * Signs the supplied document without modifying it and returns a separate signature DOM.
     * The payload, including comments, is canonicalized and referenced by a fixed local
     * URN. Use {@link Validate#validateExternal(Document, Document)} with the original
     * payload to verify the result.
     *
     * @param dom namespace-aware payload document, left unchanged
     * @param alias private-key alias in the configured keystore
     * @return a new document whose root is the detached Signature
     * @throws KeyStoreException if the signing private key or certificate cannot be obtained
     * @throws XMLSignatureException if signing, algorithm selection, or verification fails
     * @throws NullPointerException if the document is null
     * @throws IllegalArgumentException if the document has no document element
     */
    public Document generateExternalSignature(Document dom, String alias)
            throws KeyStoreException, XMLSignatureException, RuntimeException {
        DetachedDocument payload = new DetachedDocument(dom);
        try {
            Reference reference = fac.newReference(DetachedDocument.REFERENCE_URI,
                    fac.newDigestMethod(DigestMethod.SHA512, null),
                    List.of(fac.newTransform(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                            (TransformParameterSpec) null)), null, null);
            KeyStore.PrivateKeyEntry keyEntry = privateKey(alias);
            XMLSignature signature = signature(keyEntry, alias, reference);
            Document detached = DocumentBuilderFactory.newDefaultNSInstance()
                    .newDocumentBuilder().newDocument();
            DOMSignContext context = new DOMSignContext(keyEntry.getPrivateKey(), detached);
            context.setURIDereferencer(payload);
            signature.sign(context);
            if (!Validate.validateExternal(dom, detached)) {
                throw new XMLSignatureException("The generated detached signature failed validation");
            }
            return detached;
        } catch (XMLSignatureException | KeyStoreException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new XMLSignatureException("Could not generate the detached signature", ex);
        }
    }

    /**
     * Loads a required private-key entry and normalizes lookup failures.
     *
     * @param alias private-key alias
     * @return the available private-key entry
     * @throws KeyStoreException if the entry is missing or cannot be recovered
     */
    private KeyStore.PrivateKeyEntry privateKey(String alias) throws KeyStoreException {
        try {
            KeyStore.PrivateKeyEntry entry = keyStoreHandler.getPrivateKeyEntry(alias);
            if (entry == null) {
                throw new KeyStoreException("No private key entry for alias: " + alias);
            }
            return entry;
        } catch (NoSuchAlgorithmException | UnrecoverableEntryException ex) {
            throw new KeyStoreException("Could not load the private key for alias: " + alias, ex);
        }
    }

    /**
     * Builds SignedInfo and embeds the certificate for a single payload reference.
     *
     * @param entry entry containing the signing private key
     * @param alias alias used to retrieve certificate information
     * @param reference payload reference to sign
     * @return an unsigned XML signature ready for marshalling
     * @throws XMLSignatureException if algorithms or key information cannot be prepared
     * @throws KeyStoreException if certificate lookup fails
     */
    private XMLSignature signature(KeyStore.PrivateKeyEntry entry, String alias, Reference reference)
            throws XMLSignatureException, KeyStoreException {
        try {
            String method = SignatureAlgorithms.forKey(entry.getPrivateKey().getAlgorithm());
            SignedInfo info = fac.newSignedInfo(fac.newCanonicalizationMethod(
                    CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null),
                    fac.newSignatureMethod(method, null), List.of(reference));
            KeyInfo keyInfo = keyStoreHandler.getKeyInfo(alias, fac);
            return fac.newXMLSignature(info, keyInfo);
        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException | UnrecoverableEntryException ex) {
            throw new XMLSignatureException(ex.getMessage(), ex);
        }
    }
}
