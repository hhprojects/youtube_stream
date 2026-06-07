#!/usr/bin/env python3
"""Thin CLI over ytmusicapi (unauthenticated). Prints one JSON object to stdout.
Usage: python3 discovery.py <trending REGION | related VIDEO_ID | moods | mood PARAMS | genrecharts REGION | playlist PLAYLIST_ID>
On failure: prints {"error": "..."} to stderr and exits non-zero."""
import sys
import json
from ytmusicapi import YTMusic


def _thumb(item):
    thumbs = item.get("thumbnails") or []
    return thumbs[-1]["url"] if thumbs else None


def _to_song(item):
    vid = item.get("videoId")
    artists = item.get("artists") or []
    artist = ", ".join(a.get("name", "") for a in artists if a.get("name")) or "Unknown"
    thumbnail = _thumb(item)
    if not thumbnail and vid:  # e.g. get_watch_playlist tracks omit thumbnails → derive from the videoId
        thumbnail = f"https://i.ytimg.com/vi/{vid}/hqdefault.jpg"
    return {
        "id": vid,
        "title": item.get("title"),
        "artist": artist,
        "thumbnail": thumbnail,
        "duration": item.get("duration"),  # "m:ss" string or None
    }


def _songs_from(items):
    return [_to_song(i) for i in items if i.get("videoId") and i.get("title")]


def cmd_trending(yt, region):
    # Unauthenticated get_charts returns chart *playlists* (no song items; top "songs" need auth).
    # videos[0] is the "Trending NN <country>" playlist — resolve it to real tracks.
    charts = yt.get_charts(country=region)
    videos = charts.get("videos") or []
    playlist_id = videos[0].get("playlistId") if videos else None
    if not playlist_id:
        return {"songs": []}
    pl = yt.get_playlist(playlist_id, limit=50)
    return {"songs": _songs_from(pl.get("tracks") or [])}


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


def cmd_genrecharts(yt, region):
    # genres section is US-only; ranked genre chart playlists, each resolved on tap via cmd_playlist.
    charts = yt.get_charts(country=region)
    genres = charts.get("genres") or []
    out = [{"key": g.get("playlistId"), "title": g.get("title")}
           for g in genres if g.get("playlistId") and g.get("title")]
    return {"charts": out}


def cmd_playlist(yt, playlist_id):
    pl = yt.get_playlist(playlist_id, limit=50)
    return {"title": pl.get("title") or "", "songs": _songs_from(pl.get("tracks") or [])}


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
        elif cmd == "genrecharts":
            region = sys.argv[2] if len(sys.argv) > 2 else "US"
            out = cmd_genrecharts(yt, region)
        elif cmd == "playlist":
            out = cmd_playlist(yt, sys.argv[2])
        else:
            print(json.dumps({"error": f"unknown command {cmd}"}), file=sys.stderr)
            sys.exit(2)
    except Exception as e:  # noqa: BLE001 — surface any ytmusicapi failure as JSON on stderr
        print(json.dumps({"error": str(e)}), file=sys.stderr)
        sys.exit(1)
    print(json.dumps(out))


if __name__ == "__main__":
    main()
