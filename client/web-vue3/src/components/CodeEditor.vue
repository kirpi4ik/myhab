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
import { defineComponent, ref, watch, shallowRef, onMounted } from 'vue';
import { Codemirror } from 'vue-codemirror';
import { javascript } from '@codemirror/lang-javascript';
import { oneDark } from '@codemirror/theme-one-dark';
import { autocompletion } from '@codemirror/autocomplete';
import { EditorView } from '@codemirror/view';
import groovyCompletionsData from '../data/groovy-completions.json';
import { useApolloClient } from '@vue/apollo-composable';
import { PORT_LIST_HINTS } from '@/graphql/queries';

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
    const { client } = useApolloClient();
    const portHints = ref([]);

    // Watch for external changes to modelValue
    watch(() => props.modelValue, (newVal) => {
      if (code.value !== newVal) {
        code.value = newVal;
      }
    });

    // Load completions from external JSON file
    const loadedCompletions = [
      ...(groovyCompletionsData.keywords || []),
      ...(groovyCompletionsData.functions || []),
      ...(groovyCompletionsData.dsl || []),
      ...(groovyCompletionsData.scenarioMethods || [])
    ];

    // Fetch device ports for dynamic hints
    const fetchPortHints = async () => {
      try {
        const { data } = await client.query({
          query: PORT_LIST_HINTS,
          fetchPolicy: 'network-only'
        });

        if (data?.devicePortList) {
          portHints.value = data.devicePortList.map(port => ({
            label: `${port.id}`,
            type: 'constant',
            info: `Port: ${port.name || port.internalRef} (${port.device?.code || 'Unknown'}) - ${port.description || 'No description'}`
          }));
        }
      } catch (error) {
        console.error('Failed to fetch port hints:', error);
      }
    };

    // Fetch hints on mount
    onMounted(() => {
      fetchPortHints();
    });

    // Custom completions for Groovy/scripting
    const customCompletions = (context) => {
      const word = context.matchBefore(/\w*/);
      if (!word || (word.from === word.to && !context.explicit)) {
        return null;
      }

      // Combine loaded completions from JSON with custom completions from props and dynamic port hints
      const completions = [
        ...loadedCompletions,
        
        // Add dynamic port ID hints
        ...portHints.value,
        
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

