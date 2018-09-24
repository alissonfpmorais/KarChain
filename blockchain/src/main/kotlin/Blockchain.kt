import BlockchainError.HasGenesisError
import BlockchainError.InvalidBlockError
import BlockchainError.NoBlocksError
import arrow.Kind
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import arrow.data.NonEmptyList
import arrow.typeclasses.MonadError
import arrow.typeclasses.bindingCatch
import java.lang.Integer.min

data class Blockchain<F, T, U>(
        val blocks: NonEmptyList<Block<T, U>>,
        val M: MonadError<F, Throwable>
) : MonadError<F, Throwable> by M {
    fun concatGenesis(block: Block<T, U>): Kind<F, Blockchain<F, T, U>> = bindingCatch {
        if (blocks.isEmpty()) copy(blocks = blocks.plus(block))
        else raiseError<Blockchain<F, T, U>>(HasGenesisError).bind()
    }

    fun validateBlock(method: String = "SHA-256", previousBlock: Block<T, U>, block: Block<T, U>): Kind<F, Block<T, U>> = bindingCatch {
        val eitherCalc: Either<BlockchainError, String> = when (blocks.all.indexOf(previousBlock) < 0) {
            true -> BlockNotPresentError.left()
            false -> {
                val name = block.name
                val data = block.data
                calcBlockHash(method = method, previousHash = previousBlock.hash, name = name, data = data)
            }
        }

        val eitherRes: Either<BlockchainError, Block<T, U>> = eitherCalc
                .flatMap { hash: String ->
                    if (block.previousHash != previousBlock.hash && hash != block.hash) InvalidBlockError.left()
                    else block.right()
                }

        fromEither(eitherRes).bind()
    }

    fun concatBlock(method: String = "SHA-256", block: Block<T, U>): Kind<F, Blockchain<F, T, U>> {
        val kindValidate: Kind<F, Block<T, U>> = when (blocks.isEmpty()) {
            true -> raiseError(NoBlocksError)
            false -> validateBlock(method = method, previousBlock = blocks.all.last(), block = block)
        }

        return kindValidate.map { b: Block<T, U> -> copy(blocks = blocks.plus(b)) }
    }

    fun getLastBlocks(count: Int): Kind<F, List<Block<T, U>>> = bindingCatch {
        if (blocks.isEmpty()) raiseError<List<Block<T, U>>>(NoBlocksError).bind()
        else blocks.all.subList(blocks.size - min(blocks.size, count), blocks.size)
    }

    fun getLastBlock(): Kind<F, Block<T, U>> = getLastBlocks(1)
            .map { lastBlocks: List<Block<T, U>> -> lastBlocks.last() }

    fun calcNextBlockHash(method: String = "SHA-256", name: T, data: U): Kind<F, String> = getLastBlock()
            .flatMap { lastBlock -> fromEither(calcBlockHash(method, lastBlock.hash, name, data)) }

    fun genBlock(method: String = "SHA-256", name: T, data: U): Kind<F, Block<T, U>> = bindingCatch {
        val lastBlock: Block<T, U> = getLastBlock().bind()
        val index: Long = lastBlock.index + 1
        val previousHash: String = lastBlock.hash
        val hash: String = calcNextBlockHash(method = method, name = name, data = data).bind()

        Block(index = index, hash = hash, previousHash = previousHash, name = name, data = data)
    }
}