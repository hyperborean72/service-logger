package com.taskdata.audit;

import java.io.StringWriter;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPPart;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.handler.MessageContext;
import javax.xml.ws.handler.soap.SOAPHandler;
import javax.xml.ws.handler.soap.SOAPMessageContext;
import javax.xml.ws.soap.SOAPFaultException;
import java.net.InetAddress;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.taskdata.audit.domain.AuditRow;

@Service("logHandler")
public class LoggingHandler implements SOAPHandler<SOAPMessageContext> {

    static final Logger logger = Logger.getLogger(LoggingHandler.class);

    @Autowired
    LoggerThread loggerThread;

    private int responseBodySize = 9990;
    private int requestBodySize = 9990;


    public static String getClientIpAddr(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    public boolean handleMessage(SOAPMessageContext context) {

        AuditRow auditRow = new AuditRow();

        auditRow.setSsoId("");
        auditRow.setInteractionId("");

        SOAPMessage soapMsg = context.getMessage();
        SOAPPart soapPart = soapMsg.getSOAPPart();

        QName serviceName = (QName) context.get(SOAPMessageContext.WSDL_SERVICE);

        HttpServletRequest request = (HttpServletRequest)context.get(SOAPMessageContext.SERVLET_REQUEST);

        String methodName = new String();

        StringWriter sw = new StringWriter();

        InetAddress inetAddress;

        boolean isResponse = (Boolean) context.get(SOAPMessageContext.MESSAGE_OUTBOUND_PROPERTY);

        auditRow.setThreadName(Thread.currentThread().getName());

        /* value is @WebService(serviceName) */
        auditRow.setApplicationName(serviceName.getLocalPart());


        try {
            inetAddress = InetAddress.getLocalHost();
            auditRow.setApplicationNode(inetAddress.toString());
        } catch (UnknownHostException e) {
            logger.error("Server IP extraction error: " + e.getMessage());
        }

        auditRow.setRequestorIp(getClientIpAddr(request));


        String requestHeaders = processHeaders(context, SOAPMessageContext.HTTP_REQUEST_HEADERS);

        int posPass = requestHeaders.indexOf("Password");

        if (posPass != -1) {
            int posLeft = requestHeaders.indexOf(" ", posPass);
            int posRight = requestHeaders.indexOf("\n", posPass);

            String maskedPasswordHeaders = requestHeaders.substring(0, posLeft+1) +
                                           requestHeaders.substring(posLeft+1, posRight).replaceAll("[^&]*", "***") +
                                           requestHeaders.substring(posRight);

            auditRow.setRequestHeaders(maskedPasswordHeaders);

        } else {
            auditRow.setRequestHeaders(requestHeaders);
        }


        int posInteractionId = requestHeaders.indexOf("X-cc-interaction-id");
        if (posInteractionId != -1) {
            int posInteractionIdLeft = requestHeaders.indexOf(" ", posInteractionId);
            int posInteractionIdRight = requestHeaders.indexOf("\n", posInteractionId);

            String interactionId = requestHeaders.substring(posInteractionIdLeft+1, posInteractionIdRight);

            auditRow.setInteractionId(interactionId);
        }

        int posSsoId = requestHeaders.indexOf("X-cc-requester-sso-id");
        if (posSsoId != -1) {
            int posSsoLeft = requestHeaders.indexOf(" ", posSsoId);
            int posSsoRight = requestHeaders.indexOf("\n", posSsoId);

            String SsoId = requestHeaders.substring(posSsoLeft+1, posSsoRight);

            auditRow.setSsoId(SsoId);
        }


        try {
            methodName = soapPart.getEnvelope().getBody().getChildNodes().item(0).getLocalName();
        } catch (SOAPException e) {
            logger.error("Method name extraction error: "+ e.getMessage());
        }
        auditRow.setWebMethod(methodName);

        try {
            if (!isResponse) { // request message

                logger.debug("Request logging start");

                long startTime = System.currentTimeMillis();
                auditRow.setRequestStart(new Timestamp(startTime));

                /* did not work for handleFault (: */
                context.put("auditRow ", auditRow);

                String uniqueKey = UUID.randomUUID().toString();
                context.put("uniqueKey", uniqueKey);
                auditRow.setRequestId(uniqueKey);

                auditRow.setEntryType("IN");

                try {
                    TransformerFactory.newInstance().newTransformer().transform(
                            new DOMSource(soapPart),
                            new StreamResult(sw));
                } catch (TransformerException e) {
                    logger.error("TransformerException detected: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                auditRow.setEntryBody(sw.toString().length() > requestBodySize ? sw.toString().substring(0,requestBodySize-1) : sw.toString());


                try {
                    SOAPHeader header = soapPart.getEnvelope().getHeader();
                    auditRow.setSoapHeaders(header == null ? "" : header.toString());

                } catch (SOAPException e) {
                    logger.error("Soap header extraction error: "+ e.getMessage());
                }

                //Is Response_Code accessible in request?
                auditRow.setRequestStatus((Integer)context.get(SOAPMessageContext.HTTP_RESPONSE_CODE));

                long endTime = System.currentTimeMillis();
                auditRow.setRequestEnd(new Timestamp(endTime));

                loggerThread.log(auditRow);

                logger.debug("Request logging end");

            } else {

                logger.debug("Response logging start");

                long startTime = System.currentTimeMillis();
                auditRow.setRequestStart(new Timestamp(startTime));

                auditRow.setRequestId((String)context.get("uniqueKey"));

                auditRow.setEntryType("OUT");

                try {
                    TransformerFactory.newInstance().newTransformer().transform(
                            new DOMSource(soapMsg.getSOAPPart()),
                            new StreamResult(sw));
                } catch (TransformerException e) {
                    logger.error("TransformerException detected: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                auditRow.setEntryBody(sw.toString().length() > responseBodySize ? sw.toString().substring(0, responseBodySize) : sw.toString());

                /*Unfortunately Response isResponse not available when LoggingHandler is invoked therefore returns null
                String responseHeaders = processHeaders(context, SOAPMessageContext.HTTP_RESPONSE_HEADERS);*/

                auditRow.setRequestHeaders(new String());

                try {
                    SOAPHeader header = soapMsg.getSOAPPart().getEnvelope().getHeader();
                    auditRow.setSoapHeaders(header == null ? "" : stripBrackets(header.toString()));

                } catch (SOAPException e) {
                    logger.error("Soap header extraction error: "+ e.getMessage());
                }

                auditRow.setRequestStatus((Integer)context.get(MessageContext.HTTP_RESPONSE_CODE));

                /* 0 always as well
                auditRow.setRequestStatus((Integer)context.get(MessageContext.SERVLET_RESPONSE));
                */

                long endTime = System.currentTimeMillis();
                auditRow.setRequestEnd(new Timestamp(endTime));

                loggerThread.log(auditRow);

                logger.debug("Response logging end");
            }

        } catch (Throwable e) {

            logger.error("Unexpected error while parsing request or response " + e.getMessage());
        }
        return true;
    }

    public boolean handleFault(SOAPMessageContext context) {

        // WTF: context.get("auditRow") returns null BUT filled with data if inspected in VARIABLES
        // AuditRow auditRow = (AuditRow)context.get("auditRow");

        AuditRow auditRow = new AuditRow();

        long startTime = System.currentTimeMillis();
        auditRow.setRequestStart(new Timestamp(startTime));

        auditRow.setInteractionId("");
        auditRow.setSsoId("");

        String requestHeaders = processHeaders(context, SOAPMessageContext.HTTP_REQUEST_HEADERS);

        int posPass = requestHeaders.indexOf("Password");

        if (posPass != -1) {
            int posLeft = requestHeaders.indexOf(" ", posPass);
            int posRight = requestHeaders.indexOf("\n", posPass);

            String maskedPasswordHeaders = requestHeaders.substring(0, posLeft+1) +
                                           requestHeaders.substring(posLeft+1, posRight).replaceAll("[^&]*", "***") +
                                           requestHeaders.substring(posRight);

            auditRow.setRequestHeaders(maskedPasswordHeaders);

        } else {
            auditRow.setRequestHeaders(requestHeaders);
        }

        int posInteractionId = requestHeaders.indexOf("X-cc-interaction-id");
        if (posInteractionId != -1) {
            int posInteractionIdLeft = requestHeaders.indexOf(" ", posInteractionId);
            int posInteractionIdRight = requestHeaders.indexOf("\n", posInteractionId);

            String interactionId = requestHeaders.substring(posInteractionIdLeft+1, posInteractionIdRight);

            auditRow.setInteractionId(interactionId);
        }

        int posSsoId = requestHeaders.indexOf("X-cc-requester-sso-id");
        if (posSsoId != -1) {
            int posSsoLeft = requestHeaders.indexOf(" ", posSsoId);
            int posSsoRight = requestHeaders.indexOf("\n", posSsoId);

            String SsoId = requestHeaders.substring(posSsoLeft+1, posSsoRight);

            auditRow.setSsoId(SsoId);
        }


        SOAPMessage soapMsg = context.getMessage();
        SOAPPart soapPart = soapMsg.getSOAPPart();

        QName serviceName = (QName) context.get(SOAPMessageContext.WSDL_SERVICE);

        HttpServletRequest request = (HttpServletRequest)context.get(SOAPMessageContext.SERVLET_REQUEST);

        String methodName = new String();

        InetAddress inetAddress;

        auditRow.setRequestId((String)context.get("uniqueKey"));
        auditRow.setEntryType("OUT");

        auditRow.setThreadName(Thread.currentThread().getName());
        auditRow.setApplicationName(serviceName.getLocalPart());
        try {
            inetAddress = InetAddress.getLocalHost();
            auditRow.setApplicationNode(inetAddress.toString());
        } catch (UnknownHostException e) {
            logger.error("Server IP extraction error: " + e.getMessage());
        }


        auditRow.setRequestorIp(getClientIpAddr(request));

        try {
            methodName = soapPart.getEnvelope().getBody().getChildNodes().item(0).getLocalName();
        } catch (SOAPException e) {
            logger.error("Method name extraction error: "+ e.getMessage());
        }
        auditRow.setWebMethod(methodName);


        try {
            SOAPBody soapBody = context.getMessage().getSOAPPart().getEnvelope().getBody();
            SOAPFaultException faultException = new SOAPFaultException(soapBody.getFault());

            auditRow.setEntryBody(faultException.getLocalizedMessage());

        } catch (SOAPException e) {
            logger.error("Soap envelope extraction error: "+ e.getMessage());
        }

        try {
            SOAPHeader header = soapPart.getEnvelope().getHeader();
            auditRow.setSoapHeaders(header == null ? "" : header.toString());

        } catch (SOAPException e) {
            logger.error("Soap header extraction error: "+ e.getMessage());
        }

        //Is Response_Code accessible in request?
        auditRow.setRequestStatus((Integer)context.get(SOAPMessageContext.HTTP_RESPONSE_CODE));

        long endTime = System.currentTimeMillis();
        auditRow.setRequestEnd(new Timestamp(endTime));

        loggerThread.log(auditRow);

        return true;
    }


    public Set<QName> getHeaders() {
        //Not required for logging
        return null;
    }


    public void close(MessageContext arg0) {
        //Not required for logging
    }

    private String stripBrackets(String in){
        return in.substring(in.indexOf("[")+1,in.lastIndexOf("]"));
    }

    public int getResponseBodySize() {
        return responseBodySize;
    }

    public void setResponseBodySize(int responseBodySize) {
        this.responseBodySize = responseBodySize;
    }

    private String processHeaders (SOAPMessageContext context, String requestResponse){

        Map httpHeaders = (Map)context.get(requestResponse);
        String value;

        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, List<String>> entry : (Set<Map.Entry<String, List<String>>>)httpHeaders.entrySet()){
            value = entry.getValue().toString();
            stringBuilder.append(entry.getKey()).append(": ").append(value.substring(1,value.length()-1)).append("\n");
        }
        return stringBuilder.toString();
    }
}