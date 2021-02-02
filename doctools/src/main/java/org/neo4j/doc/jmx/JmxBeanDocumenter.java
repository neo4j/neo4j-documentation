/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
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

import org.neo4j.doc.AsciiDocListGenerator;
import org.neo4j.doc.SettingDescription;
import org.neo4j.doc.SettingDescriptionImpl;
import org.neo4j.doc.util.FileUtil;

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
import java.io.IOException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class JmxBeanDocumenter {

    private static final String IFDEF_HTMLOUTPUT = "ifndef::nonhtmloutput[]\n";
    private static final String IFDEF_NONHTMLOUTPUT = "ifdef::nonhtmloutput[]\n";
    private static final String ENDIF = "endif::nonhtmloutput[]\n";
    private static final String JAVADOC_URL = "link:{neo4j-javadoc-base-uri}/";
    private static final Map<String, String> TYPES = new HashMap<String, String>() {{
        put("java.lang.String", "String");
        put("java.util.List", "List (java.util.List)");
        put("java.util.Date", "Date (java.util.Date)");
    }};

    private final MBeanServer mBeanServer;

    public JmxBeanDocumenter() {
        this.mBeanServer = ManagementFactory.getPlatformMBeanServer();
    }

    public void document(List<ObjectInstance> objectInstances, FileUtil fileUtil, AsciiDocListGenerator asciiDocListGenerator) throws IntrospectionException, InstanceNotFoundException, ReflectionException, IOException {
        List<SettingDescription> settingDescriptions = new ArrayList<>();
        for (ObjectInstance objectInstance : objectInstances) {
            ObjectName objectName = objectInstance.getObjectName();
            String name = objectName.getKeyProperty("name");
            PrintStream out = fileUtil.fileBackedPrintStream(name);

            settingDescriptions.add(asSettingDescription(objectName, name));
            asDetails(objectName, name, out);
            out.flush();
        }
        fileUtil.write(asciiDocListGenerator.generateListAndTableCombo(settingDescriptions), "List");

        String includes = settingDescriptions.stream()
                .map(it -> String.format("include::%s[]%n%n", fileUtil.filename(it.name())))
                .reduce("", String::concat);
        fileUtil.write(includes, "includes");
    }

    public Set<ObjectInstance> query(String query) throws MalformedObjectNameException {
        return mBeanServer.queryMBeans(new ObjectName(query), null);
    }

    private SettingDescription asSettingDescription(ObjectName objectName, String name) throws IntrospectionException, InstanceNotFoundException, ReflectionException {
        MBeanInfo mBeanInfo = mBeanServer.getMBeanInfo(objectName);
        String description = mBeanInfo.getDescription();
        String id = getId(name);
        return new SettingDescriptionImpl(id, name, Optional.of(description));
    }

    private void asDetails(ObjectName objectName, String name, PrintStream out) throws IntrospectionException, InstanceNotFoundException, ReflectionException {
        Set<ObjectInstance> mBeans = mBeanServer.queryMBeans(objectName, null);
        if (mBeans.size() != 1) {
            throw new IllegalStateException(String.format("Unexpected size [%s] of query result for [%s].", mBeans.size(), objectName));
        }
        ObjectInstance bean = mBeans.iterator().next();
        MBeanInfo info = mBeanServer.getMBeanInfo(objectName);
        String description = info.getDescription().replace('\n', ' ');
        String id = getId(name);
        String name0 = objectName.getKeyProperty("name0");
        if (name0 != null) {
            name += "/" + name0;
        }

        List<MBeanAttributeInfo> attributes = Arrays.asList(info.getAttributes());
        if (name.equalsIgnoreCase("Configuration")) {
            attributes = attributes.stream().filter(it -> !it.getName().startsWith("unsupported")).collect(Collectors.toList());
        }

        out.printf("[[%s]]%n", id);
        if (attributes.size() > 0) {
            out.printf(".MBean %s (%s) Attributes%n", name, bean.getClassName());
            attributesTable(description, out, attributes, false);
            attributesTable(description, out, attributes, true);
            out.println();
        }

        List<MBeanOperationInfo> operations = Arrays.asList(info.getOperations());
        if (operations.size() > 0) {
            out.printf(".MBean %s (%s) Operations%n", name, bean.getClassName());
            operationsTable(out, operations, false);
            operationsTable(out, operations, true);
            out.println();
        }
    }

    private void attributesTable(String description, PrintStream out, List<MBeanAttributeInfo> attributes, boolean nonHtml) {
        out.print(nonHtmlCondition(nonHtml));
        out.println("[options=\"header\", cols=\"20m,36,20m,7,7\"]");
        out.println("|===");
        out.println("|Name|Description|Type|Read|Write");
        out.printf("5.1+^e|%s%n", description);
        attributes.stream()
                .sorted(Comparator.comparing(it -> it.getName().toLowerCase()))
                .forEach(it -> attributeRow(out, it, nonHtml));
        out.println("|===");
        out.print(ENDIF);
    }

    private void attributeRow(PrintStream out, MBeanAttributeInfo attrInfo, boolean nonHtml) {
        String type = getType(attrInfo.getType());
        Descriptor descriptor = attrInfo.getDescriptor();
        type = getCompositeType(type, descriptor, nonHtml);
        out.printf("|%s|%s|%s|%s|%s%n",
                makeBreakable(attrInfo.getName(), nonHtml),
                attrInfo.getDescription().replace('\n', ' '),
                type,
                attrInfo.isReadable() ? "yes" : "no",
                attrInfo.isWritable() ? "yes" : "no");
    }

    private void operationsTable(PrintStream out, List<MBeanOperationInfo> operations, boolean nonHtml) {
        out.print(nonHtmlCondition(nonHtml));
        out.println("[options=\"header\", cols=\"23m,37,20m,20m\"]");
        out.println("|===");
        out.println("|Name|Description|ReturnType|Signature");
        operations.stream()
                .sorted(Comparator.comparing(it -> it.getName().toLowerCase()))
                .forEach(it -> operationRow(out, it, nonHtml));
        out.println("|===");
        out.print(ENDIF);
    }

    private void operationRow(PrintStream out, MBeanOperationInfo operInfo, boolean nonHtml) {
        String type = getType(operInfo.getReturnType());
        Descriptor descriptor = operInfo.getDescriptor();
        type = getCompositeType(type, descriptor, nonHtml);
        out.printf("|%s|%s|%s|",
                operInfo.getName(),
                operInfo.getDescription().replace('\n', ' '),
                type);
        MBeanParameterInfo[] params = operInfo.getSignature();
        if (params.length > 0) {
            for (int i = 0; i < params.length; i++) {
                MBeanParameterInfo param = params[i];
                out.print(param.getType());
                if (i != (params.length - 1)) {
                    out.print(",");
                }
            }
        } else {
            out.print("(no parameters)");
        }
        out.println();
    }

    private String getType(String type) {
        if (TYPES.containsKey(type)) {
            return TYPES.get(type);
        } else if (type.endsWith(";")) {
            if (type.startsWith("[L")) {
                return type.substring(2, type.length() - 1) + "[]";
            } else {
                throw new IllegalArgumentException("Don't know how to parse this type: " + type);
            }
        }
        return type;
    }

    private String getCompositeType(String type, Descriptor descriptor, boolean nonHtml) {
        String newType = type;
        if ("javax.management.openmbean.CompositeData[]".equals(type)) {
            Object originalType = descriptor.getFieldValue("originalType");
            if (originalType != null) {
                newType = getLinkedType(getType((String) originalType), nonHtml);
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
