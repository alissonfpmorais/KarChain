data class Block<T, U>(
        val index: Long,
        val hash: String,
        val previousHash: String,
        val timestamp: Long = System.currentTimeMillis(),
        val name: T,
        val data: U)