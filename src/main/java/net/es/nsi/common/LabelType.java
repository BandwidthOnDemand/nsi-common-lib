package net.es.nsi.common;

import lombok.Builder;
import lombok.Data;

/**
 *
 * @author hacksaw
 */
@Builder
@Data
public class LabelType {
  private String label;
  private int min;
  private int max;
}
