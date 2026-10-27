package net.es.nsi.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * Boundary tests for individual VLAN and MPLS labels and validated type changes.
 *
 * @author hacksaw
 */
public class SimpleLabelTest {

  /**
   * Checks both VLAN endpoints and a valid value mutation.
   */
  @Test
  public void testVlan() {
    SimpleLabel l1 = new SimpleLabel(SimpleLabel.NSI_EVTS_LABEL_TYPE, "0");
    assertEquals(SimpleLabel.NSI_EVTS_LABEL_TYPE, l1.getType());
    assertEquals("0", l1.getValue());

    SimpleLabel l2 = new SimpleLabel(SimpleLabel.NSI_EVTS_LABEL_TYPE, "4095");
    assertEquals(SimpleLabel.NSI_EVTS_LABEL_TYPE, l2.getType());
    assertEquals("4095", l2.getValue());

    SimpleLabel l3 = new SimpleLabel(SimpleLabel.NSI_EVTS_LABEL_TYPE, "200");
    assertEquals(SimpleLabel.NSI_EVTS_LABEL_TYPE, l3.getType());
    assertEquals("200", l3.getValue());

    l3.setValue("300");
    assertEquals(SimpleLabel.NSI_EVTS_LABEL_TYPE, l3.getType());
    assertEquals("300", l3.getValue());
  }

  /**
   * Checks both MPLS endpoints and a valid value mutation.
   */
  @Test
  public void testMpls() {
    SimpleLabel l1 = new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "0");
    assertEquals(SimpleLabel.NSI_MPLS_LABEL_TYPE, l1.getType());
    assertEquals("0", l1.getValue());

    SimpleLabel l2 = new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "1048575");
    assertEquals(SimpleLabel.NSI_MPLS_LABEL_TYPE, l2.getType());
    assertEquals("1048575", l2.getValue());

    SimpleLabel l3 = new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "200");
    assertEquals(SimpleLabel.NSI_MPLS_LABEL_TYPE, l3.getType());
    assertEquals("200", l3.getValue());

    l3.setValue("300");
    assertEquals(SimpleLabel.NSI_MPLS_LABEL_TYPE, l3.getType());
    assertEquals("300", l3.getValue());
  }

  /**
   * Rejects the first value above the VLAN domain.
   */
  @Test
  public void illegalVlanHigh() {
    assertThrows(IllegalArgumentException.class,
            () -> new SimpleLabel(SimpleLabel.NSI_EVTS_LABEL_TYPE, "4096"));
  }

  /**
   * Rejects negative VLAN values.
   */
  @Test
  public void illegalVlanLow() {
    assertThrows(IllegalArgumentException.class,
            () -> new SimpleLabel(SimpleLabel.NSI_EVTS_LABEL_TYPE, "-1"));
  }

  /**
   * Rejects the first value above the MPLS domain.
   */
  @Test
  public void illegalMplsHigh() {
    assertThrows(IllegalArgumentException.class,
            () -> new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "1048576"));
  }

  /**
   * Rejects negative MPLS values.
   */
  @Test
  public void illegalMplsLow() {
    assertThrows(IllegalArgumentException.class,
            () -> new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "-1"));
  }

  /**
   * Rejects a label type outside the supported short names.
   */
  @Test
  public void illegalLabel() {
    assertThrows(IllegalArgumentException.class, () -> new SimpleLabel("poop", "999"));
  }

  /**
   * Rejects changing an MPLS label to VLAN when its value exceeds the VLAN domain.
   */
  @Test
  public void illegalValueforLabel() {
    SimpleLabel l2 = new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "1048575");
    assertThrows(IllegalArgumentException.class, () -> l2.setType(SimpleLabel.NSI_EVTS_LABEL_TYPE));
  }

  /**
   * Retains the numeric value when switching to another compatible label type.
   */
  @Test
  public void testLabelSwitch() {
    SimpleLabel l2 = new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "600");
    l2.setType(SimpleLabel.NSI_EVTS_LABEL_TYPE);
    assertEquals(SimpleLabel.NSI_EVTS_LABEL_TYPE, l2.getType());
    assertEquals("600", l2.getValue());
  }
}
