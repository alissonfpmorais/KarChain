import arrow.core.Try
import arrow.core.getOrElse

data class BlockchainThrowable(val error: BlockchainError) : Throwable()

sealed class BlockchainError {
    object UnknownBlockchainError : BlockchainError()
    object HasGenesisError : BlockchainError()
    object NoBlocksError : BlockchainError()
    object InvalidBlockError : BlockchainError()
    object BlockNotPresentError : BlockchainError()
    object CalculateHashError : BlockchainError()
}

typealias UnknownBlockchainError = BlockchainError.UnknownBlockchainError
typealias HasGenesisError = BlockchainError.HasGenesisError
typealias NoBlocksError = BlockchainError.NoBlocksError
typealias InvalidBlockError = BlockchainError.InvalidBlockError
typealias BlockNotPresentError = BlockchainError.BlockNotPresentError
typealias CalculateHashError = BlockchainError.CalculateHashError

fun Throwable.toBlockchainError(): BlockchainError = Try { this as BlockchainThrowable }
        .map { e: BlockchainThrowable -> e.error }
        .getOrElse { UnknownBlockchainError }