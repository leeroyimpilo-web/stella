"""Controlled, generated audio for download and playback smoke tests."""
import base64
import gzip
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

AUDIO = gzip.decompress(base64.b64decode(Path(__file__).with_name('tone.mp3.gz.b64').read_bytes()))

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = self.path.split('?', 1)[0]
        if path == '/health':
            body, mime = b'ok', 'text/plain'
        elif path == '/links':
            body, mime = b'<!doctype html><a id="track" href="/no-probe.mp3?browser=1">Download track.mp3</a>', 'text/html'
        elif path == '/no-probe.mp3':
            if self.headers.get('Range'):
                self.send_error(403, 'Audio preflight requests are not supported')
                return
            body, mime = AUDIO, 'audio/mpeg'
        elif path == '/slow.mp3':
            time.sleep(20)
            body, mime = AUDIO, 'audio/mpeg'
        elif path == '/fake.mp3':
            body, mime = b'<!doctype html><title>Sign in</title>', 'text/html'
        elif path == '/disguised.mp3':
            body, mime = b'<!doctype html><title>Expired audio</title>', 'application/octet-stream'
        elif path == '/redirect':
            self.send_response(302)
            self.send_header('Location', '/tone.mp3')
            self.end_headers()
            return
        elif path == '/tone.mp3':
            body, mime = AUDIO, 'audio/mpeg'
            byte_range = self.headers.get('Range', '')
            if byte_range.startswith('bytes=0-'):
                end = min(int(byte_range[8:]), len(AUDIO) - 1)
                body = AUDIO[:end + 1]
                self.send_response(206)
                self.send_header('Content-Range', f'bytes 0-{end}/{len(AUDIO)}')
                self.send_header('Content-Type', mime)
                self.send_header('Content-Length', str(len(body)))
                self.end_headers()
                self.wfile.write(body)
                return
        else:
            self.send_error(404)
            return
        self.send_response(200)
        self.send_header('Content-Type', mime)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

if __name__ == '__main__':
    ThreadingHTTPServer(('127.0.0.1', 8765), Handler).serve_forever()
