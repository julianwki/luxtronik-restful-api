package com.wiersbitzki.luxtronik.services

import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse
import com.ghgande.j2mod.modbus.net.TCPMasterConnection
import com.ghgande.j2mod.modbus.procimg.SimpleRegister
import com.wiersbitzki.luxtronik.Logging
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.config.tryGetString
import org.koin.core.annotation.Single
import java.net.InetAddress

@Single
class SingleRegisterModbusService(
    config: ApplicationConfig,
    private val modbusHost: String = config.tryGetString("ktor.service.modbus.host")!!,
    private val modbusPort: Int = config.tryGetString("ktor.service.modbus.port")!!.toInt(),
    private val modbusSlave: Int = config.tryGetString("ktor.service.modbus.unitId")!!.toInt()
) : Logging {

    val connection =
        TCPMasterConnection(InetAddress.getByName(modbusHost))
            .apply {
                this.port = modbusPort
                this.timeout = 3000
            }

    fun activate(function: String, register: Int) = switch(function, register, OnOff.ON)
    fun deactivate(function: String, register: Int) = switch(function, register, OnOff.OFF)

    private enum class OnOff(
        val value: Int
    ) {
        ON(1),
        OFF(2)
    }

    private fun switch(function: String, register: Int, onOff: OnOff): Map<String, Any> {
        try {
            logger.debug("Connecting to $modbusHost:$modbusPort...")
            connection.connect()
            logger.debug("Writing register $register of slave $modbusSlave with value ${onOff.value}...")
            val registerValue = ModbusTCPTransaction(connection).writeRegister(register, onOff.value)
            return mapOf(function to registerValue)
                .also { logger.info(it.toString()) }
        } catch (e: Exception) {
            logger.error("Error while writing register $function with value ${onOff.value}", e)
            throw e
        } finally {
            if (connection.isConnected) connection.close()
        }
    }

    fun status(function: String, register: Int): Map<String, Any> {
        try {
            logger.debug("Connecting to $modbusHost:$modbusPort...")
            connection.connect()
            logger.debug("Reading register $register of slave $modbusSlave...")
            val registerValue = ModbusTCPTransaction(connection)
                .readRegister(register)
            return mapOf(function to registerValue)
                .also { logger.info(it.toString()) }
        } catch (e: Exception) {
            logger.error("Error while reading register $function", e)
            throw e
        } finally {
            if (connection.isConnected) connection.close()
        }
    }

    private fun ModbusTCPTransaction.writeRegister(register: Int, value: Int) =
        WriteSingleRegisterRequest(register, SimpleRegister(value))
            .apply {
                unitID = modbusSlave
            }.let {
                request = it
                execute()
                response as WriteSingleRegisterResponse
            }.registerValue

    private fun ModbusTCPTransaction.readRegister(register: Int) =
        ReadMultipleRegistersRequest(register, 1)
            .apply {
                unitID = modbusSlave
            }.let {
                request = it
                execute()
                response as ReadMultipleRegistersResponse
            }.registers[0].value
}