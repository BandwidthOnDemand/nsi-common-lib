package net.es.nsi.common;

import lombok.Builder;
import lombok.Data;

/**
 * Describes a numeric label family and its inclusive bounds.
 *
 * <p>Lombok supplies the builder, accessors, and value methods. Instances remain mutable;
 * changing a registered descriptor changes the validation policy used by its labels.
 *
 * @author hacksaw
 */
@Builder
@Data
public class LabelType {
  /**
   * Canonical label-family name, such as {@code vlan} or {@code mpls}.
   */
  private String label;
  /**
   * Smallest permitted numeric value, inclusive.
   */
  private int min;
  /**
   * Largest permitted numeric value, inclusive.
   */
  private int max;
}
