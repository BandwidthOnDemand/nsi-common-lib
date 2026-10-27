/**
 * Domain helpers for NSI service termination points, labels, and NML topologies.
 *
 * <p>Label expressions use short family names such as {@code vlan} and {@code mpls};
 * schema-facing helpers translate these to the corresponding namespace URIs.
 * Domain objects are mutable. Collection accessors document whether membership is
 * copied, shared, or exposed through a read-only view; contained JAXB objects and
 * labels can still be mutable.
 *
 * <p>Generated schema bindings live below {@code net.es.nsi.common.jaxb}. Their
 * definitions and generated documentation originate in {@code src/main/schema}.
 */
package net.es.nsi.common;
