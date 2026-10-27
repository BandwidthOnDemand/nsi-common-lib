import java.nio.file.Files;
import java.nio.file.Path;
import net.es.nsi.common.SimpleLabel;
import net.es.nsi.common.jaxb.DomParser;
import net.es.nsi.common.jaxb.NsaParser;
import net.es.nsi.common.jaxb.nsa.NsaType;
import net.es.nsi.common.jaxb.nsa.ObjectFactory;
import net.es.nsi.common.signing.KeyStoreHandler;
import net.es.nsi.common.signing.SignatureFactory;
import net.es.nsi.common.signing.Validate;
import net.es.nsi.common.util.Decoder;
import net.es.nsi.common.util.Encoder;
import net.es.nsi.common.util.UrlHelper;

/** Standalone Java 17 consumer with no test-framework or project-output dependencies. */
public final class CompatibilityConsumer {
    /** Prevents construction of this command-line fixture. */
    private CompatibilityConsumer() { }

    /**
     * Exercises the selected packaged library with production dependencies only.
     * @param args selected JAR, fixture directory, and generated credential file
     * @throws Exception if any consumer operation or assertion fails
     */
    public static void main(String[] args) throws Exception {
        Path expected = Path.of(args[0]).toRealPath();
        for (Class<?> type : new Class<?>[]{NsaParser.class, NsaType.class, SignatureFactory.class,
                SimpleLabel.class, Encoder.class}) {
            Path actual = Path.of(type.getProtectionDomain().getCodeSource().getLocation().toURI()).toRealPath();
            require(expected.equals(actual), "Library loaded outside selected JAR: " + actual);
        }
        try {
            Class.forName("org.junit.jupiter.api.Test");
            throw new AssertionError("Test dependencies leaked into consumer classpath");
        } catch (ClassNotFoundException expectedAbsence) {
            // The consumer must work without the test framework.
        }
        String text = "café ☃ 日本語";
        var parser = NsaParser.getInstance();
        NsaType original = new NsaType();
        original.setId("urn:test:consumer");
        original.setName(text);
        String xml = parser.jaxb2Xml(new ObjectFactory().createNsa(original));
        NsaType restored = parser.xml2Jaxb(NsaType.class, xml);
        require(original.getId().equals(restored.getId()) && text.equals(restored.getName()),
                "JAXB round trip lost identity or Unicode");
        var document = DomParser.xml2Dom("<root xmlns='urn:test'><!--signed--><value>" + text + "</value></root>");
        require(text.equals(Decoder.decode2dom("base64", "application/x-gzip", Encoder.encode(document))
                .getDocumentElement().getTextContent()), "Codec round trip lost Unicode");
        require(new SimpleLabel("vlan", "0007").equals(new SimpleLabel("vlan", "7")),
                "Label normalization differs");
        require("https://example.invalid/a%20b/c%2Fd?q=1#part".equals(
                UrlHelper.append("https://example.invalid/a%20b?q=1#part", "/c%2Fd")), "URL escapes changed");
        require("jar:file:/tmp/example.jar!/dir/child".equals(
                UrlHelper.append("jar:file:/tmp/example.jar!/dir", "child")), "Opaque URL changed");
        String credential = Files.readString(Path.of(args[2]));
        for (String algorithm : new String[]{"RSA", "DSA"}) {
            var factory = new SignatureFactory(new KeyStoreHandler(
                    Path.of(args[1], algorithm + ".p12").toString(), credential, "PKCS12"));
            var payload = DomParser.xml2Dom("<root xmlns='urn:test'><!--signed--><value>" + text + "</value></root>");
            var detached = factory.generateExternalSignature(payload, "signer");
            require(Validate.validateExternal(payload, detached), algorithm + " detached validation failed");
            payload.getDocumentElement().setAttribute("tampered", "true");
            require(!Validate.validateExternal(payload, detached), algorithm + " detached tampering accepted");
            var enveloped = factory.generateEnvelopedSignature(
                    DomParser.xml2Dom("<root xmlns='urn:test'><value>" + text + "</value></root>"), "signer");
            require(Validate.validateEnveloped(enveloped), algorithm + " enveloped validation failed");
            enveloped.getDocumentElement().setAttribute("tampered", "true");
            require(!Validate.validateEnveloped(enveloped), algorithm + " enveloped tampering accepted");
        }
        System.out.println("Packaged consumer passed on Java " + Runtime.version());
    }

    /**
     * Fails independently of the JVM's assertion-enabled flag.
     * @param condition expected invariant
     * @param message failure diagnostic
     */
    private static void require(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
}
