create table T_REQUEST_LOG (
    REQUEST_ID              VARCHAR2(50),
    REQUEST_START           TIMESTAMP NOT NULL,
    REQUEST_END             TIMESTAMP,
    APPLICATION_NODE        VARCHAR2(50),
    REQUESTOR_IP            VARCHAR2(50),
    APPLICATION_NAME        VARCHAR2(50),
    METHOD_NAME             VARCHAR2(80),
    THREAD_NAME             VARCHAR2(50),
    REQUEST_HEADERS         VARCHAR2(2000),
    SOAP_HEADERS            VARCHAR2(500),
    ENTRY_TYPE              VARCHAR2(3),
    ENTRY_BODY              CLOB,
    REQUEST_STATUS          INTEGER,
    SSO_ID                  VARCHAR2(10),
    INTERACTION_ID          VARCHAR2(100)
);