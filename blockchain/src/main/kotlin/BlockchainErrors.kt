sealed class BlockchainError : Throwable() {
    object HasGenesisError : BlockchainError()
    object NoBlocksError : BlockchainError()
    object InvalidBlockError : BlockchainError()
    object BlockNotPresentError : BlockchainError()
    object CalculateHashError : BlockchainError()
}

typealias HasGenesisError = BlockchainError.HasGenesisError
typealias NoBlocksError = BlockchainError.NoBlocksError
typealias InvalidBlockError = BlockchainError.InvalidBlockError
typealias BlockNotPresentError = BlockchainError.BlockNotPresentError
typealias CalculateHashError = BlockchainError.CalculateHashError