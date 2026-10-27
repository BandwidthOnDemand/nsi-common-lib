package net.es.nsi.common;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for label value semantics, atomic mutation, and lossless serialization.
 */
class LabelContractsTest {
    /**
     * Checks normalized numeric equality, type-sensitive identity, and set deduplication.
     */
    @Test
    void equalityAndHashingRespectBothTypeAndValue() {
        SimpleLabel a = new SimpleLabel("vlan", "10");
        SimpleLabel same = new SimpleLabel("vlan", "10");
        assertEquals(a, same);
        assertEquals(a.hashCode(), same.hashCode());
        assertEquals(a, new SimpleLabel("vlan", "010"));
        assertNotEquals(a, new SimpleLabel("vlan", "11"));
        assertNotEquals(a, new SimpleLabel("mpls", "10"));
        assertNotEquals(a, null);
        assertNotEquals(a, "vlan=10");
        assertEquals(2, new HashSet<>(List.of(a, same, new SimpleLabel("vlan", "11"))).size());
        SimpleLabel empty = new SimpleLabel();
        assertEquals(empty, empty);
        assertEquals(empty, new SimpleLabel());
        assertNotEquals(empty, a);
    }

    /**
     * Ensures failed validation cannot leave a label partially mutated.
     */
    @Test
    void rejectedMutationPreservesTheOriginalLabel() {
        SimpleLabel label = new SimpleLabel("mpls", "5000");
        assertThrows(IllegalArgumentException.class, () -> label.setType("vlan"));
        assertEquals("mpls", label.getType());
        assertEquals("5000", label.getValue());
        assertThrows(IllegalArgumentException.class, () -> label.setValue("invalid"));
        assertEquals("5000", label.getValue());
    }

    /**
     * Exercises equivalent spellings and mixed types through a parse/serialize cycle.
     *
     * @param text valid label expression, including overlapping ranges or an unspecified value
     */
    @ParameterizedTest
    @ValueSource(strings = {"vlan=", "vlan", "vlan=1,2,3,8", "mpls=16-18;vlan=1,3-5", "vlan=1,2-4,3-5", "vlan=3-3", " vlan = 0, 2 - 4 ", "vlan=;mpls=15"})
    void parsedLabelsSurviveCanonicalSerialization(String text) {
        Set<SimpleLabel> labels = SimpleLabels.fromString(text);
        assertEquals(labels, SimpleLabels.fromString(SimpleLabels.toString(labels)));
    }

    /**
     * Guards against carrying an unfinished range into the next label type.
     */
    @Test
    void serializationClosesEachRangeBeforeChangingTypes() {
        assertEquals("mpls=10-12;vlan=1-3,8", SimpleLabels.toString(
                SimpleLabels.fromString("vlan=3,1,2,8;mpls=12,10,11")));
        assertEquals("vlan=1-2,10", SimpleLabels.toString(SimpleLabels.fromString("vlan=10,2,1")));
    }

    /**
     * Checks varied mixed-type sets with a fixed random seed so failures are reproducible.
     */
    @Test
    void seededLabelSetsRoundTripWithoutLosingMembers() {
        Random random = new Random(741L);
        for (int i = 0; i < 60; i++) {
            Set<SimpleLabel> labels = new HashSet<>();
            for (int j = 0; j < 20; j++) {
                labels.add(new SimpleLabel(j % 2 == 0 ? "vlan" : "mpls", Integer.toString(random.nextInt(64))));
            }
            assertEquals(labels, SimpleLabels.fromString(SimpleLabels.toString(labels)));
        }
    }

    /**
     * Rejects ambiguous syntax, unsupported types, numeric overflow, and out-of-range values.
     *
     * @param text invalid label expression
     */
    @ParameterizedTest
    @ValueSource(strings = {"vlan=1=2", "vlan=1,", "vlan=,1", "vlan=1-2-3", "vlan=4-2", "vlan=-1", "vlan=4096", "mpls=1048576", "vlan=2147483648", "vlan=1;", "unknown=1"})
    void rejectsMalformedAndOutOfRangeLabels(String text) {
        assertThrows(IllegalArgumentException.class, () -> SimpleLabels.fromString(text));
    }

    /**
     * Requires invalid groups to fail before allocating range members, even with a small heap.
     *
     * @param text group containing an invalid range or a later invalid token
     * @param directory isolated location for the child JVM diagnostics
     * @throws Exception if the probe cannot be launched or its diagnostics cannot be read
     */
    @ParameterizedTest
    @ValueSource(strings = {
        "vlan=5000-1053575",
        "vlan=0-1048575",
        "mpls=1048576-2097151",
        "vlan=1-2,5000-1053575",
        "mpls=0-1048575,1048576",
        "mpls=0-1048575,1,",
        "custom=0-1048575"
    })
    void rejectsInvalidGroupsBeforeExpandingRanges(String text, @TempDir Path directory) throws Exception {
        Path output = directory.resolve("range-probe.log");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                "-Xmx64m", "-cp",
                System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                InvalidRangeProbe.class.getName(), text)
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Range rejection probe timed out: " + text);
            assertEquals(0, process.exitValue(), "Range rejection failed for " + text + ":\n"
                    + Files.readString(output));
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly().waitFor(5, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * Isolated entry point so allocation failures cannot exhaust the test runner's heap.
     */
    public static final class InvalidRangeProbe {
        /**
         * Requires rejection through the public API; an allocation failure exits unsuccessfully.
         * The custom family tests a nonzero lower bound without mutating the runner's registry.
         *
         * @param args one invalid label expression
         */
        public static void main(String[] args) {
            SimpleLabel.LABELS.put("custom", LabelType.builder()
                    .label("custom").min(1048576).max(2097151).build());
            Set<SimpleLabel> endpoints = Set.of(new SimpleLabel("custom", "1048576"),
                    new SimpleLabel("custom", "2097151"));
            if (!endpoints.equals(SimpleLabels.fromString("custom=1048576,2097151"))) {
                throw new AssertionError("Configured family bounds must accept their endpoints");
            }
            try {
                SimpleLabels.fromString(args[0]);
            } catch (IllegalArgumentException expected) {
                return;
            }
            throw new AssertionError("Expected IllegalArgumentException for " + args[0]);
        }
    }

    /**
     * Checks absent values, bounded range expansion, integer overflow, and comparison of every member.
     */
    @Test
    void containsSortingAndOrderedEqualityHandleAllMembers() {
        assertTrue(SimpleLabels.fromString(null).isEmpty());
        assertTrue(SimpleLabels.fromString("").isEmpty());
        assertTrue(SimpleLabels.contains("1-5,10", "2,4-5"));
        assertFalse(SimpleLabels.contains("1-5", "6"));
        assertTrue(SimpleLabels.contains(null, ""));
        assertFalse(SimpleLabels.contains(null, "1"));
        assertFalse(SimpleLabels.contains("1", null));
        assertEquals(Set.of(1, 2, 3), SimpleLabels.stringToIntegerSet("1-2,2-3"));
        assertEquals(Set.of(Integer.MAX_VALUE), SimpleLabels.stringToIntegerSet("2147483647-2147483647"));
        assertThrows(IllegalArgumentException.class, () -> SimpleLabels.stringToIntegerSet("4-2"));
        assertThrows(IllegalArgumentException.class, () -> SimpleLabels.stringToIntegerSet("0-2147483647"));
        SimpleLabel unspecified = new SimpleLabel();
        unspecified.setType("vlan");
        assertFalse(SimpleLabels.equals(List.of(unspecified, new SimpleLabel("vlan", "1")),
                List.of(unspecified, new SimpleLabel("vlan", "2"))));
        assertTrue(SimpleLabels.equals(List.of(unspecified), List.of(unspecified)));
        assertFalse(SimpleLabels.equals(List.of(), List.of(unspecified)));
    }
}
