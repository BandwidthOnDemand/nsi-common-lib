package net.es.nsi.common.jaxb;

import jakarta.xml.bind.JAXBElement;
import net.es.nsi.common.Nml;
import net.es.nsi.common.jaxb.nml.NmlBidirectionalPortType;
import net.es.nsi.common.jaxb.nml.NmlNetworkObject;
import net.es.nsi.common.jaxb.nml.NmlPortGroupType;
import net.es.nsi.common.jaxb.nml.NmlTopologyRelationType;
import net.es.nsi.common.jaxb.nml.NmlTopologyType;
import net.es.nsi.common.jaxb.nml.ServiceDefinitionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * File round-trip tests for topology documents and their imported JAXB schemas.
 *
 * @author hacksaw
 */
public class NmlParserTest {
  /**
   * Repository topology XML fixture.
   */
  private static final String NML_DOCUMENT = "src/test/resources/nml.xml";
  /**
   * JUnit-managed directory that isolates file output between tests.
   */
  @org.junit.jupiter.api.io.TempDir
  java.nio.file.Path directory;

  /**
   * Creates the test fixture; no constructor setup is required.
   */
  public NmlParserTest() {
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
   * Reads the repository topology fixture and checks its imported schema content.
   *
   * @throws Exception if fixture preparation or the exercised API fails unexpectedly
   */
  @Test
  public void testReadDocument() throws Exception {
    NmlTopologyType nml = NmlParser.getInstance().readDocument(NML_DOCUMENT);
    validateDocument(nml);
  }

  /**
   * Writes the topology fixture to an isolated temporary file and validates it after rereading.
   *
   * @throws Exception if fixture preparation or the exercised API fails unexpectedly
   */
  @Test
  public void testWriteDocument() throws Exception {
    NmlTopologyType nml = NmlParser.getInstance().readDocument(NML_DOCUMENT);
    String TARGET_FILE = directory.resolve("nml.xml").toString();
    NmlParser.getInstance().writeDocument(TARGET_FILE, nml);
    nml = NmlParser.getInstance().readDocument(TARGET_FILE);
    validateDocument(nml);
  }

  /**
   * Asserts representative topology fields and objects from imported schemas.
   *
   * @param nml parsed topology fixture
   */
  private void validateDocument(NmlTopologyType nml) {
    // Check the topology identity before inspecting imported extension bindings.
    assertEquals("urn:ogf:network:es.net:2013:", nml.getId());
    boolean found = false;
    for (NmlNetworkObject group : nml.getGroup()) {
      if (group instanceof NmlBidirectionalPortType) {
        if ("urn:ogf:network:es.net:2013::nersc-mr2:xe-7_3_0:+".equals(group.getId())) {
          found = true;
          break;
        }
      }
    }
    assertTrue(found);

    // Check that the imported service-definition element has its generated Java type.
    found = false;
    for (Object obj : nml.getAny()) {
      if (obj instanceof JAXBElement) {
        JAXBElement jaxb = (JAXBElement) obj;
        if (jaxb.getDeclaredType() == ServiceDefinitionType.class) {
          ServiceDefinitionType sd = (ServiceDefinitionType) jaxb.getValue();
          if ("urn:ogf:network:es.net:2013::ServiceDefinition:EVTS.A-GOLE".equals(sd.getId())) {
            found = true;
            break;
          }
        }
      }
    }
    assertTrue(found);

    // Check that the Ethernet extension retains its namespace and numeric Java type.
    found = false;
    for (NmlTopologyRelationType relation : nml.getRelation()) {
      if (Nml.hasInboundPort(relation.getType())) {
        for (NmlPortGroupType pg : relation.getPortGroup()) {
          if ("urn:ogf:network:es.net:2013::nersc-mr2:xe-7_3_0:+:in".equals(pg.getId())) {
            for (Object obj :pg.getAny()) {
              if (obj instanceof JAXBElement) {
                JAXBElement jaxb = (JAXBElement) obj;
                if (Nml.MaximumReservableCapacity_QNAME.equals(jaxb.getName())) {
                  if (jaxb.getValue() instanceof Long) {
                    Long max = (Long) jaxb.getValue();
                    if (max == 10000000000L) {
                      found = true;
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
    assertTrue(found);
  }
}
