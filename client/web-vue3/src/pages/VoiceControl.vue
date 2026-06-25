<template>
  <q-page padding>
    <q-card flat bordered class="voice-card">
      <q-card-section class="row items-center">
        <div>
          <div class="text-h5 text-primary">
            <q-icon name="mdi-microphone" class="q-mr-sm"/>
            {{ $t('voice.title') }}
          </div>
          <div class="text-subtitle2 text-grey-7">{{ $t('voice.subtitle') }}</div>
        </div>
        <q-space/>
        <q-btn
          v-if="exchange.length"
          flat
          dense
          round
          icon="mdi-broom"
          @click="resetConversation"
        >
          <q-tooltip>{{ $t('voice.reset') }}</q-tooltip>
        </q-btn>
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
        <!-- Conversation log -->
        <q-card-section v-if="exchange.length" class="exchange">
          <div
            v-for="(msg, i) in exchange"
            :key="i"
            class="row q-mb-sm"
            :class="msg.role === 'user' ? 'justify-end' : 'justify-start'"
          >
            <q-chat-message
              :text="[msg.text]"
              :sent="msg.role === 'user'"
              :name="msg.role === 'user' ? $t('voice.you') : $t('voice.assistant')"
              :bg-color="msg.role === 'user' ? 'primary' : 'grey-3'"
              :text-color="msg.role === 'user' ? 'white' : 'black'"
            />
          </div>
        </q-card-section>

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

          <!-- Live / final transcript (also accepts typed input) -->
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
    const exchange = ref([]);          // [{role:'user'|'assistant', text}]
    const sessionId = ref(null);       // continues a multi-turn conversation

    let recognition = null;
    let currentAudio = null;

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

    const startListening = () => {
      if (!supported.value || listening.value) return;
      // Don't capture our own voice playback.
      stopAudio();
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

    const toggleListening = () => {
      if (!supported.value) return;
      if (listening.value) {
        recognition?.stop();
        listening.value = false;
        return;
      }
      startListening();
    };

    const stopAudio = () => {
      if (currentAudio) {
        try { currentAudio.pause(); } catch (e) { /* ignore */ }
        currentAudio = null;
      }
      if (window.speechSynthesis) {
        try { window.speechSynthesis.cancel(); } catch (e) { /* ignore */ }
      }
    };

    /**
     * Speak the reply: prefer the server's neural MP3 (natural voice), else fall
     * back to the browser's SpeechSynthesis. After playback, auto-listen when the
     * assistant is awaiting a reply (a clarifying question).
     */
    const playResponse = (r) => {
      const text = r.spokenResponse;
      const followUp = () => { if (r.awaitingReply) startListening(); };

      if (r.audioContent) {
        try {
          currentAudio = new Audio(`data:${r.audioMime || 'audio/mpeg'};base64,${r.audioContent}`);
          currentAudio.onended = followUp;
          currentAudio.onerror = followUp;
          currentAudio.play().catch(() => followUp());
          return;
        } catch (e) { /* fall through to browser TTS */ }
      }

      if (text && window.speechSynthesis) {
        try {
          const utter = new SpeechSynthesisUtterance(text);
          utter.lang = speechLang.value;
          utter.onend = followUp;
          utter.onerror = followUp;
          window.speechSynthesis.speak(utter);
          return;
        } catch (e) { /* ignore */ }
      }
      followUp();
    };

    const resetConversation = () => {
      stopAudio();
      sessionId.value = null;
      exchange.value = [];
      transcript.value = '';
    };

    const submit = (text) => {
      if (!text || !text.trim() || processing.value) return;
      const phrase = text.trim();
      processing.value = true;
      exchange.value.push({role: 'user', text: phrase});
      transcript.value = '';

      client.mutate({
        mutation: VOICE_COMMAND,
        variables: {transcript: phrase, locale: speechLang.value, sessionId: sessionId.value},
        fetchPolicy: 'no-cache',
      }).then(response => {
        processing.value = false;
        const r = response.data.voiceCommand;
        if (r?.sessionId) sessionId.value = r.sessionId;
        if (r?.success) {
          const reply = r.spokenResponse || t('voice.result.done', {name: '', action: ''});
          exchange.value.push({role: 'assistant', text: reply});
          playResponse(r);
        } else {
          const err = r?.error || t('voice.result.failed');
          exchange.value.push({role: 'assistant', text: err});
        }
      }).catch(error => {
        processing.value = false;
        exchange.value.push({role: 'assistant', text: t('voice.result.failed')});
        console.error('Voice command failed:', error);
      });
    };

    onBeforeUnmount(() => {
      stopAudio();
      try { recognition?.abort(); } catch (e) { /* ignore */ }
    });

    return {
      supported,
      listening,
      processing,
      transcript,
      exchange,
      toggleListening,
      submit,
      resetConversation,
    };
  }
});
</script>

<style scoped>
.voice-card {
  max-width: 560px;
  margin: 0 auto;
}
.exchange {
  max-height: 40vh;
  overflow-y: auto;
}
</style>
