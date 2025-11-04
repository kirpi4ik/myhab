package org.myhab.domain.auth

/**
 * OAuth2 Refresh Token Domain Class
 * 
 * Represents an OAuth2 refresh token used to obtain new access tokens without user re-authentication.
 * Refresh tokens are long-lived credentials that allow clients to request new access tokens
 * when the current access token expires.
 * 
 * <p>Refresh tokens provide a better user experience by avoiding repeated login prompts,
 * while maintaining security through short-lived access tokens.</p>
 * 
 * <h3>Refresh Token Flow</h3>
 * <ol>
 *   <li>Client receives access token and refresh token during initial authorization</li>
 *   <li>Client uses access token to access protected resources</li>
 *   <li>Access token expires after validity period (e.g., 1 hour)</li>
 *   <li>Client presents refresh token to authorization server</li>
 *   <li>Authorization server validates refresh token and issues new access token</li>
 *   <li>Optionally, a new refresh token may also be issued (token rotation)</li>
 * </ol>
 * 
 * <h3>Security Considerations</h3>
 * <ul>
 *   <li><b>Long-lived:</b> Typically valid for 30-90 days or longer</li>
 *   <li><b>Revocable:</b> Can be revoked if compromised or user logs out</li>
 *   <li><b>Single-use (optional):</b> Some implementations invalidate refresh token after use</li>
 *   <li><b>Rotation (recommended):</b> Issue new refresh token with each access token refresh</li>
 *   <li><b>Secure storage:</b> Must be stored securely on client side (encrypted storage)</li>
 *   <li><b>HTTPS only:</b> Must be transmitted over secure connections</li>
 * </ul>
 * 
 * <h3>Grant Types Supporting Refresh Tokens</h3>
 * <ul>
 *   <li><b>authorization_code:</b> Yes (recommended)</li>
 *   <li><b>password:</b> Yes (for trusted clients)</li>
 *   <li><b>client_credentials:</b> No (client can request new token directly)</li>
 *   <li><b>implicit:</b> No (deprecated, not secure for refresh tokens)</li>
 * </ul>
 * 
 * <h3>Example Usage</h3>
 * <pre>
 * // Refresh token is typically created by the OAuth provider
 * def refreshToken = new RefreshToken(
 *     value: 'rt_f3d2a1b0-9876-4321-abcd-ef1234567890',
 *     expiration: new Date() + (30 * 24 * 3600), // 30 days from now
 *     authentication: serializedAuthentication
 * )
 * refreshToken.save()
 * 
 * // Client uses refresh token to get new access token
 * POST /oauth/token
 * grant_type=refresh_token
 * &refresh_token=rt_f3d2a1b0-9876-4321-abcd-ef1234567890
 * &client_id=mobile-app
 * &client_secret=secret123
 * </pre>
 * 
 * @see Client
 * @see AccessToken
 * @see AuthorizationCode
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-1.5">OAuth 2.0 Refresh Tokens</a>
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-6">OAuth 2.0 Refreshing an Access Token</a>
 * 
 * @author MyHab Development Team
 * @since 1.0
 */
class RefreshToken {

    /**
     * The actual refresh token value.
     * This is the credential that clients present when requesting a new access token.
     * 
     * <p>The token value is typically:</p>
     * <ul>
     *   <li><b>Random string:</b> UUID or cryptographically secure random value</li>
     *   <li><b>Prefixed:</b> Often prefixed with "rt_" to distinguish from access tokens</li>
     *   <li><b>Opaque:</b> Does not contain any user information (unlike JWT)</li>
     * </ul>
     * 
     * <p><b>Example:</b> "rt_f3d2a1b0-9876-4321-abcd-ef1234567890"</p>
     * 
     * <p><b>Security Note:</b> Refresh tokens should be treated as highly sensitive
     * credentials. If compromised, an attacker can obtain new access tokens indefinitely
     * until the refresh token expires or is revoked.</p>
     * 
     * @required
     * @unique Each refresh token must be globally unique
     */
    String value
    
    /**
     * Expiration date and time of the refresh token.
     * After this time, the refresh token is no longer valid and the user must re-authenticate.
     * 
     * <p><b>Best Practices:</b></p>
     * <ul>
     *   <li><b>Mobile apps:</b> 30-90 days (balance between UX and security)</li>
     *   <li><b>Web apps:</b> 7-30 days (users can re-login more easily)</li>
     *   <li><b>Trusted devices:</b> Up to 1 year (with token rotation)</li>
     *   <li><b>Public clients:</b> Shorter periods (higher security risk)</li>
     * </ul>
     * 
     * <p><b>Token Rotation:</b> If implementing token rotation, refresh tokens can have
     * shorter validity periods (e.g., 7 days) since a new refresh token is issued with
     * each access token refresh.</p>
     * 
     * <p>If null, the refresh token never expires (not recommended for security reasons).</p>
     * 
     * @nullable Can be null for non-expiring tokens (not recommended)
     * @future Should be in the future when token is created
     */
    Date expiration
    
    /**
     * Serialized authentication object containing user and client details.
     * Stores the complete OAuth2Authentication object in binary format.
     * 
     * <p>This includes:</p>
     * <ul>
     *   <li>User principal and authorities</li>
     *   <li>Client details and authorities</li>
     *   <li>Granted scopes</li>
     *   <li>Request parameters</li>
     * </ul>
     * 
     * <p>When a client uses the refresh token to obtain a new access token,
     * this authentication object is deserialized and used to create the new
     * access token with the same user and scope information.</p>
     * 
     * <p><b>Size Limit:</b> Maximum 4KB (4096 bytes)</p>
     * 
     * @required
     * @serialized Binary serialization of OAuth2Authentication
     * @maxSize 4096 bytes
     */
    byte[] authentication

    /**
     * Validation constraints for the RefreshToken domain class.
     * Defines which fields are required, nullable, unique, and size limits.
     */
    static constraints = {
        value nullable: false, blank: false, unique: true
        expiration nullable: true
        authentication nullable: false, minSize: 1, maxSize: 1024 * 4
    }

    /**
     * GORM mapping configuration for the RefreshToken domain class.
     * 
     * <ul>
     *   <li><b>version:</b> Disabled - Refresh tokens are immutable once created</li>
     * </ul>
     * 
     * <p>Versioning is disabled because refresh tokens should not be updated after creation.
     * If changes are needed (e.g., token rotation), a new refresh token should be issued
     * and the old one should be deleted or marked as revoked.</p>
     */
    static mapping = {
        version false
    }
}
