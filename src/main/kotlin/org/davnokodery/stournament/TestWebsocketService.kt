package org.davnokodery.stournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.*


@Service
class TestWebsocketService {

    @Autowired
    private lateinit var simpMessagingTemplate: SimpMessagingTemplate

    fun sendTest() {
        simpMessagingTemplate.convertAndSend("/topic/test-response/123", TestResponseMessage("Moscow time is " + Date()))
        simpMessagingTemplate.convertAndSend("/topic/test-response/234", TestResponseMessage("Spb time is " + Date()))
    }

    @Scheduled(fixedRateString = "5000")
    fun testJob() {
        println("run job")

        sendTest()
    }
}
