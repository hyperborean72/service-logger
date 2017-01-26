package com.taskdata.audit.domain;

import java.util.Date;

public class AuditRow {

    String  requestId;
    Date    requestStart;
    Date    requestEnd;
    String  applicationNode;
    String  requestorIp;
    String  applicationName;
    String  webMethod;
    String  threadName;
    String  requestHeaders;
    String  soapHeaders;
    String  entryType;
    String  entryBody;
    Integer requestStatus;
    String  ssoId;
    String  interactionId;


    public AuditRow() {
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Date getRequestStart() {
        return requestStart;
    }

    public void setRequestStart(Date requestStart) {
        this.requestStart = requestStart;
    }

    public Date getRequestEnd() {
        return requestEnd;
    }

    public void setRequestEnd(Date requestEnd) {
        this.requestEnd = requestEnd;
    }

    public String getApplicationNode() {
        return applicationNode;
    }

    public void setApplicationNode(String requestorIp) {
        this.applicationNode = requestorIp;
    }

    public String getRequestorIp() {
        return requestorIp;
    }

    public void setRequestorIp(String requestorIp) {
        this.requestorIp = requestorIp;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getWebMethod() {
        return webMethod;
    }

    public void setWebMethod(String webMethod) {
        this.webMethod = webMethod;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public Integer getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(Integer requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
    }

    public String getEntryBody() {
        return entryBody;
    }

    public void setEntryBody(String entryBody) {
        this.entryBody = entryBody;
    }

    public String getSsoId() {
        return ssoId;
    }

    public void setSsoId(String ssoId) {
        this.ssoId = ssoId;
    }

    public String getSoapHeaders() {
        return soapHeaders;
    }

    public void setSoapHeaders(String soapHeader) {
        this.soapHeaders = soapHeader;
    }

    public String getInteractionId() {
        return interactionId;
    }

    public void setInteractionId(String transactionId) {
        this.interactionId = transactionId;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }
}
