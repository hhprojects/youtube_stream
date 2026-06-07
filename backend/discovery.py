#!/usr/bin/env python3
"""Thin CLI over ytmusicapi (unauthenticated). Prints one JSON object to stdout.
Usage: python3 discovery.py <trending REGION | related VIDEO_ID | moods | mood PARAMS>
On failure: prints {"error": "..."} to stderr and exits non-zero."""
import sys
import json
from ytmusicapi import YTMusic


def _thumb(item):
    thumbs = item.get("thumbnails") or []
    return thumbs[-1]["url"] if thumbs else None


def _to_song(item):
    artists = item.get("artists") or []
    artist = ", ".join(a.get("name", "") for a in artists if a.get("name")) or "Unknown"
    return {
        "id": item.get("videoId"),
        "title": item.get("title"),
        "artist": artist,
        "thumbnail": _thumb(item),
        "duration": item.get("duration"),  # "m:ss" string or None
    }


def _songs_from(items):
    return [_to_song(i) for i in items if i.get("videoId") and i.get("title")]


def cmd_trending(yt, region):
    charts = yt.get_charts(country=region)
    # Unauthenticated: top "songs" need auth; use video/trending sections (validate shape in Task 2).
    items = []
    for section in ("videos", "trending"):
        sec = charts.get(section) or {}
        items += sec.get("items") or []
    return {"songs": _songs_from(items)}


def cmd_related(yt, video_id):
    wp = yt.get_watch_playlist(videoId=video_id)
    tracks = wp.get("tracks") or []
    deduped = [t for t in tracks if t.get("videoId") != video_id]  # seed is usually item 0
    return {"songs": _songs_from(deduped)}


def cmd_moods(yt):
    cats = yt.get_mood_categories()
    out = []
    for section, items in (cats or {}).items():
        for it in items or []:
            if it.get("params") and it.get("title"):
                out.append({"key": it["params"], "title": it["title"], "section": section})
    return {"categories": out}


def cmd_mood(yt, params):
    playlists = yt.get_mood_playlists(params) or []
    if not playlists:
        return {"title": "", "songs": []}
    first = playlists[0]
    pl = yt.get_playlist(first.get("playlistId"), limit=50)
    title = first.get("title") or pl.get("title") or ""
    return {"title": title, "songs": _songs_from(pl.get("tracks") or [])}


def main():
    if len(sys.argv) < 2:
        print(json.dumps({"error": "missing command"}), file=sys.stderr)
        sys.exit(2)
    cmd = sys.argv[1]
    yt = YTMusic()  # unauthenticated
    try:
        if cmd == "trending":
            region = sys.argv[2] if len(sys.argv) > 2 else "US"
            out = cmd_trending(yt, region)
        elif cmd == "related":
            out = cmd_related(yt, sys.argv[2])
        elif cmd == "moods":
            out = cmd_moods(yt)
        elif cmd == "mood":
            out = cmd_mood(yt, sys.argv[2])
        else:
            print(json.dumps({"error": f"unknown command {cmd}"}), file=sys.stderr)
            sys.exit(2)
    except Exception as e:  # noqa: BLE001 — surface any ytmusicapi failure as JSON on stderr
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        sys.exit(1)
    print(json.dumps(out))


if __name__ == "__main__":
    main()
