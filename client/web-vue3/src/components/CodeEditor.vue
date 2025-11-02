<template>
  <div class="code-editor-wrapper">
    <div class="code-editor-label" v-if="label">
      <q-icon v-if="icon" :name="icon" class="q-mr-sm"/>
      {{ label }}
      <span v-if="required" class="text-negative">*</span>
    </div>
    <div class="code-editor-hint" v-if="hint">{{ hint }}</div>
    <codemirror
      v-model="code"
      :placeholder="placeholder"
      :style="{ height: height, border: '1px solid #ccc', borderRadius: '4px' }"
      :autofocus="autofocus"
      :indent-with-tab="true"
      :tab-size="2"
      :extensions="extensions"
      @ready="handleReady"
      @change="handleChange"
    />
  </div>
</template>

<script>
import { defineComponent, ref, watch, shallowRef } from 'vue';
import { Codemirror } from 'vue-codemirror';
import { javascript } from '@codemirror/lang-javascript';
import { oneDark } from '@codemirror/theme-one-dark';
import { autocompletion } from '@codemirror/autocomplete';
import { EditorView } from '@codemirror/view';

export default defineComponent({
  name: 'CodeEditor',
  components: {
    Codemirror
  },
  props: {
    modelValue: {
      type: String,
      default: ''
    },
    label: {
      type: String,
      default: ''
    },
    hint: {
      type: String,
      default: ''
    },
    icon: {
      type: String,
      default: ''
    },
    required: {
      type: Boolean,
      default: false
    },
    placeholder: {
      type: String,
      default: 'Enter code here...'
    },
    height: {
      type: String,
      default: '400px'
    },
    autofocus: {
      type: Boolean,
      default: false
    },
    language: {
      type: String,
      default: 'javascript' // javascript, groovy (uses javascript), etc.
    },
    theme: {
      type: String,
      default: 'dark' // dark or light
    },
    customCompletions: {
      type: Array,
      default: () => []
    }
  },
  emits: ['update:modelValue', 'change'],
  setup(props, { emit }) {
    const code = ref(props.modelValue);
    const view = shallowRef();

    // Watch for external changes to modelValue
    watch(() => props.modelValue, (newVal) => {
      if (code.value !== newVal) {
        code.value = newVal;
      }
    });

    // Custom completions for Groovy/scripting
    const customCompletions = (context) => {
      const word = context.matchBefore(/\w*/);
      if (!word || (word.from === word.to && !context.explicit)) {
        return null;
      }

      const completions = [
        // Groovy keywords
        { label: 'def', type: 'keyword', info: 'Define a variable' },
        { label: 'class', type: 'keyword', info: 'Define a class' },
        { label: 'interface', type: 'keyword', info: 'Define an interface' },
        { label: 'trait', type: 'keyword', info: 'Define a trait' },
        { label: 'enum', type: 'keyword', info: 'Define an enum' },
        { label: 'package', type: 'keyword', info: 'Package declaration' },
        { label: 'import', type: 'keyword', info: 'Import statement' },
        { label: 'extends', type: 'keyword', info: 'Class inheritance' },
        { label: 'implements', type: 'keyword', info: 'Interface implementation' },
        { label: 'return', type: 'keyword', info: 'Return statement' },
        { label: 'if', type: 'keyword', info: 'Conditional statement' },
        { label: 'else', type: 'keyword', info: 'Else clause' },
        { label: 'for', type: 'keyword', info: 'For loop' },
        { label: 'while', type: 'keyword', info: 'While loop' },
        { label: 'switch', type: 'keyword', info: 'Switch statement' },
        { label: 'case', type: 'keyword', info: 'Case clause' },
        { label: 'break', type: 'keyword', info: 'Break statement' },
        { label: 'continue', type: 'keyword', info: 'Continue statement' },
        { label: 'try', type: 'keyword', info: 'Try block' },
        { label: 'catch', type: 'keyword', info: 'Catch block' },
        { label: 'finally', type: 'keyword', info: 'Finally block' },
        { label: 'throw', type: 'keyword', info: 'Throw exception' },
        { label: 'new', type: 'keyword', info: 'Create new instance' },
        { label: 'println', type: 'function', info: 'Print line to console' },
        { label: 'print', type: 'function', info: 'Print to console' },
        
        // Add custom completions from props
        ...props.customCompletions.map(item => ({
          label: item.label || item,
          type: item.type || 'variable',
          info: item.info || item.description || ''
        }))
      ];

      return {
        from: word.from,
        options: completions
      };
    };

    // Setup extensions
    const extensions = [
      javascript(), // Use JavaScript syntax for Groovy (similar syntax)
      autocompletion({
        override: [customCompletions]
      }),
      EditorView.lineWrapping,
    ];

    // Add theme
    if (props.theme === 'dark') {
      extensions.push(oneDark);
    }

    const handleReady = (payload) => {
      view.value = payload.view;
    };

    const handleChange = (value) => {
      emit('update:modelValue', value);
      emit('change', value);
    };

    return {
      code,
      view,
      extensions,
      handleReady,
      handleChange
    };
  }
});
</script>

<style scoped>
.code-editor-wrapper {
  width: 100%;
  margin-bottom: 16px;
}

.code-editor-label {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  color: rgba(0, 0, 0, 0.87);
  display: flex;
  align-items: center;
}

.code-editor-hint {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.54);
  margin-bottom: 8px;
}

.body--dark .code-editor-label {
  color: rgba(255, 255, 255, 0.87);
}

.body--dark .code-editor-hint {
  color: rgba(255, 255, 255, 0.54);
}
</style>

