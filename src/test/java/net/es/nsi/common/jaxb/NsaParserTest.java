package net.es.nsi.common.jaxb;

import java.io.IOException;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.nsa.HolderType;
import net.es.nsi.common.jaxb.nsa.NsaType;
import net.es.nsi.common.jaxb.nsa.TopologyReachabilityType;
import net.es.nsi.common.jaxb.nsa.TopologyType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * File round-trip tests for discovery documents and their imported JAXB schemas.
 *
 * @author hacksaw
 */
public class NsaParserTest {
  /**
   * Repository discovery XML fixture.
   */
  private static final String NSA_DOCUMENT = "src/test/resources/nsa.xml";
  /**
   * JUnit-managed directory that isolates file output between tests.
   */
  @org.junit.jupiter.api.io.TempDir
  java.nio.file.Path directory;

  /**
   * Creates the test fixture; no constructor setup is required.
   */
  public NsaParserTest() {
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
   * Reads the repository discovery fixture and checks its imported schema content.
   *
   * @throws JAXBException if fixture preparation or the exercised API fails unexpectedly
   * @throws IOException if fixture preparation or the exercised API fails unexpectedly
   */
  @Test
  public void testReadDocument() throws JAXBException, IOException {
    NsaType nsa = NsaParser.getInstance().readDocument(NSA_DOCUMENT);
    validateDocument(nsa);
  }

  /**
   * Writes the discovery fixture to an isolated temporary file and validates it after rereading.
   *
   * @throws JAXBException if fixture preparation or the exercised API fails unexpectedly
   * @throws IOException if fixture preparation or the exercised API fails unexpectedly
   */
  @Test
  public void testWriteDocument() throws JAXBException, IOException {
    NsaType nsa = NsaParser.getInstance().readDocument(NSA_DOCUMENT);
    String TARGET_FILE = directory.resolve("nsa.xml").toString();
    NsaParser.getInstance().writeDocument(TARGET_FILE, nsa);
    nsa = NsaParser.getInstance().readDocument(TARGET_FILE);
    validateDocument(nsa);
  }

  /**
   * Asserts representative discovery fields and objects from imported schemas.
   *
   * @param nsa parsed discovery fixture
   */
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
