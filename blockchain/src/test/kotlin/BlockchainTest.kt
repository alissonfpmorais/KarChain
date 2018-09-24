import arrow.core.*
import arrow.typeclasses.MonadError
import arrow.typeclasses.bindingCatch
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class BlockchainTest : StringSpec({
    val ME: MonadError<EitherPartialOf<Throwable>, Throwable> = Either.monadError()

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

    val blockchain: Either<Throwable, Blockchain<EitherPartialOf<Throwable>, String, String>> =
            createBlockchain(name = genesisName, data = genesisData, M = ME)
                    .fix()

    "concat genesis in empty blockchain are valid" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Either<Throwable, Blockchain<EitherPartialOf<Throwable>, String, String>> = blockchain
                    .bind()
                    .concatGenesis(block = genesisBlock)
                    .fix()

            newBlockchain.shouldBeRight(createBlockchain(name = genesisName, data = genesisData, M = ME).fix())
        }
    }

    "concat genesis in not empty blockchain are invalid" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(block = genesisBlock)
                    .fix()
                    .bind()

            newBlockchain.concatGenesis(block = genesisBlock)
                    .fix()
                    .shouldBeLeft(HasGenesisError)
        }
    }

    "concat regular block in empty blockchain are invalid" {
        Either.monadError<Throwable>().bindingCatch {
            blockchain
                    .bind()
                    .concatBlock(block = genesisBlock)
                    .fix()
                    .shouldBeLeft(NoBlocksError)
        }
    }

    "concat regular block with different previous hash in not empty blockchain are invalid" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(genesisBlock)
                    .fix()
                    .bind()

            newBlockchain.concatBlock(block = genesisBlock)
                    .fix()
                    .shouldBeLeft(InvalidBlockError)
        }
    }

    "concat regular block with same previous hash in not empty blockchain are valid" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(genesisBlock)
                    .fix()
                    .bind()

            val block: Block<String, String> = newBlockchain.genBlock(name = nextBlockName, data = nextBlockData).bind()

            newBlockchain.concatBlock(block = block)
                    .fix()
                    .shouldBeRight(newBlockchain.copy(blocks = newBlockchain.blocks.plus(block)))
        }
    }

    "validate same blocks in empty blockchain as invalid" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(genesisBlock)
                    .fix()
                    .bind()

            val block: Block<String, String> = newBlockchain.genBlock(name = nextBlockName, data = nextBlockData).bind()

            blockchain
                    .bind()
                    .validateBlock(previousBlock = block, block = block)
                    .fix()
                    .shouldBeLeft(BlockNotPresentError)
        }
    }

    "validate actual block and previous block in empty blockchain as invalid" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(genesisBlock)
                    .fix()
                    .bind()

            val block: Block<String, String> = newBlockchain.genBlock(name = nextBlockName, data = nextBlockData).bind()

            blockchain
                    .bind()
                    .validateBlock(previousBlock = genesisBlock, block = block)
                    .fix()
                    .shouldBeLeft(BlockNotPresentError)
        }
    }

    "validate same blocks and no empty blockchain as invalid" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(genesisBlock)
                    .fix()
                    .bind()

            val block: Block<String, String> = newBlockchain.genBlock(name = nextBlockName, data = nextBlockData).bind()

            newBlockchain.validateBlock(previousBlock = block, block = block)
                    .fix()
                    .shouldBeLeft(BlockNotPresentError)
        }
    }

    "validate actual block and previous block in no empty blockchain as valid" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(genesisBlock)
                    .fix()
                    .bind()

            val block: Block<String, String> = newBlockchain.genBlock(name = nextBlockName, data = nextBlockData).bind()

            newBlockchain.validateBlock(previousBlock = genesisBlock, block = block)
                    .fix()
                    .shouldBeRight(block)
        }
    }

    "get last block from empty blockchain" {
        Either.monadError<Throwable>().bindingCatch {
            blockchain
                    .bind()
                    .getLastBlock()
                    .fix()
                    .shouldBeLeft(NoBlocksError)
        }
    }

    "get last block from not empty blockchain" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(genesisBlock)
                    .fix()
                    .bind()

            newBlockchain.getLastBlock()
                    .fix()
                    .shouldBeRight(genesisBlock)
        }
    }

    "calculate next block hash from empty blockchain" {
        Either.monadError<Throwable>().bindingCatch {
            blockchain
                    .bind()
                    .calcNextBlockHash(name = nextBlockName, data = nextBlockData)
                    .fix()
                    .shouldBeLeft(NoBlocksError)
        }
    }

    "calculate next block hash from not empty blockchain" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(genesisBlock)
                    .fix()
                    .bind()

            newBlockchain.calcNextBlockHash(name = nextBlockName, data = nextBlockData)
                    .fix()
                    .shouldBeRight(nextBlockHash)
        }
    }

    "create block in empty blockchain" {
        Either.monadError<Throwable>().bindingCatch {
            blockchain
                    .bind()
                    .genBlock(name = nextBlockName, data = nextBlockData)
                    .fix()
                    .shouldBeLeft(NoBlocksError)
        }
    }

    "create block in not empty blockchain" {
        Either.monadError<Throwable>().bindingCatch {
            val newBlockchain: Blockchain<EitherPartialOf<Throwable>, String, String> = blockchain
                    .bind()
                    .concatGenesis(genesisBlock)
                    .fix()
                    .bind()

            val block = newBlockchain.genBlock(name = nextBlockName, data = nextBlockData).bind()

            val createdBlock: Block<String, String> = Block(
                    index = 2,
                    hash = nextBlockHash,
                    previousHash = genesisHash,
                    timestamp = block.timestamp,
                    name = "AAA-9999",
                    data = "100km")


            createdBlock.shouldBe(block)
        }
    }
})