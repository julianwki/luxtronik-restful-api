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

    private val connection =
        TCPMasterConnection(InetAddress.getByName(modbusHost))
            .apply {
                this.port = modbusPort
                this.timeout = 3000
            }

    fun activate(function: String, register: Int) =
        switch(function, register, OnOff.ON)
            .also { logger.info("Activated $function with register $register") }
    fun deactivate(function: String, register: Int) =
        switch(function, register, OnOff.OFF)
            .also { logger.info("Deactivated $function with register $register") }

    private enum class OnOff(
        val value: Int
    ) {
        ON(1),
        OFF(0)
    }

    @Synchronized
    private fun switch(function: String, register: Int, onOff: OnOff) {
        try {
            logger.debug("Connecting to $modbusHost:$modbusPort...")
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
    fun status(function: String, register: Int): Any {
        try {
            logger.debug("Connecting to $modbusHost:$modbusPort...")
            connection.connect()
            return ModbusTCPTransaction(connection).readRegister(register)
                .also { logger.info("Read $function with register $register and value $it") }
        } catch (e: Exception) {
            logger.error("Error while reading register $function", e)
            throw e
        } finally {
            if (connection.isConnected) connection.close()
        }
    }

    private fun ModbusTCPTransaction.writeRegister(register: Int, onOff: OnOff) =
        WriteSingleRegisterRequest(register, SimpleRegister(onOff.value))
            .apply {
                unitID = modbusSlave
            }.let {
                request = it
                execute()
                response as WriteSingleRegisterResponse
            }.also {
                logger.debug("Writing register $register of slave $modbusSlave with value ${onOff.value}...")
            }.registerValue

    private fun ModbusTCPTransaction.readRegister(register: Int) =
        ReadMultipleRegistersRequest(register, 1)
            .apply {
                unitID = modbusSlave
            }.let {
                request = it
                execute()
                response as ReadMultipleRegistersResponse
            }.also {
                logger.debug("Reading register $register of slave $modbusSlave...")
            }.registers[0].value

}