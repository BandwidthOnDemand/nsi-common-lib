package net.es.nsi.common;

import java.util.Set;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class SimpleLabelsTest {
  @Test
  public void testVlan() {
    Set<SimpleLabel> l1 = SimpleLabels.fromString("vlan=0-4095");
    assertEquals("0", l1.stream().findFirst().get().getValue());
    assertEquals(4096, l1.size());
    for (SimpleLabel label : l1) {
      assertEquals(SimpleLabel.NSI_EVTS_LABEL_TYPE, label.getType());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalVlanHigh() {
    Set<SimpleLabel> l1 = SimpleLabels.fromString("vlan=0-4096");
  }

  @Test
  public void testMpls() {
    Set<SimpleLabel> l1 = SimpleLabels.fromString("mpls=0-1048575");
    assertEquals(1048576, l1.size());
    assertEquals("0", l1.stream().findFirst().get().getValue());
    for (SimpleLabel label : l1) {
      assertEquals(SimpleLabel.NSI_MPLS_LABEL_TYPE, label.getType());
    }
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalMpsHigh() {
    Set<SimpleLabel> l1 = SimpleLabels.fromString("mpls=0-1048576");
  }
}
