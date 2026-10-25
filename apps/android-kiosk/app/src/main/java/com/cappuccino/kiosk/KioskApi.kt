package com.cappuccino.kiosk

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class EmployeeStatus(
    val employeeCode: String,
    val fullName: String,
    val state: String,
    val allowedActions: List<String>,
)

data class ClockResult(
    val status: EmployeeStatus,
    val eventType: String,
    val eventTime: String,
)

sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()

    /** The employee code or PIN is wrong, or the employee has no PIN yet. */
    data object InvalidCredentials : ApiResult<Nothing>()

    /** The server no longer accepts this kiosk's token (revoked or database reset). */
    data object KioskUnpaired : ApiResult<Nothing>()

    /** Too many wrong attempts; the server is temporarily refusing this code or device. */
    data object Locked : ApiResult<Nothing>()

    /** The requested action does not fit the employee's current state (e.g. clocking in twice). */
    data object InvalidAction : ApiResult<Nothing>()

    data object Offline : ApiResult<Nothing>()
}

object KioskApi {
    private const val TOKEN_HEADER = "X-Kiosk-Token"

    suspend fun pair(serverUrl: String, pairingCode: String, deviceName: String): ApiResult<String> {
        val body = JSONObject().put("pairing_code", pairingCode).put("device_name", deviceName)
        return post(serverUrl, "/api/v1/kiosk/pair", body, token = null) { it.getString("kiosk_token") }
    }

    suspend fun identify(serverUrl: String, token: String, code: String, pin: String): ApiResult<EmployeeStatus> {
        val body = JSONObject().put("employee_code", code).put("pin", pin)
        return post(serverUrl, "/api/v1/kiosk/identify", body, token) { parseStatus(it) }
    }

    suspend fun clock(
        serverUrl: String,
        token: String,
        code: String,
        pin: String,
        eventType: String,
        idempotencyKey: String,
    ): ApiResult<ClockResult> {
        val body = JSONObject()
            .put("employee_code", code)
            .put("pin", pin)
            .put("event_type", eventType)
            .put("idempotency_key", idempotencyKey)
        return post(serverUrl, "/api/v1/kiosk/clock", body, token) {
            ClockResult(parseStatus(it), it.getString("event_type"), it.getString("event_time"))
        }
    }

    private fun parseStatus(json: JSONObject): EmployeeStatus {
        val actions = json.getJSONArray("allowed_actions")
        return EmployeeStatus(
            employeeCode = json.getString("employee_code"),
            fullName = json.getString("full_name"),
            state = json.getString("state"),
            allowedActions = List(actions.length()) { actions.getString(it) },
        )
    }

    private suspend fun <T> post(
        serverUrl: String,
        path: String,
        body: JSONObject,
        token: String?,
        parse: (JSONObject) -> T,
    ): ApiResult<T> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL("${serverUrl.trim().trimEnd('/')}$path").openConnection() as HttpURLConnection).apply {
                connectTimeout = 5_000
                readTimeout = 8_000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                if (token != null) setRequestProperty(TOKEN_HEADER, token)
            }
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            when {
                code in 200..299 -> ApiResult.Success(parse(JSONObject(text)))
                code == 401 && token != null && text.contains("Kiosk not paired") -> ApiResult.KioskUnpaired
                code == 401 || code == 422 -> ApiResult.InvalidCredentials
                code == 409 -> ApiResult.InvalidAction
                code == 429 -> ApiResult.Locked
                else -> ApiResult.Offline
            }
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            ApiResult.Offline
        } finally {
            connection?.disconnect()
        }
    }
}
