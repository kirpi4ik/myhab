<template>
  <q-page padding>
    <q-card flat bordered class="voice-card">
      <q-card-section>
        <div class="text-h5 text-primary">
          <q-icon name="mdi-microphone" class="q-mr-sm"/>
          {{ $t('voice.title') }}
        </div>
        <div class="text-subtitle2 text-grey-7">
          {{ $t('voice.subtitle') }}
        </div>
      </q-card-section>

      <q-separator/>

      <!-- Unsupported-browser notice (e.g. Firefox / iOS) -->
      <q-card-section v-if="!supported">
        <q-banner dense class="bg-orange-1 text-orange-9">
          <template v-slot:avatar>
            <q-icon name="mdi-alert-circle"/>
          </template>
          {{ $t('voice.errors.not_supported') }}
        </q-banner>
      </q-card-section>

      <template v-else>
        <q-card-section class="column items-center q-gutter-md">
          <q-btn
            round
            size="28px"
            :color="listening ? 'red-6' : 'primary'"
            :icon="listening ? 'mdi-stop' : 'mdi-microphone'"
            :loading="processing"
            @click="toggleListening"
          >
            <q-tooltip>{{ listening ? $t('voice.stop') : $t('voice.start') }}</q-tooltip>
          </q-btn>

          <div class="text-center text-grey-8" style="min-height: 24px">
            <span v-if="listening">{{ $t('voice.listening') }}</span>
            <span v-else-if="processing">{{ $t('voice.processing') }}</span>
            <span v-else>{{ $t('voice.tap_to_speak') }}</span>
          </div>

          <!-- Live / final transcript -->
          <q-input
            v-model="transcript"
            outlined
            dense
            class="full-width"
            :label="$t('voice.transcript')"
            :placeholder="$t('voice.transcript_placeholder')"
            @keyup.enter="submit(transcript)"
          >
            <template v-slot:append>
              <q-btn
                flat
                dense
                round
                icon="mdi-send"
                :disable="!transcript || processing"
                @click="submit(transcript)"
              >
                <q-tooltip>{{ $t('voice.send') }}</q-tooltip>
              </q-btn>
            </template>
          </q-input>
        </q-card-section>

        <q-separator/>

        <!-- Result of the last command -->
        <q-card-section v-if="result">
          <q-banner
            dense
            :class="result.success ? 'bg-green-1 text-green-9' : 'bg-red-1 text-red-9'"
          >
            <template v-slot:avatar>
              <q-icon :name="result.success ? 'mdi-check-circle' : 'mdi-alert-circle'"/>
            </template>
            <template v-if="result.success">
              {{ result.spokenResponse || $t('voice.result.done', {name: result.peripheralName, action: result.action}) }}
            </template>
            <template v-else>
              {{ result.error || $t('voice.result.failed') }}
            </template>
          </q-banner>
        </q-card-section>
      </template>

      <q-separator/>

      <q-card-actions>
        <q-btn color="grey" icon="mdi-arrow-left" :label="$t('actions.nav.back')" flat @click="$router.go(-1)"/>
      </q-card-actions>
    </q-card>
  </q-page>
</template>

<script>
import {computed, defineComponent, onBeforeUnmount, ref} from "vue";
import {useApolloClient} from "@vue/apollo-composable";
import {useQuasar} from "quasar";
import {useI18n} from "vue-i18n";
import {VOICE_COMMAND} from "@/graphql/queries";

// Map the app's UI locale to a BCP-47 tag for speech recognition / synthesis.
const SPEECH_LANGS = {en: 'en-US', ro: 'ro-RO'};

export default defineComponent({
  name: 'VoiceControl',
  setup() {
    const $q = useQuasar();
    const {t, locale} = useI18n({useScope: 'global'});
    const {client} = useApolloClient();

    // Recognise and read back in the currently selected app language.
    const speechLang = computed(() => SPEECH_LANGS[locale.value] || 'en-US');

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    const supported = ref(!!SpeechRecognition);

    const listening = ref(false);
    const processing = ref(false);
    const transcript = ref('');
    const result = ref(null);

    let recognition = null;

    /**
     * Lazily build a SpeechRecognition instance. en-US only in v1 (see plan);
     * interim results give a live caption, then onresult fires the command.
     */
    const buildRecognition = () => {
      const rec = new SpeechRecognition();
      rec.lang = speechLang.value;
      rec.interimResults = true;
      rec.continuous = false;
      rec.maxAlternatives = 1;

      rec.onresult = (event) => {
        let text = '';
        for (let i = 0; i < event.results.length; i++) {
          text += event.results[i][0].transcript;
        }
        transcript.value = text.trim();
        // Submit once we have a final result.
        const last = event.results[event.results.length - 1];
        if (last && last.isFinal) {
          submit(transcript.value);
        }
      };

      rec.onerror = (event) => {
        listening.value = false;
        const messages = {
          'not-allowed': t('voice.errors.denied'),
          'service-not-allowed': t('voice.errors.denied'),
          'no-speech': t('voice.errors.no_speech'),
          'audio-capture': t('voice.errors.no_mic'),
          'network': t('voice.errors.network'),
        };
        const msg = messages[event.error] || t('voice.errors.generic', {message: event.error});
        $q.notify({color: 'negative', message: msg, icon: 'mdi-alert-circle', position: 'top'});
      };

      rec.onend = () => {
        listening.value = false;
      };

      return rec;
    };

    const toggleListening = () => {
      if (!supported.value) return;
      if (listening.value) {
        recognition?.stop();
        listening.value = false;
        return;
      }
      result.value = null;
      transcript.value = '';
      try {
        recognition = buildRecognition();
        recognition.start();
        listening.value = true;
      } catch (e) {
        listening.value = false;
        $q.notify({color: 'negative', message: t('voice.errors.generic', {message: e.message}), icon: 'mdi-alert-circle', position: 'top'});
      }
    };

    /**
     * Speak a short confirmation back, if the browser supports it.
     */
    const speak = (text) => {
      if (!text || !window.speechSynthesis) return;
      try {
        const utter = new SpeechSynthesisUtterance(text);
        utter.lang = speechLang.value;
        window.speechSynthesis.speak(utter);
      } catch (e) {
        // Non-fatal — speech synthesis is a nice-to-have.
      }
    };

    /**
     * Send the transcript to the backend, which resolves + executes it.
     */
    const submit = (text) => {
      if (!text || !text.trim() || processing.value) return;
      processing.value = true;
      result.value = null;
      client.mutate({
        mutation: VOICE_COMMAND,
        variables: {transcript: text.trim(), locale: speechLang.value},
        fetchPolicy: 'no-cache',
      }).then(response => {
        processing.value = false;
        const r = response.data.voiceCommand;
        result.value = r;
        if (r?.success) {
          speak(r.spokenResponse || t('voice.result.done', {name: r.peripheralName, action: r.action}));
        }
      }).catch(error => {
        processing.value = false;
        result.value = {success: false, error: t('voice.result.failed')};
        console.error('Voice command failed:', error);
      });
    };

    onBeforeUnmount(() => {
      try {
        recognition?.abort();
      } catch (e) { /* ignore */ }
    });

    return {
      supported,
      listening,
      processing,
      transcript,
      result,
      toggleListening,
      submit,
    };
  }
});
</script>

<style scoped>
.voice-card {
  max-width: 560px;
  margin: 0 auto;
}
</style>
