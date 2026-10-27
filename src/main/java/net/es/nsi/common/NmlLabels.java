package net.es.nsi.common;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import static net.es.nsi.common.NmlEthernet.isVlanLabel;
import net.es.nsi.common.jaxb.nml.NmlLabelGroupType;
import net.es.nsi.common.jaxb.nml.NmlLabelType;

/**
 * Converts NML VLAN label groups and compares JAXB label values semantically.
 *
 * <p>Generated JAXB model classes use object identity by default. The comparison and
 * intersection helpers compare label type and value instead, without modifying inputs.
 *
 * @author hacksaw
 */
public class NmlLabels {

  /**
   * Expands a VLAN label group into distinct, validated numeric labels.
   *
   * @param labelGroup non-null VLAN group with a non-empty value specification
   * @return new JAXB labels in first-occurrence order
   * @throws IllegalArgumentException if the group type, syntax, bounds, or range size is unsupported
   */
  public static Set<NmlLabelType> labelGroupToLabels(NmlLabelGroupType labelGroup) {

    Optional<String> labelType = Optional.ofNullable(labelGroup.getLabeltype());
    if (!isVlanLabel(labelType)) {
      throw new IllegalArgumentException("Invalid vlan label: " + labelGroup.getLabeltype());
    }

    String values = labelGroup.getValue();
    if (values == null || values.isBlank() || values.contains(";") || values.contains("=")) {
      throw new IllegalArgumentException("Invalid vlan values: " + values);
    }
    Set<NmlLabelType> labels = new LinkedHashSet<>();
    for (SimpleLabel value : SimpleLabels.fromString("vlan=" + values)) {
      NmlLabelType label = new NmlLabelType();
      label.setLabeltype(labelGroup.getLabeltype());
      label.setValue(value.getValue());
      labels.add(label);
    }

    return labels;
  }

  /**
   * Intersects inbound and outbound labels using semantic type/value comparison.
   *
   * <p>Returned members are the original inbound JAXB objects. A null input set
   * produces an empty intersection.
   *
   * @param inLabels inbound labels, or {@code null}
   * @param outLabels outbound labels, or {@code null}
   * @return a new mutable set containing matching inbound members
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
   * Compares nullable labels by type and value, ignoring case.
   *
   * @param a first value, or {@code null}
   * @param b second value, or {@code null}
   * @return whether the values match, including two null values
   */
  public static boolean labelEquals(NmlLabelType a, NmlLabelType b) {
    if (a == null && b == null) {
      return true;
    } else if (a == null) {
      return false;
    } else if (b == null) {
      return false;
    } else if (!same(a.getLabeltype(), b.getLabeltype())) {
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
   * Compares nullable label groups by type and value, ignoring case.
   *
   * <p>Group range strings are compared as text; equivalent range expansions are not inferred.
   *
   * @param a first value, or {@code null}
   * @param b second value, or {@code null}
   * @return whether the values match, including two null values
   */
  public static boolean labelEquals(NmlLabelGroupType a, NmlLabelGroupType b) {
    if (a == null && b == null) {
      return true;
    } else if (a == null) {
      return false;
    } else if (b == null) {
      return false;
    } else if (!same(a.getLabeltype(), b.getLabeltype())) {
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
   * Compares optional type names without dereferencing an absent name.
   *
   * @param first first name
   * @param second second name
   * @return whether both are null or equal ignoring case
   */
  private static boolean same(String first, String second) {
    return first == null ? second == null : first.equalsIgnoreCase(second);
  }
}
