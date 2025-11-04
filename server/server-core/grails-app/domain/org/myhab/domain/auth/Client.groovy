package org.myhab.domain.auth

/**
 * OAuth2 Client Domain Class
 * 
 * Represents an OAuth2 client application that can request access tokens on behalf of users.
 * This class is part of the Spring Security OAuth2 Provider implementation and stores
 * client credentials, authorized grant types, scopes, and other OAuth2 client metadata.
 * 
 * <p>The client secret is automatically encrypted before being stored in the database
 * using the Spring Security password encoder.</p>
 * 
 * <h3>OAuth2 Grant Types</h3>
 * <ul>
 *   <li><b>authorization_code</b> - Authorization Code Grant (most secure for web apps)</li>
 *   <li><b>implicit</b> - Implicit Grant (for browser-based apps, less secure)</li>
 *   <li><b>password</b> - Resource Owner Password Credentials Grant</li>
 *   <li><b>client_credentials</b> - Client Credentials Grant (for service-to-service)</li>
 *   <li><b>refresh_token</b> - Refresh Token Grant (to obtain new access tokens)</li>
 * </ul>
 * 
 * <h3>Example Usage</h3>
 * <pre>
 * def client = new Client(
 *     clientId: 'mobile-app',
 *     clientSecret: 'secret123',
 *     authorizedGrantTypes: ['authorization_code', 'refresh_token'],
 *     scopes: ['read', 'write'],
 *     redirectUris: ['https://myapp.com/callback'],
 *     accessTokenValiditySeconds: 3600,
 *     refreshTokenValiditySeconds: 2592000
 * )
 * client.save()
 * </pre>
 * 
 * @see AccessToken
 * @see RefreshToken
 * @see AuthorizationCode
 * @see <a href="https://tools.ietf.org/html/rfc6749">OAuth 2.0 RFC 6749</a>
 * 
 * @author MyHab Development Team
 * @since 1.0
 */
class Client {

    /**
     * Constant representing an empty client secret.
     * Used when no client secret is provided (e.g., for public clients).
     */
    private static final String NO_CLIENT_SECRET = ''

    /**
     * Spring Security service for password encoding.
     * Injected automatically by Grails and used to encrypt client secrets.
     */
    transient springSecurityService

    /**
     * Unique identifier for the OAuth2 client.
     * This is used by client applications to identify themselves when requesting tokens.
     * 
     * <p><b>Example:</b> "mobile-app", "web-client", "iot-device"</p>
     * 
     * @required
     * @unique
     */
    String clientId
    
    /**
     * Secret key for the OAuth2 client.
     * This is used to authenticate the client application when requesting tokens.
     * The secret is automatically encrypted before being stored in the database.
     * 
     * <p><b>Security Note:</b> Keep this secret secure and never expose it in client-side code.
     * For public clients (e.g., mobile apps, SPAs), this can be null or empty.</p>
     * 
     * @nullable For public clients
     * @encrypted Automatically encrypted using Spring Security password encoder
     */
    String clientSecret

    /**
     * Validity period for access tokens issued to this client, in seconds.
     * After this period, the access token expires and a new one must be obtained.
     * 
     * <p><b>Common Values:</b></p>
     * <ul>
     *   <li>3600 (1 hour) - Standard for web applications</li>
     *   <li>7200 (2 hours) - For longer sessions</li>
     *   <li>43200 (12 hours) - For trusted applications</li>
     * </ul>
     * 
     * <p>If null, the default validity period from the OAuth provider configuration is used.</p>
     * 
     * @nullable Uses default if not specified
     * @unit Seconds
     */
    Integer accessTokenValiditySeconds
    
    /**
     * Validity period for refresh tokens issued to this client, in seconds.
     * Refresh tokens are used to obtain new access tokens without re-authentication.
     * 
     * <p><b>Common Values:</b></p>
     * <ul>
     *   <li>2592000 (30 days) - Standard for mobile apps</li>
     *   <li>7776000 (90 days) - For long-lived sessions</li>
     *   <li>31536000 (1 year) - For trusted devices</li>
     * </ul>
     * 
     * <p>If null, the default validity period from the OAuth provider configuration is used.</p>
     * 
     * @nullable Uses default if not specified
     * @unit Seconds
     */
    Integer refreshTokenValiditySeconds

    /**
     * Additional custom information about the client.
     * This can store any extra metadata needed by the application.
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * additionalInformation = [
     *     'appName': 'MyHab Mobile',
     *     'version': '2.0',
     *     'platform': 'iOS'
     * ]
     * </pre>
     * 
     * @nullable
     */
    Map<String, Object> additionalInformation

    /**
     * Collection of authorities (roles) granted to this client.
     * These authorities define what the client application itself can do.
     * 
     * <p><b>Example:</b> ['ROLE_CLIENT', 'ROLE_TRUSTED_CLIENT']</p>
     * 
     * @nullable
     */
    static hasMany = [
            authorities: String,
            
            /**
             * Collection of OAuth2 grant types authorized for this client.
             * Defines which OAuth2 flows the client can use to obtain tokens.
             * 
             * <p><b>Valid Values:</b></p>
             * <ul>
             *   <li>authorization_code - For web applications with server-side code</li>
             *   <li>implicit - For browser-based applications (deprecated, use PKCE instead)</li>
             *   <li>password - For trusted first-party applications</li>
             *   <li>client_credentials - For service-to-service authentication</li>
             *   <li>refresh_token - To obtain new access tokens using refresh tokens</li>
             * </ul>
             * 
             * <p><b>Example:</b> ['authorization_code', 'refresh_token']</p>
             * 
             * @nullable
             */
            authorizedGrantTypes: String,
            
            /**
             * Collection of resource server identifiers that this client can access.
             * Used to restrict which resource servers the client can request tokens for.
             * 
             * <p><b>Example:</b> ['api-server', 'file-server', 'notification-service']</p>
             * 
             * @nullable
             */
            resourceIds: String,
            
            /**
             * Collection of OAuth2 scopes that this client can request.
             * Scopes define the level of access the client can request on behalf of users.
             * 
             * <p><b>Common Scopes:</b></p>
             * <ul>
             *   <li>read - Read-only access to resources</li>
             *   <li>write - Write access to resources</li>
             *   <li>delete - Delete access to resources</li>
             *   <li>admin - Administrative access</li>
             *   <li>profile - Access to user profile information</li>
             *   <li>email - Access to user email address</li>
             * </ul>
             * 
             * <p><b>Example:</b> ['read', 'write', 'profile']</p>
             * 
             * @nullable
             */
            scopes: String,
            
            /**
             * Collection of scopes that are automatically approved without user consent.
             * These scopes don't require explicit user authorization during the OAuth flow.
             * 
             * <p><b>Security Warning:</b> Only use auto-approve for highly trusted clients
             * and non-sensitive scopes. Requiring explicit user consent is more secure.</p>
             * 
             * <p><b>Example:</b> ['read'] (auto-approve read access, but require consent for write)</p>
             * 
             * @nullable
             */
            autoApproveScopes: String,
            
            /**
             * Collection of authorized redirect URIs for this client.
             * After successful authentication, the authorization server redirects the user
             * to one of these URIs with the authorization code or access token.
             * 
             * <p><b>Security Note:</b> The redirect URI in the authorization request must
             * exactly match one of these registered URIs to prevent authorization code
             * interception attacks.</p>
             * 
             * <p><b>Example:</b></p>
             * <ul>
             *   <li>https://myapp.com/oauth/callback</li>
             *   <li>https://myapp.com/auth/callback</li>
             *   <li>myapp://oauth/callback (for mobile apps)</li>
             * </ul>
             * 
             * @nullable
             */
            redirectUris: String
    ]

    /**
     * Properties that should not be persisted to the database.
     * The springSecurityService is injected at runtime and doesn't need to be stored.
     */
    static transients = ['springSecurityService']

    /**
     * Validation constraints for the Client domain class.
     * Defines which fields are required, nullable, unique, etc.
     */
    static constraints = {
        clientId blank: false, unique: true
        clientSecret nullable: true

        accessTokenValiditySeconds nullable: true
        refreshTokenValiditySeconds nullable: true

        authorities nullable: true
        authorizedGrantTypes nullable: true

        resourceIds nullable: true

        scopes nullable: true
        autoApproveScopes nullable: true

        redirectUris nullable: true
        additionalInformation nullable: true
    }

    /**
     * GORM event handler called before inserting a new client into the database.
     * Automatically encodes the client secret before saving.
     * 
     * @see #encodeClientSecret()
     */
    def beforeInsert() {
        encodeClientSecret()
    }

    /**
     * GORM event handler called before updating an existing client in the database.
     * Re-encodes the client secret only if it has been modified.
     * 
     * @see #encodeClientSecret()
     */
    def beforeUpdate() {
        if(isDirty('clientSecret')) {
            encodeClientSecret()
        }
    }

    /**
     * Encodes the client secret using the Spring Security password encoder.
     * If no client secret is provided, uses an empty string constant.
     * 
     * <p>This method is called automatically by the beforeInsert and beforeUpdate
     * event handlers to ensure the client secret is always encrypted before storage.</p>
     * 
     * <p><b>Security Note:</b> The encoded secret is one-way hashed and cannot be
     * decrypted. To verify a client secret, it must be re-encoded and compared.</p>
     * 
     * @see #NO_CLIENT_SECRET
     * @see #beforeInsert()
     * @see #beforeUpdate()
     */
    protected void encodeClientSecret() {
        clientSecret = clientSecret ?: NO_CLIENT_SECRET
        clientSecret = springSecurityService?.passwordEncoder ? springSecurityService.encodePassword(clientSecret) : clientSecret
    }
    
    /**
     * GORM mapping configuration for the Client domain class.
     * 
     * <ul>
     *   <li><b>version:</b> Enables optimistic locking to prevent concurrent update conflicts</li>
     *   <li><b>autowire:</b> Enables automatic dependency injection (e.g., springSecurityService)</li>
     * </ul>
     */
    static mapping = {
        version true
        autowire true
    }
}
