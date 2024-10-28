package org.testaco

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TestacoExtensionDumpedFileNameTest {
 val uut = TestacoExtension()
 @Test
 fun getResultFileName() {
  assertEquals("target/testaco/datasource/foo_result.json", uut.getResultFileName("datasource", "foo.json"))
  assertEquals("target/testaco/datasource/foo.bar.baz_result.json", uut.getResultFileName("datasource", "foo.bar.baz.json"))
  assertEquals("target/testaco/data.source/foo_result.json", uut.getResultFileName("data.source", "foo.json"))
  assertEquals("target/testaco/data.source/foo_result", uut.getResultFileName("data.source", "foo"))
  assertEquals("target/testaco/datasource/_result.json", uut.getResultFileName("datasource", ".json"))
 }
}