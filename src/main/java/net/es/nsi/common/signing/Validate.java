package net.es.nsi.common.signing;

import java.util.List;
import java.util.Objects;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Verifies XML signature integrity using a certificate embedded in KeyInfo.
 *
 * <p>Each input must contain exactly one XML Signature. Secure validation is enabled.
 * Successful verification does not establish certificate trust, validity, or signer
 * identity; applications must enforce those policies separately.
 */
public class Validate {
    /**
     * Logger for diagnostic details when cryptographic validation fails.
     */
    private static final Logger LOG = LogManager.getLogger(Validate.class);

    /**
     * Validates the unique enveloped signature and its signed references.
     *
     * @param doc non-null namespace-aware document containing one signature
     * @return {@code true} if the signature value and all references validate
     * @throws Exception if the signature is missing, ambiguous, malformed, or cannot be processed
     */
    public static boolean validateEnveloped(Document doc) throws Exception {
        DOMValidateContext context = context(doc);
        XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);
        return validate(signature, context);
    }

    /**
     * Validates a detached signature against a caller-supplied payload.
     * Only one reference to the fixed payload URN and one inclusive canonicalization
     * transform with comments are accepted. Payload resolution performs no external I/O.
     *
     * @param doc non-null payload document with a document element
     * @param sig non-null document whose root is the sole XML Signature
     * @return {@code true} if the reference policy, signature value, and payload digest all validate
     * @throws Exception if either document or the signature cannot be processed
     */
    public static boolean validateExternal(Document doc, Document sig) throws Exception {
        DetachedDocument payload = new DetachedDocument(doc);
        DOMValidateContext context = context(sig);
        Element root = sig.getDocumentElement();
        if (!XMLSignature.XMLNS.equals(root.getNamespaceURI()) || !"Signature".equals(root.getLocalName())) {
            throw new MarshalException("The detached signature must be the document element");
        }
        context.setURIDereferencer(payload);
        XMLSignature signature = XMLSignatureFactory.getInstance("DOM").unmarshalXMLSignature(context);
        List<Reference> references = signature.getSignedInfo().getReferences();
        if (references.size() != 1 || !DetachedDocument.REFERENCE_URI.equals(references.get(0).getURI())) {
            return false;
        }
        List<Transform> transforms = references.get(0).getTransforms();
        if (transforms.size() != 1
                || !CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS.equals(transforms.get(0).getAlgorithm())) {
            return false;
        }
        return validate(signature, context);
    }

    /**
     * Finds exactly one signature and enables secure validation with cached references.
     *
     * @param document non-null signature-bearing document
     * @return validation context rooted at the signature
     * @throws MarshalException if no signature or more than one signature is present
     */
    private static DOMValidateContext context(Document document) throws MarshalException {
        Objects.requireNonNull(document, "document");
        NodeList signatures = document.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
        if (signatures.getLength() == 0) {
            throw new MarshalException("Cannot find Signature element");
        }
        if (signatures.getLength() != 1) {
            throw new MarshalException("Expected exactly one Signature element");
        }
        DOMValidateContext context = new DOMValidateContext(new X509KeySelector(), signatures.item(0));
        context.setProperty("org.jcp.xml.dsig.secureValidation", Boolean.TRUE);
        context.setProperty("javax.xml.crypto.dsig.cacheReference", Boolean.TRUE);
        return context;
    }

    /**
     * Checks signature integrity and logs signature-value diagnostics on failure.
     *
     * @param signature unmarshalled signature
     * @param context validation context with the required key and URI resolvers
     * @return the combined signature and reference validation result
     * @throws Exception if cryptographic validation cannot be completed
     */
    private static boolean validate(XMLSignature signature, DOMValidateContext context) throws Exception {
        boolean valid = signature.validate(context);
        if (!valid) {
            LOG.debug("Signature failed validation; signature value valid = {}",
                    signature.getSignatureValue().validate(context));
        }
        return valid;
    }
}
