package net.es.nsi.common;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import static net.es.nsi.common.NmlEthernet.isVlanLabel;
import net.es.nsi.common.jaxb.nml.NmlLabelGroupType;
import net.es.nsi.common.jaxb.nml.NmlLabelType;

/**
 *
 * @author hacksaw
 */
public class NmlLabels {

  public static Set<NmlLabelType> labelGroupToLabels(NmlLabelGroupType labelGroup) {

    Optional<String> labelType = Optional.ofNullable(labelGroup.getLabeltype());
    if (!isVlanLabel(labelType)) {
      throw new IllegalArgumentException("Invalid vlan label: " + labelGroup.getLabeltype());
    }

    Set<NmlLabelType> labels = new LinkedHashSet<>();

    // Split the vlan first by comma, then by hyphen.
    Pattern pattern = Pattern.compile(",");
    String[] comma = pattern.split(labelGroup.getValue());
    for (int i = 0; i < comma.length; i++) {
      // Now by hyphen.
      pattern = Pattern.compile("-");
      String[] hyphen = pattern.split(comma[i]);

      // Just a single vlan.
      if (hyphen.length == 1) {
        NmlLabelType label = new NmlLabelType();
        label.setLabeltype(labelGroup.getLabeltype());
        label.setValue(hyphen[0]);
        labels.add(label);
      } // Two vlans in a range.
      else if (hyphen.length > 1 && hyphen.length < 3) {
        int min = Integer.parseInt(hyphen[0]);
        int max = Integer.parseInt(hyphen[1]);
        for (int j = min; j < max + 1; j++) {
          NmlLabelType label = new NmlLabelType();
          label.setLabeltype(labelGroup.getLabeltype());
          label.setValue(Integer.toString(j));
          labels.add(label);
        }
      } // This is unsupported.
      else {
        throw new IllegalArgumentException("Invalid vlan string format: " + labelGroup.getValue());
      }
    }
    return labels;
  }

  /**
   * Merge the labels from two uni ports into a single intersecting set for the bidirectional port.
   *
   * @param inLabels
   * @param outLabels
   * @return
   */
  public static Set<NmlLabelType> mergeLabels(Set<NmlLabelType> inLabels, Set<NmlLabelType> outLabels) {
    Set<NmlLabelType> results = new LinkedHashSet<>();
    if (inLabels != null && outLabels != null) {
      for (NmlLabelType inLabel : inLabels) {
        for (NmlLabelType outLabel : outLabels) {
          if (labelEquals(inLabel, outLabel)) {
            results.add(inLabel);
            break;
          }
        }
      }
    }

    return results;
  }

  /**
   *
   * @param a
   * @param b
   * @return
   */
  public static boolean labelEquals(NmlLabelType a, NmlLabelType b) {
    if (a == null && b == null) {
      return true;
    } else if (a == null) {
      return false;
    } else if (b == null) {
      return false;
    } else if (!a.getLabeltype().equalsIgnoreCase(b.getLabeltype())) {
      return false;
    }

    if (a.getValue() == null && b.getValue() == null) {
      return true;
    } else if (b.getValue() == null) {
      return false;
    } else if (a.getValue() == null) {
      return false;
    } else if (!a.getValue().equalsIgnoreCase(b.getValue())) {
      return false;
    }

    return true;
  }

  /**
   *
   * @param a
   * @param b
   * @return
   */
  public static boolean labelEquals(NmlLabelGroupType a, NmlLabelGroupType b) {
    if (a == null && b == null) {
      return true;
    } else if (a == null) {
      return false;
    } else if (b == null) {
      return false;
    } else if (!a.getLabeltype().equalsIgnoreCase(b.getLabeltype())) {
      return false;
    }

    if (a.getValue() == null && b.getValue() == null) {
      return true;
    } else if (b.getValue() == null) {
      return false;
    } else if (a.getValue() == null) {
      return false;
    } else if (!a.getValue().equalsIgnoreCase(b.getValue())) {
      return false;
    }

    return true;
  }
}
