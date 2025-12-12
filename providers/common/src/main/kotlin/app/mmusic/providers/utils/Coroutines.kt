package app.mmusic.providers.utils

import kotlinx.coroutines.CancellationException

inline fun <T> runCatchingCancellable(block: () -> T): Result<T> {
    return runCatching(block).onFailure { throwable ->
        if (throwable is CancellationException) throw throwable
    }
}
