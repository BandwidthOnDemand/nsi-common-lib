package net.es.nsi.common.jaxb;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.nsa.NsaType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests generic JAXB parsing of an NSA discovery fixture from characters and bytes.
 *
 * @author hacksaw
 */
public class JaxbParserTest {
  /**
   * Discovery XML including vCard and routing namespaces for binding compatibility.
   */
  private static final String document = "<ns4:nsa xmlns:ns4=\"http://schemas.ogf.org/nsi/2014/02/discovery/nsa\" xmlns:ns2=\"urn:ietf:params:xml:ns:vcard-4.0\" xmlns:ns3=\"http://nordu.net/namespaces/2013/12/gnsbod\" expires=\"2023-03-20T06:27:57.136-07:00\" id=\"urn:ogf:network:netherlight.net:2013:nsa:safnari\" version=\"2022-03-20T13:25:04.281Z\">\n" +
"<name>NetherLight Safnari</name>\n" +
"<softwareVersion>2.1.3-SNAPSHOT (502f04d)</softwareVersion>\n" +
"<startTime>2022-03-20T13:23:42.288</startTime>\n" +
"<adminContact>\n" +
"<ns2:vcard>\n" +
"<ns2:uid>\n" +
"<ns2:uri>https://agg.netherlight.net/nsi-v2/ConnectionServiceProvider#adminContact</ns2:uri>\n" +
"</ns2:uid>\n" +
"<ns2:prodid>\n" +
"<ns2:text>safnari </ns2:text>\n" +
"</ns2:prodid>\n" +
"<ns2:rev>\n" +
"<ns2:timestamp>20220320T132342Z</ns2:timestamp>\n" +
"</ns2:rev>\n" +
"<ns2:kind>\n" +
"<ns2:text>individual</ns2:text>\n" +
"</ns2:kind>\n" +
"<ns2:fn>\n" +
"<ns2:text>Hans Trompert</ns2:text>\n" +
"</ns2:fn>\n" +
"<ns2:n>\n" +
"<ns2:surname>Trompert</ns2:surname>\n" +
"<ns2:given>Hans</ns2:given>\n" +
"</ns2:n>\n" +
"</ns2:vcard>\n" +
"</adminContact>\n" +
"<location>\n" +
"<longitude>4.954585</longitude>\n" +
"<latitude>52.3567</latitude>\n" +
"</location>\n" +
"<interface>\n" +
"<type>application/vnd.ogf.nsi.dds.v1+xml</type>\n" +
"<href>https://dds.netherlight.net/dds</href>\n" +
"</interface>\n" +
"<interface>\n" +
"<type>application/vnd.ogf.nsi.cs.v2.requester+soap</type>\n" +
"<href>https://agg.netherlight.net/nsi-v2/ConnectionServiceRequester</href>\n" +
"</interface>\n" +
"<interface>\n" +
"<type>application/vnd.ogf.nsi.cs.v2.provider+soap</type>\n" +
"<href>https://agg.netherlight.net/nsi-v2/ConnectionServiceProvider</href>\n" +
"</interface>\n" +
"<feature type=\"vnd.ogf.nsi.cs.v2.role.aggregator\"/>\n" +
"<peersWith>urn:ogf:network:surf.nl:2020:nsa</peersWith>\n" +
"<peersWith>urn:ogf:network:dev.automation.surf.net:2017:nsa</peersWith>\n" +
"<peersWith>urn:ogf:network:surf.nl:2020:onsaclient</peersWith>\n" +
"<peersWith>urn:ogf:network:icair.org:2013:nsa</peersWith>\n" +
"<peersWith>urn:ogf:network:es.net:2013:nsa:nsi-aggr-west</peersWith>\n" +
"<peersWith>urn:ogf:network:sense-rm.es.net:2013:netherlight</peersWith>\n" +
"<peersWith>urn:ogf:network:ampath.net:2013:nsa</peersWith>\n" +
"<peersWith>urn:ogf:network:lsanca.pacificwave.net:2016:nsa</peersWith>\n" +
"<peersWith>urn:ogf:network:sttlwa.pacificwave.net:2016:nsa</peersWith>\n" +
"<peersWith>urn:ogf:network:snvaca.pacificwave.net:2016:nsa</peersWith>\n" +
"<peersWith>urn:ogf:network:tokyjp.pacificwave.net:2018:nsa</peersWith>\n" +
"<peersWith>urn:ogf:network:cipo.rnp.br:2014:nsa:safnari</peersWith>\n" +
"<peersWith>urn:ogf:network:canarie.ca:2017:nsa</peersWith>\n" +
"<peersWith>urn:ogf:network:calit2.optiputer.net:2020:nsa</peersWith>\n" +
"</ns4:nsa>";

  /**
   * Creates the test fixture; no constructor setup is required.
   */
  public JaxbParserTest() {
  }

  /**
   * No suite-wide initialization is required by this fixture.
   */
  @BeforeAll
  public static void setUpClass() {
  }

  /**
   * No suite-wide cleanup is required by this fixture.
   */
  @AfterAll
  public static void tearDownClass() {
  }

  /**
   * No additional per-test setup is required.
   */
  @BeforeEach
  public void setUp() {
  }

  /**
   * No additional per-test cleanup is required.
   */
  @AfterEach
  public void tearDown() {
  }

  /**
   * Checks that both input overloads bind the expected discovery identifier.
   *
   * @throws JAXBException if fixture preparation or the exercised API fails unexpectedly
   * @throws IllegalArgumentException if fixture preparation or the exercised API fails unexpectedly
   * @throws IOException if fixture preparation or the exercised API fails unexpectedly
   */
  @Test
  public void testXml2Jaxb() throws JAXBException, IllegalArgumentException, IOException {
    JaxbParser parser = new JaxbParser("net.es.nsi.common.jaxb.nsa");
    try {
      NsaType nsa = parser.xml2Jaxb(NsaType.class, document);
      assertEquals("urn:ogf:network:netherlight.net:2013:nsa:safnari", nsa.getId());
    }
    catch (JAXBException | IllegalArgumentException ex) {
      System.err.printf("Exception caught: %s", ex.getMessage());
      throw ex;
    }

    InputStream targetStream = new ByteArrayInputStream(document.getBytes());
    try {
      NsaType nsa = parser.xml2Jaxb(NsaType.class, targetStream);
      assertEquals("urn:ogf:network:netherlight.net:2013:nsa:safnari", nsa.getId());
    } catch (JAXBException | IllegalArgumentException | IOException ex) {
      System.err.printf("Exception caught: %s", ex.getMessage());
      throw ex;
    }

  }
}
