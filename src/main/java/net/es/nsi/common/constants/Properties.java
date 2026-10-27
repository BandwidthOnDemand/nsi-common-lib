package net.es.nsi.common.constants;

/**
 * Shared JVM property keys and legacy configuration defaults used by NSI libraries.
 *
 * <p>Keys using the {@code javax.net.ssl} prefix belong to Java SE and keep that
 * spelling even though XML Binding and Mail APIs use Jakarta packages.
 *
 * @author hacksaw
 */
public class Properties {
    // System properties.
    /**
     * Property naming the application base directory.
     */
    public static final String SYSTEM_PROPERTY_BASEDIR = "basedir";
    /**
     * Property naming the application configuration directory.
     */
    public static final String SYSTEM_PROPERTY_CONFIGDIR = "configdir";

    /**
     * Property used to request application debug behavior.
     */
    public static final String SYSTEM_PROPERTY_DEBUG = "debug";
    /**
     * Property naming a Log4j configuration file.
     */
    public static final String SYSTEM_PROPERTY_LOG4J = "log4j.configurationFile";
    /**
     * JSSE property naming the private-key keystore file.
     */
    public static final String SYSTEM_PROPERTY_SSL_KEYSTORE = "javax.net.ssl.keyStore";
    /**
     * JSSE property supplying the keystore password.
     */
    public static final String SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";
    /**
     * JSSE property selecting the keystore format.
     */
    public static final String SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";
    /**
     * JSSE property naming the trusted-certificate store file.
     */
    public static final String SYSTEM_PROPERTY_SSL_TRUSTSTORE = "javax.net.ssl.trustStore";
    /**
     * JSSE property supplying the truststore password.
     */
    public static final String SYSTEM_PROPERTY_SSL_TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";
    /**
     * JSSE property selecting the truststore format.
     */
    public static final String SYSTEM_PROPERTY_SSL_TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";

    /**
     * Fallback keystore path, relative to the working directory.
     */
    public static final String DEFAULT_SSL_KEYSTORE = "config/keystore.jks";
    /**
     * Legacy fallback keystore password.
     */
    public static final String DEFAULT_SSL_KEYSTORE_PASSWORD = "changeit";
    /**
     * Fallback keystore format.
     */
    public static final String DEFAULT_SSL_KEYSTORE_TYPE = "JKS";
    /**
     * Fallback truststore path, relative to the working directory.
     */
    public static final String DEFAULT_SSL_TRUSTSTORE = "config/truststore.jks";
    /**
     * Legacy fallback truststore password.
     */
    public static final String DEFAULT_SSL_TRUSTSTORE_PASSWORD = "changeit";
    /**
     * Fallback truststore format.
     */
    public static final String DEFAULT_SSL_TRUSTSTORE_TYPE = "JKS";
}
