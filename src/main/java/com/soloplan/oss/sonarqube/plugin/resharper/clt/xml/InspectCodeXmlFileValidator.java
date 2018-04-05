package com.soloplan.oss.sonarqube.plugin.resharper.clt.xml;

import org.sonar.api.utils.log.Logger;
import org.sonar.api.utils.log.Loggers;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Result;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.InputStream;
import java.net.URL;

/**
 * A validator class for issue files generated by the {@code InspectCode} command line tool using an XML Schema Definition file location
 * within resources of the plugin.
 */
public final class InspectCodeXmlFileValidator {

  /**
   * Defines the base path within the resources of the JAR file where the XML Schema Definition (XSD) file is located, which purposely
   * matches the package name of this class using slashes as separators instead of full stops. Additionally, this value is used within the
   * XSD file to define the namespace using the Uniform Resource Name (URN) scheme.
   * <p/>
   * The value of this literal should correspond to {@code /com/soloplan/oss/sonarqube/plugin/resharper/clt/xml}.
   */
  private static final String RESOURCE_URN_BASE =
      "/" + InspectCodeXmlFileValidator.class.getPackageName().replaceAll("\\.", "/");

  /**
   * Defines the full path within the resources of the JAR file where the XML Schema Definition (XSD) file is located, including its file
   * name.
   * <p/>
   * The value of this constant should correspond to {@code /com/soloplan/oss/sonarqube/plugin/resharper/clt/xml/inspectcode_schema_definition.xsd}.
   */
  private static final String INSPECTCODE_XSD_RESOURCE =
      RESOURCE_URN_BASE + "/inspectcode_schema_definition.xsd";

  /**
   * Defines the XML namespace used within the XML Schema Definition (XSD) including the Uniform Resource Name scheme ({@code urn:}).
   * <p/>
   * The value of this constant should correspond to {@code urn:/com/soloplan/oss/sonarqube/plugin/resharper/clt/xml}.
   */
  private static final String INSPECTCODE_XSD_NAMESPACE =
      "urn:" + RESOURCE_URN_BASE;

  /**
   * Gets an implementation of the {@link Logger} interface for this class.
   * <p/>
   * Please note, that message arguments are defined with {@code {}}, but not with
   * <a href="https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html">Formatter</a> syntax.
   *
   * @see Logger
   */
  private static final Logger LOGGER = Loggers.get(InspectCodeXmlFileValidator.class);

  /**
   * Validates the supplied input stream against the XML Schema Definition for output files of the {@code InspectCode} command line tool.
   *
   * @param xmlFileInputStream
   *     The {@link InputStream} of the XML file to be validated against the XML Schema Definition.
   *
   * @return {@code True}, if the supplied XML has been validated successfully, {@code false} otherwise.
   */
  public static boolean validateXmlFile(InputStream xmlFileInputStream) {
    try {
      // Create a validator instance using the XML schema definition from the resources of the plugin
      final SchemaFactory factory = javax.xml.validation.SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      final URL xmlSchemaUrl = InspectCodeXmlFileValidator.class.getResource(INSPECTCODE_XSD_RESOURCE);
      final Validator validator = factory.newSchema(xmlSchemaUrl).newValidator();

      // Create a SAXParserFactory instance and set it up to support XML namespaces
      final SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
      saxParserFactory.setNamespaceAware(true);

      final SAXSource source = new SAXSource(new NamespaceFilter(saxParserFactory.newSAXParser().getXMLReader()), new InputSource(xmlFileInputStream));
      final Result validationResult = new SAXResult();
      validator.validate(source, validationResult);

      return true;

    } catch (Exception e) {
      LOGGER.error("An exception occurred while trying to validate the supplied XML input stream.", e);
    }

    return false;
  }

  /**
   * This class should infer the namespace of the xml schema definition for each XML file, because the output of the {@code InspectCode}
   * command line tool does not include such a definition.
   * <p/>
   * Apparently, this simply does not work, and I have no clue why... :-/
   */
  private static class NamespaceFilter
      extends XMLFilterImpl {

    /**
     * Creates a new instance of the {@link NamespaceFilter} class, supplying the argument {@code xmlReader} to the base class.
     *
     * @param xmlReader
     *     The implementation of the {@link XMLReader} interface to be used within this instance.
     */
    private NamespaceFilter(XMLReader xmlReader) {
      super(xmlReader);
    }

    @Override
    public void startElement(String uri, String localName, String qualifiedName, Attributes attributes)
        throws SAXException {
      // Apply default XML namespace if no namespace is set
      if (uri == null || !uri.trim().equalsIgnoreCase(INSPECTCODE_XSD_NAMESPACE)) {
        uri = INSPECTCODE_XSD_NAMESPACE;
      }
      super.startElement(uri, localName, qualifiedName, attributes);
    }
  }
}
