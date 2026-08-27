"""Tuya Cloud access for the intercom helper: live HLS URL + a single JPEG frame.

Wraps `tinytuya.Cloud`, which handles the v2 HMAC-SHA256 signing and token
refresh. The stream-allocate endpoint and its response shape can vary by account
region/product, so the URL extraction is defensive — confirm against the real
account on first deploy (the exact endpoint is logged on failure).
"""

import logging
import subprocess
import tempfile
import os

log = logging.getLogger("intercom_cloud.cloud")


class TuyaCloud:
    def __init__(self, access_id, access_secret, region):
        import tinytuya
        self.enabled = bool(access_id and access_secret and region)
        self._cloud = None
        if self.enabled:
            self._cloud = tinytuya.Cloud(apiRegion=region, apiKey=access_id, apiSecret=access_secret)

    def hls_url(self, device_id):
        """Allocate an HLS live-stream URL for a device, or None."""
        if not self.enabled:
            return None
        body = {"type": "hls"}
        # Device-scoped allocate; some accounts require the user-scoped variant.
        for endpoint in (f"/v1.0/devices/{device_id}/stream/actions/allocate",
                         f"/v1.0/m/ipc/{device_id}/stream/actions/allocate"):
            try:
                resp = self._cloud.cloudrequest(endpoint, post=body)
            except Exception as err:  # noqa: BLE001
                log.warning("stream allocate failed on %s: %s", endpoint, err)
                continue
            url = _extract_url(resp)
            if url:
                return url
            log.warning("stream allocate returned no url on %s: %s", endpoint, resp)
        return None

    def frame_jpeg(self, device_id, ffmpeg="ffmpeg", grab_seconds=8):
        """Grab a JPEG from the live HLS stream, or None.

        A freshly allocated Tuya stream opens on a black 'connecting...' splash
        for the first few seconds; grabbing frame 0 captures that spinner instead
        of the camera. So read `grab_seconds` of the stream at 1 fps, overwriting a
        single output file (-update 1), and keep the LAST frame — which is past the
        splash and shows the real image.
        """
        url = self.hls_url(device_id)
        if not url:
            return None
        fd, path = tempfile.mkstemp(suffix=".jpg")
        os.close(fd)
        try:
            subprocess.run(
                [ffmpeg, "-y", "-loglevel", "error", "-i", url,
                 "-t", str(grab_seconds), "-vf", "fps=1", "-update", "1", "-q:v", "3", path],
                timeout=grab_seconds + 15, check=True,
            )
            with open(path, "rb") as fh:
                data = fh.read()
            return data or None
        except (subprocess.SubprocessError, OSError) as err:
            log.warning("ffmpeg frame grab failed: %s", err)
            return None
        finally:
            try:
                os.remove(path)
            except OSError:
                pass


def _extract_url(resp):
    """Pull an m3u8/http URL out of the (nested) allocate response."""
    if not isinstance(resp, dict):
        return None
    result = resp.get("result", resp)
    if isinstance(result, dict):
        for key in ("url", "stream_url", "hls"):
            val = result.get(key)
            if isinstance(val, str) and val.startswith("http"):
                return val
    if isinstance(result, str) and result.startswith("http"):
        return result
    return None
