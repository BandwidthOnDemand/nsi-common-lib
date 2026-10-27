/**
 * XML serialization, MIME encoding, gzip compression, date, URI, and identifier helpers.
 *
 * <p>Text codecs use UTF-8. Compression is applied before transfer encoding and
 * reversed after transfer decoding. Stream-returning codecs transfer responsibility
 * for their wrappers to the caller; materializing methods document borrowed-stream
 * ownership separately.
 *
 * <p>XML date conversions preserve instants while creating calendars in the JVM
 * default time zone. URI helpers inspect or construct identifiers without fetching
 * their targets.
 */
package net.es.nsi.common.util;
