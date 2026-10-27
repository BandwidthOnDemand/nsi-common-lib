package net.es.nsi.common.jaxb;

/**
 * Legacy singleton entry point for separately supplied NMWG JAXB bindings.
 *
 * <p>This repository does not generate {@code net.es.nsi.common.jaxb.nmwg}. An
 * application using this parser must supply that binding package on its classpath
 * before first access; otherwise singleton initialization fails.
 *
 * @author hacksaw
 */
public class NmwgParser extends JaxbParser {
    /**
     * Optional legacy binding package that the consuming application must supply.
     */
    private static final String PACKAGES = "net.es.nsi.common.jaxb.nmwg";

    /**
     * Initializes the legacy binding context supplied by the application.
     */
    private NmwgParser() {
        super(PACKAGES);
    }

    /**
     * Defers binding lookup until the legacy parser is first requested.
     */
    private static class ParserHolder {
        /**
         * Parser instance initialized with the application-provided NMWG binding package.
         */
        public static final NmwgParser INSTANCE = new NmwgParser();
    }

    /**
     * Returns the legacy parser, initializing its binding context on first access.
     *
     * @return the shared parser instance
     * @throws ExceptionInInitializerError if the legacy binding package cannot be initialized on first access
     */
    public static NmwgParser getInstance() {
            return ParserHolder.INSTANCE;
    }
}
