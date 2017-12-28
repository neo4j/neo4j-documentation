/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.doc.jmx;

import org.neo4j.doc.SettingDescription;
import org.neo4j.doc.SettingDescriptionImpl;

import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class JmxBeanDocumenter {

    private static final String IFDEF_HTMLOUTPUT = "ifndef::nonhtmloutput[]\n";
    private static final String IFDEF_NONHTMLOUTPUT = "ifdef::nonhtmloutput[]\n";
    private static final String ENDIF = "endif::nonhtmloutput[]\n";
    private static final String JAVADOC_URL = "link:javadocs/";
    private static final Map<String, String> TYPES = new HashMap<String, String>() {{
        put("java.lang.String", "String");
        put("java.util.List", "List (java.util.List)");
        put("java.util.Date", "Date (java.util.Date)");
    }};

    private final MBeanServer mBeanServer;

    public JmxBeanDocumenter() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    public Set<ObjectInstance> query(String query) throws MalformedObjectNameException {
        return mBeanServer.queryMBeans(new ObjectName(query), null);
    }

    public SettingDescription asSettingDescription(ObjectName objectName, String name) throws IntrospectionException, InstanceNotFoundException, ReflectionException {
        MBeanInfo mBeanInfo = mBeanServer.getMBeanInfo(objectName);
        String description = mBeanInfo.getDescription();
        String id = getId(name);
        return new SettingDescriptionImpl(id, name, Optional.of(description));
    }

    public String asDetails(ObjectName objectName, String name) throws IntrospectionException, InstanceNotFoundException, ReflectionException {
        Set<ObjectInstance> mBeans = mBeanServer.queryMBeans(objectName, null);
        if (mBeans.size() != 1) {
            throw new IllegalStateException(String.format("Unexpected size [%s] of query result for [%s].", mBeans.size(), objectName));
        }
        ObjectInstance bean = mBeans.iterator().next();
        MBeanInfo info = mBeanServer.getMBeanInfo(objectName);
        String description = info.getDescription().replace('\n', ' ');
        String id = getId(name);
        return document(id, name, objectName, bean, info, description);
    }

    public String document(String id, String name, ObjectName objectName, ObjectInstance bean, MBeanInfo info, String description) {
        StringBuilder beanInfo = new StringBuilder(2048);
        String name0 = objectName.getKeyProperty("name0");
        if (name0 != null) {
            name += "/" + name0;
        }

        MBeanAttributeInfo[] attributes = info.getAttributes();
        beanInfo.append("[[")
                .append(id)
                .append("]]\n");
        if (attributes.length > 0) {
            beanInfo.append(".MBean ")
                    .append(name)
                    .append(" (")
                    .append(bean.getClassName())
                    .append(") Attributes\n");
            attributesTable(description, beanInfo, attributes, false);
            attributesTable(description, beanInfo, attributes, true);
            beanInfo.append("\n");
        }

        MBeanOperationInfo[] operations = info.getOperations();
        if (operations.length > 0) {
            beanInfo.append(".MBean ")
                    .append(name)
                    .append(" (")
                    .append(bean.getClassName())
                    .append(") Operations\n");
            operationsTable(beanInfo, operations, false);
            operationsTable(beanInfo, operations, true);
            beanInfo.append("\n");
        }

        return beanInfo.toString();
    }

    private void attributesTable(String description, StringBuilder beanInfo, MBeanAttributeInfo[] attributes, boolean nonHtml) {
        beanInfo.append(nonHtmlCondition(nonHtml))
                .append("[options=\"header\", cols=\"20m,36,20m,7,7\"]\n")
                .append("|===\n")
                .append("|Name|Description|Type|Read|Write\n")
                .append("5.1+^e|")
                .append(description)
                .append('\n');
        Arrays.stream(attributes)
                .map(attrInfo -> attributeRow(attrInfo, nonHtml))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(beanInfo::append);
        beanInfo.append("|===\n");
        beanInfo.append(ENDIF);
    }

    private String attributeRow(MBeanAttributeInfo attrInfo, boolean nonHtml) {
        String type = getType(attrInfo.getType());
        Descriptor descriptor = attrInfo.getDescriptor();
        type = getCompositeType(type, descriptor, nonHtml);
        return String.format("|%s|%s|%s|%s|%s%n",
                makeBreakable(attrInfo.getName(), nonHtml),
                attrInfo.getDescription().replace('\n', ' '),
                type,
                attrInfo.isReadable() ? "yes" : "no",
                attrInfo.isWritable() ? "yes" : "no"
        );
    }

    private void operationsTable(StringBuilder beanInfo, MBeanOperationInfo[] operations, boolean nonHtml) {
        beanInfo.append(nonHtmlCondition(nonHtml))
                .append("[options=\"header\", cols=\"23m,37,20m,20m\"]\n")
                .append("|===\n")
                .append("|Name|Description|ReturnType|Signature\n");
        Arrays.stream(operations)
                .map(operInfo -> operationRow(operInfo, nonHtml))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(beanInfo::append);
        beanInfo.append("|===\n");
        beanInfo.append(ENDIF);
    }

    private String operationRow(MBeanOperationInfo operInfo, boolean nonHtml) {
        StringBuilder operationRow = new StringBuilder(512);
        String type = getType(operInfo.getReturnType());
        Descriptor descriptor = operInfo.getDescriptor();
        type = getCompositeType(type, descriptor, nonHtml);
        operationRow.append(
                String.format("|%s|%s|%s|",
                        operInfo.getName(),
                        operInfo.getDescription().replace('\n', ' '),
                        type)
        );
        MBeanParameterInfo[] params = operInfo.getSignature();
        if (params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                MBeanParameterInfo param = params[i];
                operationRow.append(param.getType());
                if (i != (params.length - 1)) {
                    operationRow.append(',');
                }
            }
        } else {
            operationRow.append("(no parameters)");
        }
        operationRow.append('\n');
        return operationRow.toString();
    }

    private String getType(String type) {
        if (TYPES.containsKey(type)) {
            return TYPES.get(type);
        } else if (type.endsWith(";")) {
            if (type.startsWith("[L")) {
                return type.substring(2, type.length() - 1) + "[]";
            } else {
                throw new IllegalArgumentException(
                        "Don't know how to parse this type: " + type);
            }
        }
        return type;
    }

    private String getCompositeType(String type, Descriptor descriptor, boolean nonHtml) {
        String newType = type;
        if ("javax.management.openmbean.CompositeData[]".equals(type)) {
            Object originalType = descriptor.getFieldValue("originalType");
            if (originalType != null) {
                newType = getLinkedType(getType((String) originalType),
                        nonHtml);
                if (nonHtml) {
                    newType += " as CompositeData[]";
                } else {
                    newType += " as http://docs.oracle.com/javase/7/docs/api/javax/management/openmbean/CompositeData.html"
                            + "[CompositeData][]";
                }
            }
        }
        return newType;
    }

    private String getLinkedType(String type, boolean nonHtml) {
        if (!type.startsWith("org.neo4j")) {
            if (!type.startsWith("java.util.List<org.neo4j.")) {
                return type;
            } else {
                String typeInList = type.substring(15, type.length() - 1);
                return String.format("java.util.List<%s>", getLinkedType(typeInList, nonHtml));
            }
        } else if (nonHtml || type.startsWith("org.neo4j.kernel")) {
            return type;
        } else {
            StringBuilder url = new StringBuilder(160);
            url.append(JAVADOC_URL);
            String typeString = type;
            if (type.endsWith("[]")) {
                typeString = type.substring(0, type.length() - 2);
            }
            url.append(typeString.replace('.', '/'))
                    .append(".html[")
                    .append(typeString)
                    .append("]");
            if (type.endsWith("[]")) {
                url.append("[]");
            }
            return url.toString();
        }
    }

    private String makeBreakable(String name, boolean nonHtml) {
        if (nonHtml) {
            return name.replace("_", "_\u200A")
                    .replace("NumberOf", "NumberOf\u200A")
                    .replace("InUse", "\u200AInUse")
                    .replace("Transactions", "\u200ATransactions");
        } else {
            return name;
        }
    }

    private String getId(String name) {
        return "jmx-" + name.replace(' ', '-')
                .replace('/', '-')
                .toLowerCase();
    }

    private String nonHtmlCondition(boolean nonHtml) {
        return nonHtml ? IFDEF_NONHTMLOUTPUT : IFDEF_HTMLOUTPUT;
    }

}
