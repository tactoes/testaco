/*
 * SPDX-License-Identifier: Apache-2.0
 */
package org.testaco.util

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
