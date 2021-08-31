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
public class NmlTest {
  private static final String NML_DOCUMENT = "src/test/resources/nml.xml";
  private static final String TARGET_FILE = "target/nml-test.xml";

  NmlTopologyType nmlType;

  public NmlTest() throws JAXBException, IOException {
    nmlType = NmlParser.getInstance().readDocument(NML_DOCUMENT);
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
   * Test bidirectionalPort elements.
   *
   * @throws java.lang.Exception
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
   * Validates all associated NML schema are loaded properly.
   *
   * @param nml
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

    // Verify the ServiceDefinition schema is valid.
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

    // Verify Ethernet schema is valid.
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

  private void getBidirectionalPorts() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }
}
