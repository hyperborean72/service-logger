package com.taskdata.audit.dao;

import java.util.List;
import java.util.Map;

import com.taskdata.audit.domain.AuditRow;

public interface RequestLogDAO {

    public void upsert(Map<String, Object> parameters);

    public void batchUpsert(List<AuditRow> parametersList);
}
