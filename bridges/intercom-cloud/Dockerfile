# Test stage: run pytest so broken ref parsing never ships.
FROM python:3.12-slim AS test
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt pytest
COPY intercom_cloud ./intercom_cloud
COPY tests ./tests
RUN python -m pytest -q

FROM python:3.12-slim
WORKDIR /app
# ffmpeg is used for the single-frame live grab from the Tuya HLS stream.
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY intercom_cloud ./intercom_cloud
# Force the test stage to build (its result is otherwise unused).
COPY --from=test /app/requirements.txt /tmp/.tests-passed
CMD ["python", "-m", "intercom_cloud.server", "--config", "/config/config.yaml"]
