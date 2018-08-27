import arrow.core.*
import arrow.effects.IO
import arrow.effects.async
import arrow.effects.fix
import arrow.typeclasses.binding
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.specs.StringSpec

class BlockchainTest : StringSpec({
    val blockchain: Blockchain<String, String> = Blockchain(listOf())

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

    "concat genesis in empty blockchain are valid" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        newBlockchain.shouldBeRight(Blockchain(listOf(genesisBlock)))
    }

    "concat genesis in not empty blockchain are invalid" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        newBlockchain.map { bc -> bc.concatGenesis(genesisBlock).shouldBeLeft(HasGenesisError) }
    }

    "concat regular block in empty blockchain are invalid" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: Either<BlockchainError, Block<String, String>> = newBlockchain
                .flatMap { bc -> bc
                        .genBlock(name = nextBlockName, data = nextBlockData, a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                        .mapLeft { e: Throwable -> e.toBlockchainError() }
                }

        val unchangedBlockchain: EitherBC<Blockchain<String, String>> = generatedBlock
                .flatMap { block -> blockchain.concatBlock(block = block) }

        unchangedBlockchain.shouldBeLeft(NoBlocksError)
    }

    "concat regular block with different previous hash in not empty blockchain are invalid" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        newBlockchain
                .flatMap { bc -> bc.concatBlock(block = genesisBlock) }
                .shouldBeLeft(InvalidBlockError)
    }

    "concat regular block with same previous hash in not empty blockchain are valid" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: EitherBC<Block<String, String>> = newBlockchain
                .flatMap { bc -> bc
                        .genBlock(name = nextBlockName, data = nextBlockData, a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                        .mapLeft { e: Throwable -> e.toBlockchainError() }
                }

        val changedBlockchain: EitherBC<Blockchain<String, String>> = Either
                .monadError<BlockchainError>().binding {
                    val block: Block<String, String> = generatedBlock.bind()
                    val bc: Blockchain<String, String> = newBlockchain.bind()

                    bc.concatBlock(block = block).bind()
                }
                .fix()

        val correctBlockchain: EitherBC<Blockchain<String, String>> = generatedBlock
                .map { block -> Blockchain(listOf(genesisBlock, block)) }

        correctBlockchain.map { bc -> changedBlockchain.shouldBeRight(bc) }
    }

    "validate same blocks in empty blockchain as invalid" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: EitherBC<Block<String, String>> = newBlockchain
                .flatMap { bc -> bc.genBlock(name = nextBlockName, data = nextBlockData, a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                        .mapLeft { e: Throwable -> e.toBlockchainError() }
                }

        generatedBlock.map { block: Block<String, String> ->
            blockchain.validateBlock(previousBlock = block, block = block).shouldBeLeft(BlockNotPresentError)
        }
    }

    "validate actual block and previous block in empty blockchain as invalid" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: EitherBC<Block<String, String>> = newBlockchain
                .flatMap { bc -> bc.genBlock(name = nextBlockName, data = nextBlockData, a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                        .mapLeft { e: Throwable -> e.toBlockchainError() }
                }

        generatedBlock.map { block: Block<String, String> ->
            blockchain.validateBlock(previousBlock = genesisBlock, block = block).shouldBeLeft(BlockNotPresentError)
        }
    }

    "validate same blocks and no empty blockchain as invalid" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: EitherBC<Block<String, String>> = newBlockchain
                .flatMap { bc -> bc.genBlock(name = nextBlockName, data = nextBlockData, a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                        .mapLeft { e: Throwable -> e.toBlockchainError() }
                }

        newBlockchain.map { bc ->
            generatedBlock.map { block: Block<String, String> ->
                bc.validateBlock(previousBlock = block, block = block).shouldBeLeft(BlockNotPresentError)
            }
        }
    }

    "validate actual block and previous block in no empty blockchain as valid" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: EitherBC<Block<String, String>> = newBlockchain
                .flatMap { bc -> bc
                        .genBlock(name = nextBlockName, data = nextBlockData, a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                        .mapLeft { e: Throwable -> e.toBlockchainError() }
                }

        newBlockchain.map { bc ->
            generatedBlock.map { block: Block<String, String> ->
                bc.validateBlock(previousBlock = genesisBlock, block = block).shouldBeRight(block)
            }
        }
    }

    "get last block from empty blockchain" {
        blockchain
                .getLastBlock()
                .shouldBeLeft(NoBlocksError)
    }

    "get last block from not empty blockchain" {
        blockchain
                .concatGenesis(genesisBlock)
                .flatMap { bc -> bc.getLastBlock() }
                .shouldBeRight(genesisBlock)
    }

    "calculate next block hash from empty blockchain" {
        blockchain
                .calcNextBlockHash(name = "AAA-9999", data = "100km")
                .shouldBeLeft(NoBlocksError)
    }

    "calculate next block hash from not empty blockchain" {
        blockchain
                .concatGenesis(genesisBlock)
                .flatMap { bc -> bc.calcNextBlockHash(name = nextBlockName, data = nextBlockData) }
                .shouldBeRight(nextBlockHash)
    }

    "create block in empty blockchain" {
        val block: EitherBC<Block<String, String>> = blockchain
                .genBlock(name = nextBlockName, data = nextBlockData, a = IO.async())
                .fix()
                .attempt()
                .unsafeRunSync()
                .mapLeft { e: Throwable -> e.toBlockchainError() }

        block.shouldBeLeft(NoBlocksError)
    }

    "create block in not empty blockchain" {
        val newBlockchain: EitherBC<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: EitherBC<Block<String, String>> = newBlockchain
                .flatMap { bc -> bc
                        .genBlock(name = nextBlockName, data = nextBlockData, a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                        .mapLeft { e: Throwable -> e.toBlockchainError() }
                }

        val block = Block(
                index = 2,
                hash = nextBlockHash,
                previousHash = genesisHash,
                timestamp = generatedBlock.getOrElse { genesisBlock }.timestamp,
                name = "AAA-9999",
                data = "100km")


        generatedBlock.shouldBeRight(block)
    }
})