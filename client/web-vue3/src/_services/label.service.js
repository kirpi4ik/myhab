import { Utils } from '@/_helpers';
import { authzService } from '@/_services';

/**
 * Service for generating and downloading labels.
 * Works with Brother PT-P710BT and other label printers.
 */
export const labelService = {
    /**
     * Download a cable label image
     * 
     * @param {number} cableId - The cable ID
     * @param {object} options - Optional configuration
     * @param {string} options.template - Template name (small, medium, large, brother_12mm, brother_18mm, brother_24mm)
     * @param {string[]} options.fields - Array of fields to include
     * @param {number} options.width - Custom width in pixels
     * @param {number} options.height - Custom height in pixels
     */
    async downloadCableLabel(cableId, options = {}) {
        const params = new URLSearchParams();
        
        if (options.template) params.append('template', options.template);
        if (options.fields) params.append('fields', options.fields.join(','));
        if (options.width) params.append('width', options.width);
        if (options.height) params.append('height', options.height);
        params.append('download', 'true');
        
        const url = `${Utils.host()}/api/labels/cable/${cableId}?${params.toString()}`;
        
        try {
            const response = await fetch(url, {
                method: 'GET',
                headers: {
                    'Authorization': `Bearer ${authzService.currentUserValue?.access_token || ''}`
                }
            });
            
            if (!response.ok) {
                const errorData = await response.json();
                throw new Error(errorData.error || 'Failed to generate label');
            }
            
            // Get the blob and trigger download
            const blob = await response.blob();
            const downloadUrl = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = downloadUrl;
            link.download = `cable-${cableId}-label.png`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(downloadUrl);
            
            return true;
        } catch (error) {
            console.error('Error downloading label:', error);
            throw error;
        }
    },

    /**
     * Get label image URL for preview (inline display)
     * 
     * @param {number} cableId - The cable ID
     * @param {object} options - Optional configuration
     * @returns {string} URL for the label image
     */
    getCableLabelUrl(cableId, options = {}) {
        const params = new URLSearchParams();
        
        if (options.template) params.append('template', options.template);
        if (options.fields) params.append('fields', options.fields.join(','));
        if (options.width) params.append('width', options.width);
        if (options.height) params.append('height', options.height);
        
        return `${Utils.host()}/api/labels/cable/${cableId}?${params.toString()}`;
    },

    /**
     * Get available label templates
     * 
     * @returns {Promise<Array>} Array of template configurations
     */
    async getTemplates() {
        const url = `${Utils.host()}/api/labels/templates`;
        
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${authzService.currentUserValue?.access_token || ''}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to fetch templates');
        }
        
        return response.json();
    },

    /**
     * Get available fields for an entity type
     * 
     * @param {string} entityType - The entity type (e.g., 'cable')
     * @returns {Promise<object>} Object with entityType and fields array
     */
    async getFields(entityType = 'cable') {
        const url = `${Utils.host()}/api/labels/fields?type=${entityType}`;
        
        const response = await fetch(url, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${authzService.currentUserValue?.access_token || ''}`
            }
        });
        
        if (!response.ok) {
            throw new Error('Failed to fetch fields');
        }
        
        return response.json();
    }
};
