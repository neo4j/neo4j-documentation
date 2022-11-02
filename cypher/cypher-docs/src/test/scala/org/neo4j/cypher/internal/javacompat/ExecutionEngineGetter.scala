package org.neo4j.cypher.internal.javacompat

object ExecutionEngineGetter {

  // Hacking around `protected`
  // (better to depend on this single thing than on lots of internal things like we did before)
  def getCypherExecutionEngine(engine: org.neo4j.cypher.internal.javacompat.ExecutionEngine)
    : org.neo4j.cypher.internal.ExecutionEngine =
    engine.cypherExecutionEngine
}
