package net.es.nsi.common;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import net.es.nsi.common.jaxb.nml.NmlLabelGroupType;
import net.es.nsi.common.jaxb.nml.NmlLabelType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests semantic comparison and VLAN expansion of generated NML label objects.
 */
class NmlLabelsTest {
    /**
     * Creates an independent JAXB label so equality checks do not rely on object identity.
     *
     * @param type label-type URI, possibly null
     * @param value lexical label value, possibly null
     * @return a fresh JAXB label
     */
    private static NmlLabelType label(String type, String value) {
        NmlLabelType label = new NmlLabelType();
        label.setLabeltype(type);
        label.setValue(value);
        return label;
    }

    /**
     * Creates an NML label group with the supplied type and expression.
     *
     * @param type label-type URI, possibly null
     * @param value group expression, possibly null
     * @return a fresh JAXB label group
     */
    private static NmlLabelGroupType group(String type, String value) {
        NmlLabelGroupType group = new NmlLabelGroupType();
        group.setLabeltype(type);
        group.setValue(value);
        return group;
    }

    /**
     * Checks that repeated values in overlapping ranges yield one semantic label each.
     */
    @Test
    void expandsVlanGroupsAndDeduplicatesOverlappingValues() {
        Set<NmlLabelType> labels = NmlLabels.labelGroupToLabels(group(NmlEthernet.VLAN_LABEL, "1-3,2,5"));
        assertEquals(4, labels.size());
        assertEquals(Set.of("1", "2", "3", "5"), labels.stream().map(NmlLabelType::getValue).collect(Collectors.toSet()));
        assertTrue(labels.stream().allMatch(l -> NmlEthernet.VLAN_LABEL.equals(l.getLabeltype())));
    }

    /**
     * Ensures NML group conversion applies VLAN range and syntax validation.
     *
     * @param value invalid VLAN group expression
     */
    @ParameterizedTest
    @ValueSource(strings = {"4-2", "-1", "4096", "1-2-3", "1,", "1;mpls=2", ""})
    void rejectsInvalidVlanGroupValues(String value) {
        assertThrows(IllegalArgumentException.class, () -> NmlLabels.labelGroupToLabels(group(NmlEthernet.VLAN_LABEL, value)));
    }

    /**
     * Checks intersection by type and value across distinct JAXB instances, including null fields.
     */
    @Test
    void mergesBySemanticLabelValueAndHandlesAbsentValues() {
        NmlLabelType first = label(NmlEthernet.VLAN_LABEL, "1");
        NmlLabelType second = label(NmlEthernet.VLAN_LABEL, "2");
        Set<NmlLabelType> merged = NmlLabels.mergeLabels(new LinkedHashSet<>(Set.of(first, second)),
                Set.of(label(NmlEthernet.VLAN_LABEL, "2"), label(NmlEthernet.VLAN_LABEL, "3")));
        assertEquals(Set.of(second), merged);
        assertTrue(NmlLabels.mergeLabels(null, Set.of(first)).isEmpty());
        assertTrue(NmlLabels.labelEquals((NmlLabelType) null, null));
        assertFalse(NmlLabels.labelEquals(first, null));
        assertFalse(NmlLabels.labelEquals(first, label(NmlEthernet.VLAN_LABEL, "2")));
        assertTrue(NmlLabels.labelEquals(label(null, null), label(null, null)));
        assertFalse(NmlLabels.labelEquals(label(null, "1"), first));
        assertTrue(NmlLabels.labelEquals(group(null, null), group(null, null)));
        assertFalse(NmlLabels.labelEquals(group(NmlEthernet.VLAN_LABEL, "1"), group(NmlEthernet.VLAN_LABEL, "2")));
        assertThrows(IllegalArgumentException.class, () -> NmlLabels.labelGroupToLabels(group("urn:unsupported", "1")));
    }
}
