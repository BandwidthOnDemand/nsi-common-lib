package net.es.nsi.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parses, compares, and serializes sets of numeric NSI labels.
 *
 * <p>Family groups are separated by semicolons; each group contains a family name
 * and optional comma-separated values or inclusive ranges. For example,
 * {@code vlan=1-3,8;mpls=16} represents four VLAN labels and one MPLS label.
 * Families without values are preserved as unspecified labels.
 *
 * <p>Methods create their own result collections and do not mutate input collections.
 * Mutable label objects in those collections are shared with their callers.
 */
public class SimpleLabels {
    /**
     * Parses family groups and removes duplicate type/value pairs.
     *
     * <p>Blank or {@code null} input produces an empty set. Whitespace around groups,
     * values, and range separators is ignored; empty tokens inside a value list are rejected.
     * Every token in a family group is validated against that family's bounds before
     * any range members are allocated.
     *
     * @param labels serialized label groups
     * @return a mutable set in first-occurrence order
     * @throws IllegalArgumentException if syntax, family names, values, or ranges are invalid
     */
    public static Set<SimpleLabel> fromString(String labels) throws IllegalArgumentException {
        Set<SimpleLabel> results = new LinkedHashSet<>();
        if (labels == null || labels.isBlank()) {
            return results;
        }
        for (String group : labels.split(";", -1)) {
            String[] fields = group.split("=", -1);
            if (fields.length > 2 || fields[0].isBlank()) {
                throw new IllegalArgumentException("Invalid label format: " + group);
            }
            String type = fields[0].trim();
            LabelType family = SimpleLabel.LABELS.get(type);
            if (family == null) {
                throw new IllegalArgumentException("Unknown label type " + type);
            }
            if (fields.length == 1 || fields[1].isBlank()) {
                SimpleLabel unspecified = new SimpleLabel();
                unspecified.setType(type);
                results.add(unspecified);
            } else {
                for (int value : stringToIntegerSet(fields[1], family.getMin(), family.getMax())) {
                    results.add(new SimpleLabel(type, Integer.toString(value)));
                }
            }
        }
        return results;
    }

    /**
     * Produces a canonical representation, sorting families and compressing adjacent values.
     *
     * <p>Unspecified labels are emitted as {@code family=}; if a family also has numeric
     * values, a separate group preserves those values.
     *
     * @param labels non-null set of typed labels
     * @return canonical groups separated by semicolons, or an empty string for an empty set
     * @throws IllegalArgumentException if a member or its family is null
     */
    public static String toString(Set<SimpleLabel> labels) {
        Map<String, List<SimpleLabel>> groups = new LinkedHashMap<>();
        for (SimpleLabel label : sortLabels(labels)) {
            groups.computeIfAbsent(label.getType(), key -> new ArrayList<>()).add(label);
        }
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, List<SimpleLabel>> group : groups.entrySet()) {
            List<Integer> values = new ArrayList<>();
            boolean unspecified = false;
            for (SimpleLabel label : group.getValue()) {
                if (label.getValue() == null) {
                    unspecified = true;
                } else {
                    values.add(Integer.parseInt(label.getValue()));
                }
            }
            // Keep unspecified members separate so later family groups are not lost.
            if (unspecified) {
                result.add(group.getKey() + "=");
            }
            if (!values.isEmpty()) {
                List<String> ranges = new ArrayList<>();
                int start = values.get(0);
                int last = start;
                for (int i = 1; i <= values.size(); i++) {
                    if (i < values.size() && values.get(i) == last + 1) {
                        last = values.get(i);
                    } else {
                        ranges.add(start == last ? Integer.toString(start) : start + "-" + last);
                        if (i < values.size()) {
                            start = last = values.get(i);
                        }
                    }
                }
                result.add(group.getKey() + "=" + String.join(",", ranges));
            }
        }
        return String.join(";", result);
    }

    /**
     * Sorts labels lexically by family and numerically within each family.
     *
     * <p>An unspecified value sorts before numeric values in its family.
     *
     * @param labels non-null set of typed labels
     * @return a new mutable list containing the original label objects
     * @throws IllegalArgumentException if a member or its family is null
     */
    public static List<SimpleLabel> sortLabels(Set<SimpleLabel> labels) {
        for (SimpleLabel label : labels) {
            if (label == null || label.getType() == null) {
                throw new IllegalArgumentException("Cannot sort a label without a type");
            }
        }
        return new ArrayList<>(labels.stream().sorted(Comparator.comparing(SimpleLabel::getType)
                .thenComparingInt(label -> label.getValue() == null ? -1 : Integer.parseInt(label.getValue())))
                .toList());
    }

    /**
     * Compares every member of two ordered label lists.
     *
     * @param first non-null first list
     * @param second second list
     * @return whether the lists contain equal members in the same order
     */
    public static boolean equals(List<SimpleLabel> first, List<SimpleLabel> second) {
        return first.equals(second);
    }

    /**
     * Tests containment between comma-separated numeric values and ranges.
     *
     * <p>Two null or empty strings are considered equal empty specifications. If only
     * one string is null or empty, containment is false.
     *
     * @param values candidate containing set, without a family prefix
     * @param subset candidate subset, without a family prefix
     * @return whether every subset value occurs in the containing set
     * @throws IllegalArgumentException if a non-empty specification is malformed or excessive
     */
    public static boolean contains(String values, String subset) {
        if ((values == null || values.isEmpty()) && (subset == null || subset.isEmpty())) {
            return true;
        }
        if (values == null || values.isEmpty() || subset == null || subset.isEmpty()) {
            return false;
        }
        return stringToIntegerSet(values).containsAll(stringToIntegerSet(subset));
    }

    /**
     * Expands non-negative decimal integers and inclusive ranges in encounter order.
     *
     * <p>Null or blank input produces an empty set. Duplicate integers are removed.
     * Each range is limited to 1,048,576 values to bound accidental expansion.
     * All tokens are validated before any range is expanded.
     *
     * @param values comma-separated integers or ranges, without a family prefix
     * @return a mutable set of distinct integers
     * @throws IllegalArgumentException if a token is malformed, overflows an integer, is reversed, or exceeds the range limit
     */
    public static Set<Integer> stringToIntegerSet(String values) {
        return stringToIntegerSet(values, 0, Integer.MAX_VALUE);
    }

    /**
     * Validates all range tokens against inclusive bounds before allocating their members.
     *
     * @param values comma-separated integers or ranges, without a family prefix
     * @param lowerBound smallest permitted value
     * @param upperBound largest permitted value
     * @return distinct integers in first-occurrence order
     * @throws IllegalArgumentException if syntax, bounds, or range size is invalid
     */
    private static Set<Integer> stringToIntegerSet(String values, int lowerBound, int upperBound) {
        Set<Integer> result = new LinkedHashSet<>();
        if (values == null || values.isBlank()) {
            return result;
        }
        List<Range> ranges = new ArrayList<>();
        for (String token : values.split(",", -1)) {
            String text = token.trim();
            if (!text.matches("[0-9]+(?:\\s*-\\s*[0-9]+)?")) {
                throw new IllegalArgumentException("Invalid label value: " + token);
            }
            String[] endpoints = text.split("-", -1);
            int min = Integer.parseInt(endpoints[0].trim());
            int max = endpoints.length == 1 ? min : Integer.parseInt(endpoints[1].trim());
            if (max < min || (long) max - min > SimpleLabel.NSI_MPLS_LABEL_MAX) {
                throw new IllegalArgumentException("Invalid or excessive label range: " + text);
            }
            if (min < lowerBound || max > upperBound) {
                throw new IllegalArgumentException("Label range outside bounds ["
                        + lowerBound + ", " + upperBound + "]: " + text);
            }
            ranges.add(new Range(min, max));
        }
        // Validate the complete group first: a later invalid token must not trigger a large allocation.
        for (Range range : ranges) {
            // A long induction variable also handles a range ending at Integer.MAX_VALUE.
            for (long value = range.min(); value <= range.max(); value++) {
                result.add((int) value);
            }
        }
        return result;
    }

    /**
     * Inclusive range retained while the remaining input tokens are validated.
     *
     * @param min first value in the range
     * @param max last value in the range
     */
    private record Range(int min, int max) { }
}
