import arrow.core.getOrDefault
import io.kotlintest.assertions.arrow.`try`.shouldBeFailure
import io.kotlintest.assertions.arrow.`try`.shouldBeSuccess
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.specs.StringSpec

class OperationsTest : StringSpec({
    val genesisName = "author: Alisson Morais"
    val genesisData = "GitHub: github.com/alissonfpmorais"
    val genesisHash = "b3a151bdb2cbb253e811e23019446bfdca91c29ebe18f3b70ebb4fed2c453c31"

    val nextBlockName = "AAA-9999"
    val nextBlockData = "100km"
    val nextBlockHash = "0b301d685da1fdf13ec3d970e371d4b064c62205cb1efad9f7537607963c210c"

    val genesisBlock = Block(
            index = 1,
            hash = calcHash(payload = "none,$genesisName,$genesisData").getOrDefault { "hash error" },
            previousHash = "none",
            name = genesisName,
            data = genesisData)

    "calculate hash" {
        calcHash(payload = "none,$genesisName,$genesisData")
                .shouldBeSuccess(genesisHash)
    }

    "calculate hash with invalid method" {
        calcHash(method = "ABCDEFGHIJKLMNOPQRSTUVWXYZ", payload = "none,$genesisName,$genesisData")
                .shouldBeFailure()
    }

    "calculate hash based on previous block, name and data" {
        calcBlockHash(previousBlock = genesisBlock, name = nextBlockName, data = nextBlockData)
                .shouldBeRight(nextBlockHash)
    }
})