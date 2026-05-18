package org.myhab.services.navimow

/**
 * Thrown by {@link NavimowApiClient} when the Segway cloud rejects a request
 * (HTTP &ge; 400, envelope {@code code != 1}, or {@code per-command status==ERROR}).
 *
 * <p>{@code errorCode} carries Segway's machine-readable code when present
 * (e.g. {@code "alreadyInState"}, {@code "TOKEN_EXPIRED"}), so callers like
 * the GraphQL mutation can branch on it.</p>
 */
class NavimowApiException extends RuntimeException {
    final String errorCode

    NavimowApiException(String message) {
        super(message)
        this.errorCode = null
    }

    NavimowApiException(String message, String errorCode) {
        super(message)
        this.errorCode = errorCode
    }

    NavimowApiException(String message, Throwable cause) {
        super(message, cause)
        this.errorCode = null
    }
}
