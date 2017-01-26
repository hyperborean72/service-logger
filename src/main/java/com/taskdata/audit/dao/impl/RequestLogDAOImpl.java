package com.taskdata.audit.dao.impl;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Repository;

import com.taskdata.audit.dao.RequestLogDAO;
import com.taskdata.audit.domain.AuditRow;

@Repository
public class RequestLogDAOImpl implements RequestLogDAO {

    @Qualifier("dbDataSource")
    DataSource dataSource;

    public static final String INSERT_INTO_REQUEST_LOG = "INSERT INTO T_REQUEST_LOG " +
                                                         "(REQUEST_ID, REQUEST_START, REQUEST_END, APPLICATION_NODE, REQUESTOR_IP, APPLICATION_NAME, METHOD_NAME, THREAD_NAME, REQUEST_HEADERS, SOAP_HEADERS, ENTRY_TYPE, ENTRY_BODY, REQUEST_STATUS, SSO_ID, INTERACTION_ID)" +
                                                         " VALUES (:requestId, :requestStart, :requestEnd, :applicationNode, :requestorIp, :applicationName, :webMethod, :threadName,  :requestHeaders, :soapHeaders, :entryType, :entryBody, :requestStatus, :ssoId, :interactionId)";

    static final Logger logger = Logger.getLogger(RequestLogDAOImpl.class);

    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    

    @Override
    public void upsert(Map<String, Object> parameters){

        SqlParameterSource namedParameters = new MapSqlParameterSource(parameters);

        try {
            namedParameterJdbcTemplate.update(INSERT_INTO_REQUEST_LOG, namedParameters);
        } catch (DataAccessException e) {
            logger.error("DataAccessException detected " + e.getMessage());  //To change body of catch statement use File | Settings | File Templates.
        }
    }


    @Override
    public void batchUpsert(final List<AuditRow> parametersList){

        SqlParameterSource[] params = SqlParameterSourceUtils.createBatch(parametersList.toArray());
        namedParameterJdbcTemplate.batchUpdate(INSERT_INTO_REQUEST_LOG, params);
    }

    @Autowired
    // Set DataSource on initialization
    public void setDataSource(DataSource dataSource) {

        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }
}