package net.es.nsi.common;

import java.util.List;
import java.util.Optional;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import net.es.nsi.common.jaxb.nml.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests NML relation filtering, alias selection, and Ethernet extension extraction.
 */
class NmlHelpersTest {
    /**
     * Guards against selecting inbound relations when an outbound accessor is requested.
     */
    @Test
    void keepsInboundAndOutboundPortsAndGroupsSeparate() {
        NmlTopologyType topology = new NmlTopologyType();
        NmlTopologyRelationType inbound = new NmlTopologyRelationType();
        inbound.setType(Nml.HAS_INBOUND_PORT);
        NmlTopologyRelationType outbound = new NmlTopologyRelationType();
        outbound.setType(Nml.HAS_OUTBOUND_PORT);
        NmlPortType in = new NmlPortType(); in.setId("in");
        NmlPortType out = new NmlPortType(); out.setId("out");
        NmlPortGroupType inGroup = new NmlPortGroupType(); inGroup.setId("in-group");
        NmlPortGroupType outGroup = new NmlPortGroupType(); outGroup.setId("out-group");
        inbound.getPort().add(in); inbound.getPortGroup().add(inGroup);
        outbound.getPort().add(out); outbound.getPortGroup().add(outGroup);
        topology.getRelation().addAll(List.of(inbound, outbound));
        Nml nml = new Nml(topology);
        assertEquals(List.of(in), nml.getInboundPorts());
        assertEquals(List.of(out), nml.getOutboundPorts());
        assertEquals(List.of(inGroup), nml.getInboundPortGroups());
        assertEquals(List.of(outGroup), nml.getOutboundPortGroups());
        assertTrue(new Nml(new NmlTopologyType()).getOutboundPorts().isEmpty());
        NmlBidirectionalPortType bidirectional = new NmlBidirectionalPortType(); bidirectional.setId("both");
        topology.getGroup().add(bidirectional);
        assertEquals(List.of(bidirectional), nml.getBidirectionalPorts());
        assertSame(bidirectional, nml.getBidirectionalPortsIndexed().get("both"));
        assertSame(nml, nml.get());
    }

    /**
     * Checks alias targets, absent aliases, and exact relation-type matching.
     */
    @Test
    void extractsAliasesAndRecognizesOnlyTheRequestedRelations() {
        NmlPortGroupRelationType groupRelation = new NmlPortGroupRelationType();
        groupRelation.setType(Nml.IS_ALIAS);
        NmlPortGroupType group = new NmlPortGroupType(); group.setId("group-alias");
        groupRelation.getPortGroup().add(group);
        assertEquals("group-alias", Nml.getIsAlias(List.of(groupRelation)));
        NmlPortRelationType portRelation = new NmlPortRelationType(); portRelation.setType(Nml.IS_ALIAS);
        NmlPortType port = new NmlPortType(); port.setId("port-alias");
        portRelation.getPort().add(port);
        assertEquals("port-alias", Nml.getIsAliasPort(List.of(portRelation)));
        assertNull(Nml.getIsAlias(List.of()));
        assertNull(Nml.getIsAliasPort(List.of()));
        assertTrue(Nml.hasService(Nml.HAS_SERVICE));
        assertTrue(Nml.providesLink(Nml.PROVIDES_LINK));
        assertFalse(Nml.isAlias(null));
    }

    /**
     * Ensures extension lookup uses complete QNames and represents missing metrics as empty optionals.
     */
    @Test
    void ethernetMetricsRespectNamespacesAndOptionalValues() {
        NmlEthernet empty = new NmlEthernet(List.of());
        assertTrue(empty.getCapacity().isEmpty());
        ObjectFactory factory = new ObjectFactory();
        NmlEthernet ethernet = new NmlEthernet(List.of(factory.createCapacity(100L),
                factory.createGranularity(10L), factory.createMaximumReservableCapacity(90L),
                factory.createMinimumReservableCapacity(5L), factory.createInterfaceMTU(9000),
                new JAXBElement<>(new QName("urn:other", "capacity"), Long.class, 999L), "ignored"));
        assertEquals(Optional.of(100L), ethernet.getCapacity());
        assertEquals(Optional.of(10L), ethernet.getGranularity());
        assertEquals(Optional.of(90L), ethernet.getMaximumReservableCapacity());
        assertEquals(Optional.of(5L), ethernet.getMinimumReservableCapacity());
        assertEquals(Optional.of(9000), ethernet.getInterfaceMTU());
        assertTrue(NmlEthernet.isVlanLabel(Optional.of(NmlEthernet.VLAN_LABEL)));
        assertFalse(NmlEthernet.isVlanLabel(Optional.empty()));
    }
}
