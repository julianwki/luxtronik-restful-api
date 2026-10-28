package com.wiersbitzki.luxtronik.services

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse
import com.ghgande.j2mod.modbus.net.TCPMasterConnection
import com.ghgande.j2mod.modbus.procimg.SimpleRegister
import com.wiersbitzki.luxtronik.SwitchService
import com.wiersbitzki.luxtronik.SwitchService.OnOff
import io.ktor.server.config.ApplicationConfig
import org.koin.core.annotation.Single
import java.net.InetAddress

@Single
class SingleRegisterModbusService(
    private val config: Config,
) : SwitchService {

    data class Config(
        val host: String,
        val port: Int,
        val unitId: Int
    ) {
        constructor(config: ApplicationConfig) : this(
            host = config.property("service.modbus.host").getString(),
            port = config.property("service.modbus.port").getString().toInt(),
            unitId = config.property("service.modbus.unitId").getString().toInt(),
        )
    }

    private val connection =
        TCPMasterConnection(InetAddress.getByName(config.host))
            .apply {
                this.port = config.port
                this.timeout = 3000
            }

    override fun activate(function: String, register: Int) =
        switch(function, register, OnOff.ON)
            .also { logger.info("Activated $function with register $register") }

    override fun deactivate(function: String, register: Int) =
        switch(function, register, OnOff.OFF)
            .also { logger.info("Deactivated $function with register $register") }

    @Synchronized
    override fun switch(function: String, register: Int, onOff: OnOff) {
        try {
            logger.debug("Connecting to ${config.host}:${config.port}...")
            connection.connect()
            ModbusTCPTransaction(connection).writeRegister(register, onOff)
        } catch (e: Exception) {
            logger.error("Error while writing register $function with value ${onOff.value}", e)
            throw e
        } finally {
            if (connection.isConnected) connection.close()
        }
    }

    @Synchronized
    override fun read(function: String, register: Int): Int {
        try {
            logger.debug("Connecting to ${config.host}:${config.port}...")
            connection.connect()
            return ModbusTCPTransaction(connection).readRegister(register)
                .also { logger.debug("Read $function with register $register and value $it") }
        } catch (e: Exception) {
            logger.error("Error while reading register $function", e)
            throw e
        } finally {
            if (connection.isConnected) connection.close()
        }
    }

    @Synchronized
    override fun readInput(function: String, register: Int): Int {
        try {
            logger.debug("Connecting to ${config.host}:${config.port}...")
            connection.connect()
            return ModbusTCPTransaction(connection).readInputRegister(register)
                .also { logger.debug("Read $function with register $register and value $it") }
        } catch (e: Exception) {
            logger.error("Error while reading register $function", e)
            throw e
        } finally {
            if (connection.isConnected) connection.close()
        }
    }

    @Synchronized
    override fun write(function: String, register: Int, value: Int) {
        try {
            logger.debug("Connecting to ${config.host}:${config.port}...")
            connection.connect()
            ModbusTCPTransaction(connection).writeRegister(register, value)
                .also { logger.debug("Wrote $function with register $register and value $it") }
        } catch (e: Exception) {
            logger.error("Error while reading register $function", e)
            throw e
        } finally {
            if (connection.isConnected) connection.close()
        }
    }

    private fun ModbusTCPTransaction.writeRegister(register: Int, onOff: OnOff) =
        writeRegister(register, onOff.value)

    private fun ModbusTCPTransaction.writeRegister(register: Int, value: Int) =
        WriteSingleRegisterRequest(register, SimpleRegister(value))
            .apply {
                unitID = config.unitId
            }.let {
                request = it
                execute()
                response as WriteSingleRegisterResponse
            }.also {
                logger.debug("Writing register $register of slave ${config.unitId} with value ${value}...")
            }.registerValue

    private fun ModbusTCPTransaction.readRegister(register: Int) =
        ReadMultipleRegistersRequest(register, 1)
            .apply {
                unitID = config.unitId
            }.let {
                request = it
                execute()
                response as ReadMultipleRegistersResponse
            }.also {
                logger.debug("Reading register $register of slave ${config.unitId}...")
            }.registers[0].value

    private fun ModbusTCPTransaction.readInputRegister(register: Int) =
        ReadInputRegistersRequest(register, 1)
            .apply {
                unitID = config.unitId
            }.let {
                request = it
                execute()
                response as ReadInputRegistersResponse
            }.also {
                logger.debug("Reading register $register of slave ${config.unitId}...")
            }.registers[0].value
}