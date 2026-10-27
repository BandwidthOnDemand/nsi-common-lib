package net.es.nsi.common.util;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Compatibility test for decoding a fixed Base64 and gzip discovery payload.
 *
 * @author hacksaw
 */
public class DecoderTest {
  /**
   * Discovery identifier associated with the legacy payload fixture.
   */
  private static final String NSA_ID = "urn:ogf:network:jgn-x.jp:2013:nsa";
  /**
   * Transfer encoding used by the fixed fixture.
   */
  private static final String BASE64 = "base64";
  /**
   * Compression content type used by the fixed fixture.
   */
  private static final String GZIP = "application/x-gzip";
  /**
   * Fixed encoded payload so the decoder is tested independently of the encoder.
   */
  private static final String DATA = "H4sIAAAAAAAAAJVUbWvbMBD+K8H7WCz5LW0qHJcxGHQfxmApg34pqqU4au2TkGQv/fc7v9YjY2wmxnfS89w9Ot0lvzs39aaT1ikN+yAmUbCRUGqhoNoHD4fP4S64K3JwGQPHNwgGh1a2D07eG0apK0+y4Y7o6ki0rSg4RZMozmiUUKFcqTH2G67yYCEn+6C1wJT0R2a45Y1juIUbrCu5FWHWi5jB6ZIJtBUtAekp8EY6w0vp+lQpjRNagXvWArWfjbLS7YMkSpIwjsM4PURbtr1lUUbiKAqjHYswvBKjCJTNMORPbV/ZSwXhmbwY1gdlg+SlMLh0E8ZIjw9RxIbfY4B1QSXFzMsHYUXORaPgkwbPS9+XLhnPNZqtmg2riv5kDo9W1bx5FpyIEsiiYpdlKS01gCw9ajBWd0pI+2EdHlNOoSZrjo5oMdtenv0ocgQN/miuYVZ2E15heT1vTIGn3mIdt4cojfF5nPjL/ugvxFcF65ToKZTc8voi7zvyCCvGFwWbAwf+yi8YM276OLy8vthr9Lw2ACrVSegDjnujO9rzd7oV+vt91brkfb17CyrlWyGLOL0hN9fbnL4v5TWiBivdkutb3Jr9HjWHmFrrXhR/bzavja519YbCFkauwEt7xDYvcv9mZMGNqdUYmXYghpHDcSMzmXTJFY5NTgd0frLyWMyz44hxS2vRr9/v6eA8vZinXsATNDUZuAMrp6vc/yqjdCiAzG165TQ3F1L+t9n/pOcouW+t3PSh98GlAqtrSdpvHwNa5EbiAP9Q/nRRf66cJ5Vejzu+KuRVhVnfeX2rDP99xS+E6FAuKwUAAA==";
  /**
   * Exact expected XML for the pre-encoded payload.
   */
  private static final String DOCUMENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><ns4:nsa xmlns:ns4=\"http://schemas.ogf.org/nsi/2014/02/discovery/nsa\" xmlns:ns2=\"urn:ietf:params:xml:ns:vcard-4.0\" xmlns:ns3=\"http://nordu.net/namespaces/2013/12/gnsbod\" expires=\"2022-11-13T05:59:04.100-08:00\" id=\"urn:ogf:network:jgn-x.jp:2013:nsa\" version=\"2017-10-01T00:00:00Z\"><name>jgn-x.jp</name><adminContact><ns2:vcard><ns2:uid><ns2:uri>https://glambda.dcn.jgn-x.jp:8443/connectionprovider#adminContact</ns2:uri></ns2:uid><ns2:prodid><ns2:text>jgn-x</ns2:text></ns2:prodid><ns2:rev><ns2:timestamp>20150225T031111Z</ns2:timestamp></ns2:rev><ns2:kind><ns2:text>individual</ns2:text></ns2:kind><ns2:fn><ns2:text>Jin Tanaka</ns2:text></ns2:fn><ns2:n><ns2:surname>Tanaka</ns2:surname><ns2:given>Jin</ns2:given></ns2:n></ns2:vcard></adminContact><location><longitude>137.765</longitude><latitude>35.69</latitude></location><networkId>urn:ogf:network:jgn-x.jp:2013:topology</networkId><interface><type>application/vnd.ogf.nsi.topology.v2+xml</type><href>http://ns.ps.jgn-x.jp/NSI/jgn-x_jp_2013_nml.xml</href></interface><interface><type>application/vnd.ogf.nsi.cs.v2.provider+soap</type><href>https://glambda.dcn.jgn-x.jp:8443/connectionprovider</href></interface><feature type=\"vnd.ogf.nsi.cs.v2.role.uPA\"/><peersWith>urn:ogf:network:aist.go.jp:2013:nsa:nsi-aggr</peersWith></ns4:nsa>";

  /**
   * Creates the test fixture; no constructor setup is required.
   */
  public DecoderTest() {
  }

  /**
   * No suite-wide initialization is required by this fixture.
   */
  @BeforeAll
  public static void setUpClass() {
  }

  /**
   * No suite-wide cleanup is required by this fixture.
   */
  @AfterAll
  public static void tearDownClass() {
  }

  /**
   * No additional per-test setup is required.
   */
  @BeforeEach
  public void setUp() {
  }

  /**
   * No additional per-test cleanup is required.
   */
  @AfterEach
  public void tearDown() {
  }

  /**
   * Checks both explicit-layer and convenience decoders against the exact original XML.
   *
   * @throws IOException if fixture preparation or the exercised API fails unexpectedly
   */
  @Test
  public void testDecode() throws IOException {
    String output = Decoder.decode2string(BASE64, GZIP, DATA);
    assertEquals(DOCUMENT, output);

    String decode = Decoder.decode(DATA);
    assertEquals(DOCUMENT, decode);
  }
}