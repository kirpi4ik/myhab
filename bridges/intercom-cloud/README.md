# myHAB intercom cloud helper

A small backend-only service that supplies the **media** a Tuya intercom cannot
serve locally: the last doorbell/motion snapshot and a live video URL. It
complements the [Tuya bridge](../tuya) — the bridge handles the **local** doorbell
and motion *events* over Tuya-local DPs; this helper handles the **cloud** media.

On the reference TMEZON unit local RTSP is closed, the local snapshot CGI is
blocked, and the local stream is a proprietary Hisilicon RTP tunnel — none of it
is browser-playable. So live video and on-demand snapshots come from **Tuya
Cloud** (HLS + a single-frame ffmpeg grab), while the doorbell/motion **event
image** rides the local DP push for free.

## What it does

- Subscribes to `myhab/tuya/<code>/lastpic/state` (published by the Tuya bridge's
  `pic` event DPs) and keeps the latest picture reference per device.
- `GET /snapshot.jpg?device=<code>` — the last **event** image, resolved from that
  reference (fetch the presigned URL; AES-decrypt if the ref carries a key). With
  `?live=1`, or when no event image is available, a fresh Tuya-Cloud frame grab.
  `Cache-Control: no-store`; a ~10 s cache guards the Tuya allocate rate limit.
- `GET /stream?device=<code>` — `{ "hls": "<url>", "expires": <ts> }` for the
  live-video popup.

**Backend-only.** The browser never talks to this service; the Grails
`IntercomController` proxies it under JWT (`intercom.cloud.baseUrl`). Without
Tuya-Cloud credentials the live grab and stream return `204`, so the widget shows
a placeholder — the doorbell event image still works without the cloud.

## Configuration

`config.example.yaml` ships neutral defaults only. Your real `config.yaml`
(Tuya Cloud `access_id`/`access_secret`/`region`, the device-id map, broker
credentials) is installation data — keep it in your private ops repo and mount it
at `/config/config.yaml`. Tuya Cloud credentials come from a cloud project on the
[Tuya IoT Platform](https://iot.tuya.com) linked to your Smart Life account.

## Running

```bash
pip install -r requirements.txt
python -m intercom_cloud.server --config ./config.yaml
```

Or via Docker (the image build runs the test suite and bundles ffmpeg):

```bash
docker build -t myhab-intercom-cloud .
docker run -v $(pwd)/config.yaml:/config/config.yaml:ro myhab-intercom-cloud
```

CI publishes the image as `kirpi4ik/myhab-intercom-cloud`.

## myHAB side

Set `intercom.cloud.baseUrl` (ConfigProvider) to this service's internal URL
(e.g. `http://intercom-cloud:8095`). The `IntercomController` exposes
`/api/intercom/<id>/snapshot` and `/api/intercom/<id>/stream` to the widget.

## Verify against a real account

The Tuya stream-allocate endpoint/response shape varies by region and product;
`cloud.py` tries the device- and user-scoped variants and logs the raw response
on failure. Confirm the HLS URL plays and that the motion (`115`) image decrypts
before relying on those paths — the doorbell (`154`) direct URL is the robust one.

## Tests

```bash
pip install -r requirements.txt pytest
pytest
```
