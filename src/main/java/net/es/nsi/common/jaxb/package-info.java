/**
 * Handwritten DOM and Jakarta JAXB adapters around generated NSI schema bindings.
 *
 * <p>XML parsing is namespace-aware and rejects DTDs and external entities. String
 * overloads consume already decoded characters; byte-stream overloads honor XML
 * encoding declarations. Borrowed input streams remain caller-owned, while methods
 * that open files close their own streams.
 *
 * <p>Parser instances retain a JAXB context and create marshallers and unmarshallers
 * per operation. Formatting can introduce whitespace and should precede signing.
 */
package net.es.nsi.common.jaxb;
