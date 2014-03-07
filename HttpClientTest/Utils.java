/*******************************************************************************
 * Copyright 2013
 * Ubiquitous Knowledge Processing (UKP) Lab
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package ac.uk.diamond.sample.HttpClientTest;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.RequestWrapper;
import org.apache.http.protocol.HttpContext;
import org.opensaml.common.xml.SAMLConstants;
import org.opensaml.ws.message.decoder.MessageDecodingException;
import org.opensaml.xml.Configuration;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.util.XMLHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

@SuppressWarnings("deprecation")
final class Utils
{
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);
    
    private static final String AUTH_IN_PROGRESS = CloseableHttpClient.class.getName()
            + ".AUTH_IN_PROGRESS";
    
    private static final String MIME_TYPE_PAOS = "application/vnd.paos+xml";

//  private static final QName E_PAOS_REQUEST = new QName(SAMLConstants.PAOS_NS, "Request");
//
//  private static final QName A_RESPONSE_CONSUMER_URL = new QName("responseConsumerURL");

    private static final String HEADER_AUTHORIZATION = "Authorization";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";

    private static final String HEADER_ACCEPT = "Accept";

    private static final String HEADER_PAOS = "PAOS";

    private static final List<String> REDIRECTABLE = Arrays.asList("HEAD", "GET", "CONNECT");

//    private Utils()
//    {
//        // No instances
//    }

    /**
     * Create a connection manager that trusts any certificate.
     */
    static SSLSocketFactory getAnyCertManager()
    {
        try {
            SSLSocketFactory sf = new SSLSocketFactory(new TrustStrategy()
            {
                @Override
                public boolean isTrusted(X509Certificate[] aChain, String aAuthType)
                    throws CertificateException
                {
                    return true;
                }
            });
            return sf;
        }
        catch (GeneralSecurityException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper method that deserializes and unmarshalls the message from the given stream. This
     * method has been adapted from {@code org.opensaml.ws.message.decoder.BaseMessageDecoder}.
     * 
     * @param messageStream
     *            input stream containing the message
     * 
     * @return the inbound message
     * 
     * @throws MessageDecodingException
     *             thrown if there is a problem deserializing and unmarshalling the message
     */
    static XMLObject unmarshallMessage(ParserPool parserPool, String messageStream)
        throws ClientProtocolException
    {
        try {
            Document messageDoc = parserPool.parse(new StringReader(messageStream));
            Element messageElem = messageDoc.getDocumentElement();

            Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory().getUnmarshaller(
                    messageElem);
            if (unmarshaller == null) {
                throw new ClientProtocolException(
                        "Unable to unmarshall message, no unmarshaller registered for message element "
                                + XMLHelper.getNodeQName(messageElem));
            }

            XMLObject message = unmarshaller.unmarshall(messageElem);

            return message;
        }
        catch (XMLParserException e) {
            throw new ClientProtocolException(
                    "Encountered error parsing message into its DOM representation", e);
        }
        catch (UnmarshallingException e) {
            throw new ClientProtocolException(
                    "Encountered error unmarshalling message from its DOM representation", e);
        }
    }

    static String xmlToString(XMLObject aObject)
        throws IOException
    {
        Document doc;
        try {
            doc = Configuration.getMarshallerFactory().getMarshaller(aObject).marshall(aObject)
                    .getOwnerDocument();
        }
        catch (MarshallingException e) {
            throw new IOException(e);
        }

        try {
            Source source = new DOMSource(doc);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        }
        catch (TransformerException e) {
            throw new IOException(e);
        }
    }
    
    static String xmlToString(Element doc)
    {
        StringWriter sw = new StringWriter();
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        }
        catch (TransformerException e) {
            LOG.error("Unable to print message contents: ", e);
            return "<ERROR: " + e.getMessage()+ ">";
        }
    }
}
