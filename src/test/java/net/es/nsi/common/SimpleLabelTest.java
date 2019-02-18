package net.es.nsi.common;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class SimpleLabelTest {

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

  @Test(expected = IllegalArgumentException.class)
  public void illegalVlanHigh() {
    SimpleLabel simpleLabel = new SimpleLabel(SimpleLabel.NSI_EVTS_LABEL_TYPE, "4096");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalVlanLow() {
    SimpleLabel simpleLabel = new SimpleLabel(SimpleLabel.NSI_EVTS_LABEL_TYPE, "-1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalMplsHigh() {
    SimpleLabel simpleLabel = new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "1048576");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalMplsLow() {
    SimpleLabel simpleLabel = new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "-1");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalLabel() {
    SimpleLabel simpleLabel = new SimpleLabel("poop", "999");
  }

  @Test(expected = IllegalArgumentException.class)
  public void illegalValueforLabel() {
    SimpleLabel l2 = new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "1048575");
    l2.setType(SimpleLabel.NSI_EVTS_LABEL_TYPE);
  }

  @Test
  public void testLabelSwitch() {
    SimpleLabel l2 = new SimpleLabel(SimpleLabel.NSI_MPLS_LABEL_TYPE, "600");
    l2.setType(SimpleLabel.NSI_EVTS_LABEL_TYPE);
    assertEquals(SimpleLabel.NSI_EVTS_LABEL_TYPE, l2.getType());
    assertEquals("600", l2.getValue());
  }
}
