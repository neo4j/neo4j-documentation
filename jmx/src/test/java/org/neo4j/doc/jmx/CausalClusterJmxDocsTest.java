/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.doc.jmx;

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.causalclustering.discovery.Cluster;
import org.neo4j.causalclustering.discovery.CoreClusterMember;
import org.neo4j.doc.AsciiDocListGenerator;
import org.neo4j.doc.SettingDescription;
import org.neo4j.doc.SettingDescriptionImpl;
import org.neo4j.test.causalclustering.ClusterRule;

import javax.management.Descriptor;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServer;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.neo4j.kernel.configuration.Settings.NO_DEFAULT;
import static org.neo4j.kernel.configuration.Settings.STRING;
import static org.neo4j.kernel.configuration.Settings.setting;

public class CausalClusterJmxDocsTest {

    private static final String IFDEF_HTMLOUTPUT = "ifndef::nonhtmloutput[]\n";
    private static final String IFDEF_NONHTMLOUTPUT = "ifdef::nonhtmloutput[]\n";
    private static final String ENDIF = "endif::nonhtmloutput[]\n";
    private static final String QUERY = "org.neo4j:*";
    private static final String JAVADOC_URL = "link:javadocs/";
    private static final Map<String, String> TYPES = new HashMap<String, String>() {{
        put("java.lang.String", "String");
        put("java.util.List", "List (java.util.List)");
        put("java.util.Date", "Date (java.util.Date)");
    }};
    private final Path outPath = Paths.get("target", "docs", "ops");
    private final Path includesFilePath = outPath.resolve("jmx-includes.asciidoc");

    @Rule
    public final ClusterRule clusterRule = new ClusterRule( getClass() );

    private MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

    private Cluster cluster;

    @Test
    public void shouldFindCausalClusteringJmxBeans() throws Exception {
        // given
        cluster = clusterRule
                .withNumberOfCoreMembers( 3 )
                .withInstanceCoreParam(setting("jmx.port", STRING, NO_DEFAULT), id -> Integer.toString(9913 + id))
                .startCluster();
        CoreClusterMember coreClusterMember = cluster.getCoreMemberById(0);

        // when
        String core0JmxPort = coreClusterMember.settingValue("jmx.port");
        String core1JmxPort = cluster.getCoreMemberById(1).settingValue("jmx.port");

        // then
        assertEquals("9913", core0JmxPort);
        assertNotEquals("9913", core1JmxPort);

        // when
        Collection<ObjectInstance> objectInstances = mBeanServer.queryMBeans(new ObjectName(QUERY), null).stream()
                .collect(Collectors.toMap(it -> it.getObjectName().getKeyProperty("name"), p -> p, (p, q) -> p))
                .values();

        // then
        assertFalse(objectInstances.isEmpty());

        document(objectInstances);
    }

    private void document(Collection<ObjectInstance> objectInstances) throws IntrospectionException, InstanceNotFoundException, ReflectionException, IOException {
        List<SettingDescription> settingDescriptions = new ArrayList<>();
        for (ObjectInstance objectInstance : objectInstances) {
            ObjectName objectName = objectInstance.getObjectName();
            String name = objectName.getKeyProperty("name");
            settingDescriptions.add(document(objectName, name));
        }

        write(new AsciiDocListGenerator("jmx-list", "MBeans exposed by Neo4j", false).generateListAndTableCombo(settingDescriptions), path("List"));

        String includes = settingDescriptions.stream()
                .map(it -> String.format("include::jmx-%s.adoc[]%n%n", it.name().replace(" ", "-").toLowerCase()))
                .reduce("", String::concat);
        write(includes, includesFilePath);
    }

    private SettingDescription document(ObjectName objectName, String name) throws IntrospectionException, InstanceNotFoundException, ReflectionException, IOException {
        Set<ObjectInstance> mBeans = mBeanServer.queryMBeans(objectName, null);
        if (mBeans.size() != 1) {
            throw new IllegalStateException(String.format("Unexpected size [%s] of query result for [%s].", mBeans.size(), objectName));
        }
        ObjectInstance bean = mBeans.iterator().next();
        MBeanInfo info = mBeanServer.getMBeanInfo(objectName);
        String description = info.getDescription().replace('\n', ' ');
        String id = getId(name);
        document(id, name, objectName, bean, info, description);
        return new SettingDescriptionImpl(id, name, Optional.of(description));
    }

    private void document(String id, String name, ObjectName objectName, ObjectInstance bean, MBeanInfo info, String description) throws IOException {
        StringBuilder beanInfo = new StringBuilder(2048);
        Path filePath = path(name);
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
            writeAttributesTable(description, beanInfo, attributes, false);
            writeAttributesTable(description, beanInfo, attributes, true);
            beanInfo.append("\n");
        }

        MBeanOperationInfo[] operations = info.getOperations();
        if (operations.length > 0) {
            beanInfo.append(".MBean ")
                    .append(name)
                    .append(" (")
                    .append(bean.getClassName())
                    .append(") Operations\n");
            writeOperationsTable(beanInfo, operations, false);
            writeOperationsTable(beanInfo, operations, true);
            beanInfo.append("\n");
        }

        if (beanInfo.length() > 0) {
            write(beanInfo.toString(), filePath);
        }
    }

    private void writeAttributesTable(String description, StringBuilder beanInfo, MBeanAttributeInfo[] attributes, boolean nonHtml) {
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

    private void writeOperationsTable(StringBuilder beanInfo, MBeanOperationInfo[] operations, boolean nonHtml) {
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

    private String nonHtmlCondition(boolean nonHtml) {
        return nonHtml ? IFDEF_NONHTMLOUTPUT : IFDEF_HTMLOUTPUT;
    }

    private Path path(String name) {
        String filename = String.format("jmx-%s.adoc", name.replace(" ", "-").toLowerCase());
        return outPath.resolve(filename);
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

    private void write(String content, Path filePath) throws IOException {
        Path parentDir = filePath.getParent();
        if (!Files.exists(parentDir)) {
            Files.createDirectories(parentDir);
        }
        Files.write(filePath, content.getBytes("UTF-8"));
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

}
