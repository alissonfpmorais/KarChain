import arrow.core.*
import arrow.effects.IO
import arrow.effects.async
import arrow.effects.fix
import arrow.typeclasses.binding
import io.kotlintest.assertions.arrow.`try`.shouldBeFailure
import io.kotlintest.assertions.arrow.`try`.shouldBeSuccess
import io.kotlintest.assertions.arrow.either.shouldBeLeft
import io.kotlintest.assertions.arrow.either.shouldBeRight
import io.kotlintest.specs.StringSpec

class BlockchainTest : StringSpec({
    val blockchain: Blockchain<String, String> = Blockchain(listOf())
    val genesisName = "author: Alisson Morais"
    val genesisData = "LinkedIn: linkedin.com/in/alissonmorais, GitHub: github.com/alissonfpmorais"
    val genesisBlock = Block(
            index = 1,
            hash = calcHash(payload = "none,$genesisName,$genesisData").getOrDefault { "hash error" },
            previousHash = "none",
            name = genesisName,
            data = genesisData)

    "concat genesis in empty blockchain are valid" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        newBlockchain.shouldBeSuccess(Blockchain(listOf(genesisBlock)))
    }

    "concat genesis in not empty blockchain are invalid" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        newBlockchain.map { bc -> bc.concatGenesis(genesisBlock).shouldBeFailure() }
    }

    "concat regular block in empty blockchain are invalid" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: Either<Throwable, Block<String, String>> = newBlockchain
                .toEither()
                .flatMap { bc -> bc
                        .genBlock(name = "AAA-9999", data = "100km", a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                }

        val unchangedBlockchain: Either<Throwable, Blockchain<String, String>> = generatedBlock
                .flatMap { block -> blockchain
                        .concatBlock(block = block)
                        .toEither()
                }

        unchangedBlockchain.shouldBeLeft(NoBlocksError)
    }

    "concat regular block with different previous hash in not empty blockchain are invalid" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        newBlockchain
                .flatMap { bc -> bc.concatBlock(block = genesisBlock) }
                .toEither()
                .shouldBeLeft(InvalidBlockError)
    }

    "concat regular block with same previous hash in not empty blockchain are valid" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: Either<Throwable, Block<String, String>> = newBlockchain
                .toEither()
                .flatMap { bc -> bc
                        .genBlock(name = "AAA-9999", data = "100km", a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                }

        val changedBlockchain: Either<Throwable, Blockchain<String, String>> = Either
                .monadError<Throwable>().binding {
                    val block: Block<String, String> = generatedBlock.bind()
                    println("block, $block")

                    newBlockchain
                            .toEither()
                            .bind()
                            .concatBlock(block = block)
                            .toEither()
                            .bind()
                }
                .fix()

        val correctBlockchain: Either<Throwable, Blockchain<String, String>> = generatedBlock
                .map { block -> Blockchain(listOf(genesisBlock, block)) }

        correctBlockchain.map { bc -> changedBlockchain.shouldBeRight(bc) }
    }

    "validate same blocks in empty blockchain as invalid" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: Either<Throwable, Block<String, String>> = newBlockchain
                .toEither()
                .flatMap { bc -> bc.genBlock(name = "AAA-9999", data = "100km", a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                }

        generatedBlock.map { block: Block<String, String> ->
            blockchain.validateBlock(previousBlock = block, block = block).shouldBeFailure()
        }
    }

    "validate actual block and previous block in empty blockchain as invalid" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: Either<Throwable, Block<String, String>> = newBlockchain
                .toEither()
                .flatMap { bc -> bc.genBlock(name = "AAA-9999", data = "100km", a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                }

        generatedBlock.map { block: Block<String, String> ->
            blockchain.validateBlock(previousBlock = genesisBlock, block = block).shouldBeFailure()
        }
    }

    "validate same blocks and no empty blockchain as invalid" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: Either<Throwable, Block<String, String>> = newBlockchain
                .toEither()
                .flatMap { bc -> bc.genBlock(name = "AAA-9999", data = "100km", a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                }

        newBlockchain.map { bc ->
            generatedBlock.map { block: Block<String, String> ->
                bc.validateBlock(previousBlock = block, block = block).shouldBeFailure()
            }
        }
    }

    "validate actual block and previous block in no empty blockchain as valid" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: Either<Throwable, Block<String, String>> = newBlockchain
                .toEither()
                .flatMap { bc -> bc
                        .genBlock(name = "AAA-9999", data = "100km", a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                }

        newBlockchain.map { bc ->
            generatedBlock.map { block: Block<String, String> ->
                bc.validateBlock(previousBlock = genesisBlock, block = block).shouldBeSuccess(true)
            }
        }
    }

    "get last block from empty blockchain" {
        blockchain
                .getLastBlock()
                .toEither()
                .shouldBeLeft(NoBlocksError)
    }

    "get last block from not empty blockchain" {
        blockchain
                .concatGenesis(genesisBlock)
                .flatMap { bc -> bc.getLastBlock() }
                .shouldBeSuccess(genesisBlock)
    }

    "calculate next block hash from empty blockchain" {
        blockchain
                .calcNextBlockHash(name = "AAA-9999", data = "100km")
                .toEither()
                .shouldBeLeft(NoBlocksError)
    }

    "calculate next block hash from not empty blockchain" {
        blockchain
                .concatGenesis(genesisBlock)
                .flatMap { bc -> bc.calcNextBlockHash(name = "AAA-9999", data = "100km") }
                .shouldBeSuccess("0d12db8cc138ca8ab3708495eef06e7790d9efef38f13cb6c1b46637dc6cb80a")
    }

    "create block in empty blockchain" {
        val block = blockchain
                .genBlock(name = "AAA-9999", data = "100km", a = IO.async())
                .fix()
                .attempt()
                .unsafeRunSync()

        block.shouldBeLeft(NoBlocksError)
    }

    "create block in not empty blockchain" {
        val newBlockchain: Try<Blockchain<String, String>> = blockchain.concatGenesis(genesisBlock)
        val generatedBlock: Either<Throwable, Block<String, String>> = newBlockchain
                .toEither()
                .flatMap { bc -> bc
                        .genBlock(name = "AAA-9999", data = "100km", a = IO.async())
                        .fix()
                        .attempt()
                        .unsafeRunSync()
                }

        val block = Block(
                index = 2,
                hash = "0d12db8cc138ca8ab3708495eef06e7790d9efef38f13cb6c1b46637dc6cb80a",
                previousHash = genesisBlock.hash,
                timestamp = generatedBlock.getOrElse { genesisBlock }.timestamp,
                name = "AAA-9999",
                data = "100km")


        generatedBlock.shouldBeRight(block)
    }
})