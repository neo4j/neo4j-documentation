/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j Enterprise Edition. The included source
 * code can be redistributed and/or modified under the terms of the
 * GNU AFFERO GENERAL PUBLIC LICENSE Version 3
 * (http://www.fsf.org/licensing/licenses/agpl-3.0.html) with the
 * Commons Clause, as found in the associated LICENSE.txt file.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * Neo4j object code can be licensed independently from the source
 * under separate terms from the AGPL. Inquiries can be directed to:
 * licensing@neo4j.com
 *
 * More information is also available at:
 * https://neo4j.com/licensing/
 */
package org.neo4j.doc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import static org.neo4j.configuration.GraphDatabaseSettings.DEFAULT_DATABASE_NAME;

public class ProcedureReferenceGenerator {

    private final String query = "CALL dbms.procedures()";
    private final String ENTERPRISE_FEATURE_ROLE_TEMPLATE = "[enterprise-edition]#%s#";
    private final Neo4jInstance neo;
    private boolean includeRolesColumn = true;
    private boolean inlineEditionRole = false;
    private Predicate<Procedure> filter;

    private PrintStream out;

    public ProcedureReferenceGenerator() {
        this.neo = new Neo4jInstance();
    }

    public String document(String id, String title, String edition, Predicate<Procedure> filter) throws IOException
    {
        this.filter = filter;
        this.includeRolesColumn = !edition.equalsIgnoreCase("community");
        this.inlineEditionRole = edition.equalsIgnoreCase("both");
        Map<String, Procedure> communityProcedures = edition.equalsIgnoreCase("enterprise") ? Collections.emptyMap() : communityEditionProcedures();
        Map<String, Procedure> enterpriseProcedures = edition.equalsIgnoreCase("community") ? Collections.emptyMap() : enterpriseEditionProcedures();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.out = new PrintStream(baos);

        out.printf("[[%s]]%n", id);
        if (!inlineEditionRole && includeRolesColumn) {
            out.printf("[role=enterprise-edition]%n");
        }
        out.printf(".%s%n", title);
        out.printf("[options=header, cols=\"%s\"]%n", includeRolesColumn ? "a,a,m,m,a" : "a,a,m,m");
        out.printf("|===%n");
        out.printf("|Name%n|Description%n|Signature%n|Mode");
        if (includeRolesColumn) {
            out.printf("|").printf(inlineEditionRole ? ENTERPRISE_FEATURE_ROLE_TEMPLATE : "%s", "Roles").printf("%n");
        }
        document(communityProcedures, enterpriseProcedures);
        out.printf("|===%n");
        out.flush();
        return baos.toString();
    }

    private Map<String, Procedure> communityEditionProcedures() throws IOException {
        DatabaseManagementService managementService = neo.newCommunityInstance();
        GraphDatabaseService db = managementService.database( DEFAULT_DATABASE_NAME );
        Map<String, Procedure> procedures = procedures(db);
        managementService.shutdown();
        return procedures;
    }

    private Map<String, Procedure> enterpriseEditionProcedures() throws IOException {
        DatabaseManagementService managementService = neo.newEnterpriseInstance();
        GraphDatabaseService db = managementService.database( DEFAULT_DATABASE_NAME );
        Map<String, Procedure> procedures = procedures(db);
        managementService.shutdown();
        return procedures;
    }

    private Map<String, Procedure> procedures(GraphDatabaseService db) {
        Map<String, Procedure> procedures;
        try ( Transaction tx = db.beginTx() )
        {
            try ( Result result = tx.execute( query ) )
            {
                procedures = parseResult( result );
            }
        }
        return procedures;
    }

    private Map<String, Procedure> parseResult(Result result) {
        Map<String, Procedure> procedures = new HashMap<>();
        result.stream().forEach(row -> {
            Procedure p = new Procedure(row);
            procedures.put(p.name(), p);
        });
        return procedures;
    }

    private void document(Map<String, Procedure> communityProcedures, Map<String, Procedure> enterpriseProcedures) {
        enterpriseProcedures.values().forEach(
                proc -> proc.setEnterpriseOnly(!proc.equals(communityProcedures.get(proc.name())))
        );
        Stream.concat(
                enterpriseProcedures.values().stream(),
                communityProcedures.values().stream().filter(proc -> !proc.equals(enterpriseProcedures.get(proc.name())))
        ).sorted(
                Comparator
                        .comparing(Procedure::enterpriseOnly)
                        .thenComparing(Procedure::name)
        ).filter(filter).forEach(it -> {
            out.printf("|%s |%s |%s |%s",
                    it.enterpriseOnly() ? String.format(inlineEditionRole ? ENTERPRISE_FEATURE_ROLE_TEMPLATE : "%s", it.name()) : it.name(),
                    it.description(),
                    it.signature(),
                    it.mode()
            );
            if (includeRolesColumn) {
                out.printf(" |%s", null == it.roles() ? "N/A" : String.format(inlineEditionRole ? ENTERPRISE_FEATURE_ROLE_TEMPLATE : "%s", String.join(", ", it.roles())));
            }
            out.printf("%n");
        });
    }

    class Procedure {
        private String name;
        private String signature;
        private String description;
        private String mode;
        private List<String> roles;
        private Boolean enterpriseOnly;
        Procedure(Map<String, Object> row) {
            setName((String) row.get("name"));
            this.signature = (String) row.get("signature");
            this.description = (String) row.get("description");
            this.mode = (String) row.get("mode");
            this.roles = (List<String>) row.get("roles");
            this.enterpriseOnly = false;
        }

        String name() {
            return name;
        }
        void setName(String name) {
            this.name = name.endsWith("()") ? name : name + "()";
        }
        String signature() {
            return signature;
        }
        String description() {
            return description;
        }
        String mode() {
            return mode;
        }
        List<String> roles() {
            return roles;
        }
        Boolean enterpriseOnly() { return enterpriseOnly; }
        void setEnterpriseOnly(Boolean enterpriseOnly) { this.enterpriseOnly = enterpriseOnly; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Procedure procedure = (Procedure) o;

            if (!name.equals(procedure.name)) return false;
            return signature.equals(procedure.signature);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + signature.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Procedure{" +
                    "name='" + name + '\'' +
                    ", signature='" + signature + '\'' +
                    '}';
        }
    }

}
