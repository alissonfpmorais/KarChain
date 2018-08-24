import arrow.Kind
import arrow.core.*
import arrow.effects.typeclasses.Async
import arrow.typeclasses.bindingCatch
import java.lang.Integer.min

sealed class BlockchainError : Throwable()
object UnknownBlockchainError : BlockchainError()
object HasGenesisError : BlockchainError()
object NoBlocksError : BlockchainError()
object InvalidBlockError : BlockchainError()
object CalculateHashError : BlockchainError()

data class Blockchain<T, U>(val blocks: List<Block<T, U>> = listOf())

fun <T, U> Blockchain<T, U>.concatGenesis(block: Block<T, U>): Try<Blockchain<T, U>> = Try {
    if (blocks.isEmpty()) this.copy(blocks.plus(block))
    else throw HasGenesisError
}

fun <T, U> Blockchain<T, U>.concatBlock(method: String = "SHA-256", block: Block<T, U>): Try<Blockchain<T, U>> = Try {
    if (blocks.isEmpty()) throw NoBlocksError
    else when (validateBlock(method = method, previousBlock = blocks.last(), block = block)) {
        is Failure -> throw InvalidBlockError
        is Success -> this.copy(blocks.plus(block))
    }
}

fun <T, U> Blockchain<T, U>.validateBlock(method: String = "SHA-256", previousBlock: Block<T, U>, block: Block<T, U>): Try<Boolean> = Try
        .monadError().bindingCatch {
            if (blocks.indexOf(previousBlock) < 0 || block.previousHash != previousBlock.hash) throw InvalidBlockError
            else {
                val name = block.name
                val data = block.data
                val attemptToHash: Try<String> = calcBlockHash(method = method, previousBlock = previousBlock, name = name, data = data)

                when (attemptToHash) {
                    is Failure -> throw attemptToHash.exception
                    is Success -> if (attemptToHash.value != block.hash) throw InvalidBlockError else true
                }
            }
        }
        .fix()

fun <T, U> Blockchain<T, U>.getLastBlocks(count: Int): Try<List<Block<T, U>>> = Try {
    if (blocks.isEmpty()) throw NoBlocksError
    else blocks.subList(blocks.size - min(blocks.size, count), blocks.size)
}

fun <T, U> Blockchain<T, U>.getLastBlock(): Try<Block<T, U>> = getLastBlocks(1)
        .map { blocks -> blocks.last() }

fun <T, U> Blockchain<T, U>.calcNextBlockHash(method: String = "SHA-256", name: T, data: U): Try<String> = getLastBlock()
        .flatMap { lastBlock -> calcBlockHash(method = method, previousBlock = lastBlock, name = name, data = data) }

fun <F, T, U> Blockchain<T, U>.genBlock(method: String = "SHA-256", name: T, data: U, a: Async<F>): Kind<F, Block<T, U>> = a
        .async { callback: (Either<Throwable, Block<T, U>>) -> Unit -> Try
                .monadError().bindingCatch {
                    val lastBlock: Block<T, U> = getLastBlock().bind()
                    val index: Long = lastBlock.index + 1
                    val previousHash: String = lastBlock.hash
                    val hash: String = calcNextBlockHash(method = method, name = name, data = data).bind()

                    Block(index = index, hash = hash, previousHash = previousHash, name = name, data = data)
                }
                .fix()
                .fold(
                        { error: Throwable ->
                            when (error) {
                                is BlockchainError -> callback(error.left())
                                else -> callback(UnknownBlockchainError.left())
                            }
                        },
                        { block -> callback(block.right()) }
                )
        }