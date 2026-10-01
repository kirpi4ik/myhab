# Export stage: encoders -> int8 ONNX. Needs torch + optimum, which the runtime image does not ship.
FROM python:3.12-slim AS export
WORKDIR /build
RUN pip install --no-cache-dir torch --index-url https://download.pytorch.org/whl/cpu \
 && pip install --no-cache-dir "optimum[onnxruntime]"
COPY voice_nlu ./voice_nlu
COPY export_models.py .
RUN python export_models.py --out /models

# Test stage: run pytest so a broken resolver never ships.
FROM python:3.12-slim AS test
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt pytest httpx
COPY voice_nlu ./voice_nlu
COPY tests ./tests
RUN python -m pytest -q

FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY voice_nlu ./voice_nlu
COPY --from=export /models /models
# Force the test stage to build (its result is otherwise unused).
COPY --from=test /app/requirements.txt /tmp/.tests-passed
ENV NLU_ENCODER=e5-small NLU_MODELS_DIR=/models NLU_DATA_DIR=/data NLU_THREADS=2 NLU_PORT=8090
EXPOSE 8090
VOLUME /data
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8090/health', timeout=3)"
CMD ["python", "-m", "voice_nlu.app"]
