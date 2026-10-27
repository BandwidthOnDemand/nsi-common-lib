package net.es.nsi.common.jaxb;

import jakarta.xml.bind.JAXBElement;
import java.io.InputStream;
import net.es.nsi.common.jaxb.nml.NmlTopologyType;
import net.es.nsi.common.jaxb.nsa.NsaType;
import net.es.nsi.common.jaxb.signatures.ObjectFactory;
import net.es.nsi.common.jaxb.signatures.SignatureType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

/**
 * Checks Jakarta JAXB bindings across discovery, topology, and XML signature schemas.
 */
class JakartaBindingTest {

    /**
     * Preserves discovery identity, imported vCard content, and interface count through XML.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void discoveryDocumentRoundTrip() throws Exception {
        NsaParser parser = NsaParser.getInstance();
        try (InputStream source = getClass().getResourceAsStream("/nsa.xml")) {
            assertNotNull(source);
            NsaType original = parser.readDocument(source);
            JAXBElement<NsaType> element = new net.es.nsi.common.jaxb.nsa.ObjectFactory()
                    .createNsa(original);

            String xml = parser.jaxb2Xml(element);
            NsaType restored = parser.xml2Jaxb(NsaType.class, xml);

            assertEquals(original.getId(), restored.getId());
            assertEquals(original.getAdminContact().getVcard().getFn().getText(),
                    restored.getAdminContact().getVcard().getFn().getText());
            assertEquals(original.getInterface().size(), restored.getInterface().size());
        }
    }

    /**
     * Preserves topology identity and relation count through generated Jakarta bindings.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void topologyDocumentRoundTrip() throws Exception {
        NmlParser parser = NmlParser.getInstance();
        try (InputStream source = getClass().getResourceAsStream("/nml.xml")) {
            assertNotNull(source);
            NmlTopologyType original = parser.readDocument(source);
            JAXBElement<NmlTopologyType> element = new net.es.nsi.common.jaxb.nml.ObjectFactory()
                    .createTopology(original);

            String xml = parser.jaxb2Xml(element);
            NmlTopologyType restored = parser.xml2Jaxb(NmlTopologyType.class, xml);

            assertEquals(original.getId(), restored.getId());
            assertEquals(original.getRelation().size(), restored.getRelation().size());
        }
    }

    /**
     * Confirms the generated signature binding can marshal and unmarshal its schema root.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void signatureDocumentRoundTrip() throws Exception {
        JaxbParser parser = new JaxbParser("net.es.nsi.common.jaxb.signatures");
        SignatureType original = new SignatureType();
        original.setId("signature-1");

        String xml = parser.jaxb2Xml(new ObjectFactory().createSignature(original));
        SignatureType restored = parser.xml2Jaxb(SignatureType.class, xml);

        assertEquals("signature-1", restored.getId());
    }
}
