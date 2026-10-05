#!/usr/bin/env python3
import json, os, re, urllib.request, wave, math, random, struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STORY = ROOT / "app/src/main/java/com/stella/game/story/ChapterOne.kt"
OUT = ROOT / "app/src/main/assets"
VOICE = OUT / "voice/chapter1"
AMBIENCE = OUT / "audio/ambience"
SFX = OUT / "audio/sfx"
for p in (VOICE, AMBIENCE, SFX): p.mkdir(parents=True, exist_ok=True)

api_key = os.environ.get("ELEVENLABS_API_KEY", "").strip()
if not api_key:
    raise SystemExit("ELEVENLABS_API_KEY is missing")

def api(url, method="GET", body=None):
    req = urllib.request.Request(url, method=method)
    req.add_header("xi-api-key", api_key)
    req.add_header("Content-Type", "application/json")
    data = json.dumps(body).encode() if body is not None else None
    with urllib.request.urlopen(req, data=data, timeout=120) as r:
        return r.read(), r.headers.get("Content-Type","")

raw,_ = api("https://api.elevenlabs.io/v1/voices")
voices = json.loads(raw).get("voices", [])
by_name = {v.get("name","").lower(): v["voice_id"] for v in voices if v.get("voice_id")}

preferences = {
    "STELLA": ["rachel","sarah","jessica","alice"],
    "NARRATOR": ["adam","george","charlie","antoni"],
    "SYSTEM": ["antoni","josh","liam","brian"],
    "UNKNOWN": ["clyde","adam","daniel","callum"],
    "PLAYER": ["adam","liam","josh","charlie"],
}
def select_voice(speaker):
    for name in preferences.get(speaker, []):
        if name in by_name: return by_name[name]
    if voices: return voices[0]["voice_id"]
    raise RuntimeError("No ElevenLabs voices available")

voice_ids = {s: select_voice(s) for s in preferences}

text = STORY.read_text(encoding="utf-8")
scene_blocks = re.split(r'\n\s*Scene\(', text)[1:]
manifest = {}
total_chars = 0

for block in scene_blocks:
    scene_match = re.search(r'id\s*=\s*"([^"]+)"', block)
    if not scene_match: continue
    scene_id = scene_match.group(1)
    lines_match = re.search(r'lines\s*=\s*listOf\((.*?)\)\s*,\s*choices\s*=', block, re.S)
    if not lines_match: continue
    lines = re.findall(r'StoryLine\(Speaker\.([A-Z]+),\s*"((?:\\.|[^"])*)"', lines_match.group(1), re.S)
    entries = []
    for idx, (speaker, escaped) in enumerate(lines):
        spoken = bytes(escaped, "utf-8").decode("unicode_escape")
        filename = f"{scene_id}_{idx:02d}.mp3"
        path = VOICE / filename
        entries.append({"index": idx, "speaker": speaker, "file": f"voice/chapter1/{filename}", "text": spoken})
        if path.exists() and path.stat().st_size > 1000:
            continue
        voice_id = voice_ids.get(speaker, voice_ids["NARRATOR"])
        settings = {
            "stability": 0.58 if speaker == "STELLA" else 0.66,
            "similarity_boost": 0.78,
            "style": 0.20 if speaker in ("STELLA","UNKNOWN") else 0.08,
            "use_speaker_boost": True
        }
        body = {"text": spoken, "model_id": "eleven_flash_v2_5", "voice_settings": settings}
        audio,_ = api(f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}?output_format=mp3_44100_128", "POST", body)
        path.write_bytes(audio)
        total_chars += len(spoken)
        print("generated", filename, len(spoken), "chars")
    manifest[scene_id] = entries

(OUT / "voice/manifest.json").write_text(json.dumps({
    "provider":"ElevenLabs",
    "model":"eleven_flash_v2_5",
    "voices":voice_ids,
    "scenes":manifest
}, indent=2), encoding="utf-8")

def write_wave(path, seconds, generator, rate=12000):
    n=int(seconds*rate)
    with wave.open(str(path),"wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(rate)
        frames=[]
        for i in range(n):
            t=i/rate
            x=max(-1,min(1,generator(t,i,rate)))
            frames.append(struct.pack("<h", int(x*32767)))
        w.writeframes(b"".join(frames))

random.seed(7)
def cryo(t,i,r):
    return .045*math.sin(2*math.pi*54*t)+.025*math.sin(2*math.pi*108*t)+.012*(random.random()*2-1)
def corridor(t,i,r):
    pulse=max(0, math.sin(2*math.pi*.38*t))**10
    return .035*math.sin(2*math.pi*43*t)+.02*(random.random()*2-1)+.025*pulse*math.sin(2*math.pi*120*t)
def bridge(t,i,r):
    return .035*math.sin(2*math.pi*35*t)+.018*math.sin(2*math.pi*70*t)+.008*(random.random()*2-1)
def signal(t,i,r):
    return .02*(random.random()*2-1)+.025*math.sin(2*math.pi*(180+30*math.sin(t*.8))*t)

for name,gen in [("cryo_hum.wav",cryo),("corridor_creaks.wav",corridor),("bridge_low.wav",bridge),("radio_static.wav",signal)]:
    path=AMBIENCE/name
    if not path.exists(): write_wave(path, 5.0, gen)

def pickup(t,i,r):
    env=max(0,1-t/.55); return env*.22*math.sin(2*math.pi*(520+850*t)*t)
def click(t,i,r):
    env=max(0,1-t/.12); return env*.18*math.sin(2*math.pi*720*t)
def alert(t,i,r):
    env=max(0,1-t/.7); return env*.16*math.sin(2*math.pi*(420 if int(t*10)%2==0 else 580)*t)
for name,sec,gen in [("pickup.wav",.55,pickup),("choice.wav",.12,click),("alert.wav",.7,alert)]:
    path=SFX/name
    if not path.exists(): write_wave(path, sec, gen)

print("Audio pack ready. Newly generated voice characters:", total_chars)
