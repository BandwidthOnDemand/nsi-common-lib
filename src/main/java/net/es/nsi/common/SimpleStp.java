package net.es.nsi.common;

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import net.es.nsi.common.jaxb.nml.NmlLabelGroupType;
import net.es.nsi.common.jaxb.nml.NmlPortGroupType;
import net.es.nsi.common.jaxb.nml.NmlPortType;

/**
 * A mutable NSI service termination point with an optional set of numeric labels.
 *
 * <p>The first six colon-separated components form the network identifier; remaining
 * components form the local identifier. A question mark introduces labels using
 * {@link SimpleLabels} syntax. Constructors validate supplied STP identifiers, while
 * component setters assign values directly.
 *
 * <p>Collection inputs are copied, but label objects remain shared. Equality compares
 * the canonical STP string ignoring case, and hashing uses the same Unicode folding.
 * Do not mutate an STP or its labels while it is a hash-based collection member.
 * Instances are not thread-safe.
 *
 * @author hacksaw
 */
public class SimpleStp {
  /**
   * URN prefix used by NSI network identifiers.
   */
  public static final String NSI_NETWORK_URN_PREFIX = "urn:ogf:network:";
  /**
   * Separator between network and local identifier components.
   */
  public static final String NSI_URN_SEPARATOR = ":";
  /**
   * Separator introducing an STP label specification.
   */
  public static final String NSI_LABEL_SEPARATOR = "?";
  /**
   * Number of colon-separated components belonging to the network identifier.
   */
  public static final int NSI_NETWORK_LENGTH = 6;

  /**
   * NML label-type URI mapped to the short MPLS family name.
   */
  public static final String NSI_MPLS_LABEL_URN = "http://nsi.nordu.net/nml#mpls";
  /**
   * NML label-type URI mapped to the short VLAN family name.
   */
  public static final String NSI_VLAN_LABEL_URN = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";

  /**
   * Network component, or {@code null} until assigned.
   */
  private String networkId;
  /**
   * Local component, or {@code null} until assigned.
   */
  private String localId;
  /**
   * Owned set of shared, mutable label members.
   */
  private Set<SimpleLabel> labels = new HashSet<>();

  /**
   * Literal question-mark separator used to split the optional label suffix.
   */
  private static Pattern questionPattern = Pattern.compile("\\?");
  /**
   * Colon separator used to extract network and local identifier components.
   */
  private static Pattern colonPattern = Pattern.compile(NSI_URN_SEPARATOR);

  /**
   * Creates an empty, underspecified STP whose identifier serializes as an empty string.
   */
  public SimpleStp() {
    networkId = null;
    localId = null;
  }

  /**
   * Parses an identifier and copies the supplied label set.
   *
   * <p>The label objects themselves are shared with the caller.
   *
   * @param stpId non-null, unlabelled NSI STP identifier
   * @param labels non-null set of labels
   * @throws IllegalArgumentException if the identifier prefix or local component is invalid
   */
  public SimpleStp(String stpId, Set<SimpleLabel> labels) throws IllegalArgumentException {
    parseId(stpId);
    this.labels = new HashSet<>(labels);
  }

  /**
   * Parses an identifier and associates one label object with it.
   *
   * @param stpId non-null, unlabelled NSI STP identifier
   * @param label label object to associate
   * @throws IllegalArgumentException if the identifier prefix or local component is invalid
   */
  public SimpleStp(String stpId, SimpleLabel label) throws IllegalArgumentException {
    parseId(stpId);
    this.labels.add(label);
  }

  /**
   * Parses an STP identifier and its optional label specification.
   *
   * <p>Null or empty input creates an empty STP. Multiple question-mark separators
   * are rejected.
   *
   * @param stpId STP identifier, optionally followed by {@code ?family=values}
   * @throws IllegalArgumentException if the identifier or label specification is invalid
   */
  public SimpleStp(String stpId) throws IllegalArgumentException {
    if (Strings.isNullOrEmpty(stpId)) {
      // Empty input represents an STP whose components have not been assigned yet.
      return;
    }

    String[] question = questionPattern.split(stpId, -1);
    if (question.length > 2) {
      throw new IllegalArgumentException("Invalid STP identifier: " + stpId);
    }
    parseId(question[0]);

    // Only the suffix carries labels; the identifier portion remains unchanged.
    if (question.length > 1) {
      try {
        this.labels = SimpleLabels.fromString(question[1]);
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("Invalid STP identifier: " + stpId, ex);
      }
    }
  }

  /**
   * Builds an STP from an NML port and expands its supported labels.
   *
   * @param nmlPort non-null source port
   * @throws IllegalArgumentException if identifiers or label types are unsupported
   */
  public SimpleStp(NmlPortType nmlPort) throws IllegalArgumentException {
    parseId(nmlPort.getId());

    // Generate the STP identifiers associated with each port and label.
    if (nmlPort.getLabel() != null) {
      String stpId = createStpId(nmlPort);
      addStpId(stpId);
    }
  }

  /**
   * Builds an STP from an NML port group and expands its supported labels.
   *
   * @param nmlPort non-null source port group
   * @throws IllegalArgumentException if identifiers or label types are unsupported
   */
  public SimpleStp(NmlPortGroupType nmlPort) throws IllegalArgumentException {
    parseId(nmlPort.getId());

    // Generate the STP identifiers associated with each port and label.
    if (!nmlPort.getLabelGroup().isEmpty()) {
      String stpId = createStpId(nmlPort);
      addStpId(stpId);
    }
  }

  /**
   * Builds an STP string from a single NML port label.
   *
   * <p>Recognized VLAN/MPLS namespace fragments become short family names. A missing
   * label or label type leaves the port identifier unlabelled.
   *
   * @param pt non-null source port with an identifier
   * @return the identifier and any supported label suffix
   * @throws IllegalArgumentException if a non-empty label type is unsupported
   */
  public static String createStpId(NmlPortType pt) {
    // We need to build a label component for the STP Id.  NML uses label
    // types with a namespace and a # before the label type.  We want only
    // the label type string.
    StringBuilder identifier = new StringBuilder(pt.getId());
    if (pt.getLabel() != null) {
      String labelType = pt.getLabel().getLabeltype();
      if (labelType != null && !labelType.isEmpty()) {
        if (!NSI_MPLS_LABEL_URN.equalsIgnoreCase(labelType)  &&
                !NSI_VLAN_LABEL_URN.equalsIgnoreCase(labelType)) {
          throw new IllegalArgumentException("NmlPortType has unknown label type: " + labelType);
        }

        int pos = labelType.lastIndexOf("#");
        String type = labelType.substring(pos + 1);
        if (type != null && !type.isEmpty()) {
          identifier.append("?");
          identifier.append(type);

          if (pt.getLabel().getValue() != null && !pt.getLabel().getValue().isEmpty()) {
            identifier.append("=");
            identifier.append(pt.getLabel().getValue());
          }
        }
      }
    }

    return identifier.toString();
  }

  /**
   * Builds an STP string from a port group containing at most one label group.
   *
   * @param pg non-null source port group with an identifier
   * @return the identifier and any supported group label suffix
   * @throws IllegalArgumentException if multiple groups or an unsupported label type are present
   */
  public static String createStpId(NmlPortGroupType pg) throws IllegalArgumentException {
    if (pg.getLabelGroup().size() > 1) {
      // This code only handles a single label group at the moment.
      throw new IllegalArgumentException("NmlLabelGroupType has multiple label entries: " + pg.getId());
    }

    // We need to build a label component for the STP Id.  NML uses label
    // types with a namespace and a # before the label type.  We want only
    // the label type string.
    StringBuilder identifier = new StringBuilder(pg.getId());
    for (NmlLabelGroupType lgt : pg.getLabelGroup()) {
      String labelType = lgt.getLabeltype();
      if (labelType != null && !labelType.isEmpty()) {
        if (!NSI_MPLS_LABEL_URN.equalsIgnoreCase(labelType)  &&
                !NSI_VLAN_LABEL_URN.equalsIgnoreCase(labelType)) {
          throw new IllegalArgumentException("NmlPortGroupType has unknown label type: " + labelType);
        }

        int pos = labelType.lastIndexOf("#");
        String type = labelType.substring(pos + 1);
        if (type != null && !type.isEmpty()) {
          identifier.append("?");
          identifier.append(type);

          if (lgt.getValue() != null && !lgt.getValue().isEmpty()) {
            identifier.append("=");
            identifier.append(lgt.getValue());
          }
        }
      }
    }

    return identifier.toString();
  }

  /**
   * Adds labels from another rendering of this STP, initializing its ID if necessary.
   *
   * @param stpId identifier with an optional label suffix
   * @throws IllegalArgumentException if the supplied suffix or required identifier is invalid
   */
  private void addStpId(String stpId) {
    if (Strings.isNullOrEmpty(stpId)) {
      return;
    }

    String[] question = questionPattern.split(stpId, -1);
    if (question.length > 2) {
      throw new IllegalArgumentException("Invalid STP identifier: " + stpId);
    }

    if (networkId == null) {
      parseId(question[0]);
    }

    // Only the suffix carries labels; the identifier portion remains unchanged.
    if (question.length > 1) {
      try {
        this.labels.addAll(SimpleLabels.fromString(question[1]));
      } catch (IllegalArgumentException ex) {
        throw new IllegalArgumentException("Invalid stpId: " + stpId, ex);
      }
    }
  }

  /**
   * Checks whether the STP lacks exactly one concrete label value.
   *
   * @return {@code true} for zero labels, multiple labels, or a label with no value
   */
  public boolean isUnderSpecified() {
    return labels.size() != 1 || labels.iterator().next().getValue() == null;
  }

  /**
   * Copies up to the first six colon-separated components of an identifier.
   *
   * <p>This extraction helper does not validate the NSI URN prefix; constructors
   * perform that validation.
   *
   * @param id non-null colon-separated identifier
   * @return the extracted network component
   */
  public static String parseNetworkId(String id) {
    StringBuilder sb = new StringBuilder();
    String[] components = colonPattern.split(id, -1);

    for (int i = 0; i < NSI_NETWORK_LENGTH && i < components.length; i++) {
      sb.append(components[i]);
      sb.append(NSI_URN_SEPARATOR);
    }

    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  /**
   * Extracts all components after the six-component network identifier.
   *
   * @param id non-null unlabelled identifier
   * @return the local component, preserving internal colon separators
   * @throws IllegalArgumentException if no non-empty local component is present
   */
  public static String parseLocalId(String id) throws IllegalArgumentException {
    StringBuilder sb = new StringBuilder();
    String[] components = colonPattern.split(id, -1);

    if (components.length <= NSI_NETWORK_LENGTH || components[components.length - 1].isEmpty()) {
      throw new IllegalArgumentException("STP missing local id component: " + id);
    }

    for (int i = NSI_NETWORK_LENGTH; i < components.length; i++) {
      sb.append(components[i]);
      sb.append(NSI_URN_SEPARATOR);
    }

    sb.deleteCharAt(sb.length() - 1);
    return sb.toString();
  }

  /**
   * Validates the NSI prefix and stores the network and local components.
   *
   * @param id unlabelled identifier
   * @throws IllegalArgumentException if the prefix is invalid or the local component is missing
   */
  private void parseId(String id) throws IllegalArgumentException {
    if (id == null || !id.regionMatches(true, 0, NSI_NETWORK_URN_PREFIX, 0, NSI_NETWORK_URN_PREFIX.length())) {
      throw new IllegalArgumentException("Invalid STP identifier: " + id);
    }
    StringBuilder nsb = new StringBuilder();
    String[] components = colonPattern.split(id, -1);

    if (components.length <= NSI_NETWORK_LENGTH || components[components.length - 1].isEmpty()) {
      throw new IllegalArgumentException("STP missing local id component: " + id);
    }

    for (int i = 0; i < NSI_NETWORK_LENGTH && i < components.length; i++) {
      nsb.append(components[i]);
      nsb.append(NSI_URN_SEPARATOR);
    }

    nsb.deleteCharAt(nsb.length() - 1);
    this.networkId = nsb.toString();

    StringBuilder lsb = new StringBuilder();
    for (int i = NSI_NETWORK_LENGTH; i < components.length; i++) {
      lsb.append(components[i]);
      lsb.append(NSI_URN_SEPARATOR);
    }

    lsb.deleteCharAt(lsb.length() - 1);
    this.localId = lsb.toString();
  }

  /**
   * Returns the local identifier component.
   *
   * @return the local identifier, or {@code null} before assignment
   */
  public String getLocalId() {
    return localId;
  }

  /**
   * Assigns the local component directly, without validating its syntax.
   *
   * @param id new local component, possibly {@code null}
   */
  public void setLocalId(String id) {
    this.localId = id;
  }

  /**
   * Returns the network identifier component.
   *
   * @return the network identifier, or {@code null} before assignment
   */
  public String getNetworkId() {
    return networkId;
  }

  /**
   * Removes the NSI URN prefix from the assigned network identifier.
   *
   * @return the network suffix after {@code urn:ogf:network:}
   */
  public String getNetworkLabel() {
    return networkId.substring(NSI_NETWORK_URN_PREFIX.length());
  }

  /**
   * Assigns the network component directly, without validating its syntax.
   *
   * @param id new network component, possibly {@code null}
   */
  public void setNetworkId(String id) {
    this.networkId = id;
  }

  /**
   * Serializes the current identifier and sorted label groups.
   *
   * @return the STP string; an empty STP with no labels produces an empty string
   */
  public String getStpId() {
    StringBuilder sb = new StringBuilder(getId());

    if (!labels.isEmpty()) {
      sb.append(NSI_LABEL_SEPARATOR);
      sb.append(SimpleLabels.toString(labels));
    }

    return sb.toString();
  }

  /**
   * Combines the network and local components without labels.
   *
   * @return the unlabelled identifier, or an empty string if either component is null
   */
  public String getId() {
    if (networkId == null || localId == null) {
      return "";
    }
    StringBuilder sb = new StringBuilder(networkId);
    sb.append(NSI_URN_SEPARATOR);
    sb.append(localId);
    return sb.toString();
  }

  /**
   * Removes the label suffix from a non-null STP string.
   *
   * @param stpId STP string to split at the first question mark
   * @return the unlabelled prefix
   */
  public static String getId(String stpId) {
    String[] components = questionPattern.split(stpId);
    return components[0];
  }

  /**
   * Builds one STP string for each label member.
   *
   * @return a new mutable list, or one unlabelled identifier when there are no labels
   */
  public List<String> getMemberStpId() {
    List<String> result = new ArrayList<>();
    if (labels.isEmpty()) {
      result.add(getId());
    } else {
      labels.stream().map((label) -> {
        StringBuilder sb = new StringBuilder(getId());
        sb.append(NSI_LABEL_SEPARATOR);
        sb.append(label.toString());
        return sb;
      }).forEach((sb) -> {
        result.add(sb.toString());
      });
    }
    return result;
  }

  /**
   * Exposes a read-only live view of the owned label set.
   *
   * <p>Members remain mutable; callers must not change them while they are set members.
   *
   * @return an unmodifiable view, not a snapshot
   */
  public Set<SimpleLabel> getLabels() {
    return Collections.unmodifiableSet(labels);
  }

  /**
   * Replaces all labels with a snapshot of the supplied set.
   *
   * <p>A snapshot is taken before clearing, so this method also accepts its own live view.
   *
   * @param labels non-null replacement label set; members are not cloned
   */
  public void setLabels(Set<SimpleLabel> labels) {
    Set<SimpleLabel> copy = new HashSet<>(labels);
    this.labels.clear();
    this.labels.addAll(copy);
  }

  /**
   * Retains only labels present in the supplied set.
   *
   * @param labels non-null labels to retain
   * @return whether the owned set changed
   */
  public boolean intersectLabels(Set<SimpleLabel> labels) {
    return this.labels.retainAll(labels);
  }

  /**
   * Adds a shared label object to the owned set.
   *
   * @param label typed label to add
   */
  public void addLabel(SimpleLabel label) {
    this.labels.add(label);
  }

  /**
   * Adds the supplied shared label objects to the owned set.
   *
   * @param labels non-null set of typed labels
   */
  public void addLabels(Set<SimpleLabel> labels) {
    this.labels.addAll(labels);
  }

  /**
   * Creates an STP using the string constructor.
   *
   * @param stpId identifier and optional label suffix, or null for an empty STP
   * @return a new STP
   * @throws IllegalArgumentException if a non-empty identifier or its labels are invalid
   */
  public static SimpleStp getSimpleStp(String stpId) {
    return new SimpleStp(stpId);
  }

  /**
   * Returns the canonical rendering used by {@link #getStpId()}.
   *
   * @return the current STP string
   */
  @Override
  public String toString() {
    return this.getStpId();
  }

  /**
   * Compares canonical STP strings without case distinctions.
   *
   * @param object object to compare
   * @return whether both objects have the same class and case-insensitive STP rendering
   */
  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }

    if ((object == null) || (object.getClass() != this.getClass())) {
      return false;
    }

    SimpleStp that = (SimpleStp) object;
    String thisStpId = this.getStpId();
    String thatStpId = that.getStpId();

    if (Strings.isNullOrEmpty(thisStpId) && Strings.isNullOrEmpty(thatStpId)) {
      return true;
    }

    if (Strings.isNullOrEmpty(thisStpId) && !Strings.isNullOrEmpty(thatStpId)) {
      return false;
    }

    if (!Strings.isNullOrEmpty(thisStpId) && Strings.isNullOrEmpty(thatStpId)) {
      return false;
    }

    return thisStpId.equalsIgnoreCase(thatStpId);
  }

  /**
   * Hashes the canonical rendering using case folding consistent with equality.
   *
   * @return the hash of the current STP state
   */
  @Override
  public int hashCode() {
    int hash = 0;
    var characters = getStpId().codePoints().iterator();
    while (characters.hasNext()) {
      // Match the Unicode folding used by String.equalsIgnoreCase.
      int folded = Character.toLowerCase(Character.toUpperCase(characters.nextInt()));
      hash = 31 * hash + folded;
    }
    return 31 * 69 + hash;
  }
}
