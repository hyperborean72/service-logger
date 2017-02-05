The purpose of the ServiceLogger is to provide detailed information during SOAP service-based server intercourse
in environment where several SOAP based services run on server on several nodes and consumed from any IP on the web.
It is implemented as Spring component (of service type) that implements javax.xml.ws.handler.soap.SOAPHandler and extracts detailed information from SOAP context and request headers like IP address and SSO of the person that sent request, 
SOAP service and web method names, request body and status etc

Another Spring component which is in fact a separate thread is injected in request handler 
and serves to push the accumulated information into database

DAO layer is implemented as Spring component of repository type that interacts with database via JDBC.

ServiceLogger is distributed as jar library added to classpath of the projects to be logged
