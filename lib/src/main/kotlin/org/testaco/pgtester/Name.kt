package org.testaco.pgtester

data class Name(val id: Long?, val name: String, val aliases: List<Alias>)
data class Alias(val id: Long?, val alias: String)
