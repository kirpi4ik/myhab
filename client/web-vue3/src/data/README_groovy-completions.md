# Groovy Completions Configuration

This directory contains the autocomplete hints configuration for the CodeEditor component used in Scenario editing.

## File Structure

### `groovy-completions.json`

Contains autocomplete hints organized into four categories:

- **keywords**: Groovy language keywords (def, class, if, else, etc.)
- **functions**: Built-in Groovy functions and collection methods (println, each, collect, etc.)
- **dsl**: MyHAB DSL-specific variables and objects (scenario, log, context, etc.)
- **scenarioMethods**: ScenarioService methods available in DSL context (switchOn, switchOff, pause, etc.)

## Usage in CodeEditor

The `CodeEditor.vue` component automatically loads these completions:

```vue
<CodeEditor
  v-model="scenarioCode"
  language="groovy"
  :custom-completions="additionalCompletions"
/>
```

## Adding New Completions

### 1. Edit `groovy-completions.json`

Add entries to the appropriate category:

```json
{
  "keywords": [
    { "label": "yourKeyword", "type": "keyword", "info": "Description" }
  ],
  "functions": [
    { "label": "yourFunction", "type": "function", "info": "Usage example" }
  ],
  "dsl": [
    { "label": "yourVariable", "type": "variable", "info": "What it does" }
  ]
}
```

### 2. Field Descriptions

- **label**: The text that will be suggested (what the user types)
- **type**: The completion type for styling (keyword, function, method, variable, class, etc.)
- **info**: Tooltip/description shown when hovering over the suggestion

### 3. Completion Types

- `keyword` - Language keywords (blue in most themes)
- `function` - Built-in functions
- `method` - Object methods
- `variable` - Variables and objects
- `class` - Class names
- `interface` - Interface names
- `property` - Object properties

## Runtime Custom Completions

You can also add completions at runtime by passing them via props:

```javascript
const customCompletions = [
  { label: 'myDevice', type: 'variable', info: 'Current device instance' },
  { label: 'performAction', type: 'function', info: 'Execute device action' }
];
```

```vue
<CodeEditor
  v-model="code"
  :custom-completions="customCompletions"
/>
```

## Examples

### Basic Keyword
```json
{ "label": "def", "type": "keyword", "info": "Define a variable" }
```

### Function with Usage Example
```json
{ 
  "label": "collect", 
  "type": "method", 
  "info": "Transform collection elements: list.collect { it * 2 }" 
}
```

### DSL Context Variable
```json
{ 
  "label": "scenario", 
  "type": "variable", 
  "info": "Current scenario context" 
}
```

### Scenario Service Method
```json
{ 
  "label": "switchOn", 
  "type": "method", 
  "info": "Switch device/port ON: switchOn([peripheral: 'name'])" 
}
```

## Notes

- Changes to the JSON file require a rebuild/restart to take effect
- The `_comment` and `_structure` fields are for documentation only
- Custom completions from props are merged with loaded completions
- No need to restart for runtime custom completions (via props)

## Related Files

- `src/components/CodeEditor.vue` - Main component
- `src/pages/infra/scenario/ScenarioEdit.vue` - Uses CodeEditor for scenarios
- `src/pages/infra/job/JobEdit.vue` - Uses CodeEditor for jobs

