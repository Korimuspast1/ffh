package com.ffh.vpn.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VpnStatus { DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING, ERROR }

data class VpnState(
    val status: VpnStatus = VpnStatus.DISCONNECTED,
    val serverId: String? = null,
    val serverName: String? = null,
    val subscriptionId: String? = null,
    val startedAt: Long? = null,
    val message: String? = null
)

object VpnStateHolder {
    private val _state = MutableStateFlow(VpnState())
    val state: StateFlow<VpnState> = _state.asStateFlow()

    fun update(block: (VpnState) -> VpnState) {
        _state.value = block(_state.value)
    }
}
