import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import arrow.core.getOrDefault
import arrow.effects.IO
import arrow.effects.async
import arrow.effects.fix
import io.kotlintest.assertions.arrow.`try`.shouldBeFailure
import io.kotlintest.assertions.arrow.`try`.shouldBeSuccess
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.specs.StringSpec

class OperationsTest : StringSpec({
    val genesisName = "author: Alisson Morais"
    val genesisData = "LinkedIn: linkedin.com/in/alissonmorais, GitHub: github.com/alissonfpmorais"
    val genesisBlock = Block(
            index = 1,
            hash = calcHash(payload = "none,$genesisName,$genesisData").getOrDefault { "hash error" },
            previousHash = "none",
            name = genesisName,
            data = genesisData)

    "calculate hash" {
        calcHash(payload = "none,$genesisName,$genesisData")
                .shouldBeSuccess("2974706ee0ebed4c689b064b717f4c89a3020a07a45f2152e73e9c498d77843f")
    }

    "calculate hash with invalid method" {
        calcHash(method = "ABCDEFGHIJKLMNOPQRSTUVWXYZ", payload = "none,$genesisName,$genesisData")
                .toEither()
                .shouldBeLeft(CalculateHashError)
    }

    "calculate hash based on previous block, name and data" {
        calcBlockHash(previousBlock = genesisBlock, name = "AAA-9999", data = "100km")
                .shouldBeSuccess("0d12db8cc138ca8ab3708495eef06e7790d9efef38f13cb6c1b46637dc6cb80a")
    }
})