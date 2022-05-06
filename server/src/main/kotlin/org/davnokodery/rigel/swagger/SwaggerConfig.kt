package org.davnokodery.rigel.swagger

import com.fasterxml.classmate.TypeResolver
import org.davnokodery.rigel.ClientWsMessage
import org.davnokodery.rigel.ServerWsMessage
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import springfox.documentation.builders.PathSelectors
import springfox.documentation.builders.RequestHandlerSelectors
import springfox.documentation.spi.DocumentationType
import springfox.documentation.spring.web.plugins.Docket

@Configuration
class SwaggerConfig {
    @Bean
    fun api(): Docket {
        return Docket(DocumentationType.OAS_30)
            .select()
            .apis(RequestHandlerSelectors.any())
            .paths(PathSelectors.any())
            .build()
    }

    @Bean
    fun clientMessages(typeResolver: TypeResolver): Docket {
        return Docket(DocumentationType.OAS_30)
            .groupName("client messages")
            .additionalModels(typeResolver.resolve(ClientWsMessage::class.java))
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.davnokodery.rigel.swagger"))
            .paths(PathSelectors.any())
            .build()
    }

    @Bean
    fun serverMessages(typeResolver: TypeResolver): Docket {
        return Docket(DocumentationType.OAS_30)
            .groupName("server messages")
            .additionalModels(typeResolver.resolve(ServerWsMessage::class.java))
            .select()
            .apis(RequestHandlerSelectors.basePackage("org.davnokodery.rigel.swagger"))
            .paths(PathSelectors.any())
            .build()
    }
}
