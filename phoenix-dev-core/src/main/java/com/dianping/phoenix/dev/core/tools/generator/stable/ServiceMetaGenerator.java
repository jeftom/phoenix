/**
 * Project: phoenix-router
 * 
 * File Created at 2013-4-15
 * $Id$
 * 
 * Copyright 2010 dianping.com.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Dianping Company. ("Confidential Information").  You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with dianping.com.
 */
package com.dianping.phoenix.dev.core.tools.generator.stable;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import com.dianping.phoenix.dev.core.tools.generator.TemplateBasedFileGenerator;

/**
 * @author Leo Liang
 * 
 */
public class ServiceMetaGenerator extends TemplateBasedFileGenerator<ServiceMetaContext> {
    private static final String TEMPLATE = "service-meta.vm";

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.dianping.phoenix.misc.file.TemplateBasedFileGenerator#getTemplate()
     */
    @Override
    protected String getTemplate() {
        return TEMPLATE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.dianping.phoenix.misc.file.TemplateBasedFileGenerator#getArgs(java
     * .lang.Object)
     */
    @Override
    protected Object getArgs(ServiceMetaContext context) throws Exception {
        Map<String, Integer> servicePortMapping = new LinkedHashMap<String, Integer>();

        Class.forName(context.getDriverClass());

        Connection conn = null;
        Statement stmt = null;
        try {
            if (StringUtils.isNotBlank(context.getUser())) {
                conn = DriverManager.getConnection(context.getUrl(), context.getUser(), context.getPwd());
            } else {
                conn = DriverManager.getConnection(context.getUrl());
            }
            stmt = conn.createStatement();
            ResultSet rs = stmt
                    .executeQuery("SELECT s.serviceName AS serviceName, h.port1 AS port FROM jrobin_host h, service s WHERE s.projectId = h.projectId AND h.port1 IS NOT NULL ORDER BY PORT ASC;");
            while (rs.next()) {
                String serviceName = rs.getString("serviceName");
                int port = rs.getInt("port");
                if (StringUtils.isNotBlank(serviceName)) {
                    servicePortMapping.put(serviceName, port);
                }
            }
        } finally {
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (Exception e) {

                }
            }
            if (conn != null) {
                try {
                    conn.close();
                } catch (Exception e) {

                }
            }
        }

        return servicePortMapping;

    }

    public static void main(String[] args) throws Exception {
        ServiceMetaGenerator serviceMetaGenerator = new ServiceMetaGenerator();
        if (args == null || args.length == 0) {
            System.out
                    .println("Usage com.dianping.phoenix.dev.core.tools.generator.stable.ServiceMetaGenerator destfile [jdbcUrl] [dbUsername] [dbPassword]");
            System.exit(1);
        }

        String destFile = args[0];
        String jdbcUrl = (args.length < 2 || StringUtils.isBlank(args[1])) ? "jdbc:mysql://192.168.7.105:3306/hawk"
                : args[1];
        String dbUsername = (args.length < 3 || StringUtils.isBlank(args[2])) ? "dpcom_hawk" : args[2];
        String dbPassword = (args.length < 4 || StringUtils.isBlank(args[3])) ? "123456" : args[3];

        serviceMetaGenerator.generate(new File(destFile), new ServiceMetaContext("com.mysql.jdbc.Driver", jdbcUrl,
                dbUsername, dbPassword));
    }
}
