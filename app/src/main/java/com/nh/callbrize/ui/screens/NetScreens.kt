package com.nh.callbrize.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.nh.callbrize.navigation.Screen
import com.nh.callbrize.ui.AppViewModel
import com.nh.callbrize.ui.GameMode
import com.nh.callbrize.ui.GoldLight
import com.nh.callbrize.ui.NetKind
import com.nh.callbrize.ui.components.BodyText
import com.nh.callbrize.ui.components.GlassPanel
import com.nh.callbrize.ui.components.GoldButton
import com.nh.callbrize.ui.components.OutlineGoldButton
import com.nh.callbrize.ui.components.ScreenFrame
import com.nh.callbrize.ui.components.SectionTitle

/** Returns a function that asks for the Bluetooth permission (Android 12+) and then runs [onGranted]. */
@Composable
private fun rememberBtPermission(onGranted: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) onGranted()
    }
    return {
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        } else onGranted()
    }
}

@Composable
fun NetMenuScreen(vm: AppViewModel) {
    val saved by vm.settings.collectAsState()
    var name by remember { mutableStateOf(saved.name) }
    var kind by remember { mutableStateOf(NetKind.WIFI) }

    val hostBt = rememberBtPermission {
        vm.setNetProfile(name)
        vm.startHosting(NetKind.BLUETOOTH)
        vm.nav.go(Screen.HostLobby)
    }
    val joinBt = rememberBtPermission {
        vm.setNetProfile(name)
        vm.prepareJoin(NetKind.BLUETOOTH)
        vm.nav.go(Screen.JoinGame)
    }

    ScreenFrame("Wi-Fi / Bluetooth", { vm.nav.back() }) {
        GlassPanel {
            Text("Your name", color = GoldLight, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name, onValueChange = { name = it.take(16) }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (kind == NetKind.WIFI) GoldButton("Wi-Fi", { kind = NetKind.WIFI }, Modifier.weight(1f))
            else OutlineGoldButton("Wi-Fi", { kind = NetKind.WIFI }, Modifier.weight(1f))
            if (kind == NetKind.BLUETOOTH) GoldButton("Bluetooth", { kind = NetKind.BLUETOOTH }, Modifier.weight(1f))
            else OutlineGoldButton("Bluetooth", { kind = NetKind.BLUETOOTH }, Modifier.weight(1f))
        }
        BodyText(
            if (kind == NetKind.WIFI) "All phones must be on the same Wi-Fi, or friends can join the host's phone hotspot."
            else "Pair the phones first in Android Settings > Bluetooth. No internet needed."
        )
        GoldButton("Host a game", {
            if (kind == NetKind.WIFI) {
                vm.setNetProfile(name)
                vm.startHosting(NetKind.WIFI)
                vm.nav.go(Screen.HostLobby)
            } else hostBt()
        }, subtitle = "Up to 3 friends join; computers fill empty seats")
        OutlineGoldButton("Join a game", {
            if (kind == NetKind.WIFI) {
                vm.setNetProfile(name)
                vm.prepareJoin(NetKind.WIFI)
                vm.nav.go(Screen.JoinGame)
            } else joinBt()
        }, subtitle = "Connect to a friend who is hosting")
    }
}

@Composable
fun HostLobbyScreen(vm: AppViewModel) {
    val wifi = vm.netKind == NetKind.WIFI
    ScreenFrame("Host Game", { vm.leaveLobby() }) {
        GlassPanel {
            SectionTitle(if (wifi) "Wi-Fi game" else "Bluetooth game")
            if (vm.lobbyInfo.isNotBlank()) BodyText(vm.lobbyInfo, bold = true)
            BodyText(
                if (wifi) "Friends choose Wi-Fi > Join a game and type this address."
                else "Friends choose Bluetooth > Join a game and tap this phone's name (phones must be paired)."
            )
        }
        GlassPanel {
            SectionTitle("Players (${1 + vm.lobbyPlayers.size}/4)")
            BodyText("1. You")
            vm.lobbyPlayers.forEachIndexed { i, n -> BodyText("${i + 2}. $n") }
            BodyText("Empty seats are played by the computer.")
        }
        if (vm.netStatus.isNotBlank()) BodyText(vm.netStatus)
        GoldButton(
            "Start Game", { vm.startHostedMatch() },
            enabled = vm.mode == GameMode.HOST && vm.lobbyPlayers.isNotEmpty()
        )
    }
}

@Composable
fun JoinScreen(vm: AppViewModel) {
    var ip by remember { mutableStateOf("") }
    ScreenFrame("Join Game", { vm.leaveLobby() }) {
        if (vm.netKind == NetKind.WIFI) {
            GlassPanel {
                SectionTitle("Host address")
                OutlinedTextField(
                    value = ip, onValueChange = { ip = it.trim().take(40) }, singleLine = true,
                    label = { Text("e.g. 192.168.1.5") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                )
            }
            GoldButton("Connect", { vm.joinWifi(ip) }, enabled = ip.isNotBlank())
        } else {
            SectionTitle("Paired devices")
            vm.btDevices.forEach { (n, a) -> OutlineGoldButton(n, { vm.joinBluetooth(a) }, subtitle = a) }
            OutlineGoldButton("Refresh", { vm.refreshBt() })
        }
        if (vm.netStatus.isNotBlank()) BodyText(vm.netStatus)
    }
}
