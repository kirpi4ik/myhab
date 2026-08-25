# Test stage: run pytest so a broken mapping never ships.
FROM python:3.12-slim AS test
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt pytest
COPY tuya_bridge ./tuya_bridge
COPY tests ./tests
RUN python -m pytest -q

FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY tuya_bridge ./tuya_bridge
# Force the test stage to build (its result is otherwise unused).
COPY --from=test /app/requirements.txt /tmp/.tests-passed
CMD ["python", "-m", "tuya_bridge.bridge", "--config", "/config/config.yaml"]
