/*
 * NSI Commons (nsi-common-lib) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.nsi.common;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import net.es.nsi.common.jaxb.NmlParser;
import net.es.nsi.common.jaxb.nml.NmlBidirectionalPortType;
import net.es.nsi.common.jaxb.nml.NmlPortGroupRelationType;
import net.es.nsi.common.jaxb.nml.NmlPortGroupType;
import net.es.nsi.common.jaxb.nml.NmlPortRelationType;
import net.es.nsi.common.jaxb.nml.NmlPortType;
import net.es.nsi.common.jaxb.nml.NmlTopologyRelationType;
import net.es.nsi.common.jaxb.nml.NmlTopologyType;
import net.es.nsi.common.jaxb.nml.ObjectFactory;

/**
 * Adds relation and port lookup helpers to a generated NML topology model.
 *
 * <p>The topology constructor is shallow: lists, JAXB members, and mutable property
 * objects are shared with the supplied model. Lookup methods return new collections containing those
 * same members; neither the wrapper nor the generated model is thread-safe.
 *
 * @author hacksaw
 */
public class Nml extends NmlTopologyType {
  // Topology relationship types.
  /**
   * NML relation identifying inbound ports or port groups.
   */
  public final static String HAS_INBOUND_PORT = "http://schemas.ogf.org/nml/2013/05/base#hasInboundPort";
  /**
   * NML relation identifying outbound ports or port groups.
   */
  public final static String HAS_OUTBOUND_PORT = "http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort";
  /**
   * NML relation identifying an attached service.
   */
  public final static String HAS_SERVICE = "http://schemas.ogf.org/nml/2013/05/base#hasService";
  /**
   * NML relation identifying an alias target.
   */
  public final static String IS_ALIAS = "http://schemas.ogf.org/nml/2013/05/base#isAlias";
  /**
   * NML relation identifying a provided link.
   */
  public final static String PROVIDES_LINK = "http://schemas.ogf.org/nml/2013/05/base#providesLink";

  /**
   * Factory available for constructing generated NML elements.
   */
  ObjectFactory FACTORY = new ObjectFactory();

  /**
   * Wraps an existing topology while sharing its JAXB lists and members.
   *
   * @param nml non-null source topology
   */
  public Nml(NmlTopologyType nml) {
    this.any = nml.getAny();
    this.group = nml.getGroup();
    this.id = nml.getId();
    this.lifetime = nml.getLifetime();
    this.link = nml.getLink();
    this.location = nml.getLocation();
    this.name = nml.getName();
    this.node = nml.getNode();
    this.port = nml.getPort();
    this.relation = nml.getRelation();
    this.service = nml.getService();
    this.version = nml.getVersion();
  }

  /**
   * Reads and wraps a topology XML file.
   *
   * @param filename path to the source XML file
   * @throws JAXBException if XML, its namespace, or its bindings are invalid
   * @throws IOException if the file cannot be read
   */
  public Nml(String filename) throws JAXBException, IOException {
    this(NmlParser.getInstance().readDocument(filename));
  }

  /**
   * Exposes this wrapper through its generated topology type.
   *
   * @return this object, not a copy
   */
  public NmlTopologyType get() {
    return (NmlTopologyType) this;
  }

  /**
   * Collects bidirectional ports in the topology group list.
   *
   * @return a new mutable list of original JAXB members in encounter order
   */
  public List<NmlBidirectionalPortType> getBidirectionalPorts() {
    List<NmlBidirectionalPortType> ports = new ArrayList<>();

    this.getGroup().stream().filter((g) -> (g instanceof NmlBidirectionalPortType)).forEachOrdered((g) -> {
      ports.add((NmlBidirectionalPortType) g);
    });

    return ports;
  }

  /**
   * Indexes bidirectional ports by their model identifiers.
   *
   * <p>When identifiers repeat, the last encountered port replaces the earlier value.
   *
   * @return a new mutable map whose values are original JAXB members
   */
  public Map<String, NmlBidirectionalPortType> getBidirectionalPortsIndexed() {
    Map<String, NmlBidirectionalPortType> ports = new HashMap<>();

    this.getGroup().stream().filter((g) -> (g instanceof NmlBidirectionalPortType)).forEachOrdered((g) -> {
      ports.put(g.getId(), (NmlBidirectionalPortType) g);
    });

    return ports;
  }

  /**
   * Collects port groups from inbound relations.
   *
   * @return a new mutable list of original JAXB members in encounter order
   */
  public List<NmlPortGroupType> getInboundPortGroups() {
    return this.getRelation().stream()
            .filter(r -> hasInboundPort(r.getType()))
            .map(NmlTopologyRelationType::getPortGroup)
            .flatMap(List::stream)
            .collect(Collectors.toList());
  }

  /**
   * Collects port groups from outbound relations.
   *
   * @return a new mutable list of original JAXB members in encounter order
   */
  public List<NmlPortGroupType> getOutboundPortGroups() {
    return this.getRelation().stream()
            .filter(r -> hasOutboundPort(r.getType()))
            .map(NmlTopologyRelationType::getPortGroup)
            .flatMap(List::stream)
            .collect(Collectors.toList());
  }

  /**
   * Collects ports from inbound relations.
   *
   * @return a new mutable list of original JAXB members in encounter order
   */
  public List<NmlPortType> getInboundPorts() {
    return this.getRelation().stream()
            .filter(r -> hasInboundPort(r.getType()))
            .map(NmlTopologyRelationType::getPort)
            .flatMap(List::stream)
            .collect(Collectors.toList());
  }

  /**
   * Collects ports from outbound relations.
   *
   * @return a new mutable list of original JAXB members in encounter order
   */
  public List<NmlPortType> getOutboundPorts() {
    return this.getRelation().stream()
            .filter(r -> hasOutboundPort(r.getType()))
            .map(NmlTopologyRelationType::getPort)
            .flatMap(List::stream)
            .collect(Collectors.toList());
  }

  /**
   * Returns the first target of the first matching port-group alias relation.
   *
   * @param relations non-null relation list
   * @return the target identifier, or {@code null} if no alias or no target occurs in that first relation
   */
  public static String getIsAlias(List<NmlPortGroupRelationType> relations) {
    Optional<NmlPortGroupRelationType> relation = relations.stream()
            .filter((pgrt) -> (isAlias(pgrt.getType()))).findFirst();

    if (relation.isPresent()) {
      // Found the isAlias relation but now we need to find the portId.
      Optional<NmlPortGroupType> pgt = relation.get().getPortGroup().stream().findFirst();
      if (pgt.isPresent()) {
        return pgt.get().getId();
      }
    }

    return null;
  }

  /**
   * Returns the first target of the first matching port alias relation.
   *
   * @param relations non-null relation list
   * @return the target identifier, or {@code null} if no alias or no target occurs in that first relation
   */
  public static String getIsAliasPort(List<NmlPortRelationType> relations) {
    Optional<NmlPortRelationType> relation = relations.stream()
            .filter((prt) -> (isAlias(prt.getType()))).findFirst();

    if (relation.isPresent()) {
      // Found the isAlias relation but now we need to find the portId.
      Optional<NmlPortType> pgt = relation.get().getPort().stream().findFirst();
      if (pgt.isPresent()) {
        return pgt.get().getId();
      }
    }

    return null;
  }

  /**
   * Recognizes the inbound-port relation URI, ignoring case.
   *
   * @param type relation URI, possibly {@code null}
   * @return whether the URI matches this relation
   */
  public static boolean hasInboundPort(String type) {
    return Nml.HAS_INBOUND_PORT.equalsIgnoreCase(type);
  }

  /**
   * Recognizes the outbound-port relation URI, ignoring case.
   *
   * @param type relation URI, possibly {@code null}
   * @return whether the URI matches this relation
   */
  public static boolean hasOutboundPort(String type) {
    return Nml.HAS_OUTBOUND_PORT.equalsIgnoreCase(type);
  }

  /**
   * Recognizes the service relation URI, ignoring case.
   *
   * @param type relation URI, possibly {@code null}
   * @return whether the URI matches this relation
   */
  public static boolean hasService(String type) {
    return Nml.HAS_SERVICE.equalsIgnoreCase(type);
  }

  /**
   * Recognizes the alias relation URI, ignoring case.
   *
   * @param type relation URI, possibly {@code null}
   * @return whether the URI matches this relation
   */
  public static boolean isAlias(String type) {
    return Nml.IS_ALIAS.equalsIgnoreCase(type);
  }

  /**
   * Recognizes the provided-link relation URI, ignoring case.
   *
   * @param type relation URI, possibly {@code null}
   * @return whether the URI matches this relation
   */
  public static boolean providesLink(String type) {
    return Nml.PROVIDES_LINK.equalsIgnoreCase(type);
  }

  /**
   * Qualified name of the {@code LabelGroup} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName LabelGroup_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "LabelGroup");
  /**
   * Qualified name of the {@code Port} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName Port_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Port");
  /**
   * Qualified name of the {@code Node} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName Node_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Node");
  /**
   * Qualified name of the {@code NSA} element in {@code http://schemas.ogf.org/nsi/2013/09/topology#}.
   */
  public final static QName NSA_QNAME = new QName("http://schemas.ogf.org/nsi/2013/09/topology#", "NSA");
  /**
   * Qualified name of the {@code PortGroup} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName PortGroup_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "PortGroup");
  /**
   * Qualified name of the {@code AdaptationService} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName AdaptationService_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "AdaptationService");
  /**
   * Qualified name of the {@code granularity} element in {@code http://schemas.ogf.org/nml/2012/10/ethernet}.
   */
  public final static QName Granularity_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "granularity");
  /**
   * Qualified name of the {@code serviceDefinition} element in {@code http://schemas.ogf.org/nsi/2013/12/services/definition}.
   */
  public final static QName ServiceDefinition_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/services/definition", "serviceDefinition");
  /**
   * Qualified name of the {@code maximumReservableCapacity} element in {@code http://schemas.ogf.org/nml/2012/10/ethernet}.
   */
  public final static QName MaximumReservableCapacity_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "maximumReservableCapacity");
  /**
   * Qualified name of the {@code BidirectionalLink} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName BidirectionalLink_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "BidirectionalLink");
  /**
   * Qualified name of the {@code DeadaptationService} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName DeadaptationService_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "DeadaptationService");
  /**
   * Qualified name of the {@code Topology} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName Topology_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Topology");
  /**
   * Qualified name of the {@code LinkGroup} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName LinkGroup_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "LinkGroup");
  /**
   * Qualified name of the {@code Relation} element in {@code http://schemas.ogf.org/nsi/2013/09/topology#}.
   */
  public final static QName Relation_QNAME = new QName("http://schemas.ogf.org/nsi/2013/09/topology#", "Relation");
  /**
   * Qualified name of the {@code Label} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName Label_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Label");
  /**
   * Qualified name of the {@code BidirectionalPort} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName BidirectionalPort_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "BidirectionalPort");
  /**
   * Qualified name of the {@code interfaceMTU} element in {@code http://schemas.ogf.org/nml/2012/10/ethernet}.
   */
  public final static QName InterfaceMTU_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "interfaceMTU");
  /**
   * Qualified name of the {@code Service} element in {@code http://schemas.ogf.org/nsi/2013/09/topology#}.
   */
  public final static QName NsaService_QNAME = new QName("http://schemas.ogf.org/nsi/2013/09/topology#", "Service");
  /**
   * Qualified name of the {@code Link} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName Link_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Link");
  /**
   * Qualified name of the {@code SwitchingService} element in {@code http://schemas.ogf.org/nml/2013/05/base#}.
   */
  public final static QName SwitchingService_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "SwitchingService");
  /**
   * Qualified name of the {@code label} element in {@code http://schemas.ogf.org/nml/2012/10/ethernet}.
   */
  public final static QName EthLabel_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "label");
  /**
   * Qualified name of the {@code capacity} element in {@code http://schemas.ogf.org/nml/2012/10/ethernet}.
   */
  public final static QName Capacity_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "capacity");
  /**
   * Qualified name of the {@code encoding} element in {@code http://schemas.ogf.org/nml/2012/10/ethernet}.
   */
  public final static QName Encoding_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "encoding");
  /**
   * Qualified name of the {@code minimumReservableCapacity} element in {@code http://schemas.ogf.org/nml/2012/10/ethernet}.
   */
  public final static QName MinimumReservableCapacity_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "minimumReservableCapacity");
}
