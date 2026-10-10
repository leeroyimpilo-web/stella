# Stella Rap Studio

An independent rap music project in the existing Stella repository. It does **not** modify the Lotto project.

## Included

- Responsive music-composer interface with original motivational rap lyrics
- Browser-generated hip-hop beat preview (works without an external provider)
- ACE-Step 1.5 REST API integration for lyric-to-song generation
- Background job polling, in-browser MP3 player, and MP3 download
- Optional bearer token stored in backend environment, not frontend
- Basic request validation and per-IP generation throttling

## Run

Requires Node.js 20 or newer:

```bash
cd rap-studio
npm start
```

Visit http://localhost:3000. The local beat preview works immediately.

## Enable AI music and vocals

Install ACE-Step v1.5 on a PC or GPU machine (official source: https://github.com/ace-step/ACE-Step-1.5).
Use `uv run acestep-api` to run the model REST service (default port 8001).

Set environment variables for **this Node server**:

```bash
ACESTEP_URL=http://127.0.0.1:8001
ACESTEP_API_KEY=your-optional-server-token
PORT=3000
```

With a hosted server, set `ACESTEP_URL` to the trusted URL of your ACE-Step host.
**Do not expose an unauthenticated ACE-Step server publicly.** Set up TLS, API keys and network access controls for a public deployment. Existing basic rate limits are not sufficient protection on their own.

API workflow uses `POST /release_task`, `POST /query_result`, and `GET /v1/audio` per official documentation:
https://ace-step.github.io/ACE-Step-1.5/en/API

## Reality of free generation

The ACE-Step model code is free/open source, but it is not a free, permanently hosted inference API. A GPU-hosting subscription or suitable local hardware may still be required. Without the external model server, full AI-vocal generation is disabled, and only the local browser beat preview works.

## Sample track

The companion **Built From Nothing** rap demo was created locally with procedurally synthesized drums/bass/melody and an eSpeak robotic vocal. It is **not** an ACE-Step AI-sung vocal recording. It can be downloaded separately from the chat where this project was created.
