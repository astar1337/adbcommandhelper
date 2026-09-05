package utils.helpers

suspend fun connectGateway(
    name: String,
    deviceId: String? = null,
    onLineOutput: ((String) -> Unit)? = null
): MaestroRunner.TestResult {
    CancellableProcess.resetCancelFlag()
    return MaestroRunner.runFlowCancellable(
        flowFileName = "puredomevpnmastertest.yaml",
        flowsDir = MaestroRunner.VPN_FLOWS_DIR,
        deviceId = deviceId,
        extraEnv = mapOf("GATEWAY_NAME" to name),
        onLineOutput = onLineOutput
    )
}