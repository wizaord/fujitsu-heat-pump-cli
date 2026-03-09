package com.airstage.cli

import com.airstage.adapter.inbound.cli.buildClimCli
import com.airstage.adapter.outbound.airstage.AirStageLocalClient
import com.airstage.domain.service.HeatPumpService
import com.airstage.fake.FakeAirStageServer
import com.github.ajalt.clikt.core.main
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class ClimCliIT {

    private lateinit var server: FakeAirStageServer
    private var port: Int = 0
    private lateinit var client: AirStageLocalClient

    @BeforeEach
    fun setUp() {
        server = FakeAirStageServer()
        port = server.start()
        client = AirStageLocalClient(
            host = "127.0.0.1:$port",
            deviceId = "94BB43E75769",
            useHttps = false,
        )
    }

    @AfterEach
    fun tearDown() {
        client.close()
        server.stop()
    }

    @Test
    fun `status affiche l etat courant de la PAC eteinte en mode HEAT`() {
        server.setParameterValue("iu_onoff", "0")
        server.setParameterValue("iu_op_mode", "4")
        server.setParameterValue("iu_set_tmp", "220")
        server.setParameterValue("iu_indoor_tmp", "7050")

        val output = runCli("status")

        assertThat(output).contains("OFF")
        assertThat(output).contains("HEAT")
        assertThat(output).contains("22.0")
        assertThat(output).contains("20.5")
        assertThat(output).contains("AGYG14KVCA")
    }

    @Test
    fun `status affiche la temperature exterieure comme NA quand non supportee`() {
        server.setParameterValue("iu_outdoor_tmp", "65535")

        val output = runCli("status")

        // indoor temp is still displayed, but outdoor shows N/A
        assertThat(output).contains("N/A")
    }

    @Test
    fun `status affiche la temperature exterieure quand disponible`() {
        server.setParameterValue("iu_outdoor_tmp", "6800") // (6800 - 5000) / 100 = 18.0°C

        val output = runCli("status")

        assertThat(output).contains("18.0")
    }

    @Test
    fun `on allume la PAC`() {
        server.setParameterValue("iu_onoff", "0")

        val output = runCli("on")

        assertThat(server.getLastSetValue("iu_onoff")).isEqualTo("1")
        assertThat(output).contains("allumée")
    }

    @Test
    fun `off eteint la PAC`() {
        server.setParameterValue("iu_onoff", "1")

        val output = runCli("off")

        assertThat(server.getLastSetValue("iu_onoff")).isEqualTo("0")
        assertThat(output).contains("éteinte")
    }

    @Test
    fun `set configure la temperature`() {
        val output = runCli("set", "--temp", "23.5")

        assertThat(server.getLastSetValue("iu_set_tmp")).isEqualTo("235")
        assertThat(output).contains("23.5")
    }

    @Test
    fun `set configure le mode`() {
        val output = runCli("set", "--mode", "COOL")

        assertThat(server.getLastSetValue("iu_op_mode")).isEqualTo("1")
        assertThat(output).contains("COOL")
    }

    @Test
    fun `set configure la vitesse du ventilateur`() {
        val output = runCli("set", "--fan", "HIGH")

        assertThat(server.getLastSetValue("iu_fan_spd")).isEqualTo("11")
        assertThat(output).contains("HIGH")
    }

    @Test
    fun `set configure temperature mode et ventilateur en une seule commande`() {
        runCli("set", "--temp", "20.0", "--mode", "AUTO", "--fan", "QUIET")

        assertThat(server.getLastSetValue("iu_set_tmp")).isEqualTo("200")
        assertThat(server.getLastSetValue("iu_op_mode")).isEqualTo("0")
        assertThat(server.getLastSetValue("iu_fan_spd")).isEqualTo("2")
    }

    @Test
    fun `set sans options affiche un message d avertissement`() {
        val output = runCli("set")

        assertThat(output).contains("Aucun paramètre")
        assertThat(server.getSetRequests()).isEmpty()
    }

    @Test
    fun `set ne modifie pas le mode quand seule la temperature est specifiee`() {
        server.clearSetRequests()

        runCli("set", "--temp", "25.0")

        // Aucune requête SetParam ne doit concerner iu_op_mode
        val touchedParams = server.getSetRequests().flatMap { it.keys }
        assertThat(touchedParams).doesNotContain("iu_op_mode")
        assertThat(touchedParams).doesNotContain("iu_fan_spd")
    }

    @Test
    fun `status affiche AUTO pour le mode AUTO`() {
        server.setParameterValue("iu_op_mode", "0")

        val output = runCli("status")

        assertThat(output).contains("AUTO")
    }

    @Test
    fun `status fonctionne quand la PAC est allumee`() {
        server.setParameterValue("iu_onoff", "1")

        val output = runCli("status")

        assertThat(output).contains("ON")
    }

    // ---- Helper ----

    private fun runCli(vararg args: String): String {
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream, true, Charsets.UTF_8)
        val originalOut = System.out
        System.setOut(printStream)
        try {
            val useCase = HeatPumpService(client)
            val cli = buildClimCli(useCase)
            cli.main(args.toList())
        } finally {
            System.setOut(originalOut)
            printStream.flush()
        }
        return outputStream.toString(Charsets.UTF_8)
    }
}
