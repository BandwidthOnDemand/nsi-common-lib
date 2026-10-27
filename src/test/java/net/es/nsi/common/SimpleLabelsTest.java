package net.es.nsi.common;

import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

/**
 * Exercises complete supported label domains and their upper-bound validation.
 *
 * @author hacksaw
 */
public class SimpleLabelsTest {
  /**
   * Expands the full VLAN range and checks the type of every member.
   */
  @Test
  public void testVlan() {
    Set<SimpleLabel> l1 = SimpleLabels.fromString("vlan=0-4095");
    assertEquals("0", l1.stream().findFirst().get().getValue());
    assertEquals(4096, l1.size());
    for (SimpleLabel label : l1) {
      assertEquals(SimpleLabel.NSI_EVTS_LABEL_TYPE, label.getType());
    }
  }

  /**
   * Rejects a VLAN range extending beyond the supported maximum.
   */
  @Test
  public void illegalVlanHigh() {
    assertThrows(IllegalArgumentException.class, () -> SimpleLabels.fromString("vlan=0-4096"));
  }

  /**
   * Expands the complete MPLS domain, including the largest permitted bounded range.
   */
  @Test
  public void testMpls() {
    Set<SimpleLabel> l1 = SimpleLabels.fromString("mpls=0-1048575");
    assertEquals(1048576, l1.size());
    assertEquals("0", l1.stream().findFirst().get().getValue());
    for (SimpleLabel label : l1) {
      assertEquals(SimpleLabel.NSI_MPLS_LABEL_TYPE, label.getType());
    }
  }

  /**
   * Rejects an MPLS range extending beyond the supported maximum.
   */
  @Test
  public void illegalMpsHigh() {
    assertThrows(IllegalArgumentException.class, () -> SimpleLabels.fromString("mpls=0-1048576"));
  }
}
