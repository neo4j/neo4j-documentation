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
package org.neo4j.doc;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ProcedureReferenceGenerator {

    private final String query = "CALL dbms.procedures()";
    private final Neo4jInstance neo;

    private PrintStream out;

    public ProcedureReferenceGenerator() {
        this.neo = new Neo4jInstance();
    }

    public String document(String id, String title) {
        Map<String, Procedure> communityProcedures = communityEditionProcedures();
        Map<String, Procedure> enterpriseProcedures = enterpriseEditionProcedures();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.out = new PrintStream(baos);

        out.printf("[%s]%n", id);
        out.printf(".%s%n", title);
        out.printf("[options=header]%n");
        out.printf("|===%n");
        out.printf("|Name%n|Description%n|Signature%n[roles=enterprise]|Roles (Enterprise Edition)%n");
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
        try (Transaction ignore = db.beginTx(); Result result = db.execute(query);) {
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
        communityProcedures.values().forEach(it -> {
            out.printf("|%s |%s |%s |%s%n",
                    it.name(),
                    it.description(),
                    it.signature(),
                    enterpriseProcedures.containsKey(it.name()) ? String.join(",", enterpriseProcedures.get(it.name()).roles()) : "N/A"
            );
        });
        List<Procedure> distinctEnterpriseProcedures = enterpriseProcedures.entrySet().stream()
                .filter(it -> !communityProcedures.keySet().contains(it.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
        distinctEnterpriseProcedures.forEach(it -> {
            out.printf("|%s |%s |%s |%s%n",
                    it.name(),
                    it.description(),
                    it.signature(),
                    String.join(",", it.roles())
            );
        });
    }

    class Procedure {
        private String name;
        private String signature;
        private String description;
        private List<String> roles;
        Procedure(Map<String, Object> row) {
            this.name = (String) row.get("name");
            this.signature = (String) row.get("signature");
            this.description = (String) row.get("description");
            this.roles = (List<String>) row.get("roles");
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
    }

}
