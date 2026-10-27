package net.es.nsi.common;

import java.util.List;
import java.util.Optional;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 * Extracts optional Ethernet metrics from an NML extension-element list.
 *
 * <p>Only JAXB elements in the Ethernet namespace are considered. Values retain
 * their schema-defined numeric representation, and a later occurrence of a metric
 * replaces an earlier one. Missing or null metric values produce empty optionals.
 * Lombok provides accessors and value methods for the extracted properties.
 *
 * @author hacksaw
 */
@lombok.Data
public class NmlEthernet {
  /**
   * Namespace of NML Ethernet extension elements.
   */
  public final static String ETHERNET_NAMESPACE = "http://schemas.ogf.org/nml/2012/10/ethernet";
  /**
   * Local name of the reservation granularity metric.
   */
  public final static String GRANULARITY = "granularity";
  /**
   * Local name of the maximum reservable capacity metric.
   */
  public final static String MAXIMUMRESERVABLECAPACITY = "maximumReservableCapacity";
  /**
   * Local name of the minimum reservable capacity metric.
   */
  public final static String MINIMUMRESERVABLECAPACITY = "minimumReservableCapacity";
  /**
   * Local name of the interface MTU metric.
   */
  public final static String INTERFACEMTU = "interfaceMTU";
  /**
   * Local name of the Ethernet label element.
   */
  public final static String ETHLABEL = "label";
  /**
   * Local name of the total capacity metric.
   */
  public final static String CAPACITY = "capacity";
  /**
   * Local name of the encoding element.
   */
  public final static String ENCODING = "encoding";

  /**
   * Qualified name of the Ethernet {@code granularity} element.
   */
  public final static QName GRANULARITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "granularity");
  /**
   * Qualified name of the Ethernet {@code maximumReservableCapacity} element.
   */
  public final static QName MAXIMUMRESERVABLECAPACITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "maximumReservableCapacity");
  /**
   * Qualified name of the Ethernet {@code interfaceMTU} element.
   */
  public final static QName INTERFACEMTU_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "interfaceMTU");
  /**
   * Qualified name of the Ethernet {@code label} element.
   */
  public final static QName ETHLABEL_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "label");
  /**
   * Qualified name of the Ethernet {@code capacity} element.
   */
  public final static QName CAPACITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "capacity");
  /**
   * Qualified name of the Ethernet {@code encoding} element.
   */
  public final static QName ENCODING_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "encoding");
  /**
   * Qualified name of the Ethernet {@code minimumReservableCapacity} element.
   */
  public final static QName MINIMUMRESERVABLECAPACITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "minimumReservableCapacity");

  /**
   * NML Ethernet URI identifying VLAN label values.
   */
  public final static String VLAN_LABEL = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";
  /**
   * Short name used for VLAN labels in STP identifiers.
   */
  public final static String VLAN = "vlan";

  /**
   * Extracted {@code granularity} value, absent when the input does not supply it.
   */
  private Optional<Long> granularity = Optional.empty();
  /**
   * Extracted {@code maximumReservableCapacity} value, absent when the input does not supply it.
   */
  private Optional<Long> maximumReservableCapacity = Optional.empty();
  /**
   * Extracted {@code minimumReservableCapacity} value, absent when the input does not supply it.
   */
  private Optional<Long> minimumReservableCapacity = Optional.empty();
  /**
   * Extracted {@code interfaceMTU} value, absent when the input does not supply it.
   */
  private Optional<Integer> interfaceMTU = Optional.empty();
  /**
   * Extracted {@code capacity} value, absent when the input does not supply it.
   */
  private Optional<Long> capacity = Optional.empty();

  /**
   * Extracts supported metrics without modifying the extension list.
   *
   * @param obj non-null extension elements from a generated NML model
   * @throws ClassCastException if a recognized element contains a value of the wrong schema type
   */
  public NmlEthernet(List<Object> obj) {
    for (Object any : obj) {
      if (any instanceof JAXBElement) {
        JAXBElement<?> element = (JAXBElement<?>) any;
        if (isEthernetNamespace(element)) {
          switch(getElementName(element)) {
            case NmlEthernet.GRANULARITY:
              this.granularity = Optional.ofNullable((Long)element.getValue());
              break;
            case NmlEthernet.CAPACITY:
              this.capacity = Optional.ofNullable((Long)element.getValue());
              break;
            case NmlEthernet.INTERFACEMTU:
              this.interfaceMTU = Optional.ofNullable((Integer)element.getValue());
              break;
            case NmlEthernet.MAXIMUMRESERVABLECAPACITY:
              this.maximumReservableCapacity = Optional.ofNullable((Long)element.getValue());
              break;
            case NmlEthernet.MINIMUMRESERVABLECAPACITY:
              this.minimumReservableCapacity = Optional.ofNullable((Long)element.getValue());
              break;
            default:
              break;
          }
        }
      }
    }
  }

  /**
   * Recognizes the Ethernet namespace URI, ignoring case.
   *
   * @param namespace namespace URI, possibly {@code null}
   * @return whether the namespace matches
   */
  public static boolean isEthernetNamespace(String namespace) {
    return ETHERNET_NAMESPACE.equalsIgnoreCase(namespace);
  }

  /**
   * Checks the namespace of a JAXB extension element.
   *
   * @param jaxb non-null extension element
   * @return whether the element belongs to the Ethernet namespace
   */
  public static boolean isEthernetNamespace(JAXBElement<?> jaxb) {
    return isEthernetNamespace(jaxb.getName().getNamespaceURI());
  }

  /**
   * Returns the local part of a JAXB element name.
   *
   * @param jaxb non-null extension element
   * @return the local element name
   */
  public static String getElementName(JAXBElement<?> jaxb) {
    return jaxb.getName().getLocalPart();
  }

  /**
   * Recognizes an optional NML VLAN label-type URI, ignoring case.
   *
   * @param label non-null optional label-type URI
   * @return whether a present value identifies the VLAN label family
   */
  public static boolean isVlanLabel(Optional<String> label) {
    if (!label.isPresent()) {
      return false;
    }
    return VLAN_LABEL.equalsIgnoreCase(label.get());
  }

}
