import arrow.core.Try
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

fun calcHash(method: String = "SHA-256", payload: String): Try<String> =
        Try {
            try { MessageDigest.getInstance(method).digest(payload.toByteArray(StandardCharsets.UTF_8)) }
            catch (e: Exception) { throw CalculateHashError }
        }
        .map { digest: ByteArray -> digest.fold("") { message: String, byte: Byte -> message + "%02x".format(byte) } }

fun <T, U> calcBlockHash(method: String = "SHA-256", previousBlock: Block<T, U>, name: T, data: U): Try<String> =
        calcHash(method = method, payload = "${previousBlock.hash},${name.toString()},${data.toString()}")