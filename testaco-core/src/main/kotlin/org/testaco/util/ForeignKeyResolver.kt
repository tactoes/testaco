/*
 * Copyright 2026 Geir Hedemark
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */package org.testaco.util

import org.testaco.schema.Schema

class ForeignKeyResolver(private val schema: Schema) {

    fun insertOrder(tables: List<String>): List<String> {
        val graph = buildDependencyGraph(tables)
        return topologicalSort(graph, tables)
    }

    fun truncateOrder(tables: List<String>): List<String> {
        return insertOrder(tables).reversed()
    }

    fun hasCycles(tables: List<String>): Boolean {
        val graph = buildDependencyGraph(tables)
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun dfs(node: String): Boolean {
            if (node in inStack) return true
            if (node in visited) return false
            visited.add(node)
            inStack.add(node)
            for (dep in graph[node] ?: emptyList()) {
                if (dfs(dep)) return true
            }
            inStack.remove(node)
            return false
        }

        return tables.any { dfs(it) }
    }

    private fun buildDependencyGraph(tables: List<String>): Map<String, List<String>> {
        val tableSet = tables.toSet()
        val graph = mutableMapOf<String, MutableList<String>>()
        for (table in tables) {
            graph[table] = mutableListOf()
            val tableDef = schema.tables[table] ?: continue
            for (fk in tableDef.foreignKeys) {
                if (fk.references in tableSet) {
                    graph[table]!!.add(fk.references)
                }
            }
        }
        return graph
    }

    private fun topologicalSort(graph: Map<String, List<String>>, tables: List<String>): List<String> {
        val result = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val inStack = mutableSetOf<String>()

        fun visit(node: String) {
            if (node in visited) return
            if (node in inStack) return
            inStack.add(node)
            for (dep in graph[node] ?: emptyList()) {
                visit(dep)
            }
            inStack.remove(node)
            visited.add(node)
            result.add(node)
        }

        for (table in tables) {
            visit(table)
        }
        return result
    }
}
