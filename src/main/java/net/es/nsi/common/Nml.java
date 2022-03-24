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
import javax.xml.bind.JAXBException;
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
 *
 * @author hacksaw
 */
public class Nml extends NmlTopologyType {
  // Topology relationship types.
  public final static String HAS_INBOUND_PORT = "http://schemas.ogf.org/nml/2013/05/base#hasInboundPort";
  public final static String HAS_OUTBOUND_PORT = "http://schemas.ogf.org/nml/2013/05/base#hasOutboundPort";
  public final static String HAS_SERVICE = "http://schemas.ogf.org/nml/2013/05/base#hasService";
  public final static String IS_ALIAS = "http://schemas.ogf.org/nml/2013/05/base#isAlias";
  public final static String PROVIDES_LINK = "http://schemas.ogf.org/nml/2013/05/base#providesLink";

  ObjectFactory FACTORY = new ObjectFactory();

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

  public Nml(String filename) throws JAXBException, IOException {
    this(NmlParser.getInstance().readDocument(filename));
  }

  public NmlTopologyType get() {
    return (NmlTopologyType) this;
  }

  public List<NmlBidirectionalPortType> getBidirectionalPorts() {
    List<NmlBidirectionalPortType> ports = new ArrayList<>();

    this.getGroup().stream().filter((g) -> (g instanceof NmlBidirectionalPortType)).forEachOrdered((g) -> {
      ports.add((NmlBidirectionalPortType) g);
    });

    return ports;
  }

  public Map<String, NmlBidirectionalPortType> getBidirectionalPortsIndexed() {
    Map<String, NmlBidirectionalPortType> ports = new HashMap<>();

    this.getGroup().stream().filter((g) -> (g instanceof NmlBidirectionalPortType)).forEachOrdered((g) -> {
      ports.put(g.getId(), (NmlBidirectionalPortType) g);
    });

    return ports;
  }

  public List<NmlPortGroupType> getInboundPortGroups() {
    return this.getRelation().stream()
            .filter(r -> hasInboundPort(r.getType()))
            .map(NmlTopologyRelationType::getPortGroup)
            .flatMap(List::stream)
            .collect(Collectors.toList());
  }

  public List<NmlPortGroupType> getOutboundPortGroups() {
    return this.getRelation().stream()
            .filter(r -> hasOutboundPort(r.getType()))
            .map(NmlTopologyRelationType::getPortGroup)
            .flatMap(List::stream)
            .collect(Collectors.toList());
  }

  public List<NmlPortType> getInboundPorts() {
    return this.getRelation().stream()
            .filter(r -> hasInboundPort(r.getType()))
            .map(NmlTopologyRelationType::getPort)
            .flatMap(List::stream)
            .collect(Collectors.toList());
  }

  public List<NmlPortType> getOutboundPorts() {
    return this.getRelation().stream()
            .filter(r -> hasInboundPort(r.getType()))
            .map(NmlTopologyRelationType::getPort)
            .flatMap(List::stream)
            .collect(Collectors.toList());
  }

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

  public static boolean hasInboundPort(String type) {
    return Nml.HAS_INBOUND_PORT.equalsIgnoreCase(type);
  }

  public static boolean hasOutboundPort(String type) {
    return Nml.HAS_OUTBOUND_PORT.equalsIgnoreCase(type);
  }

  public static boolean hasService(String type) {
    return Nml.HAS_SERVICE.equalsIgnoreCase(type);
  }

  public static boolean isAlias(String type) {
    return Nml.IS_ALIAS.equalsIgnoreCase(type);
  }

  public static boolean providesLink(String type) {
    return Nml.PROVIDES_LINK.equalsIgnoreCase(type);
  }

  public final static QName LabelGroup_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "LabelGroup");
  public final static QName Port_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Port");
  public final static QName Node_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Node");
  public final static QName NSA_QNAME = new QName("http://schemas.ogf.org/nsi/2013/09/topology#", "NSA");
  public final static QName PortGroup_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "PortGroup");
  public final static QName AdaptationService_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "AdaptationService");
  public final static QName Granularity_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "granularity");
  public final static QName ServiceDefinition_QNAME = new QName("http://schemas.ogf.org/nsi/2013/12/services/definition", "serviceDefinition");
  public final static QName MaximumReservableCapacity_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "maximumReservableCapacity");
  public final static QName BidirectionalLink_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "BidirectionalLink");
  public final static QName DeadaptationService_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "DeadaptationService");
  public final static QName Topology_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Topology");
  public final static QName LinkGroup_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "LinkGroup");
  public final static QName Relation_QNAME = new QName("http://schemas.ogf.org/nsi/2013/09/topology#", "Relation");
  public final static QName Label_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Label");
  public final static QName BidirectionalPort_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "BidirectionalPort");
  public final static QName InterfaceMTU_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "interfaceMTU");
  public final static QName NsaService_QNAME = new QName("http://schemas.ogf.org/nsi/2013/09/topology#", "Service");
  public final static QName Link_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "Link");
  public final static QName SwitchingService_QNAME = new QName("http://schemas.ogf.org/nml/2013/05/base#", "SwitchingService");
  public final static QName EthLabel_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "label");
  public final static QName Capacity_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "capacity");
  public final static QName Encoding_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "encoding");
  public final static QName MinimumReservableCapacity_QNAME = new QName("http://schemas.ogf.org/nml/2012/10/ethernet", "minimumReservableCapacity");
}
