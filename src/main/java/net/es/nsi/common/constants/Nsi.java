package net.es.nsi.common.constants;

/**
 * Wire identifiers shared by NSI discovery, topology, and connection-service clients.
 *
 * <p>These strings are protocol data rather than display labels. Historical service
 * identifiers are retained for interoperability with existing peers.
 *
 * @author hacksaw
 */
public class Nsi {

  /**
   * Generic XML media type.
   */
  public static final String APPLICATION_XML = "application/xml";
  /**
   * XML media type for DDS version 1.
   */
  public static final String NSI_DDS_V1_XML = "application/vnd.ogf.nsi.dds.v1+xml";
  /**
   * JSON media type for DDS version 1.
   */
  public static final String NSI_DDS_V1_JSON = "application/vnd.ogf.nsi.dds.v1+json";
  /**
   * Transfer-encoding token for Base64 DDS content.
   */
  public static final String NSI_DDS_BASE64 = "base64";
  /**
   * Media type for an NSA discovery document, version 1.
   */
  public static final String NSI_NSA_V1 = "application/vnd.ogf.nsi.nsa.v1+xml";
  /**
   * Media type for an NSI topology document, version 1.
   */
  public static final String NSI_TOPOLOGY_V1 = "application/vnd.ogf.nsi.topology.v1+xml";
  /**
   * Media type for an NSI topology document, version 2.
   */
  public static final String NSI_TOPOLOGY_V2 = "application/vnd.ogf.nsi.topology.v2+xml";
  /**
   * SOAP media type for NSI connection-service version 2.
   */
  public static final String NSI_CS_V2 = "application/vnd.org.ogf.nsi.cs.v2+soap";

  /**
   * SOAP requester media type for connection-service version 2.
   */
  public static final String NSI_CS_REQUESTER_V2 = "application/vnd.ogf.nsi.cs.v2.requester+soap";
  /**
   * SOAP provider media type for connection-service version 2.
   */
  public static final String NSI_CS_PROVIDER_V2 = "application/vnd.ogf.nsi.cs.v2.provider+soap";

  /**
   * Legacy SOAP provider media type for connection-service version 1.1.
   */
  public static final String NSI_CS_PROVIDER_V1_1 = "application/vnd.ogf.nsi.cs.v1-1.provider+soap";
  /**
   * Legacy SOAP requester media type for connection-service version 1.1.
   */
  public static final String NSI_CS_REQUESTER_V1_1 = "application/vnd.ogf.nsi.cs.v1-1.requester+soap";

  /**
   * Legacy SOAP provider media type for connection-service version 1.
   */
  public static final String NSI_CS_PROVIDER_V1 = "application/vnd.ogf.nsi.cs.v1.provider+soap";
  /**
   * Legacy SOAP requester media type for connection-service version 1.
   */
  public static final String NSI_CS_REQUESTER_V1 = "application/vnd.ogf.nsi.cs.v1.requester+soap";

  /**
   * DDS document-type token for NSA discovery version 1, without the application prefix.
   */
  public static final String NSI_DOC_TYPE_NSA_V1 = "vnd.ogf.nsi.nsa.v1+xml";
  /**
   * DDS document-type token for topology version 1, without the application prefix.
   */
  public static final String NSI_DOC_TYPE_TOPOLOGY_V1 = "vnd.ogf.nsi.topology.v1+xml";
  /**
   * DDS document-type token for topology version 2, without the application prefix.
   */
  public static final String NSI_DOC_TYPE_TOPOLOGY_V2 = "vnd.ogf.nsi.topology.v2+xml";

  /**
   * Discovery feature identifier for the NSI uRA role.
   */
  public static final String NSI_CS_URA = "vnd.ogf.nsi.cs.v2.role.uRA";
  /**
   * Discovery feature identifier for the NSI uPA role.
   */
  public static final String NSI_CS_UPA = "vnd.ogf.nsi.cs.v2.role.uPA";
  /**
   * Discovery feature identifier for the NSI aggregator role.
   */
  public static final String NSI_CS_AGG = "vnd.ogf.nsi.cs.v2.role.aggregator";

  /**
   * Discovery topology relation identifying an NSA peer.
   */
  public static final String NML_PEERSWITH_RELATION = "http://schemas.ogf.org/nsi/2013/09/topology#peersWith";

  // Point-to-point service definitions.
  /**
   * EVTS point-to-point service-definition identifier.
   */
  public static final String NSI_SERVICETYPE_EVTS = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";
  /**
   * Legacy OpenNSA point-to-point QName representation.
   */
  public static final String NSI_SERVICETYPE_EVTS_OPENNSA_1 = "{http://schemas.ogf.org/nsi/2013/12/services/point2point}p2ps";
  /**
   * Legacy OpenNSA EVTS service-description URI.
   */
  public static final String NSI_SERVICETYPE_EVTS_OPENNSA_2 = "http://services.ogf.org/nsi/2013/07/descriptions/EVTS.A-GOLE";
  /**
   * Legacy OSCARS EVTS service-definition URI.
   */
  public static final String NSI_SERVICETYPE_EVTS_OSCARS = "http://services.ogf.org/nsi/2013/07/definitions/EVTS.A-GOLE";
  /**
   * ESnet layer-2 point-to-point service-description URI.
   */
  public static final String NSI_SERVICETYPE_L2_LB_ES = "http://services.ogf.org/nsi/2018/06/descriptions/l2-lb-es";

  // Multi-point service definition.
  /**
   * ESnet layer-2 multipoint service-description URI.
   */
  public static final String NSI_SERVICETYPE_L2_MP_ES = "http://services.ogf.org/nsi/2018/06/descriptions/l2-mp-es";
}
