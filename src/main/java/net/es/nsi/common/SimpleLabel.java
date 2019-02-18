package net.es.nsi.common;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hacksaw
 */
public class SimpleLabel {
    public static final String NSI_EVTS_LABEL_TYPE = "vlan";
    public static final int NSI_EVTS_LABEL_MIN = 0;
    public static final int NSI_EVTS_LABEL_MAX = 4095;

    public static final String NSI_MPLS_LABEL_TYPE = "mpls";
    public static final int NSI_MPLS_LABEL_MIN = 0;
    public static final int NSI_MPLS_LABEL_MAX = 1048575;

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

    public final static String HASH = "#";
    public final static String QUESTION = "?";
    public final static String EQUALS = "=";
    public final static String HYPHEN = "-";
    public final static String COMMA = ",";
    public final static String LABELTYPE_SEPARATOR = ";";

    private LabelType type;
    private String value;

    public SimpleLabel() {

    }

    public SimpleLabel(String type, String value) throws IllegalArgumentException {
      setType(type);
      setValue(value);
    }

    /**
     * @return the type
     */
    public String getType() {
      if (type == null) {
        return null;
      }
      return type.getLabel();
    }

    /**
     * @param type the type to set
     */
    final public void setType(String type) throws IllegalArgumentException {
      this.type = LABELS.get(type);
      if (this.type == null) {
        throw new IllegalArgumentException("Unknown label type " + type);
      }

      // If we have a value make sure it is not out of range.
      if (!Strings.isNullOrEmpty(value)) {
        int val = Integer.parseInt(value);
        if (val < this.type.getMin() || val > this.type.getMax()) {
          throw new IllegalArgumentException("Label value of type " + this.type.getLabel() + " is out of range " + value);
        }
      }
    }

    /**
     * @return the value
     */
    public String getValue() {
      return value;
    }

    /**
     * @param value the value to set
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
      this.value = value;
    }

    @Override
    public String toString() {
        return type.getLabel() + "=" + value;
    }

    @Override
    public boolean equals(Object object){
      if (this.type == null) {
        return false;
      }

        if (object == this) {
            return true;
        }

        if((object == null) || (object.getClass() != this.getClass())) {
            return false;
        }

        SimpleLabel that = (SimpleLabel) object;
        if (this.type.getLabel() == null && that.getType() == null) {
            return true;
        }

        if (this.type.getLabel() == null && that.getType() != null) {
            return false;
        }

        if (this.type.getLabel() != null && that.getType() == null) {
            return false;
        }

        if (!this.type.getLabel().equalsIgnoreCase(this.getType())) {
            return false;
        }

        if (this.value == null && that.getValue() == null) {
            return true;
        }

        if (this.value == null && that.getValue() != null) {
            return false;
        }

        if (this.value != null && that.getValue() == null) {
            return false;
        }

        return this.value.equalsIgnoreCase(this.getValue());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((type == null) ? 0 : type.hashCode());
        result = prime * result
                + ((value == null) ? 0 : value.hashCode());
        return result;
    }
}
