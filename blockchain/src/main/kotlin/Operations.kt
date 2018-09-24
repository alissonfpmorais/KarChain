import arrow.Kind
import arrow.core.Either
import arrow.core.Try
import arrow.core.getOrDefault
import arrow.data.NonEmptyList
import arrow.typeclasses.MonadError
import arrow.typeclasses.bindingCatch
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

fun calcHash(method: String = "SHA-256", payload: String): Try<String> =
        Try { MessageDigest.getInstance(method).digest(payload.toByteArray(StandardCharsets.UTF_8)) }
                .map { digest: ByteArray ->
                    digest.fold("") { message: String, byte: Byte -> message + "%02x".format(byte) }
                }

fun <T, U> calcBlockHash(method: String = "SHA-256", previousHash: String, name: T, data: U): Either<BlockchainError, String> =
        calcHash(method = method, payload = "$previousHash,${name.toString()},${data.toString()}")
                .toEither()
                .mapLeft { CalculateHashError }

fun <F, T, U> createBlockchain(method: String = "SHA-256", name: T, data: U, M: MonadError<F, Throwable>): Kind<F, Blockchain<F, T, U>> = M.bindingCatch {
    val blockchain: Either<Throwable, Blockchain<F, T, U>> = calcHash(method, payload = "none,$name,$data")
            .map { hash: String ->
                val genesisBlock = Block(
                        index = 1,
                        hash = hash,
                        previousHash = "none",
                        name = name,
                        data = data)

                Blockchain(NonEmptyList(genesisBlock), M)
            }
            .toEither()

    fromEither(blockchain).bind()
}