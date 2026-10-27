package net.es.nsi.common.signing;

import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Security;
import java.security.MessageDigest;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.KeySelectorException;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import net.es.nsi.common.constants.Properties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Tests signing on Java 17, 21, 25, and 27, provider compatibility, payload integrity,
 * and adversarial XML signatures.
 *
 * <p>System-property access is locked and restored because provider and JSSE configuration
 * is process-wide. Keystores and private keys are temporary test fixtures.
 */
@ResourceLock(Resources.SYSTEM_PROPERTIES)
class SignatureFactoryTest {
    /**
     * Process-wide signing properties that each test snapshots and restores.
     */
    private static final String[] SYSTEM_PROPERTIES = {
        "jsr105Provider", Properties.SYSTEM_PROPERTY_SSL_KEYSTORE,
        Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD, Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE
    };

    /**
     * JUnit-managed directory containing all generated keystores.
     */
    @TempDir
    static Path directory;
    /**
     * Original property values, including null for previously absent properties.
     */
    private final Map<String, String> savedProperties = new HashMap<>();

    /**
     * Creates supported RSA/DSA keys, an unsupported EC key, and an unrelated RSA certificate.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @BeforeAll
    static void createKeystores() throws Exception {
        for (String algorithm : new String[]{"RSA", "DSA", "EC"}) {
            SigningTestSupport.createKeystore(keystore(algorithm), algorithm);
        }
        SigningTestSupport.createKeystore(keystore("other-RSA"), "RSA");
    }

    /**
     * Selects a fixture path without loading or creating the store.
     *
     * @param algorithm fixture name, including the unrelated RSA variant
     * @return the temporary keystore path
     */
    private static Path keystore(String algorithm) {
        return directory.resolve(algorithm + ".p12");
    }

    /**
     * Snapshots signing configuration and clears provider overrides to isolate each test.
     */
    @BeforeEach
    void saveProperties() {
        for (String property : SYSTEM_PROPERTIES) {
            savedProperties.put(property, System.getProperty(property));
        }
        System.clearProperty("jsr105Provider");
    }

    /**
     * Restores both previous values and previously absent properties after each test.
     */
    @AfterEach
    void restoreProperties() {
        savedProperties.forEach((name, value) -> {
            if (value == null) {
                System.clearProperty(name);
            } else {
                System.setProperty(name, value);
            }
        });
    }

    /**
     * Loads the named disposable PKCS12 signing fixture.
     *
     * @param algorithm fixture name to load
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     * @return a handler for the selected fixture store
     */
    private static KeyStoreHandler handler(String algorithm) throws Exception {
        return new KeyStoreHandler(keystore(algorithm).toString(), SigningTestSupport.PASSWORD, "PKCS12");
    }

    /**
     * Creates a fresh payload with namespaces, a processing instruction, a comment, and Unicode.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     * @return a new independent payload document
     */
    private static Document document() throws Exception {
        return parse("<?before test?><n:document xmlns:n='urn:test:nsi' id='original'>"
                + "<!--signed comment--><n:value label='alpha'>café</n:value></n:document>");
    }

    /**
     * Parses trusted in-memory test XML with namespace awareness.
     *
     * @param xml trusted test XML; this helper does not represent the production security boundary
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     * @return the namespace-aware fixture DOM
     */
    private static Document parse(String xml) throws Exception {
        return DocumentBuilderFactory.newDefaultNSInstance().newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
    }

    /**
     * Serializes and reparses a DOM to check verification across an XML transport boundary.
     *
     * @param document DOM to round-trip
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     * @return the reparsed document
     */
    private static Document roundTrip(Document document) throws Exception {
        StringWriter xml = new StringWriter();
        TransformerFactory.newDefaultInstance().newTransformer()
                .transform(new DOMSource(document), new StreamResult(xml));
        return parse(xml.toString());
    }

    /**
     * Finds the first signature-namespace element used by a controlled fixture.
     *
     * @param document signature-bearing test DOM
     * @param name signature element local name
     * @return the first matching signature element
     */
    private static Element element(Document document, String name) {
        return (Element) document.getElementsByTagNameNS(XMLSignature.XMLNS, name).item(0);
    }

    /**
     * Exercises either signing form with the shared fixture alias.
     *
     * @param factory signing factory under test
     * @param document payload DOM
     * @param detached true for a separate signature document
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     * @return the signature-bearing document
     */
    private static Document sign(SignatureFactory factory, Document document, boolean detached)
            throws Exception {
        return detached ? factory.generateExternalSignature(document, "signer")
                : factory.generateEnvelopedSignature(document, "signer");
    }

    /**
     * Dispatches to the validator corresponding to the chosen signature form.
     *
     * @param document original payload for detached validation
     * @param signature signature-bearing DOM
     * @param detached true for detached validation
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     * @return the selected validator result
     */
    private static boolean valid(Document document, Document signature, boolean detached)
            throws Exception {
        return detached ? Validate.validateExternal(document, signature)
                : Validate.validateEnveloped(signature);
    }

    /**
     * Checks default JCA provider discovery with the JSSE-configured fixture store.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void defaultConstructorUsesPublicProviderDiscovery() throws Exception {
        System.setProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE, keystore("RSA").toString());
        System.setProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD, SigningTestSupport.PASSWORD);
        System.setProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE, "PKCS12");
        Document signed = new SignatureFactory().generateEnvelopedSignature(document(), "signer");
        assertTrue(Validate.validateEnveloped(signed));
    }

    /**
     * Checks supported algorithms, serialization round trips, and detached DOM ownership.
     *
     * @param algorithm supported RSA or DSA key algorithm
     * @param detached true for a separate signature document
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @CsvSource({"RSA,false", "DSA,false", "RSA,true", "DSA,true"})
    void signsAndValidatesOnJava21(String algorithm, boolean detached) throws Exception {
        Document original = document();
        Document before = (Document) original.cloneNode(true);
        Document signature = sign(new SignatureFactory(handler(algorithm)), original, detached);

        assertEquals(algorithm.equals("RSA") ? SignatureMethod.RSA_SHA256 : SignatureMethod.DSA_SHA256,
                element(signature, "SignatureMethod").getAttribute("Algorithm"));
        assertEquals(DigestMethod.SHA512, element(signature, "DigestMethod").getAttribute("Algorithm"));
        assertEquals(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                element(signature, "CanonicalizationMethod").getAttribute("Algorithm"));
        assertTrue(valid(original, signature, detached));
        assertTrue(valid(roundTrip(original), roundTrip(signature), detached));
        if (detached) {
            assertTrue(before.isEqualNode(original), "Detached signing must preserve the source DOM");
            assertNotSame(original, signature);
            assertEquals("Signature", signature.getDocumentElement().getLocalName());
        }
    }

    /**
     * Ensures altering signed payload text invalidates either signature form.
     *
     * @param algorithm supported signing key algorithm
     * @param detached true for detached signing
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @CsvSource({"RSA,false", "DSA,false", "RSA,true", "DSA,true"})
    void rejectsChangedContent(String algorithm, boolean detached) throws Exception {
        Document original = document();
        Document signature = sign(new SignatureFactory(handler(algorithm)), original, detached);
        original.getElementsByTagNameNS("urn:test:nsi", "value").item(0).setTextContent("tampered");
        assertFalse(valid(original, signature, detached));
    }

    /**
     * Ensures signature-value tampering is detected.
     *
     * @param detached true for detached signing
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsChangedSignature(boolean detached) throws Exception {
        Document original = document();
        Document signature = sign(new SignatureFactory(handler("RSA")), original, detached);
        Element value = element(signature, "SignatureValue");
        String encoded = value.getTextContent();
        value.setTextContent((encoded.charAt(0) == 'A' ? "B" : "A") + encoded.substring(1));
        assertFalse(valid(original, signature, detached));
    }

    /**
     * Checks that a detached signature is bound to the supplied payload rather than an empty DOM.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void detachedVerificationUsesTheSuppliedDocument() throws Exception {
        Document original = document();
        Document signature = new SignatureFactory(handler("RSA"))
                .generateExternalSignature(original, "signer");
        Document other = document();
        other.getDocumentElement().setAttribute("id", "different");
        assertFalse(Validate.validateExternal(other, signature));
        assertTrue(Validate.validateExternal(original, signature));
    }

    /**
     * Ensures detached canonicalization includes comments and attribute values.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void detachedSignatureCoversCommentsAndAttributes() throws Exception {
        Document original = document();
        Document signature = new SignatureFactory(handler("RSA"))
                .generateExternalSignature(original, "signer");
        Document changed = (Document) original.cloneNode(true);
        changed.getDocumentElement().getFirstChild().setNodeValue("tampered comment");
        assertFalse(Validate.validateExternal(changed, signature));
        changed = (Document) original.cloneNode(true);
        ((Element) changed.getElementsByTagNameNS("urn:test:nsi", "value").item(0))
                .setAttribute("label", "tampered attribute");
        assertFalse(Validate.validateExternal(changed, signature));
    }

    /**
     * Reports the unsupported EC algorithm through the signing exception contract.
     *
     * @param detached true for detached signing
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsUnsupportedSigningKey(boolean detached) throws Exception {
        SignatureFactory factory = new SignatureFactory(handler("EC"));
        XMLSignatureException failure = assertThrows(XMLSignatureException.class,
                () -> sign(factory, document(), detached));
        assertTrue(failure.getMessage().contains("EC"));
    }

    /**
     * Checks provider lookup by registered JCA name.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void acceptsRegisteredProviderName() throws Exception {
        System.setProperty("jsr105Provider", XMLSignatureFactory.getInstance("DOM").getProvider().getName());
        Document signed = new SignatureFactory(handler("RSA"))
                .generateEnvelopedSignature(document(), "signer");
        assertTrue(Validate.validateEnveloped(signed));
    }

    /**
     * Preserves legacy class-name configuration without opening encapsulated JDK packages.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void acceptsRegisteredProviderClassWithoutReflectiveAccess() throws Exception {
        System.setProperty("jsr105Provider", XMLSignatureFactory.getInstance("DOM").getProvider().getClass().getName());
        assertNotNull(new SignatureFactory(handler("RSA")));
    }

    /**
     * Includes the configured provider name in an explicit configuration error.
     */
    @Test
    void rejectsUnknownProviderClearly() {
        System.setProperty("jsr105Provider", "no.such.Provider");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> new SignatureFactory(handler("RSA")));
        assertTrue(error.getMessage().contains("no.such.Provider"));
    }

    /**
     * Exercises construction of an externally accessible provider that is not globally registered.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void acceptsUnregisteredPublicProviderClass() throws Exception {
        System.setProperty("jsr105Provider", PublicProvider.class.getName());
        assertNull(Security.getProvider("NSITestProvider"));
        Document signature = new SignatureFactory(handler("RSA"))
                .generateEnvelopedSignature(document(), "signer");
        assertTrue(Validate.validateEnveloped(signature));
    }

    /**
     * Public fixture provider that delegates DOM services through standard JCA APIs.
     * It remains unregistered to exercise the explicit provider-class fallback.
     */
    public static class PublicProvider extends Provider {
        /**
         * Publishes DOM signature and key-information services backed by the default provider.
         */
        public PublicProvider() {
            super("NSITestProvider", "1", "Test XML signature provider");
            Provider delegate = XMLSignatureFactory.getInstance("DOM").getProvider();
            for (String type : List.of("XMLSignatureFactory", "KeyInfoFactory")) {
                putService(new Service(this, type, "DOM", "unused", List.of(), Map.of()) {
                    /**
                     * Creates the requested delegated DOM service without reflecting into JDK internals.
                     *
                     * @param parameter provider construction parameter; unused
                     * @return a delegated XMLSignatureFactory or KeyInfoFactory
                     * @throws NoSuchAlgorithmException if the delegated DOM service is unavailable
                     */
                    @Override
                    public Object newInstance(Object parameter) throws NoSuchAlgorithmException {
                        return type.equals("XMLSignatureFactory")
                                ? XMLSignatureFactory.getInstance("DOM", delegate)
                                : KeyInfoFactory.getInstance("DOM", delegate);
                    }
                });
            }
        }
    }

    /**
     * Checks the shared algorithm policy for raw keys and certificate-backed keys.
     *
     * @param algorithm RSA or DSA key algorithm
     * @param x509 true for X509Data; false for KeyValue
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @CsvSource({"RSA,false", "DSA,false", "RSA,true", "DSA,true"})
    void selectorsAcceptSha256AndRejectMismatchedOrLegacyMethods(String algorithm, boolean x509)
            throws Exception {
        KeyStoreHandler handler = handler(algorithm);
        PublicKey key = handler.getX509Certificate("signer").getPublicKey();
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
        KeyInfo info = x509 ? handler.getKeyInfo("signer", factory)
                : factory.getKeyInfoFactory().newKeyInfo(List.of(factory.getKeyInfoFactory().newKeyValue(key)));
        KeySelector selector = x509 ? new X509KeySelector() : new KeyValueKeySelector();
        DOMValidateContext context = new DOMValidateContext(key, document().getDocumentElement());
        String supported = algorithm.equals("RSA") ? SignatureMethod.RSA_SHA256 : SignatureMethod.DSA_SHA256;
        String mismatched = algorithm.equals("RSA") ? SignatureMethod.DSA_SHA256 : SignatureMethod.RSA_SHA256;
        String legacy = algorithm.equals("RSA") ? SignatureMethod.RSA_SHA1 : SignatureMethod.DSA_SHA1;

        assertEquals(key, selector.select(info, KeySelector.Purpose.VERIFY,
                factory.newSignatureMethod(supported, null), context).getKey());
        assertThrows(KeySelectorException.class, () -> selector.select(info, KeySelector.Purpose.VERIFY,
                factory.newSignatureMethod(mismatched, null), context));
        assertThrows(KeySelectorException.class, () -> selector.select(info, KeySelector.Purpose.VERIFY,
                factory.newSignatureMethod(legacy, null), context));
    }

    /**
     * Rejects signed metadata that points outside the local payload contract.
     *
     * @param changed reference or transform to replace with an unsupported value
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @ValueSource(strings = {"reference", "transform"})
    void detachedVerificationRejectsOtherTargetsAndTransforms(String changed) throws Exception {
        String uri = changed.equals("reference") ? "https://example.invalid/other"
                : DetachedDocument.REFERENCE_URI;
        String transform = changed.equals("transform") ? CanonicalizationMethod.INCLUSIVE
                : CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS;
        Document signature = suppliedDigestSignature(uri, transform, SignatureMethod.RSA_SHA256);
        assertFalse(Validate.validateExternal(parse("<payload>other</payload>"), signature));
    }

    /**
     * Requires secure validation to reject a legacy SHA-1 signature method.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void secureValidationRejectsRsaSha1() throws Exception {
        Document signature = suppliedDigestSignature(DetachedDocument.REFERENCE_URI,
                CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, SignatureMethod.RSA_SHA1);
        assertThrows(MarshalException.class, () -> Validate.validateExternal(document(), signature));
    }

    /**
     * Builds one signed reference with a supplied digest so adversarial metadata needs no URI I/O.
     *
     * @param uri reference target under test
     * @param transform transform URI under test
     * @param method signature-method URI
     * @return a detached signature for the controlled payload
     * @throws Exception if fixture signing fails
     */
    private static Document suppliedDigestSignature(String uri, String transform, String method)
            throws Exception {
        return suppliedDigestSignature(uri, transform, method, 1);
    }

    /**
     * Signs controlled reference metadata using a precomputed payload digest.
     * This permits testing reference policy independently of URI dereferencing.
     *
     * @param uri target shared by the fixture references
     * @param transform transform URI for each reference
     * @param method signature-method URI
     * @param count number of signed references to create
     * @return the adversarial signature fixture
     * @throws Exception if fixture signing fails
     */
    private static Document suppliedDigestSignature(String uri, String transform, String method, int count)
            throws Exception {
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
        KeyStoreHandler handler = handler("RSA");
        byte[] digest = MessageDigest.getInstance("SHA-512")
                .digest("<payload>other</payload>".getBytes(StandardCharsets.UTF_8));
        List<Reference> references = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            references.add(factory.newReference(uri, factory.newDigestMethod(DigestMethod.SHA512, null),
                    List.of(factory.newTransform(transform, (TransformParameterSpec) null)), null, null, digest));
        }
        SignedInfo info = factory.newSignedInfo(factory.newCanonicalizationMethod(
                CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, (C14NMethodParameterSpec) null),
                factory.newSignatureMethod(method, null), references);
        Document signature = DocumentBuilderFactory.newDefaultNSInstance().newDocumentBuilder().newDocument();
        // A supplied digest allows construction of adversarial signed metadata without dereferencing a URI.
        factory.newXMLSignature(info, handler.getKeyInfo("signer", factory))
                .sign(new DOMSignContext(handler.getPrivateKeyEntry("signer").getPrivateKey(), signature));
        return signature;
    }

    /**
     * Checks explicit missing-key errors and rejection of a substituted embedded certificate.
     *
     * @param detached true for detached signing
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rejectsMissingAliasesAndCertificatesThatDoNotMatchTheSigningKey(boolean detached) throws Exception {
        SignatureFactory factory = new SignatureFactory(handler("RSA"));
        assertThrows(java.security.KeyStoreException.class, () -> {
            if (detached) factory.generateExternalSignature(document(), "missing");
            else factory.generateEnvelopedSignature(document(), "missing");
        });
        Document original = document();
        Document signature = sign(factory, original, detached);
        element(signature, "X509Certificate").setTextContent(java.util.Base64.getEncoder()
                .encodeToString(handler("other-RSA").getX509Certificate("signer").getEncoded()));
        assertFalse(valid(original, signature, detached));
    }

    /**
     * Ensures signing cannot report success when its embedded certificate does not match the key.
     *
     * @param detached true for detached signing
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void selfValidationFailureIsReportedToTheCaller(boolean detached) throws Exception {
        KeyStoreHandler mismatched = new KeyStoreHandler(keystore("RSA").toString(), SigningTestSupport.PASSWORD, "PKCS12") {
            /**
             * Substitutes another certificate to force the signer self-validation failure path.
             *
             * @param alias fixture private-key alias
             * @param factory factory creating key information
             * @return key information containing an unrelated RSA certificate
             * @throws java.security.NoSuchAlgorithmException retained from the overridden fixture API
             * @throws java.security.UnrecoverableEntryException retained from the overridden fixture API
             * @throws java.security.KeyStoreException if unrelated certificate lookup fails
             */
            @Override
            public KeyInfo getKeyInfo(String alias, XMLSignatureFactory factory)
                    throws java.security.NoSuchAlgorithmException, java.security.UnrecoverableEntryException,
                    java.security.KeyStoreException {
                try {
                    return handler("other-RSA").getKeyInfo(alias, factory);
                } catch (Exception error) {
                    throw new java.security.KeyStoreException(error);
                }
            }
        };
        assertThrows(XMLSignatureException.class,
                () -> sign(new SignatureFactory(mismatched), document(), detached));
    }

    /**
     * Rejects absent, duplicate, incomplete, and incorrectly nested signatures.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void rejectsAmbiguousOrMalformedSignatureStructures() throws Exception {
        assertThrows(MarshalException.class, () -> Validate.validateEnveloped(document()));
        Document original = document();
        Document signed = new SignatureFactory(handler("RSA")).generateEnvelopedSignature(original, "signer");
        signed.getDocumentElement().appendChild(element(signed, "Signature").cloneNode(true));
        assertThrows(MarshalException.class, () -> Validate.validateEnveloped(signed));
        Document detached = new SignatureFactory(handler("RSA")).generateExternalSignature(document(), "signer");
        Element reference = element(detached, "Reference");
        reference.getParentNode().removeChild(reference);
        assertThrows(MarshalException.class, () -> Validate.validateExternal(document(), detached));
        Document wrapper = document();
        Document nested = new SignatureFactory(handler("RSA")).generateExternalSignature(document(), "signer");
        wrapper.getDocumentElement().appendChild(wrapper.importNode(nested.getDocumentElement(), true));
        assertThrows(MarshalException.class, () -> Validate.validateExternal(document(), wrapper));
    }

    /**
     * Requires exactly one payload reference even when extra references are cryptographically valid.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void rejectsSignedExtraReferencesEvenWhenBothDigestsMatch() throws Exception {
        Document signature = suppliedDigestSignature(DetachedDocument.REFERENCE_URI,
                CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, SignatureMethod.RSA_SHA256, 2);
        assertFalse(Validate.validateExternal(parse("<payload>other</payload>"), signature));
    }

    /**
     * Checks missing, unrelated, and unusable key information without relying on null-pointer failures.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void missingKeyInfoAndInvalidKeyInfoAreExplicitFailures() throws Exception {
        Document original = document();
        Document detached = new SignatureFactory(handler("RSA")).generateExternalSignature(original, "signer");
        Element info = element(detached, "KeyInfo");
        info.getParentNode().removeChild(info);
        assertThrows(XMLSignatureException.class, () -> Validate.validateExternal(original, detached));
        XMLSignatureFactory factory = XMLSignatureFactory.getInstance("DOM");
        var method = factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null);
        var context = new DOMValidateContext(handler("RSA").getX509Certificate("signer").getPublicKey(), original.getDocumentElement());
        for (KeySelector selector : List.of(new X509KeySelector(), new KeyValueKeySelector())) {
            assertThrows(KeySelectorException.class, () -> selector.select(null, KeySelector.Purpose.VERIFY, method, context));
            assertThrows(KeySelectorException.class, () -> selector.select(
                    factory.getKeyInfoFactory().newKeyInfo(List.of(factory.getKeyInfoFactory().newKeyName("missing"))),
                    KeySelector.Purpose.VERIFY, method, context));
        }
    }

    /**
     * Distinguishes canonical attribute ordering from meaningful payload whitespace.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void canonicalizationIgnoresAttributeOrderButPreservesTextWhitespace() throws Exception {
        Document original = parse("<root beta='2' alpha='1'><value> x </value></root>");
        Document detached = new SignatureFactory(handler("RSA")).generateExternalSignature(original, "signer");
        assertTrue(Validate.validateExternal(parse("<root alpha='1' beta='2'><value> x </value></root>"), detached));
        assertFalse(Validate.validateExternal(parse("<root alpha='1' beta='2'><value>x</value></root>"), detached));
    }
}
