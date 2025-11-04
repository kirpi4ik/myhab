/**
 * Composable for handling SVG interactions and parsing
 */
import { ref } from 'vue';

export function useSvgInteraction() {
  const nodes = ['path', 'rect', 'circle', 'polygon', 'polyline', 'text', 'g'];

  /**
   * Parse asset ID from SVG element ID
   * Format: 
   *   - "asset-<category>-id-<id>-<index>" (e.g., "asset-light-id-2524-1")
   *   - "nav-<direction>-<index>" (e.g., "nav-back-1")
   * Examples: 
   *   - "asset-light-id-2524-1" → { type: 'asset', category: 'LIGHT', id: '2524', assetOrder: '1' }
   *   - "nav-back-1" → { type: 'nav', info: 'back' }
   */
  const parseAssetId = (id) => {
    // Use a more specific regex for asset IDs
    // Format: asset-<category>-id-<id>-<index>
    const assetMatch = id.match(/^asset-([a-z]+)-id-(\d+)-(\d+)$/i);
    if (assetMatch) {
      return {
        type: 'asset',
        info: assetMatch[1].toLowerCase(),
        category: assetMatch[1].toUpperCase(),
        id: parseInt(assetMatch[2], 10),
        assetOrder: assetMatch[3],
      };
    }
    
    // Format: nav-<direction>-<index>
    const navMatch = id.match(/^nav-([a-z]+)-(\d+)$/i);
    if (navMatch) {
      return {
        type: 'nav',
        info: navMatch[1].toLowerCase(),
        category: null,
        id: null,
        assetOrder: navMatch[2],
      };
    }
    
    // Fallback to old regex for any other format
    const matches = [...id.matchAll(/([A-Za-z_]+)-(([0-9]+)(-([0-9])))*/g)];
    return {
      type: matches[0]?.[1] || null,
      info: matches[1]?.[1] || null,
      category: matches[1]?.[1]?.toUpperCase() || null,
      id: matches[2]?.[3] ? parseInt(matches[2]?.[3], 10) : null,
      assetOrder: matches[2]?.[5] || null,
    };
  };

  /**
   * Find closest SVG node element
   */
  const findClosestNode = (target) => {
    let closest = null;
    let i = 0;
    
    while (closest === null && i < nodes.length) {
      closest = target.closest(nodes[i]);
      i++;
    }
    
    return closest;
  };

  /**
   * Apply focus effect to SVG element
   */
  const applyFocusEffect = (element) => {
    if (!element) return;
    
    const originalClass = element.getAttribute('class') || 'no-focus';
    element.setAttribute('class', 'focus');
    
    setTimeout(() => {
      element.setAttribute('class', originalClass);
    }, 100);
  };

  /**
   * Get CSS class for asset based on category and state
   */
  const getAssetClass = (category, state, deviceStatus) => {
    switch (category) {
      case 'LIGHT':
        return state ? 'bulb-on' : 'bulb-off';
      case 'HEAT':
        return state ? 'heat-on' : 'heat-off';
      case 'MOTION':
        return state ? 'motion-off' : 'motion-on';
      case 'TEMP':
        return deviceStatus === 'OFFLINE' ? 'device-offline' : '';
      case 'DOOR_LOCK':
        return 'lock';
      default:
        return '';
    }
  };

  /**
   * Transform SVG document
   */
  const transformSvg = (svg, peripherals, svgPages, currentPageId) => {
    for (const node of nodes) {
      const elements = svg.getElementsByTagName(node);
      for (let i = 0; i < elements.length; i++) {
        initializeSvgElement(svg, elements[i], peripherals, svgPages, currentPageId);
      }
    }
    return svg;
  };

  /**
   * Initialize individual SVG element
   */
  const initializeSvgElement = (svg, element, peripherals, svgPages, currentPageId) => {
    const wrapper = document.createElementNS('http://www.w3.org/2000/svg', 'a');
    const parsed = parseAssetId(element.id);
    let cssClass = '';

    if (parsed.type === 'asset') {
      // peripherals is already the plain object (not a ref) when passed here
      const peripheral = peripherals && peripherals[parsed.id];
      
      // Debug logging removed - use browser devtools if needed
      
      cssClass = getAssetClass(parsed.category, peripheral?.state, peripheral?.deviceStatus);

      // Update text content for specific categories
      if (parsed.category === 'TEMP' && peripheral?.portValue) {
        let degree = peripheral.portValue;
        if (degree.length === 3 && !degree.includes('.')) {
          degree = degree / 10;
        }
        if (element.firstChild) {
          element.firstChild.textContent = `${degree}℃`;
        }
      } else if (parsed.category === 'LUMINOSITY' && peripheral?.portValue) {
        const textElement = element.getElementsByTagName('text').item(0);
        if (textElement) {
          textElement.textContent = `${peripheral.portValue / 10}%`;
          textElement.setAttribute('class', 'luminosity-text');
        }
      }
    } else if (parsed.type === 'nav') {
      if (parsed.info === 'back' || parsed.info === 'forward') {
        cssClass = 'nav-button';
        
        // Hide back button on first page
        if (parsed.info === 'back' && currentPageId === svgPages[0].id) {
          cssClass = 'hidden';
        }
        // Hide forward button on last page
        else if (parsed.info === 'forward' && currentPageId === svgPages[svgPages.length - 1].id) {
          cssClass = 'hidden';
        }
      } else {
        cssClass = 'bulb-off';
      }
    }

    element.setAttribute('class', cssClass);
    element.parentNode.insertBefore(wrapper, element);
    wrapper.appendChild(element);
  };

  return {
    nodes,
    parseAssetId,
    findClosestNode,
    applyFocusEffect,
    getAssetClass,
    transformSvg
  };
}

