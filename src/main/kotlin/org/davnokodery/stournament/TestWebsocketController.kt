package org.davnokodery.stournament

import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.CrossOrigin

data class TestRequestMessage(val content: String)
data class TestResponseMessage(val content: String)


@Controller
@CrossOrigin
class TestWebsocketController {
    @MessageMapping("/test-request")
//    @SendTo("/topic/test-response")
    fun respond(request: TestRequestMessage)
//    : TestResponseMessage
    {
        println("received: " + request.content)
//        return TestResponseMessage("Hello, ${request.content}")
    }
}
