/*
 * Copyright (c) 2002-2019 "Neo Technology,"
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
package org.neo4j.doc;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ProcedureReferenceGenerator {

    private final String query = "CALL dbms.procedures()";
    private final String ENTERPRISE_FEATURE_ROLE_TEMPLATE = "[enterprise-edition]#%s#";
    private final Neo4jInstance neo;
    private boolean includeRolesColumn = true;
    private Predicate<Procedure> filter;

    private PrintStream out;

    public ProcedureReferenceGenerator() {
        this.neo = new Neo4jInstance();
    }

    public String document(String id, String title, String edition, Predicate<Procedure> filter) {
        this.filter = filter;
        this.includeRolesColumn = !edition.equalsIgnoreCase("community");
        Map<String, Procedure> communityProcedures = edition.equalsIgnoreCase("enterprise") ? Collections.emptyMap() : communityEditionProcedures();
        Map<String, Procedure> enterpriseProcedures = edition.equalsIgnoreCase("community") ? Collections.emptyMap() : enterpriseEditionProcedures();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.out = new PrintStream(baos);

        out.printf("[[%s]]%n", id);
        out.printf(".%s%n", title);
        out.printf("[options=header, cols=\"%s\"]%n", includeRolesColumn ? "a,a,m,a" : "a,a,m");
        out.printf("|===%n");
        out.printf("|Name%n|Description%n|Signature%n");
        if (includeRolesColumn) {
            out.printf("|").printf(ENTERPRISE_FEATURE_ROLE_TEMPLATE, "Roles").printf("%n");
        }
        document(communityProcedures, enterpriseProcedures);
        out.printf("|===%n");
        out.flush();
        return baos.toString();
    }

    private Map<String, Procedure> communityEditionProcedures() {
        GraphDatabaseService db = neo.newCommunityInstance();
        Map<String, Procedure> procedures = procedures(db);
        db.shutdown();
        return procedures;
    }

    private Map<String, Procedure> enterpriseEditionProcedures() {
        GraphDatabaseService db = neo.newEnterpriseInstance();
        Map<String, Procedure> procedures = procedures(db);
        db.shutdown();
        return procedures;
    }

    private Map<String, Procedure> procedures(GraphDatabaseService db) {
        Map<String, Procedure> procedures;
        try (Transaction ignore = db.beginTx(); Result result = db.execute(query)) {
            procedures = parseResult(result);
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
            out.printf("|%s |%s |%s",
                    it.enterpriseOnly() ? String.format(ENTERPRISE_FEATURE_ROLE_TEMPLATE, it.name()) : it.name(),
                    it.description(),
                    it.signature()
            );
            if (includeRolesColumn) {
                out.printf(" |%s", null == it.roles() ? "N/A" : String.format(ENTERPRISE_FEATURE_ROLE_TEMPLATE, String.join(", ", it.roles())));
            }
            out.printf("%n");
        });
    }


    class Procedure {
        private String name;
        private String signature;
        private String description;
        private List<String> roles;
        private Boolean enterpriseOnly;
        Procedure(Map<String, Object> row) {
            this.name = (String) row.get("name");
            this.signature = (String) row.get("signature");
            this.description = (String) row.get("description");
            this.roles = (List<String>) row.get("roles");
            this.enterpriseOnly = false;
        }

        String name() {
            return name;
        }
        String signature() {
            return signature;
        }
        String description() {
            return description;
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
