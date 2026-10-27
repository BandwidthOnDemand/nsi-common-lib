package net.es.nsi.common.signing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStoreException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests keystore loading, certificate metadata, and explicit lookup failure contracts.
 */
class KeyStoreHandlerTest {
    /**
     * JUnit-managed directory for valid and invalid keystore fixtures.
     */
    @TempDir static Path directory;
    /**
     * RSA keystore created once for this suite.
     */
    static Path store;

    /**
     * Creates one RSA store for the suite in its temporary directory.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @BeforeAll static void createStore() throws Exception {
        store = directory.resolve("test.p12");
        SigningTestSupport.createKeystore(store, "RSA");
    }

    /**
     * Checks key material, embedded certificate data, and continued access after moving the store file.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void loadsPrivateKeysCertificatesAndKeyInfoAndReleasesTheFile() throws Exception {
        Path copy = directory.resolve("copy.p12");
        Files.copy(store, copy);
        KeyStoreHandler handler = new KeyStoreHandler(copy.toString(), SigningTestSupport.PASSWORD, "PKCS12");
        assertEquals("RSA", handler.getPrivateKeyEntry("signer").getPrivateKey().getAlgorithm());
        var certificate = handler.getX509Certificate("signer");
        var info = handler.getKeyInfo("signer", XMLSignatureFactory.getInstance("DOM"));
        assertInstanceOf(X509Data.class, info.getContent().get(0));
        assertTrue(((X509Data) info.getContent().get(0)).getContent().contains(certificate));
        Files.move(copy, directory.resolve("moved.p12"));
        assertEquals(certificate, handler.getX509Certificate("signer"));
    }

    /**
     * Requires checked failures for unavailable stores, bad passwords, unknown formats, and corrupted bytes.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void missingFilesWrongPasswordsInvalidTypesAndCorruptionHaveExplicitFailures() throws Exception {
        assertThrows(IOException.class, () -> new KeyStoreHandler(directory.resolve("missing.p12").toString(), SigningTestSupport.PASSWORD, "PKCS12"));
        assertThrows(IOException.class, () -> new KeyStoreHandler(store.toString(), SigningTestSupport.PASSWORD + 'x', "PKCS12"));
        assertThrows(KeyStoreException.class, () -> new KeyStoreHandler(store.toString(), SigningTestSupport.PASSWORD, "unsupported-type"));
        assertThrows(KeyStoreException.class, () -> new KeyStoreHandler("", SigningTestSupport.PASSWORD, "PKCS12"));
        Path corrupt = Files.writeString(directory.resolve("corrupt.p12"), "not a keystore");
        assertThrows(IOException.class, () -> new KeyStoreHandler(corrupt.toString(), SigningTestSupport.PASSWORD, "PKCS12"));
    }

    /**
     * Ensures missing private keys and certificates produce keystore errors.
     *
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    @Test
    void missingAliasesNeverBecomeNullPointerFailures() throws Exception {
        KeyStoreHandler handler = new KeyStoreHandler(store.toString(), SigningTestSupport.PASSWORD, "PKCS12");
        assertThrows(KeyStoreException.class, () -> handler.getPrivateKeyEntry("absent"));
        assertThrows(KeyStoreException.class, () -> handler.getX509Certificate("absent"));
    }
}
