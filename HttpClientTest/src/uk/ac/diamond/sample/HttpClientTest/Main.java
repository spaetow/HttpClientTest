package uk.ac.diamond.sample.HttpClientTest;

import java.util.List;

import javax.security.sasl.AuthenticationException;
import javax.validation.constraints.NotNull;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.log4j.Logger;
import org.apache.http.util.EntityUtils;

import org.opensaml.DefaultBootstrap;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.ws.soap.client.SOAPClientException;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.parse.BasicParserPool;
import org.opensaml.xml.schema.XSAny;
import org.opensaml.xml.schema.XSString;

import de.tudarmstadt.ukp.shibhttpclient.ShibHttpClient;
import uk.ac.diamond.shibbolethecpauthclient.ShibbolethECPAuthClient;

public class Main {

    /** Logger instance. */
    private static final Logger logger = Logger.getLogger(Main.class);

    /** List of IdPs to try and authenticate against. */
    @NotNull
//    private final static String IdP = "https://umbrella.psi.ch/idp/profile/SAML2/SOAP/ECP";
    private final static String IdP = "https://umbrellaid.org/idp/profile/SAML2/SOAP/ECP";

    /**
     * Identifies the Shibboleth SP we want to run ECP against
     */
    @NotNull
//    private final static String SP = "https://umbrella.psi.ch/secure";
//    private final static String SP = "https://umbrellaid.org/secure";
//    private final static String SP = "https://cas-ubuntu.diamond.ac.uk/securedumbrella";
    private final static String SP = "https://umbrellaid.org/euu/login";
    
    
    /**
     * Authenticates the given credentials against the list of Shibboleth IdPs,
     * @param credentials the username and password to authenticate
     * @return a Principal object representing the user 
     * @throws AuthenticationException
     */

    public static void main(String[] args) {

        final String username = "StefanPaetowDiamond";
        final String password = "xxx";	// test
//        final String password = "xxx";	// live

        final String attributeId = "urn:oid:1.3.6.1.4.1.9999.1.1.1"; //EAAHash
//        final String attributeId = "EAAHash"; //EAAHash
        
        logger.debug("Attempting to authenticate " + username + " at " + IdP);
        try {

            // Initialise the library
            DefaultBootstrap.bootstrap();
            final BasicParserPool parserPool = new BasicParserPool();
            parserPool.setNamespaceAware(true);
        /*    
            logger.info("---------------------------------------------------------------------------------------------------");
            logger.info("---- Begin of ShibHttpClient Test -----------------------------------------------------------------");
            logger.info("---------------------------------------------------------------------------------------------------");

            // no proxy, no self-cert (default)
//            final ShibHttpClient shibClient = new ShibHttpClient(IdP, username, password);
            // no proxy, with self-cert
            final ShibHttpClient shibClient = new ShibHttpClient(IdP, username, password, true);
            // no proxy, no self-cert (explicit)
//            final ShibHttpClient shibClient = new ShibHttpClient(IdP, username, password, new HttpHost("wwwcache.rl.ac.uk", 8080), false, false);
            // proxy, self-cert (explicit)
//            final ShibHttpClient shibClient = new ShibHttpClient(IdP, username, password, new HttpHost("wwwcache.rl.ac.uk", 8080), true, true);

            try {
            	HttpResponse response = shibClient.execute(new HttpGet(SP));
    			logger.info("HttpResponse::Status: " + response.getStatusLine());
            	logger.info("HttpResponse::Entity: " + EntityUtils.toString(response.getEntity()));
            }
            catch (final Exception e) {
            	logger.error("ShibHttpClient::execute failed with other exception: " + e.toString());
            } // try authenticate
*/
            
            logger.info("---------------------------------------------------------------------------------------------------");
            logger.info("---- Begin of ShibbolethECPAuthClient Test --------------------------------------------------------");
            logger.info("---------------------------------------------------------------------------------------------------");
            
            // Instantiate a copy of the client, catch any errors that occur
//            final ShibbolethECPAuthClient seac = new ShibbolethECPAuthClient(IdP, SP, true); // no explicit proxy
            final ShibbolethECPAuthClient seac = new ShibbolethECPAuthClient(null, IdP, SP, true); // explicit null proxy
//            final ShibbolethECPAuthClient seac = new ShibbolethECPAuthClient(new HttpHost("wwwcache.rl.ac.uk", 8080), IdP, SP, false); // explicit non-null proxy

            // if we get an exception here with our 'chained' get(...) calls, we have a problem anyway!
            boolean idFound = false;
            String requiredAttributeValue = null;
            List<Attribute> attributes = seac.authenticate(username, password)
            								.getAssertions().get(0)
            								.getAttributeStatements().get(0)
            								.getAttributes();

            if (!attributes.isEmpty()) {
                for (Attribute attribute : attributes) {
                    if ((attribute.getName().indexOf(attributeId) == 0) ||
                        (attribute.getFriendlyName().indexOf(attributeId) == 0)) {
                        idFound = true;
                        XMLObject attributeValue = attribute.getAttributeValues().get(0);
                        if (attributeValue instanceof XSString) {
                        	requiredAttributeValue = ((XSString) attributeValue).getValue();
                        } else if (attributeValue instanceof XSAny) {
                        	requiredAttributeValue = ((XSAny) attributeValue).getTextContent();
                        }
                        logger.info("Attribute: " + attributeId + ", value: " + requiredAttributeValue);
                    } // if getName()...
                } // for attribute...
            } // if not empty
            
            if (!idFound) {
                logger.info("The Shibboleth attribute " + attributeId + " was not returned by the Shibboleth server");
            }

        }
        catch (final AuthenticationException e) {
        	logger.error("Failed to authenticate " + username + " at " + IdP + ". Error: " + e.toString());
        }
        catch (final SOAPClientException e) {
        	logger.error("Shibboleth ECP client raised SOAP error: " + e.toString());
        }
        catch (final Exception e) {
        	logger.error("ShibbolethECPAuthClient::authenticate failed with other exception: " + e.toString());
        } // try authenticate
    }
}
