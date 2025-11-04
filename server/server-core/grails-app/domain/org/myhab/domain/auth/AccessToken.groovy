package org.myhab.domain.auth

/**
 * OAuth2 Access Token Domain Class
 * 
 * Represents an OAuth2 access token issued to a client application.
 * Access tokens are short-lived credentials that grant access to protected resources
 * on behalf of a resource owner (user).
 * 
 * <p>Access tokens are bearer tokens, meaning anyone who possesses the token can use it
 * to access the protected resources. They should be transmitted securely (HTTPS) and
 * stored securely to prevent unauthorized access.</p>
 * 
 * <h3>Token Lifecycle</h3>
 * <ol>
 *   <li>Client requests authorization from user</li>
 *   <li>User grants authorization</li>
 *   <li>Client exchanges authorization code for access token</li>
 *   <li>Client uses access token to access protected resources</li>
 *   <li>Access token expires after validity period</li>
 *   <li>Client uses refresh token to obtain new access token (if available)</li>
 * </ol>
 * 
 * <h3>Security Considerations</h3>
 * <ul>
 *   <li>Access tokens should have short validity periods (e.g., 1 hour)</li>
 *   <li>Tokens must be transmitted over HTTPS only</li>
 *   <li>Tokens should be stored securely on the client side</li>
 *   <li>Expired tokens should be rejected by resource servers</li>
 *   <li>Tokens can be revoked if compromised</li>
 * </ul>
 * 
 * <h3>Example Usage</h3>
 * <pre>
 * // Access token is typically created by the OAuth provider
 * def accessToken = new AccessToken(
 *     value: 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...',
 *     tokenType: 'Bearer',
 *     clientId: 'mobile-app',
 *     username: 'john_doe',
 *     expiration: new Date() + 3600, // 1 hour from now
 *     scope: ['read', 'write'],
 *     authenticationKey: 'unique-auth-key-123',
 *     authentication: serializedAuthentication
 * )
 * accessToken.save()
 * </pre>
 * 
 * @see Client
 * @see RefreshToken
 * @see AuthorizationCode
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-1.4">OAuth 2.0 Access Tokens</a>
 * 
 * @author MyHab Development Team
 * @since 1.0
 */
class AccessToken {

    /**
     * Unique key derived from the authentication object.
     * Used to quickly look up access tokens by authentication details.
     * 
     * <p>This is typically a hash of the authentication object and is used
     * for efficient database queries when checking if a token already exists
     * for a specific authentication.</p>
     * 
     * @required
     * @unique
     */
    String authenticationKey
    
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
     * <p><b>Size Limit:</b> Maximum 4KB (4096 bytes)</p>
     * 
     * @required
     * @serialized Binary serialization of OAuth2Authentication
     * @maxSize 4096 bytes
     */
    byte[] authentication

    /**
     * Username of the resource owner (user) who authorized the token.
     * Identifies which user's resources the token can access.
     * 
     * <p>This can be null for client_credentials grant type, where the client
     * acts on its own behalf rather than on behalf of a user.</p>
     * 
     * <p><b>Example:</b> "john_doe", "admin", "user@example.com"</p>
     * 
     * @nullable For client_credentials grant type
     */
    String username
    
    /**
     * Identifier of the OAuth2 client that owns this token.
     * References the clientId from the Client domain class.
     * 
     * <p>This identifies which application the token was issued to and is
     * used to enforce client-specific restrictions and quotas.</p>
     * 
     * <p><b>Example:</b> "mobile-app", "web-client", "iot-device"</p>
     * 
     * @required
     * @see Client#clientId
     */
    String clientId

    /**
     * The actual access token value (bearer token).
     * This is the credential that clients present when accessing protected resources.
     * 
     * <p>The token value can be:</p>
     * <ul>
     *   <li><b>Random string:</b> UUID or cryptographically secure random value</li>
     *   <li><b>JWT:</b> JSON Web Token containing claims and signature</li>
     * </ul>
     * 
     * <p><b>Example (Random):</b> "f3d2a1b0-9876-4321-abcd-ef1234567890"</p>
     * <p><b>Example (JWT):</b> "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"</p>
     * 
     * @required
     * @unique Each token must be globally unique
     */
    String value
    
    /**
     * Type of the access token.
     * Indicates how the token should be used when accessing protected resources.
     * 
     * <p><b>Standard Value:</b> "Bearer" (most common)</p>
     * <p>Bearer tokens are included in the Authorization header: <code>Authorization: Bearer {token}</code></p>
     * 
     * <p>Other possible values (less common):</p>
     * <ul>
     *   <li>"MAC" - Message Authentication Code tokens</li>
     *   <li>"Proof-of-Possession" - PoP tokens</li>
     * </ul>
     * 
     * @required
     * @default "Bearer"
     */
    String tokenType

    /**
     * Expiration date and time of the access token.
     * After this time, the token is no longer valid and must be refreshed or re-issued.
     * 
     * <p><b>Best Practices:</b></p>
     * <ul>
     *   <li>Web apps: 1 hour (3600 seconds)</li>
     *   <li>Mobile apps: 1-2 hours</li>
     *   <li>Trusted apps: Up to 12 hours</li>
     *   <li>Never: Infinite validity (security risk)</li>
     * </ul>
     * 
     * <p>Resource servers should check this expiration and reject expired tokens.</p>
     * 
     * @required
     * @future Must be in the future when token is created
     */
    Date expiration
    
    /**
     * Additional custom information about the token.
     * Can store any extra metadata needed by the application.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * additionalInformation = [
     *     'issued_at': 1609459200,
     *     'device_id': 'mobile-device-123',
     *     'ip_address': '192.168.1.100',
     *     'user_agent': 'MyHab Mobile/2.0'
     * ]
     * </pre>
     * 
     * @nullable
     */
    Map<String, Object> additionalInformation

    /**
     * Associated refresh token value.
     * If present, the client can use this refresh token to obtain a new access token
     * when the current one expires, without requiring user re-authentication.
     * 
     * <p>This is a one-to-one relationship: each access token can have at most one
     * associated refresh token.</p>
     * 
     * @nullable Not all grant types issue refresh tokens
     * @see RefreshToken
     */
    static hasOne = [refreshToken: String]
    
    /**
     * Collection of OAuth2 scopes granted to this token.
     * Defines what level of access this token provides to protected resources.
     * 
     * <p>Scopes limit what the token can be used for, even if the user has broader permissions.
     * Resource servers should check these scopes before granting access to specific operations.</p>
     * 
     * <p><b>Example:</b> ['read', 'write', 'profile']</p>
     * 
     * <p><b>Usage in Resource Server:</b></p>
     * <pre>
     * if (token.scope.contains('write')) {
     *     // Allow write operation
     * } else {
     *     // Deny - token only has read scope
     * }
     * </pre>
     * 
     * @required At least one scope must be granted
     * @see Client#scopes
     */
    static hasMany = [scope: String]

    /**
     * Validation constraints for the AccessToken domain class.
     * Defines which fields are required, nullable, unique, and size limits.
     */
    static constraints = {
        username nullable: true
        clientId nullable: false, blank: false
        value nullable: false, blank: false, unique: true
        tokenType nullable: false, blank: false
        expiration nullable: false
        scope nullable: false
        refreshToken nullable: true
        authenticationKey nullable: false, blank: false, unique: true
        authentication nullable: false, minSize: 1, maxSize: 1024 * 4
        additionalInformation nullable: true
    }

    /**
     * GORM mapping configuration for the AccessToken domain class.
     * 
     * <ul>
     *   <li><b>version:</b> Disabled - Access tokens are immutable once created</li>
     *   <li><b>scope lazy:</b> Disabled - Always eagerly fetch scopes with the token</li>
     * </ul>
     * 
     * <p>Versioning is disabled because access tokens should not be updated after creation.
     * If changes are needed, a new token should be issued instead.</p>
     */
    static mapping = {
        version false
        scope lazy: false
    }
}
