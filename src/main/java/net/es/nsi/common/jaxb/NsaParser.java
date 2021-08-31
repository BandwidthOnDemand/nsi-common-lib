package net.es.nsi.common.jaxb;

import java.io.IOException;
import java.io.InputStream;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import net.es.nsi.common.jaxb.nsa.NsaType;
import net.es.nsi.common.jaxb.nsa.ObjectFactory;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class NsaParser extends JaxbParser {

  private static final String PACKAGES = "net.es.nsi.common.jaxb.nsa";
  private static final ObjectFactory FACTORY = new ObjectFactory();

  private NsaParser() {
    super(PACKAGES);
  }

  /**
   * An internal static class that invokes our private constructor on object creation.
   */
  private static class ParserHolder {

    public static final NsaParser INSTANCE = new NsaParser();
  }

  /**
   * Returns an instance of this singleton class.
   *
   * @return An object of the NmwgParser.
   */
  public static NsaParser getInstance() {
    return ParserHolder.INSTANCE;
  }

  public NsaType readDocument(String filename) throws JAXBException, IOException {
    return this.parseFile(NsaType.class, filename);
  }

  public NsaType readDocument(InputStream is) throws JAXBException, IOException {
    return this.xml2Jaxb(NsaType.class, is);
  }

  public void writeDocument(String file, NsaType nsa) throws JAXBException, IOException {
    // Parse the specified file.
    JAXBElement<NsaType> element = FACTORY.createNsa(nsa);
    this.writeFile(element, file);
  }
}
