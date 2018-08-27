import BlockchainError.BlockNotPresentError
import BlockchainError.HasGenesisError
import BlockchainError.InvalidBlockError
import BlockchainError.NoBlocksError
import arrow.Kind
import arrow.core.*
import arrow.effects.typeclasses.Async
import arrow.typeclasses.binding
import java.lang.Integer.min

typealias EitherBC<T> = Either<BlockchainError, T>

data class Blockchain<T, U>(val blocks: List<Block<T, U>> = listOf()) {
    fun concatGenesis(block: Block<T, U>): EitherBC<Blockchain<T, U>> = when {
        blocks.isEmpty() -> copy(blocks = blocks.plus(block)).right()
        else -> HasGenesisError.left()
    }

    fun validateBlock(method: String = "SHA-256", previousBlock: Block<T, U>, block: Block<T, U>): EitherBC<Block<T, U>> {
        val eitherCalc: EitherBC<String> = when (blocks.indexOf(previousBlock) < 0) {
            true -> BlockNotPresentError.left()
            false -> {
                val name = block.name
                val data = block.data
                calcBlockHash(method = method, previousBlock = previousBlock, name = name, data = data)
            }
        }

        return eitherCalc
                .flatMap { hash: String ->
                    if (block.previousHash != previousBlock.hash && hash != block.hash) InvalidBlockError.left()
                    else block.right()
                }
    }

    fun concatBlock(method: String = "SHA-256", block: Block<T, U>): EitherBC<Blockchain<T, U>> {
        val eitherValidate: EitherBC<Block<T, U>> = when (blocks.isEmpty()) {
            true -> NoBlocksError.left()
            false -> validateBlock(method = method, previousBlock = blocks.last(), block = block)
        }

        return eitherValidate.map { b: Block<T, U> -> copy(blocks = blocks.plus(b)) }
    }

    fun getLastBlocks(count: Int): EitherBC<List<Block<T, U>>> = when (blocks.isEmpty()) {
        true -> NoBlocksError.left()
        false -> blocks.subList(blocks.size - min(blocks.size, count), blocks.size).right()
    }

    fun getLastBlock(): EitherBC<Block<T, U>> = getLastBlocks(1)
            .map { subBlocks: List<Block<T, U>> -> subBlocks.last() }

    fun calcNextBlockHash(method: String = "SHA-256", name: T, data: U): EitherBC<String> = getLastBlock()
            .flatMap { lastBlock -> calcBlockHash(method = method, previousBlock = lastBlock, name = name, data = data) }

    fun <F> genBlock(method: String = "SHA-256", name: T, data: U, a: Async<F>): Kind<F, Block<T, U>> = a
            .async { callback: (Either<Throwable, Block<T, U>>) -> Unit -> Either
                    .monadError<BlockchainError>().binding {
                        val lastBlock: Block<T, U> = getLastBlock().bind()
                        val index: Long = lastBlock.index + 1
                        val previousHash: String = lastBlock.hash
                        val hash: String = calcNextBlockHash(method = method, name = name, data = data).bind()

                        Block(index = index, hash = hash, previousHash = previousHash, name = name, data = data)
                    }
                    .fix()
                    .fold(
                            { error: BlockchainError -> callback(BlockchainThrowable(error).left()) },
                            { block -> callback(block.right()) }
                    )
            }
}