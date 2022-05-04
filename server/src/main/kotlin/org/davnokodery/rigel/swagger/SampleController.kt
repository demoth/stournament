package org.davnokodery.rigel.swagger

import io.swagger.annotations.Api
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@Api("Really a bogus controller")
class SampleController {
    @GetMapping
    fun getSample(): String {
        return "OK"
    }
}
