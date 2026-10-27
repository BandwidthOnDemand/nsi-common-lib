package net.es.nsi.common;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A mutable label with a registered family and an optional numeric value.
 *
 * <p>Values are validated against {@link #LABELS} and normalized to decimal strings.
 * The no-argument constructor also supports labels whose family or value has not yet
 * been assigned. Equality and hashing use the family name and normalized value.
 *
 * <p>Do not mutate a label while it is stored in a hash-based collection. Instances
 * and the shared descriptor registry are not thread-safe.
 *
 * @author hacksaw
 */
public class SimpleLabel {
    /**
     * Family name for Ethernet VLAN labels.
     */
    public static final String NSI_EVTS_LABEL_TYPE = "vlan";
    /**
     * Inclusive lower bound for VLAN labels.
     */
    public static final int NSI_EVTS_LABEL_MIN = 0;
    /**
     * Inclusive upper bound for VLAN labels.
     */
    public static final int NSI_EVTS_LABEL_MAX = 4095;

    /**
     * Family name for MPLS labels.
     */
    public static final String NSI_MPLS_LABEL_TYPE = "mpls";
    /**
     * Inclusive lower bound for MPLS labels.
     */
    public static final int NSI_MPLS_LABEL_MIN = 0;
    /**
     * Inclusive upper bound for MPLS labels.
     */
    public static final int NSI_MPLS_LABEL_MAX = 1048575;

    /**
     * Mutable registry of label-family descriptors, initially containing VLAN and MPLS families.
     *
     * <p>Registry updates require external coordination with all readers.
     */
    public final static Map<String, LabelType> LABELS = new HashMap<>();

    static {
        LABELS.put(NSI_EVTS_LABEL_TYPE,
                LabelType.builder()
                        .label(NSI_EVTS_LABEL_TYPE)
                        .min(NSI_EVTS_LABEL_MIN)
                        .max(NSI_EVTS_LABEL_MAX)
                        .build()
        );

        LABELS.put(NSI_MPLS_LABEL_TYPE,
                LabelType.builder()
                        .label(NSI_MPLS_LABEL_TYPE)
                        .min(NSI_MPLS_LABEL_MIN)
                        .max(NSI_MPLS_LABEL_MAX)
                        .build()
        );
    };

    /**
     * Separator between a namespace URI and its label-family fragment.
     */
    public final static String HASH = "#";
    /**
     * Separator between an STP identifier and its label specification.
     */
    public final static String QUESTION = "?";
    /**
     * Separator between a label family and its values.
     */
    public final static String EQUALS = "=";
    /**
     * Separator between the endpoints of an inclusive numeric range.
     */
    public final static String HYPHEN = "-";
    /**
     * Separator between values or ranges in one label family.
     */
    public final static String COMMA = ",";
    /**
     * Separator between label-family groups in a serialized set.
     */
    public final static String LABELTYPE_SEPARATOR = ";";

    /**
     * Registered descriptor for this label family, or null before assignment.
     */
    private LabelType type;
    /**
     * Normalized decimal value, or null when the label is unspecified.
     */
    private String value;

    /**
     * Creates a label with neither a family nor a value assigned.
     */
    public SimpleLabel() {

    }

    /**
     * Creates a validated label with a normalized decimal value.
     *
     * @param type registered family name
     * @param value decimal integer accepted by the family
     * @throws IllegalArgumentException if the family is unknown or the value is invalid or out of range
     */
    public SimpleLabel(String type, String value) throws IllegalArgumentException {
      setType(type);
      setValue(value);
    }

    /**
     * Returns the registered family name.
     *
     * @return the family name, or {@code null} before a family is assigned
     */
    public String getType() {
      if (type == null) {
        return null;
      }
      return type.getLabel();
    }

    /**
     * Changes the family after validating any existing value against its bounds.
     *
     * <p>A rejected change leaves the previous family intact.
     *
     * @param type registered family name
     * @throws IllegalArgumentException if the family is unknown or the current value is outside its bounds
     */
    final public void setType(String type) throws IllegalArgumentException {
      LabelType selected = LABELS.get(type);
      if (selected == null) {
        throw new IllegalArgumentException("Unknown label type " + type);
      }

      // Validate before assigning so a rejected family change preserves the old state.
      if (!Strings.isNullOrEmpty(value)) {
        int val = Integer.parseInt(value);
        if (val < selected.getMin() || val > selected.getMax()) {
          throw new IllegalArgumentException("Label value of type " + selected.getLabel() + " is out of range " + value);
        }
      }
      this.type = selected;
    }

    /**
     * Returns the normalized decimal value.
     *
     * @return the value, or {@code null} for an unspecified label
     */
    public String getValue() {
      return value;
    }

    /**
     * Validates and normalizes a numeric value without changing the family.
     *
     * @param value decimal integer to assign
     * @throws IllegalArgumentException if the family is unset or the value is invalid or out of range
     */
    final public void setValue(String value) throws IllegalArgumentException {
      if (type == null) {
        throw new IllegalArgumentException("Label type is not set.");
      }

      // At this point we only know about integer label types.
      int val = Integer.parseInt(value);
      if (val < type.getMin() || val > type.getMax()) {
        throw new IllegalArgumentException("Label value of type " + type.getLabel() + " is out of range " + value);
      }
      this.value = Integer.toString(val);
    }

    /**
     * Formats this label as {@code family=value}.
     *
     * @return the serialized label; an unset value is rendered as the text {@code null}
     * @throws NullPointerException if no family has been assigned
     */
    @Override
    public String toString() {
        return type.getLabel() + "=" + value;
    }

    /**
     * Compares the family and normalized value, including unassigned fields.
     *
     * @param object object to compare
     * @return whether both objects have the same concrete class, family, and value
     */
    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object == null || object.getClass() != getClass()) {
            return false;
        }
        SimpleLabel that = (SimpleLabel) object;
        return Objects.equals(getType(), that.getType()) && Objects.equals(value, that.value);
    }

    /**
     * Hashes the same family and value fields used by {@link #equals(Object)}.
     *
     * @return the hash of the current label state
     */
    @Override
    public int hashCode() {
        return Objects.hash(getType(), value);
    }
}
