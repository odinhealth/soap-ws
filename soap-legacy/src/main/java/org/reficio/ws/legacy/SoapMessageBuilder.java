/**
 * Copyright (c) 2012-2013 Reficio (TM) - Reestablish your software!. All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.reficio.ws.legacy;

import com.ibm.wsdl.xml.WSDLReaderImpl;
import java.io.File;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.wsdl.Binding;
import javax.wsdl.BindingInput;
import javax.wsdl.BindingOperation;
import javax.wsdl.BindingOutput;
import javax.wsdl.Definition;
import javax.wsdl.Message;
import javax.wsdl.Part;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.wsdl.extensions.soap12.SOAP12Binding;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import org.apache.log4j.Logger;
import org.apache.xmlbeans.SchemaGlobalElement;
import org.apache.xmlbeans.SchemaType;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;
import org.reficio.ws.SoapBuilderException;
import org.reficio.ws.SoapContext;
import org.reficio.ws.annotation.ThreadSafe;
import org.reficio.ws.common.Wsdl11Writer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * This class was extracted from the soapUI code base by centeractive ag in October 2011.
 * The main reason behind the extraction was to separate the code that is responsible
 * for the generation of the SOAP messages from the rest of the soapUI's code that is
 * tightly coupled with other modules, such as soapUI's graphical user interface, etc.
 * The goal was to create an open-source java project whose main responsibility is to
 * handle SOAP message generation and SOAP transmission purely on an XML level.
 * <br/>
 * centeractive ag would like to express strong appreciation to SmartBear Software and
 * to the whole team of soapUI's developers for creating soapUI and for releasing its
 * source code under a free and open-source licence. centeractive ag extracted and
 * modifies some parts of the soapUI's code in good faith, making every effort not
 * to impair any existing functionality and to supplement it according to our
 * requirements, applying best practices of software design.
 *
 * Changes done:
 * - changing location in the package structure
 * - removal of dependencies and code parts that are out of scope of SOAP message generation
 * - minor fixes to make the class compile out of soapUI's code base
 * - rename to SoapBuilder (from SoapMessageBuilder)
 * - slight reorganization of the public API and arguments of the methods
 * - introduction of OperationWrapper and SoapContext classes
 * - addition of saveWSDL and createAndSave methods
 */

/**
 * Builds SOAP requests according to WSDL/XSD definitions
 *
 * @author Ole.Matzura
 */
@ThreadSafe
class SoapMessageBuilder {

    private final static Logger log = Logger.getLogger(SoapMessageBuilder.class);

    // should be thread safe it not modified after it has been initialized
    private final Definition definition;

    // should be thread safe it not modified after it has been initialized
    private final SchemaDefinitionWrapper definitionWrapper;

    // ----------------------------------------------------------
    // Constructors and factory methods
    // ----------------------------------------------------------
    /**
     * @param wsdlUrl url of the wsdl to import
     * @throws WSDLException thrown in case of import errors
     */
    public SoapMessageBuilder(final URL wsdlUrl) throws WSDLException {
        final WSDLReader reader = new WSDLReaderImpl();
        reader.setFeature("javax.wsdl.verbose", false);
        this.definition = reader.readWSDL(wsdlUrl.toString());
        this.definitionWrapper = new SchemaDefinitionWrapper(this.definition, wsdlUrl.toString());
    }

    /**
     * Constructs a new SoapBuilder instance importing the wsdl from the specified wsdlUrl.
     * If the import is successful it saves the wsdl/xsd files to the target folder giving
     * the top-level wsdl the specified baseName and setting the documentBaseUri to the newly
     * saved wsdl uri. If the import is not successful an exception will be thrown and files
     * will not be saved. Method expects that the targetFolder already exists.
     *
     * @param wsdlUrl      url of the wsdl to import
     * @param targetFolder folder in which all the files are be stored - folder has to exist, no subfolders are created,
     * @param fileBaseName name of the top level file, without extension -> wsdl will be added by default
     * @return instance of the soap-builder which documentBaseUri is set to the url of the locally saved wsdl
     * @throws WSDLException thrown in case of import errors
     */
    public static SoapMessageBuilder createAndSave(final URL wsdlUrl, final File targetFolder, final String fileBaseName)
            throws WSDLException {
        final SoapMessageBuilder soapBuilder = new SoapMessageBuilder(wsdlUrl);
        final URL url = soapBuilder.saveWsdl(fileBaseName, targetFolder);
        soapBuilder.getDefinition().setDocumentBaseURI(url.toString());
        return soapBuilder;
    }

    // ----------------------------------------------------------
    // WSDLs and XSDs MARSHALLER
    // ----------------------------------------------------------
    private static void saveDefinition(final String fileBaseName, final Definition definition, final File targetFolder) {
        if (targetFolder.exists() == false || targetFolder.isDirectory() == false) {
            throw new IllegalArgumentException("Target folder does not exist or is not a folder [" + targetFolder.getPath() + "]");
        }
        final Wsdl11Writer writer = new Wsdl11Writer(targetFolder);
        writer.writeWSDL(fileBaseName, definition);
    }

    private static URL getSavedWsdlUrl(final String fileBaseName, final File targetFolder) {
        final File file = new File(targetFolder, fileBaseName + ".wsdl");
        try {
            return file.toURI().toURL();
        } catch (final MalformedURLException e) {
            throw new SoapBuilderException("Error saving url", e);
        }
    }

    /**
     * Saves wsdl recursively fetching all referenced wsdls and schemas fixing their location tags
     *
     * @param fileBaseName name of the top level file, without extension -> wsdl will be added by default
     * @param targetFolder folder in which all the files are stored - folder has to exist, no subfolders are created,
     */
    public URL saveWsdl(final String fileBaseName, final File targetFolder) {
        saveDefinition(fileBaseName, this.definition, targetFolder);
        return getSavedWsdlUrl(fileBaseName, targetFolder);
    }

    /**
     * Saves wsdl recursively fetching all referenced wsdls and schemas fixing their location tags
     *
     * @param fileBaseName name of the top level file, without extension -> wsdl will be added by default
     * @param wsdlUrl      url of the wsdl to save
     * @param targetFolder folder in which all the files are be stored - folder has to exist, no subfolders are created,
     * @throws WSDLException thrown in case of import errors
     */
    public static URL saveWsdl(final String fileBaseName, final URL wsdlUrl, final File targetFolder) throws WSDLException {
        final WSDLReader reader = new WSDLReaderImpl();
        reader.setFeature("javax.wsdl.verbose", false);
        final Definition definition = reader.readWSDL(wsdlUrl.toString());
        saveDefinition(fileBaseName, definition, targetFolder);
        return getSavedWsdlUrl(fileBaseName, targetFolder);
    }

    // ----------------------------------------------------------
    // EMPTY MESSAGE GENERATORS
    // ----------------------------------------------------------
    public String buildEmptyMessage(final QName bindingQName, final SoapContext context) {
        return buildEmptyMessage(getSoapVersion(getBindingByName(bindingQName)), context);
    }

    public String buildEmptyMessage(final Binding binding, final SoapContext context) {
        return buildEmptyMessage(getSoapVersion(binding), context);
    }

    public static String buildEmptyMessage(final SoapVersion soapVersion, final SoapContext context) {
        final SampleXmlUtil generator = new SampleXmlUtil(false, context);
        return generator.createSample(soapVersion.getEnvelopeType());
    }

    // ----------------------------------------------------------
    // FAULT MESSAGE GENERATORS
    // ----------------------------------------------------------
    public static String buildFault(final String faultcode, final String faultstring, final SoapVersion soapVersion,
            final SoapContext context) {
        final SampleXmlUtil generator = new SampleXmlUtil(false, context);
        generator.setTypeComment(false);
        generator.setIgnoreOptional(true);
        String emptyResponse = buildEmptyFault(generator, soapVersion, context);
        if (soapVersion == SoapVersion.Soap11) {
            emptyResponse = XmlUtils.setXPathContent(emptyResponse, "//faultcode", faultcode);
            emptyResponse = XmlUtils.setXPathContent(emptyResponse, "//faultstring", faultstring);
        } else if (soapVersion == SoapVersion.Soap12) {
            emptyResponse = XmlUtils.setXPathContent(emptyResponse, "//soap:Value", faultcode);
            emptyResponse = XmlUtils.setXPathContent(emptyResponse, "//soap:Text", faultstring);
            emptyResponse = XmlUtils.setXPathContent(emptyResponse, "//soap:Text/@xml:lang", "en");
        }
        return emptyResponse;
    }

    public String buildFault(final String faultcode, final String faultstring, final QName bindingQName, final SoapContext context) {
        return buildFault(faultcode, faultstring, getSoapVersion(getBindingByName(bindingQName)), context);
    }

    public String buildFault(final String faultcode, final String faultstring, final Binding binding, final SoapContext context) {
        return buildFault(faultcode, faultstring, getSoapVersion(binding), context);
    }

    public String buildEmptyFault(final QName bindingQName, final SoapContext context) {
        return buildEmptyFault(getSoapVersion(getBindingByName(bindingQName)), context);
    }

    public String buildEmptyFault(final Binding binding, final SoapContext context) {
        return buildEmptyFault(getSoapVersion(binding), context);
    }

    public static String buildEmptyFault(final SoapVersion soapVersion, final SoapContext context) {
        final SampleXmlUtil generator = new SampleXmlUtil(false, context);
        final String emptyResponse = buildEmptyFault(generator, soapVersion, context);
        return emptyResponse;
    }

    // ----------------------------------------------------------
    // INPUT MESSAGE GENERATORS
    // ----------------------------------------------------------
    public String buildSoapMessageFromInput(final Binding binding, final BindingOperation bindingOperation, final SoapContext context)
            throws Exception {
        final SoapVersion soapVersion = getSoapVersion(binding);
        final boolean inputSoapEncoded = WsdlUtils.isInputSoapEncoded(bindingOperation);
        final SampleXmlUtil xmlGenerator = new SampleXmlUtil(inputSoapEncoded, context);

        final XmlObject object = XmlObject.Factory.newInstance();
        final XmlCursor cursor = object.newCursor();
        cursor.toNextToken();
        if (context.isAlwaysBuildEnvelope()) {
            cursor.beginElement(soapVersion.getEnvelopeQName());
        }

        if (inputSoapEncoded) {
            cursor.insertNamespace("xsi", Constants.XSI_NS);
            cursor.insertNamespace("xsd", Constants.XSD_NS);
        }

        cursor.toFirstChild();
        if (context.isAlwaysBuildBody()) {
            cursor.beginElement(soapVersion.getBodyQName());
        }
        cursor.toFirstChild();

        if (WsdlUtils.isRpc(this.definition, bindingOperation)) {
            buildRpcRequest(bindingOperation, soapVersion, cursor, xmlGenerator, context);
        } else {
            buildDocumentRequest(bindingOperation, cursor, xmlGenerator);
        }

        if (context.isAlwaysBuildHeaders()) {
            final BindingInput bindingInput = bindingOperation.getBindingInput();
            if (bindingInput != null) {
                final List<?> extensibilityElements = bindingInput.getExtensibilityElements();
                final List<WsdlUtils.SoapHeader> soapHeaders = WsdlUtils.getSoapHeaders(extensibilityElements);
                addHeaders(soapHeaders, soapVersion, cursor, xmlGenerator);
            }
        }
        cursor.dispose();

        try {
            final StringWriter writer = new StringWriter();
            XmlUtils.serializePretty(object, writer);
            return writer.toString();
        } catch (final Exception e) {
            e.printStackTrace();
            return object.xmlText();
        }
    }

    // ----------------------------------------------------------
    // OUTPUT MESSAGE GENERATORS
    // ----------------------------------------------------------
    public String buildSoapMessageFromOutput(final Binding binding, final BindingOperation bindingOperation, final SoapContext context)
            throws Exception {
        final boolean inputSoapEncoded = WsdlUtils.isInputSoapEncoded(bindingOperation);
        final SampleXmlUtil xmlGenerator = new SampleXmlUtil(inputSoapEncoded, context);
        final SoapVersion soapVersion = getSoapVersion(binding);

        final XmlObject object = XmlObject.Factory.newInstance();
        final XmlCursor cursor = object.newCursor();
        cursor.toNextToken();
        if (context.isAlwaysBuildEnvelope()) {
            cursor.beginElement(soapVersion.getEnvelopeQName());
        }

        if (inputSoapEncoded) {
            cursor.insertNamespace("xsi", Constants.XSI_NS);
            cursor.insertNamespace("xsd", Constants.XSD_NS);
        }

        cursor.toFirstChild();
        if (context.isAlwaysBuildBody()) {
            cursor.beginElement(soapVersion.getBodyQName());
        }
        cursor.toFirstChild();

        if (WsdlUtils.isRpc(this.definition, bindingOperation)) {
            buildRpcResponse(bindingOperation, soapVersion, cursor, xmlGenerator, context);
        } else {
            buildDocumentResponse(bindingOperation, cursor, xmlGenerator);
        }

        if (context.isAlwaysBuildHeaders()) {
            // bindingOutput will be null for one way operations,
            // but then we shouldn't be here in the first place???
            final BindingOutput bindingOutput = bindingOperation.getBindingOutput();
            if (bindingOutput != null) {
                final List<?> extensibilityElements = bindingOutput.getExtensibilityElements();
                final List<WsdlUtils.SoapHeader> soapHeaders = WsdlUtils.getSoapHeaders(extensibilityElements);
                addHeaders(soapHeaders, soapVersion, cursor, xmlGenerator);
            }
        }
        cursor.dispose();

        try {
            final StringWriter writer = new StringWriter();
            XmlUtils.serializePretty(object, writer);
            return writer.toString();
        } catch (final Exception e) {
            log.warn("Exception during message generation", e);
            return object.xmlText();
        }
    }

    // ----------------------------------------------------------
    // UTILS
    // ----------------------------------------------------------
    public Definition getDefinition() {
        return this.definition;
    }

    public SchemaDefinitionWrapper getSchemaDefinitionWrapper() {
        return this.definitionWrapper;
    }

    public BindingOperation getOperationByName(
            final QName bindingName, final String operationName, final String operationInputName, final String operationOutputName) {
        final Binding binding = getBindingByName(bindingName);
        if (binding == null) {
            return null;
        }
        final BindingOperation operation = binding.getBindingOperation(operationName, operationInputName, operationOutputName);
        if (operation == null) {
            throw new SoapBuilderException("Operation not found");
        }
        return operation;
    }

    public Binding getBindingByName(final QName bindingName) {
        final Binding binding = this.definition.getBinding(bindingName);
        if (binding == null) {
            throw new SoapBuilderException("Binding not found");
        }
        return binding;
    }

    public List<QName> getBindingNames() {
        return new ArrayList<QName>(this.definition.getAllBindings().keySet());
    }

    public static SoapVersion getSoapVersion(final Binding binding) {
        final List<?> list = binding.getExtensibilityElements();

        final SOAPBinding soapBinding = WsdlUtils.getExtensiblityElement(list, SOAPBinding.class);
        if (soapBinding != null) {
            if ((soapBinding.getTransportURI().startsWith(Constants.SOAP_HTTP_TRANSPORT) || soapBinding
                    .getTransportURI().startsWith(Constants.SOAP_MICROSOFT_TCP))) {
                return SoapVersion.Soap11;
            }
        }

        final SOAP12Binding soap12Binding = WsdlUtils.getExtensiblityElement(list, SOAP12Binding.class);
        if (soap12Binding != null) {
            if (soap12Binding.getTransportURI().startsWith(Constants.SOAP_HTTP_TRANSPORT)
                    || soap12Binding.getTransportURI().startsWith(Constants.SOAP12_HTTP_BINDING_NS)
                    || soap12Binding.getTransportURI().startsWith(Constants.SOAP_MICROSOFT_TCP)) {
                return SoapVersion.Soap12;
            }
        }
        throw new SoapBuilderException("SOAP binding not recognized");
    }

    // --------------------------------------------------------------------------
    // Internal methods - END OF PUBLIC API
    // --------------------------------------------------------------------------
    private void addHeaders(
            final List<WsdlUtils.SoapHeader> headers, final SoapVersion soapVersion, final XmlCursor cursor,
            final SampleXmlUtil xmlGenerator) throws Exception {
        // reposition
        cursor.toStartDoc();
        cursor.toChild(soapVersion.getEnvelopeQName());
        cursor.toFirstChild();

        cursor.beginElement(soapVersion.getHeaderQName());
        cursor.toFirstChild();

        for (int i = 0; i < headers.size(); i++) {
            final WsdlUtils.SoapHeader header = headers.get(i);

            final Message message = this.definition.getMessage(header.getMessage());
            if (message == null) {
                log.error("Missing message for header: " + header.getMessage());
                continue;
            }

            final Part part = message.getPart(header.getPart());

            if (part != null)
                createElementForPart(part, cursor, xmlGenerator);
            else
                log.error("Missing part for header; " + header.getPart());
        }
    }

    private void buildDocumentResponse(final BindingOperation bindingOperation, final XmlCursor cursor, final SampleXmlUtil xmlGenerator)
            throws Exception {
        final Part[] parts = WsdlUtils.getOutputParts(bindingOperation);

        for (int i = 0; i < parts.length; i++) {
            final Part part = parts[i];

            if (!WsdlUtils.isAttachmentOutputPart(part, bindingOperation)
                    && (part.getElementName() != null || part.getTypeName() != null)) {
                final XmlCursor c = cursor.newCursor();
                c.toLastChild();
                createElementForPart(part, c, xmlGenerator);
                c.dispose();
            }
        }
    }

    private void buildDocumentRequest(final BindingOperation bindingOperation, final XmlCursor cursor, final SampleXmlUtil xmlGenerator)
            throws Exception {
        final Part[] parts = WsdlUtils.getInputParts(bindingOperation);

        for (int i = 0; i < parts.length; i++) {
            final Part part = parts[i];
            if (!WsdlUtils.isAttachmentInputPart(part, bindingOperation)
                    && (part.getElementName() != null || part.getTypeName() != null)) {
                final XmlCursor c = cursor.newCursor();
                c.toLastChild();
                createElementForPart(part, c, xmlGenerator);
                c.dispose();
            }
        }
    }

    private void createElementForPart(final Part part, final XmlCursor cursor, final SampleXmlUtil xmlGenerator) throws Exception {
        final QName elementName = part.getElementName();
        final QName typeName = part.getTypeName();

        if (elementName != null) {
            cursor.beginElement(elementName);

            if (this.definitionWrapper.hasSchemaTypes()) {
                final SchemaGlobalElement elm = this.definitionWrapper.getSchemaTypeLoader().findElement(elementName);
                if (elm != null) {
                    cursor.toFirstChild();
                    xmlGenerator.createSampleForType(elm.getType(), cursor);
                } else
                    log.error("Could not find element [" + elementName + "] specified in part [" + part.getName() + "]");
            }

            cursor.toParent();
        } else {
            // cursor.beginElement( new QName(
            // wsdlContext.getWsdlDefinition().getTargetNamespace(), part.getName()
            // ));
            cursor.beginElement(part.getName());
            if (typeName != null && this.definitionWrapper.hasSchemaTypes()) {
                final SchemaType type = this.definitionWrapper.getSchemaTypeLoader().findType(typeName);

                if (type != null) {
                    cursor.toFirstChild();
                    xmlGenerator.createSampleForType(type, cursor);
                } else
                    log.error("Could not find type [" + typeName + "] specified in part [" + part.getName() + "]");
            }

            cursor.toParent();
        }
    }

    private void buildRpcRequest(
            final BindingOperation bindingOperation, final SoapVersion soapVersion, final XmlCursor cursor,
            final SampleXmlUtil xmlGenerator, final SoapContext context)
            throws Exception {
        // rpc requests use the operation name as root element
        String ns = WsdlUtils.getSoapBodyNamespace(bindingOperation.getBindingInput().getExtensibilityElements());
        if (ns == null) {
            ns = WsdlUtils.getTargetNamespace(this.definition);
            log.warn("missing namespace on soapbind:body for RPC request, using targetNamespace instead (BP violation)");
        }
        
        if (context.isBindingOperation()) {
            cursor.beginElement(new QName(ns, bindingOperation.getName()));
        }
        // TODO
        if (xmlGenerator.isSoapEnc())
            cursor.insertAttributeWithValue(new QName(soapVersion.getEnvelopeNamespace(),
                    "encodingStyle"), soapVersion.getEncodingNamespace());

        final Part[] inputParts = WsdlUtils.getInputParts(bindingOperation);
        for (int i = 0; i < inputParts.length; i++) {
            final Part part = inputParts[i];

            if (WsdlUtils.isAttachmentInputPart(part, bindingOperation)) {
                // TODO - generation of attachment flag could be externalized
                // if (iface.getSettings().getBoolean(WsdlSettings.ATTACHMENT_PARTS)) {
                final XmlCursor c = cursor.newCursor();
                c.toLastChild();
                c.beginElement(part.getName());
                c.insertAttributeWithValue("href", part.getName() + "Attachment");
                c.dispose();
                // }
            } else {
                if (this.definitionWrapper.hasSchemaTypes()) {
                    final QName typeName = part.getTypeName();
                    if (typeName != null) {
                        // TODO - Don't know whether will work
                        // SchemaType type = wsdlContext.getInterfaceDefinition().findType(typeName);
                        final SchemaType type = this.definitionWrapper.findType(typeName);

                        if (type != null) {
                            final XmlCursor c = cursor.newCursor();
                            c.toLastChild();
                            c.insertElement(part.getName());
                            c.toPrevToken();

                            xmlGenerator.createSampleForType(type, c);
                            c.dispose();
                        } else
                            log.warn("Failed to find type [" + typeName + "]");
                    } else {
                        final SchemaGlobalElement element = this.definitionWrapper.getSchemaTypeLoader().findElement(part.getElementName());
                        if (element != null) {
                            final XmlCursor c = cursor.newCursor();
                            c.toLastChild();
                            c.insertElement(element.getName());
                            c.toPrevToken();

                            xmlGenerator.createSampleForType(element.getType(), c);
                            c.dispose();
                        } else
                            log.warn("Failed to find element [" + part.getElementName() + "]");
                    }
                }
            }
        }
    }

    private void buildRpcResponse(
            final BindingOperation bindingOperation, final SoapVersion soapVersion, final XmlCursor cursor,
            final SampleXmlUtil xmlGenerator, final SoapContext context)
            throws Exception {
        // rpc requests use the operation name as root element
        final BindingOutput bindingOutput = bindingOperation.getBindingOutput();
        String ns = bindingOutput == null ? null : WsdlUtils.getSoapBodyNamespace(bindingOutput
                .getExtensibilityElements());

        if (ns == null) {
            ns = WsdlUtils.getTargetNamespace(this.definition);
            log.warn("missing namespace on soapbind:body for RPC response, using targetNamespace instead (BP violation)");
        }

        if (context.isBindingOperation()) {
            cursor.beginElement(new QName(ns, bindingOperation.getName() + "Response"));
        }
        if (xmlGenerator.isSoapEnc())
            cursor.insertAttributeWithValue(new QName(soapVersion.getEnvelopeNamespace(),
                    "encodingStyle"), soapVersion.getEncodingNamespace());

        final Part[] inputParts = WsdlUtils.getOutputParts(bindingOperation);
        for (int i = 0; i < inputParts.length; i++) {
            final Part part = inputParts[i];
            if (WsdlUtils.isAttachmentOutputPart(part, bindingOperation)) {
                // if( iface.getSettings().getBoolean( WsdlSettings.ATTACHMENT_PARTS ) )
                {
                    final XmlCursor c = cursor.newCursor();
                    c.toLastChild();
                    c.beginElement(part.getName());
                    c.insertAttributeWithValue("href", part.getName() + "Attachment");
                    c.dispose();
                }
            } else {
                if (this.definitionWrapper.hasSchemaTypes()) {
                    final QName typeName = part.getTypeName();
                    if (typeName != null) {
                        final SchemaType type = this.definitionWrapper.findType(typeName);

                        if (type != null) {
                            final XmlCursor c = cursor.newCursor();
                            c.toLastChild();
                            c.insertElement(part.getName());
                            c.toPrevToken();

                            xmlGenerator.createSampleForType(type, c);
                            c.dispose();
                        } else
                            log.warn("Failed to find type [" + typeName + "]");
                    } else {
                        final SchemaGlobalElement element = this.definitionWrapper.getSchemaTypeLoader().findElement(part.getElementName());
                        if (element != null) {
                            final XmlCursor c = cursor.newCursor();
                            c.toLastChild();
                            c.insertElement(element.getName());
                            c.toPrevToken();

                            xmlGenerator.createSampleForType(element.getType(), c);
                            c.dispose();
                        } else
                            log.warn("Failed to find element [" + part.getElementName() + "]");
                    }
                }
            }
        }
    }

    private static String buildEmptyFault(final SampleXmlUtil generator, final SoapVersion soapVersion, final SoapContext context) {
        String emptyResponse = buildEmptyMessage(soapVersion, context);
        try {
            final XmlObject xmlObject = XmlUtils.createXmlObject(emptyResponse);
            final XmlCursor cursor = xmlObject.newCursor();

            if (cursor.toChild(soapVersion.getEnvelopeQName()) && cursor.toChild(soapVersion.getBodyQName())) {
                final SchemaType faultType = soapVersion.getFaultType();
                final Node bodyNode = cursor.getDomNode();
                final Document dom = XmlUtils.parseXml(generator.createSample(faultType));
                bodyNode.appendChild(bodyNode.getOwnerDocument().importNode(dom.getDocumentElement(), true));
            }

            cursor.dispose();
            emptyResponse = xmlObject.toString();
        } catch (final Exception e) {
            throw new SoapBuilderException(e);
        }
        return emptyResponse;
    }
}
