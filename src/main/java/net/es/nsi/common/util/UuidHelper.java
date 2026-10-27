package net.es.nsi.common.util;

import java.util.UUID;

/**
 * Creates randomly generated UUID URNs for NSI identifiers.
 *
 * @author hacksaw
 */
public class UuidHelper {
  /**
   * URN namespace prefix for UUID identifiers.
   */
  private final static String URN_UUID = "urn:uuid:";

  /**
   * Generates a random version-4 UUID in URN form.
   *
   * @return a value such as {@code urn:uuid:550e8400-e29b-41d4-a716-446655440000}
   */
  public static String getUUID() {
    return URN_UUID + UUID.randomUUID().toString();
  }
}
