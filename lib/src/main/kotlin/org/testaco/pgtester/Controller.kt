package org.testaco.pgtester

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/api")
@RestController
class Controller @Autowired constructor(val service: NameService) {
    @PostMapping("/")
    fun create(@RequestBody name: Name): Name = service.create(name)

    @DeleteMapping("/̉{id}")
    fun delete(@PathVariable id: Long): Int = service.delete(id)
}
