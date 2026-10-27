package net.es.nsi.common.signing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Creates temporary signing fixtures with the keytool shipped in the running JDK.
 */
final class SigningTestSupport {
    /**
     * Test-only password generated once at runtime and shared by disposable signing fixtures
     * for the lifetime of this class.
     */
    static final String PASSWORD = generatePassword();
    /**
     * Prevents construction of the signing-fixture utility.
     */
    private SigningTestSupport() { }

    /**
     * Generates an ASCII password suitable for keytool and PKCS12 test fixtures.
     *
     * @return a hexadecimal encoding of 16 securely generated random bytes
     */
    private static String generatePassword() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * Creates a one-day self-signed PKCS12 fixture with alias {@code signer}.
     * Process output is captured for assertion diagnostics, and a timeout prevents hung builds.
     *
     * @param path new keystore path in a test-owned temporary directory
     * @param algorithm keytool key algorithm, such as RSA, DSA, or EC
     * @throws Exception if fixture preparation or the exercised API fails unexpectedly
     */
    static void createKeystore(Path path, String algorithm) throws Exception {
        Path output = path.resolveSibling(path.getFileName() + ".log");
        Process process = new ProcessBuilder(
                Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
                "-genkeypair", "-alias", "signer", "-keyalg", algorithm,
                "-keysize", algorithm.equals("EC") ? "256" : "2048", "-validity", "1",
                "-dname", "CN=NSI Signing Test", "-storetype", "PKCS12", "-keystore", path.toString(),
                "-storepass", PASSWORD, "-keypass", PASSWORD, "-noprompt")
                .redirectErrorStream(true).redirectOutput(output.toFile()).start();
        if (!process.waitFor(30, TimeUnit.SECONDS)) {
            process.destroyForcibly().waitFor();
            fail("keytool timed out for " + algorithm);
        }
        assertEquals(0, process.exitValue(), Files.readString(output));
    }
}
