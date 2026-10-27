package net.es.nsi.common.signing;

import com.google.common.base.Strings;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import net.es.nsi.common.constants.Properties;

/**
 * Loads signing keys and leaf X.509 certificates from a file-backed Java keystore.
 *
 * <p>The keystore password also protects each private-key entry. Constructors read and
 * close the keystore file immediately; later lookups use the loaded in-memory store.
 * Certificate access does not establish certificate trust or check validity dates.
 *
 * @author hacksaw
 */
public class KeyStoreHandler {
    /**
     * Path of the keystore loaded during construction.
     */
    private final String keyStore;
    /**
     * Password used to open the store and recover private-key entries.
     */
    private final String keyStorePassword;
    /**
     * Java keystore format, such as {@code PKCS12}.
     */
    private final String keyStoreType;
    /**
     * Loaded keystore used by subsequent alias lookups.
     */
    private KeyStore ks;

    /**
     * Loads the store configured by the {@code javax.net.ssl.keyStore}, password,
     * and type system properties, using {@link Properties} defaults when absent.
     *
     * @throws KeyStoreException if the filename is empty or the keystore type is unavailable
     * @throws FileNotFoundException if the keystore file cannot be opened
     * @throws IOException if the store cannot be read or its password is incorrect
     * @throws NoSuchAlgorithmException if the integrity-check algorithm is unavailable
     * @throws CertificateException if a stored certificate cannot be loaded
     */
    public KeyStoreHandler() throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        keyStore = System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE, Properties.DEFAULT_SSL_KEYSTORE);
        keyStorePassword = System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_PASSWORD, Properties.DEFAULT_SSL_KEYSTORE_PASSWORD);
        keyStoreType = System.getProperty(Properties.SYSTEM_PROPERTY_SSL_KEYSTORE_TYPE, Properties.DEFAULT_SSL_KEYSTORE_TYPE);

        if (Strings.isNullOrEmpty(keyStore)) {
            throw new KeyStoreException("Keystore file name not provided (javax.net.ssl.keyStore).");
        }

        ks = KeyStore.getInstance(keyStoreType);
        try (FileInputStream source = new FileInputStream(keyStore)) {
            ks.load(source, keyStorePassword.toCharArray());
        }
    }

    /**
     * Loads a keystore from the specified file.
     *
     * @param keyStore nonempty keystore filename
     * @param keyStorePassword non-null store and private-key password
     * @param keyStoreType non-null keystore format supported by a registered provider
     * @throws KeyStoreException if the filename is empty or the keystore type is unavailable
     * @throws FileNotFoundException if the keystore file cannot be opened
     * @throws IOException if the store cannot be read or its password is incorrect
     * @throws NoSuchAlgorithmException if the integrity-check algorithm is unavailable
     * @throws CertificateException if a stored certificate cannot be loaded
     */
    public KeyStoreHandler(String keyStore, String keyStorePassword, String keyStoreType) throws KeyStoreException, FileNotFoundException, IOException, NoSuchAlgorithmException, CertificateException {
        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyStoreType = keyStoreType;

        if (Strings.isNullOrEmpty(keyStore)) {
            throw new KeyStoreException("Keystore file name not provided (javax.net.ssl.keyStore).");
        }

        ks = KeyStore.getInstance(keyStoreType);
        try (FileInputStream source = new FileInputStream(keyStore)) {
            ks.load(source, keyStorePassword.toCharArray());
        }
    }

    /**
     * Looks up an alias and requires it to contain a private key and certificate.
     *
     * @param alias keystore alias to resolve
     * @return the private-key entry
     * @throws NoSuchAlgorithmException if the entry recovery algorithm is unavailable
     * @throws UnrecoverableEntryException if the entry cannot be recovered with the configured password
     * @throws KeyStoreException if the alias does not identify a private-key entry or the store cannot be accessed
     */
    public KeyStore.PrivateKeyEntry getPrivateKeyEntry(String alias) throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
        KeyStore.Entry entry = ks.getEntry(alias, new KeyStore.PasswordProtection(keyStorePassword.toCharArray()));
        if (!(entry instanceof KeyStore.PrivateKeyEntry privateKey)) {
            throw new KeyStoreException("No private key entry for alias: " + alias);
        }
        return privateKey;
    }

    /**
     * Returns the leaf certificate associated with a private-key alias.
     *
     * @param alias private-key alias
     * @return the X.509 certificate
     * @throws NoSuchAlgorithmException if the entry recovery algorithm is unavailable
     * @throws UnrecoverableEntryException if the entry cannot be recovered with the configured password
     * @throws KeyStoreException if the alias does not identify a private-key entry or the store cannot be accessed
     * @throws ClassCastException if the entry contains a certificate of another type
     */
    public X509Certificate getX509Certificate(String alias) throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
        return (X509Certificate) getPrivateKeyEntry(alias).getCertificate();
    }

    /**
     * Builds signature key information containing the subject name and leaf certificate.
     * The certificate chain is not included.
     *
     * @param alias private-key alias whose certificate is embedded
     * @param fac signature factory that creates the key information
     * @return new key information for the alias
     * @throws NoSuchAlgorithmException if the entry recovery algorithm is unavailable
     * @throws UnrecoverableEntryException if the entry cannot be recovered with the configured password
     * @throws KeyStoreException if the alias does not identify a private-key entry or the store cannot be accessed
     */
    public KeyInfo getKeyInfo(String alias, XMLSignatureFactory fac) throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509Certificate x509Certificate = getX509Certificate(alias);
        List<Object> x509Content = new ArrayList<>();
        x509Content.add(x509Certificate.getSubjectX500Principal().getName());
        x509Content.add(x509Certificate);
        X509Data xd = kif.newX509Data(x509Content);
        KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));
        return ki;
    }
}
