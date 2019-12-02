package net.corda.examples.energyaccount.clients.api

import com.fasterxml.jackson.databind.ObjectMapper
import net.corda.client.jackson.JacksonSupport
import net.corda.client.rpc.CordaRPCClient
import net.corda.client.rpc.RPCException
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.NetworkHostAndPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean

/**
 * Our Spring Boot application.
 */
@SpringBootApplication
open class CordaEnergyAccountWebApp {

    @Value("\${corda.host}")
    lateinit var cordaHost: String

    @Value("\${corda.user}")
    lateinit var cordaUser: String

    @Value("\${corda.password}")
    lateinit var cordaPassword: String

    @Bean
    open fun rpcClient(): CordaRPCOps {
        log.info("Connecting to Corda on $cordaHost using username $cordaUser and password $cordaPassword")
        // TODO remove this when CordaRPC gets proper connection retry, please
        var maxRetries = 10
        do {
            try {
                return CordaRPCClient(NetworkHostAndPort.parse(cordaHost))
                        .start(cordaUser, cordaPassword).proxy
            } catch (ex: RPCException) {
                if (maxRetries-- > 0) {
                    Thread.sleep(1000)
                } else {
                    throw ex
                }
            }
        } while (true)
    }

    @Bean
    open fun objectMapper(@Autowired cordaRPCOps: CordaRPCOps): ObjectMapper {
        val mapper = JacksonSupport.createDefaultMapper(cordaRPCOps)
        return mapper
    }


    companion object {
        private val log = LoggerFactory.getLogger(this::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(CordaEnergyAccountWebApp::class.java, *args)
        }
    }
}
