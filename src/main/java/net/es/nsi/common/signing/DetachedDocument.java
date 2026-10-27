package net.es.nsi.common.signing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import javax.xml.crypto.Data;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.TransformException;
import javax.xml.crypto.dsig.TransformService;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 * Resolves a detached payload exclusively from the caller's DOM without external I/O.
 *
 * <p>The fixed URI binds a signature to the complete supplied document, including
 * comments and attributes. The document is borrowed and must remain stable during use.
 */
final class DetachedDocument implements URIDereferencer {
    /**
     * Local protocol identifier for the single detached document payload.
     */
    static final String REFERENCE_URI = "urn:net:es:nsi:common:document";
    /**
     * Borrowed payload DOM; never modified by dereferencing.
     */
    private final Document document;

    /**
     * Binds the local payload reference to a document.
     *
     * @param document non-null payload with a document element
     * @throws NullPointerException if the document is null
     * @throws IllegalArgumentException if no document element exists
     */
    DetachedDocument(Document document) {
        this.document = Objects.requireNonNull(document, "document");
        if (document.getDocumentElement() == null) {
            throw new IllegalArgumentException("The detached document must have a document element");
        }
    }

    /**
     * Canonicalizes the entire payload as XML octets using the public DOM signature API.
     *
     * @param reference reference whose URI must equal the fixed payload URN
     * @param context non-null cryptographic context for canonicalization
     * @return inclusive canonical XML with comments
     * @throws URIReferenceException if the URI is unexpected or canonicalization fails
     * @throws NullPointerException if either argument is null
     */
    @Override
    public Data dereference(URIReference reference, XMLCryptoContext context) throws URIReferenceException {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(context, "context");
        if (!REFERENCE_URI.equals(reference.getURI())) {
            throw new URIReferenceException("Unexpected detached document reference: " + reference.getURI());
        }

        try {
            // External references must return octets. Canonicalize through the public DOM API.
            TransformService canonicalizer = TransformService.getInstance(
                    CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS, "DOM");
            canonicalizer.init((TransformParameterSpec) null);
            Document parameters = DocumentBuilderFactory.newDefaultNSInstance()
                    .newDocumentBuilder().newDocument();
            Element transform = parameters.createElementNS(XMLSignature.XMLNS, "Transform");
            parameters.appendChild(transform);
            canonicalizer.marshalParams(new DOMStructure(transform), context);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            canonicalizer.transform(nodes(), context, output);
            return new OctetStreamData(new ByteArrayInputStream(output.toByteArray()),
                    REFERENCE_URI, "application/xml");
        } catch (GeneralSecurityException | MarshalException | TransformException
                | ParserConfigurationException ex) {
            throw new URIReferenceException("Could not canonicalize the detached document", ex);
        }
    }

    /**
     * Collects document nodes and attributes for canonicalization, excluding a document type.
     * An iterative traversal avoids consuming the Java call stack on deeply nested XML.
     *
     * @return a repeatable node-set view retaining the original DOM nodes
     */
    private NodeSetData<Node> nodes() {
        List<Node> nodes = new ArrayList<>();
        Deque<Node> pending = new ArrayDeque<>();
        pending.push(document);
        while (!pending.isEmpty()) {
            Node node = pending.pop();
            if (node.getNodeType() != Node.DOCUMENT_TYPE_NODE) {
                nodes.add(node);
            }
            NamedNodeMap attributes = node.getAttributes();
            if (attributes != null) {
                for (int i = 0; i < attributes.getLength(); i++) {
                    nodes.add(attributes.item(i));
                }
            }
            // Reverse insertion preserves document order when the stack is consumed.
            for (Node child = node.getLastChild(); child != null; child = child.getPreviousSibling()) {
                pending.push(child);
            }
        }
        List<Node> result = Collections.unmodifiableList(nodes);
        return result::iterator;
    }
}
