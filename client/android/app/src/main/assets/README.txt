Wake word uses an on-device microWakeWord TFLite model — no account, no key,
fully offline. Place TWO files in this folder:

    wake_word.json          (config; filename must equal WakeWordEngine.WAKE_CONFIG_ASSET)
    <your-model>.tflite     (the model; its filename is named by "model" in the JSON)

Where to get a model (free, open-source):
  - ESPHome ships ready-made models (e.g. "okay_nabu", "hey_jarvis", "hey_mycroft"):
    https://github.com/esphome/micro-wake-word-models  (models/v2/*.json + *.tflite)
  - Or train a custom "Hey myHAB" model with the microWakeWord trainer:
    https://github.com/kahrendt/microWakeWord

wake_word.json shape (the app reads these keys; extra keys are ignored):
    {
      "wake_word": "okay nabu",
      "model": "okay_nabu.tflite",
      "micro": {
        "probability_cutoff": 0.97,
        "sliding_window_size": 5,
        "feature_step_size": 10
      }
    }

Rename the model to match "model" above and drop both files here. The .tflite is
git-ignored (models are large / swappable). Without these files the wake-word
toggle reports "unavailable"; tap-to-talk still works.

Feature extraction + inference run in the native :microwakeword module (the real
TF microfrontend + TFLite-Micro), so detection matches how the model was trained
— no client-side DSP approximation.
