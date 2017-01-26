package com.taskdata.audit;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import javax.annotation.PreDestroy;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.taskdata.audit.dao.impl.RequestLogDAOImpl;
import com.taskdata.audit.domain.AuditRow;

@Component("loggerThread")
public class LoggerThread extends Thread {

    @Autowired
    RequestLogDAOImpl requestLogDAO;

    @Autowired
    DataSource dataSource;

    static final Logger logger = Logger.getLogger(LoggerThread.class);

    private volatile boolean suspendFlag = false;

    private int partitionSize = 20;

    static final int queueSize = 10000;

    static final long sleepTime = 60000L;

    public LoggerThread() {
        start();
    }

    private BlockingQueue<AuditRow> itemsToLog = new LinkedBlockingQueue<AuditRow>(queueSize);

    public void run() {
        AuditRow item;

        try {
            while(true){
                synchronized (this) {
                    if (suspendFlag) {
                        wait();
                    }
                }
                try {
                    if (!suspendFlag) {
                        List<AuditRow> batchList = new ArrayList<AuditRow>();

                        item = itemsToLog.take();
                        batchList.add(item);

                        itemsToLog.drainTo(batchList);
                        splitInsert(batchList);
                    }
                } catch (Throwable e) {
                    logger.error("Unexpected error while running logger service " + e.getMessage());
                    sleep(sleepTime);
                }
            }
        } catch (InterruptedException e) {
            logger.error("InterruptedException while running the logger: " + e);
        }
    }

    /* outer thread adds element to Queue */

    public void log(AuditRow row) {
        if (suspendFlag) return;
        boolean isSuccess = itemsToLog.offer(row);

        if (!isSuccess){
            logger.debug("Logger thread is overfull");
        }
    }


    public synchronized void restartLogging(){
        suspendFlag = false;
        notify();
    }


    public synchronized void stopLogging(){
        drainQueue();
        suspendFlag = true;
    }


    private void drainQueue(){
        List<AuditRow> shutdownList = new ArrayList<AuditRow>();
        itemsToLog.drainTo(shutdownList);
        splitInsert(shutdownList);
    }


    public static Map<String, Object> introspect(Object obj) throws Exception {

        Map<String, Object> result = new HashMap<String, Object>();
        BeanInfo info = Introspector.getBeanInfo(obj.getClass());
        for (PropertyDescriptor pd : info.getPropertyDescriptors()) {
            Method reader = pd.getReadMethod();
            if (reader != null)
                result.put(pd.getName(), reader.invoke(obj));
        }
        return result;
    }

    /* utility to split Queue into chunks and perform batch db insert on each chunk */
    public void splitInsert(List<AuditRow> auditRowList) {
      requestLogDAO.batchUpsert(auditRowList);
    }

    /*public void splitInsert(List<AuditRow> auditRowList) {

        List<AuditRow> auditRowSubList = new ArrayList<AuditRow>();

        int listSize = auditRowList.size();

        for (int i = 0; i < listSize; i += partitionSize) {
            auditRowSubList = auditRowList.subList(i, i + Math.min(partitionSize, listSize - i));
            requestLogDAO.batchUpsert(auditRowSubList);
            auditRowSubList.clear();
        }
    }*/

    /* used in case NamedParameterJdbcTemplate consumes MapSqlParameterSource[] */
    /* public void splitConvertInsert(List<AuditRow> auditRowList){

        List<Map<String, Object>> batchItemsAsMap = new ArrayList<Map<String, Object>>();
        List<AuditRow> auditRowSubList = new ArrayList<AuditRow>();

        int blSize = auditRowList.size();
        if (blSize > partitionSize){
            for (int i = 0; i < blSize; i += partitionSize) {
                auditRowSubList = auditRowList.subList(i,i + partitionSize);
                for (AuditRow auditRow : auditRowSubList){
                    try {
                        batchItemsAsMap.add(introspect(auditRow));
                    } catch (Exception e) {
                        logger.error("Introspection error: " + e);
                    }
                }
                requestLogDAO.batchUpsert(batchItemsAsMap);
                batchItemsAsMap.clear();
            }
        } else {
            for (AuditRow auditRow : auditRowList){
                try {
                    batchItemsAsMap.add(introspect(auditRow));
                } catch (Exception e) {
                    logger.error("Introspection error: " + e);
                }
            }
            requestLogDAO.batchUpsert(batchItemsAsMap);
            batchItemsAsMap.clear();
        }
    }   */


    public int getPartitionSize() {
        return partitionSize;
    }

    public void setPartitionSize(int partitionSize) {
        this.partitionSize = partitionSize;
    }


    public boolean isSuspendFlag() {
        return suspendFlag;
    }


    @PreDestroy
    private void drainOnDestroy(){
        drainQueue();
        Thread.currentThread().interrupt();
    }
}