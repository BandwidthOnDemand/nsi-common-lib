package net.es.nsi.common;

import java.util.HashSet;
import java.util.Set;
import net.es.nsi.common.jaxb.nml.NmlLabelGroupType;
import net.es.nsi.common.jaxb.nml.NmlLabelType;
import net.es.nsi.common.jaxb.nml.NmlPortGroupType;
import net.es.nsi.common.jaxb.nml.NmlPortType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests STP grammar, label-set ownership, and consistent equality and hashing.
 */
class SimpleStpTest {
    /**
     * Valid STP identifier shared by grammar and ownership tests.
     */
    private static final String ID = "urn:ogf:network:example.net:2026:topology:device:port";

    /**
     * Checks identifier components, label expansion, round trips, and the read-only labels view.
     */
    @Test
    void parsesIdentifiersAndExpandsMemberLabels() {
        SimpleStp stp = new SimpleStp(ID + "?vlan=1-2,4");
        assertEquals("urn:ogf:network:example.net:2026:topology", stp.getNetworkId());
        assertEquals("device:port", stp.getLocalId());
        assertEquals("example.net:2026:topology", stp.getNetworkLabel());
        assertEquals(ID, stp.getId());
        assertEquals(ID, SimpleStp.getId(stp.getStpId()));
        assertEquals(stp.getNetworkId(), SimpleStp.parseNetworkId(ID));
        assertEquals(stp.getLocalId(), SimpleStp.parseLocalId(ID));
        assertEquals(stp, SimpleStp.getSimpleStp(stp.toString()));
        assertTrue(stp.isUnderSpecified());
        assertTrue(new SimpleStp(ID + "?vlan").isUnderSpecified());
        assertEquals(Set.of(ID + "?vlan=1", ID + "?vlan=2", ID + "?vlan=4"), new HashSet<>(stp.getMemberStpId()));
        assertThrows(UnsupportedOperationException.class, () -> stp.getLabels().clear());
    }

    /**
     * Ensures copied membership survives caller mutation and self-assignment, with idempotent intersections.
     */
    @Test
    void labelSetsAreOwnedByTheStpAndIntersectionsAreStable() {
        Set<SimpleLabel> labels = new HashSet<>(SimpleLabels.fromString("vlan=1-3"));
        SimpleStp stp = new SimpleStp(ID, labels);
        labels.clear();
        assertEquals(3, stp.getLabels().size());
        assertTrue(stp.intersectLabels(SimpleLabels.fromString("vlan=2-4")));
        assertFalse(stp.intersectLabels(SimpleLabels.fromString("vlan=2-4")));
        stp.setLabels(stp.getLabels());
        assertEquals(2, stp.getLabels().size());
        stp.setLabels(Set.of(new SimpleLabel("vlan", "2")));
        assertFalse(stp.isUnderSpecified());
        stp.addLabel(new SimpleLabel("vlan", "4"));
        stp.addLabels(Set.of(new SimpleLabel("mpls", "5")));
        assertEquals(stp, new SimpleStp(stp.getStpId()));
    }

    /**
     * Checks case-insensitive identifiers and equal hash codes for equal STPs.
     */
    @Test
    void equalityAndHashingUseTheSameCasePolicy() {
        SimpleStp lower = new SimpleStp(ID);
        SimpleStp upper = new SimpleStp(ID.toUpperCase());
        assertEquals(lower, upper);
        assertEquals(lower.hashCode(), upper.hashCode());
        assertNotEquals(lower, new SimpleStp(ID + "2"));
        assertNotEquals(lower, null);
        assertEquals(ID, lower.getMemberStpId().get(0));
        SimpleStp single = new SimpleStp(ID, new SimpleLabel("vlan", "1"));
        assertFalse(single.isUnderSpecified());
    }

    /**
     * Checks that default, null, and empty construction agree before an identifier is assembled.
     */
    @Test
    void emptyIdentifiersHaveAConsistentRepresentation() {
        SimpleStp empty = new SimpleStp();
        assertEquals("", empty.getStpId());
        assertEquals(empty, new SimpleStp(""));
        assertEquals(empty.hashCode(), new SimpleStp((String) null).hashCode());
        assertTrue(empty.isUnderSpecified());
        SimpleStp expected = new SimpleStp(ID);
        empty.setNetworkId(expected.getNetworkId());
        empty.setLocalId(expected.getLocalId());
        assertEquals(expected, empty);
    }

    /**
     * Guards against inconsistent hashing for Unicode case variants accepted by equalsIgnoreCase.
     */
    @Test
    void unicodeCaseVariantsObeyEqualityAndHashingContracts() {
        SimpleStp first = new SimpleStp(ID.replace("example.net", "i.example"));
        SimpleStp second = new SimpleStp(ID.replace("example.net", "İ.example"));
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    /**
     * Rejects invalid URN structure and invalid or ambiguous label suffixes.
     *
     * @param value malformed STP identifier
     */
    @ParameterizedTest
    @ValueSource(strings = {"invalid", "urn:not:network:example.net:2026:topology:port", "urn:ogf:network:example.net:2026:topology", "urn:ogf:network:example.net:2026:topology:", "urn:ogf:network:example.net:2026:topology:port?vlan=5000", "urn:ogf:network:example.net:2026:topology:port?vlan=1?mpls=2"})
    void rejectsMalformedIdentifiers(String value) {
        assertThrows(IllegalArgumentException.class, () -> new SimpleStp(value));
    }

    /**
     * Checks JAXB port conversion and explicit rejection of unsupported or multiple label groups.
     */
    @Test
    void convertsNmlPortsAndRejectsUnsupportedLabelGroups() {
        NmlPortType port = new NmlPortType();
        port.setId(ID);
        NmlLabelType label = new NmlLabelType();
        label.setLabeltype(SimpleStp.NSI_VLAN_LABEL_URN);
        label.setValue("10");
        port.setLabel(label);
        assertEquals(ID + "?vlan=10", SimpleStp.createStpId(port));
        assertEquals(new SimpleStp(ID + "?vlan=10"), new SimpleStp(port));
        label.setLabeltype("urn:unknown:label");
        assertThrows(IllegalArgumentException.class, () -> SimpleStp.createStpId(port));
        NmlPortGroupType group = new NmlPortGroupType();
        group.setId(ID);
        NmlLabelGroupType range = new NmlLabelGroupType();
        range.setLabeltype(SimpleStp.NSI_MPLS_LABEL_URN);
        range.setValue("100-102");
        group.getLabelGroup().add(range);
        assertEquals(new SimpleStp(ID + "?mpls=100-102"), new SimpleStp(group));
        group.getLabelGroup().add(range);
        assertThrows(IllegalArgumentException.class, () -> SimpleStp.createStpId(group));
    }
}
