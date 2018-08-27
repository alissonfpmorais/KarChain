import arrow.core.Try
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

fun calcHash(method: String = "SHA-256", payload: String): Try<String> =
        Try { MessageDigest.getInstance(method).digest(payload.toByteArray(StandardCharsets.UTF_8)) }
                .map { digest: ByteArray ->
                    digest.fold("") { message: String, byte: Byte -> message + "%02x".format(byte) }
                }

fun <T, U> calcBlockHash(method: String = "SHA-256", previousBlock: Block<T, U>, name: T, data: U): EitherBC<String> =
        calcHash(method = method, payload = "${previousBlock.hash},${name.toString()},${data.toString()}")
                .toEither()
                .mapLeft { CalculateHashError }