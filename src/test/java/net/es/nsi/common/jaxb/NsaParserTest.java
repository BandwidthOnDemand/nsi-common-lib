package net.es.nsi.common.jaxb;

import java.io.IOException;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.nsa.HolderType;
import net.es.nsi.common.jaxb.nsa.NsaType;
import net.es.nsi.common.jaxb.nsa.TopologyReachabilityType;
import net.es.nsi.common.jaxb.nsa.TopologyType;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class NsaParserTest {
  private static final String NSA_DOCUMENT = "src/test/resources/nsa.xml";
  private static final String TARGET_FILE = "target/nsa-test.xml";

  public NsaParserTest() {
  }

  @BeforeClass
  public static void setUpClass() {
  }

  @AfterClass
  public static void tearDownClass() {
  }

  @Before
  public void setUp() {
  }

  @After
  public void tearDown() {
  }

  /**
   * Test of readTopology method, of class NsaParser.
   * @throws jakarta.xml.bind.JAXBException
   * @throws java.io.IOException
   */
  @Test
  public void testReadDocument() throws JAXBException, IOException {
    NsaType nsa = NsaParser.getInstance().readDocument(NSA_DOCUMENT);
    validateDocument(nsa);
  }

  /**
   * Test of writeTopology method, of class NsaParser.
   * @throws jakarta.xml.bind.JAXBException
   * @throws java.io.IOException
   */
  @Test
  public void testWriteDocument() throws JAXBException, IOException {
    NsaType nsa = NsaParser.getInstance().readDocument(NSA_DOCUMENT);
    NsaParser.getInstance().writeDocument(TARGET_FILE, nsa);
    nsa = NsaParser.getInstance().readDocument(TARGET_FILE);
    validateDocument(nsa);
  }

  private void validateDocument(NsaType nsa) {
    // Verify we have the expected nsaId from NSA document namespace.
    assertEquals("urn:ogf:network:es.net:2013:nsa:nsi-aggr-west", nsa.getId());

    // Verify the VCard schema was properly included.
    assertEquals("Chin Guok", nsa.getAdminContact().getVcard().getFn().getText());

    // Verify the GangOfThree routing schema is included.
    boolean found = false;
    List<HolderType> other = nsa.getOther();
    for (HolderType holder : other) {
      for (Object obj : holder.getAny()) {
        if (obj instanceof JAXBElement) {
          JAXBElement jaxb = (JAXBElement) obj;
          if (jaxb.getDeclaredType() == TopologyReachabilityType.class) {
            TopologyReachabilityType top = (TopologyReachabilityType) jaxb.getValue();
            for (TopologyType t : top.getTopology()) {
              if ("urn:ogf:network:sttlwa.pacificwave.net:2016:topology".equals(t.getId())) {
                found = true;
                break;
              }
            }
          }
        }
      }
    }
    assertTrue(found);
  }
}
