package org.myhab.utils

import groovy.json.JsonSlurper

/**
 * Utility class for extracting meaningful error messages from HTTP responses.
 * Handles HTML error pages (like Cloudflare), JSON error responses, and other formats.
 * 
 * This utility prevents logging entire HTML error pages when external services are down,
 * instead extracting concise, meaningful error information.
 * 
 * Usage:
 *   String errorMsg = HttpErrorUtil.extractErrorMessage(response.status, response.body)
 *   log.error("API call failed: ${errorMsg}")
 */
class HttpErrorUtil {
    
    /**
     * Extract meaningful error message from HTTP response body
     * Handles HTML error pages (like Cloudflare) and JSON error responses
     * 
     * @param statusCode HTTP status code
     * @param responseBody Response body as string
     * @return Concise error message suitable for logging
     */
    static String extractErrorMessage(int statusCode, String responseBody) {
        if (!responseBody) {
            return "Empty response body"
        }
        
        // Check if response is HTML (error page)
        if (responseBody.trim().startsWith('<!DOCTYPE') || responseBody.trim().startsWith('<html')) {
            // Try to extract error code from HTML
            def errorCodeMatch = responseBody =~ /Error code (\d+)/
            if (errorCodeMatch) {
                return "HTTP ${statusCode} - Server error page (Error code: ${errorCodeMatch[0][1]})"
            }
            // Try to extract title
            def titleMatch = responseBody =~ /<title>([^<]+)<\/title>/
            if (titleMatch) {
                return "HTTP ${statusCode} - ${titleMatch[0][1]}"
            }
            return "HTTP ${statusCode} - Server returned HTML error page (likely service unavailable)"
        }
        
        // Try to parse as JSON
        try {
            def json = new JsonSlurper().parseText(responseBody)
            if (json.error) {
                return "HTTP ${statusCode} - ${json.error}${json.error_description ? ': ' + json.error_description : ''}"
            }
            if (json.message) {
                return "HTTP ${statusCode} - ${json.message}"
            }
        } catch (Exception e) {
            // Not JSON, return first 200 chars of response
            def truncated = responseBody.length() > 200 ? responseBody.substring(0, 200) + '...' : responseBody
            return "HTTP ${statusCode} - ${truncated}"
        }
        
        // Fallback: return first 200 chars
        def truncated = responseBody.length() > 200 ? responseBody.substring(0, 200) + '...' : responseBody
        return "HTTP ${statusCode} - ${truncated}"
    }
}

