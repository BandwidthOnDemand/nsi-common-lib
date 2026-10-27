package net.es.nsi.common;

import java.io.IOException;
import java.util.List;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.NmlParser;
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
 * Fixture-based regression coverage for extracting bidirectional topology ports.
 *
 * @author hacksaw
 */
public class NmlTest {
  /**
   * Repository topology fixture used by this suite.
   */
  private static final String NML_DOCUMENT = "src/test/resources/nml.xml";
  /**
   * Legacy output-path constant retained with the existing fixture.
   */
  private static final String TARGET_FILE = "target/nml-test.xml";

  /**
   * Fresh mutable topology model owned by this test instance.
   */
  NmlTopologyType nmlType;

  /**
   * Loads a fresh topology fixture for each test instance.
   *
   * @throws JAXBException if fixture preparation or the exercised API fails unexpectedly
   * @throws IOException if fixture preparation or the exercised API fails unexpectedly
   */
  public NmlTest() throws JAXBException, IOException {
    nmlType = NmlParser.getInstance().readDocument(NML_DOCUMENT);
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
   * Checks the fixture port count and a known bidirectional port identifier.
   *
   * @throws Exception if fixture preparation or the exercised API fails unexpectedly
   */
  @Test
  public void bidirectionalPortTest() throws Exception {
    Nml nml = new Nml(nmlType);
    List<NmlBidirectionalPortType> ports = nml.getBidirectionalPorts();

    assertEquals(36, ports.size());

    // Find a specific port.
    boolean found = false;
    for (NmlBidirectionalPortType p : ports) {
      if ("urn:ogf:network:es.net:2013::nersc-mr2:xe-7_3_0:+".equals(p.getId())) {
        found = true;
        break;
      }
    }
    assertTrue(found);
  }

  /**
   * Asserts the topology identifier and bound service-definition and Ethernet extensions.
   *
   * @param nml parsed topology fixture
   */
  private void validateDocument(NmlTopologyType nml) {
    // Verify we have the expected topologyId from NML document namespace.
    assertEquals("urn:ogf:network:es.net:2013:", nml.getId());
    boolean found = false;
    for (NmlNetworkObject group : nml.getGroup()) {
      if (group instanceof NmlBidirectionalPortType) {
        System.out.println(group.getClass().getCanonicalName());

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

  /**
   * Legacy unused placeholder; throws UnsupportedOperationException when invoked.
   */
  private void getBidirectionalPorts() {
    throw new UnsupportedOperationException("Not supported yet.");
  }
}
