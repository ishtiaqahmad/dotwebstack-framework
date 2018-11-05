package org.dotwebstack.framework.frontend.soap.handlers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.ws.rs.container.ContainerRequestContext;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.Port;

import lombok.NonNull;
import org.dotwebstack.framework.frontend.soap.action.SoapAction;
import org.dotwebstack.framework.frontend.soap.wsdlreader.SoapUtils;
import org.glassfish.jersey.message.internal.DataSourceProvider;
import org.glassfish.jersey.process.Inflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;


public class SoapRequestHandlerXop extends SoapRequestHandler
    implements Inflector<ContainerRequestContext, String> {

  private static final Logger LOG = LoggerFactory.getLogger(SoapRequestHandlerXop.class);

  public SoapRequestHandlerXop(@NonNull Definition wsdlDefinition, @NonNull Port wsdlPort,
      @NonNull Map<String, SoapAction> soapActions) {
    super(wsdlDefinition, wsdlPort, soapActions);
  }

  @Override
  public String apply(ContainerRequestContext data) {
    final String soapActionName = data.getHeaderString("SOAPAction");
    String msg = ERROR_RESPONSE;
    LOG.debug("Handling SOAP Multipart XOP request, SOAPAction: {}", soapActionName);

    BindingOperation wsdlBindingOperation =
        SoapUtils.findWsdlBindingOperation(wsdlPort, soapActionName);
    if (wsdlBindingOperation == null) {
      // No operation found. Return the error message.
      LOG.warn("Not found BindingOperation: {}", soapActionName);
    } else {
      // Retrieve the input message for parameters
      Document inputDocument = getInputDocument(data);
      msg = buildSoapResponse(msg, wsdlBindingOperation, inputDocument);
    }

    return createResponse(msg);
  }

  private String createResponse(String msg) {
    MimeMultipart mimeMultipart = new MimeMultipart();
    MimeBodyPart textPart = new MimeBodyPart();
    try {
      textPart.setText(msg, "utf-8");
      textPart.addHeader("Content-Type", "application/xop+xml;charset=utf-8;type=\"text/xml\"");
      textPart.addHeader("Content-Transfer-Encoding", "8bit");
      mimeMultipart.addBodyPart(textPart);
    } catch (MessagingException e) {
      LOG.error("Error creating Mime Response: {}", e.getMessage());
    }
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      mimeMultipart.writeTo(baos);
      return getStringFromInputStream(new ByteArrayInputStream(baos.toByteArray()));
    } catch (Exception ex) {
      LOG.warn("Error convertin MimeMultipart to String: {}", ex.getMessage());
    }
    return ERROR_RESPONSE;

  }

  private ByteArrayInputStream mimeParser(InputStream isMtm) {

    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      MimeMultipart mp =
          new MimeMultipart(new DataSourceProvider.ByteArrayDataSource(isMtm, "multipart/related"));
      BodyPart bodyPart = mp.getBodyPart(0);

      bodyPart.writeTo(baos);

      return new ByteArrayInputStream(baos.toByteArray());
    } catch (Exception ex) {
      LOG.warn("Error extracting soapbody: {}", ex.getMessage());
    }
    return null;
  }

  private Document getInputDocument(final ContainerRequestContext data) {
    if (data.hasEntity()) {
      return retrieveInputMessage(mimeParser(data.getEntityStream()));
    }
    return null;
  }
}