package sapala.s2sauthservice.buildmanagerservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import java.io.File

@Configuration
class Env {
    @Value("\${build.version}")
    private val version: String? = null
    fun version() = version!!

    @Value("\${server.port}")
    private val port: Int? = null
    fun port() = port!!

    @Value("\${host}")
    private val host: String? = null
    fun host() = host!!

    @Value("\${build-manager-service.build-directory}")
    private val buildDirectory: String? = null
    fun buildDirectory() = File(buildDirectory!!)

    @Value("\${build-service.url}")
    private val buildServiceUrl: String? = null
    fun buildServiceUrl() = buildServiceUrl!!
}
