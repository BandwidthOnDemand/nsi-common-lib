package net.es.nsi.common;

import java.util.List;
import java.util.Optional;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

/**
 *
 * @author hacksaw
 */
@lombok.Data
public class NmlEthernet {
  public final static String ETHERNET_NAMESPACE = "http://schemas.ogf.org/nml/2012/10/ethernet";
  public final static String GRANULARITY = "granularity";
  public final static String MAXIMUMRESERVABLECAPACITY = "maximumReservableCapacity";
  public final static String MINIMUMRESERVABLECAPACITY = "minimumReservableCapacity";
  public final static String INTERFACEMTU = "interfaceMTU";
  public final static String ETHLABEL = "label";
  public final static String CAPACITY = "capacity";
  public final static String ENCODING = "encoding";

  public final static QName GRANULARITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "granularity");
  public final static QName MAXIMUMRESERVABLECAPACITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "maximumReservableCapacity");
  public final static QName INTERFACEMTU_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "interfaceMTU");
  public final static QName ETHLABEL_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "label");
  public final static QName CAPACITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "capacity");
  public final static QName ENCODING_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "encoding");
  public final static QName MINIMUMRESERVABLECAPACITY_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "minimumReservableCapacity");

  public final static String VLAN_LABEL = "http://schemas.ogf.org/nml/2012/10/ethernet#vlan";
  public final static String VLAN = "vlan";

  // Ethernet port metrics.
  private Optional<Long> granularity = Optional.empty();
  private Optional<Long> maximumReservableCapacity = Optional.empty();
  private Optional<Long> minimumReservableCapacity = Optional.empty();
  private Optional<Integer> interfaceMTU = Optional.empty();
  private Optional<Long> capacity = Optional.empty();

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

  public static boolean isEthernetNamespace(String namespace) {
    return ETHERNET_NAMESPACE.equalsIgnoreCase(namespace);
  }

  public static boolean isEthernetNamespace(JAXBElement<?> jaxb) {
    return isEthernetNamespace(jaxb.getName().getNamespaceURI());
  }

  public static String getElementName(JAXBElement<?> jaxb) {
    return jaxb.getName().getLocalPart();
  }

  public static boolean isVlanLabel(Optional<String> label) {
    if (!label.isPresent()) {
      return false;
    }
    return VLAN_LABEL.equalsIgnoreCase(label.get());
  }

}
