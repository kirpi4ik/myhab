package org.myhab.domain.auth

/**
 * OAuth2 Authorization Code Domain Class
 * 
 * Represents an OAuth2 authorization code used in the Authorization Code Grant flow.
 * Authorization codes are short-lived, single-use credentials that are exchanged for
 * access tokens in the most secure OAuth2 flow.
 * 
 * <p>The Authorization Code Grant is the recommended flow for web applications with
 * server-side code, as it provides the highest level of security by never exposing
 * access tokens to the user's browser.</p>
 * 
 * <h3>Authorization Code Flow</h3>
 * <ol>
 *   <li>Client redirects user to authorization server with authorization request</li>
 *   <li>User authenticates and grants authorization to the client</li>
 *   <li>Authorization server creates authorization code and redirects user back to client</li>
 *   <li>Client receives authorization code in redirect URI</li>
 *   <li>Client exchanges authorization code for access token (server-to-server)</li>
 *   <li>Authorization server validates code and issues access token</li>
 *   <li>Authorization code is immediately invalidated (single-use)</li>
 * </ol>
 * 
 * <h3>Security Considerations</h3>
 * <ul>
 *   <li><b>Short-lived:</b> Typically valid for 1-10 minutes only</li>
 *   <li><b>Single-use:</b> Must be invalidated immediately after being exchanged for a token</li>
 *   <li><b>Bound to client:</b> Can only be used by the client it was issued to</li>
 *   <li><b>Redirect URI validation:</b> Must match the registered redirect URI exactly</li>
 *   <li><b>PKCE recommended:</b> Use PKCE extension for public clients (mobile/SPA)</li>
 *   <li><b>HTTPS only:</b> Must be transmitted over secure connections</li>
 * </ul>
 * 
 * <h3>Authorization Request Example</h3>
 * <pre>
 * GET /oauth/authorize?
 *   response_type=code
 *   &client_id=mobile-app
 *   &redirect_uri=https://myapp.com/callback
 *   &scope=read write
 *   &state=xyz123
 * </pre>
 * 
 * <h3>Authorization Response Example</h3>
 * <pre>
 * HTTP/1.1 302 Found
 * Location: https://myapp.com/callback?
 *   code=SplxlOBeZQQYbYS6WxSbIA
 *   &state=xyz123
 * </pre>
 * 
 * <h3>Token Exchange Example</h3>
 * <pre>
 * POST /oauth/token
 * Content-Type: application/x-www-form-urlencoded
 * 
 * grant_type=authorization_code
 * &code=SplxlOBeZQQYbYS6WxSbIA
 * &redirect_uri=https://myapp.com/callback
 * &client_id=mobile-app
 * &client_secret=secret123
 * </pre>
 * 
 * <h3>PKCE Extension (Recommended for Public Clients)</h3>
 * <p>Proof Key for Code Exchange (PKCE) adds an extra layer of security for public clients
 * (mobile apps, SPAs) that cannot securely store client secrets:</p>
 * 
 * <ol>
 *   <li>Client generates random code_verifier</li>
 *   <li>Client creates code_challenge = SHA256(code_verifier)</li>
 *   <li>Client includes code_challenge in authorization request</li>
 *   <li>Client includes code_verifier in token exchange request</li>
 *   <li>Server verifies SHA256(code_verifier) == code_challenge</li>
 * </ol>
 * 
 * @see Client
 * @see AccessToken
 * @see RefreshToken
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-1.3.1">OAuth 2.0 Authorization Code</a>
 * @see <a href="https://tools.ietf.org/html/rfc6749#section-4.1">OAuth 2.0 Authorization Code Grant</a>
 * @see <a href="https://tools.ietf.org/html/rfc7636">PKCE (RFC 7636)</a>
 * 
 * @author MyHab Development Team
 * @since 1.0
 */
class AuthorizationCode {

    /**
     * Serialized authentication object containing user and client details.
     * Stores the complete OAuth2Authentication object in binary format.
     * 
     * <p>This includes:</p>
     * <ul>
     *   <li>User principal and authorities</li>
     *   <li>Client details and authorities</li>
     *   <li>Requested scopes</li>
     *   <li>Redirect URI</li>
     *   <li>Request parameters</li>
     *   <li>PKCE code_challenge (if using PKCE)</li>
     * </ul>
     * 
     * <p>When the client exchanges the authorization code for an access token,
     * this authentication object is deserialized and validated. The server verifies:</p>
     * <ul>
     *   <li>Client ID matches</li>
     *   <li>Redirect URI matches</li>
     *   <li>Code hasn't been used before</li>
     *   <li>Code hasn't expired</li>
     *   <li>PKCE code_verifier matches code_challenge (if using PKCE)</li>
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
     * The actual authorization code value.
     * This is the short-lived, single-use code that the client exchanges for an access token.
     * 
     * <p>The code value is typically:</p>
     * <ul>
     *   <li><b>Random string:</b> Cryptographically secure random value</li>
     *   <li><b>Short:</b> Usually 20-40 characters (balance between security and usability)</li>
     *   <li><b>URL-safe:</b> Can be safely transmitted in URL query parameters</li>
     *   <li><b>Opaque:</b> Does not contain any user information</li>
     * </ul>
     * 
     * <p><b>Example:</b> "SplxlOBeZQQYbYS6WxSbIA"</p>
     * 
     * <p><b>Security Note:</b> Authorization codes should:</p>
     * <ul>
     *   <li>Expire quickly (1-10 minutes)</li>
     *   <li>Be single-use only (invalidated after exchange)</li>
     *   <li>Be bound to the client that requested them</li>
     *   <li>Be bound to the redirect URI used in the authorization request</li>
     * </ul>
     * 
     * <p><b>Attack Prevention:</b></p>
     * <ul>
     *   <li><b>Authorization Code Interception:</b> Use PKCE for public clients</li>
     *   <li><b>Replay Attacks:</b> Single-use codes prevent reuse</li>
     *   <li><b>Code Injection:</b> Validate redirect URI and client ID</li>
     * </ul>
     * 
     * @required
     * @unique Each authorization code must be globally unique
     * @singleUse Must be invalidated after being exchanged for a token
     * @shortLived Typically expires in 1-10 minutes
     */
    String code

    /**
     * Validation constraints for the AuthorizationCode domain class.
     * Defines which fields are required, nullable, unique, and size limits.
     */
    static constraints = {
        code nullable: false, blank: false, unique: true
        authentication nullable: false, minSize: 1, maxSize: 1024 * 4
    }

    /**
     * GORM mapping configuration for the AuthorizationCode domain class.
     * 
     * <ul>
     *   <li><b>version:</b> Disabled - Authorization codes are immutable once created</li>
     * </ul>
     * 
     * <p>Versioning is disabled because authorization codes should not be updated after creation.
     * They are short-lived and single-use, so they are either valid or deleted.</p>
     * 
     * <p><b>Cleanup Strategy:</b> Authorization codes should be automatically cleaned up after:</p>
     * <ul>
     *   <li>Being successfully exchanged for an access token (immediate deletion)</li>
     *   <li>Expiring (scheduled cleanup job recommended)</li>
     *   <li>Failed exchange attempts (consider rate limiting)</li>
     * </ul>
     */
    static mapping = {
        version false
    }
}
