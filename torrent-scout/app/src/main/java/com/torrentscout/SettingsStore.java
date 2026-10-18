package com.torrentscout;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public final class SettingsStore {
    public static final class Indexer {
        public final String name, url, apiKey;
        public Indexer(String name, String url, String apiKey) {
            this.name = name;
            this.url = url;
            this.apiKey = apiKey;
        }
    }

    private final SharedPreferences prefs;

    public SettingsStore(Context context) {
        prefs = context.getSharedPreferences("torrent_scout", Context.MODE_PRIVATE);
    }

    public synchronized List<Indexer> indexers() {
        List<Indexer> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString("indexers", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject j = a.optJSONObject(i);
                if (j != null) out.add(new Indexer(j.optString("name"), j.optString("url"), j.optString("key")));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private void writeIndexers(List<Indexer> list) {
        JSONArray a = new JSONArray();
        for (Indexer i : list) {
            JSONObject j = new JSONObject();
            try { j.put("name", i.name); j.put("url", i.url); j.put("key", i.apiKey); } catch (Exception ignored) {}
            a.put(j);
        }
        prefs.edit().putString("indexers", a.toString()).apply();
    }

    public synchronized void addIndexer(Indexer indexer) {
        List<Indexer> list = indexers();
        list.add(indexer);
        writeIndexers(list);
    }

    public synchronized void removeIndexer(int position) {
        List<Indexer> list = indexers();
        if (position >= 0 && position < list.size()) {
            list.remove(position);
            writeIndexers(list);
        }
    }

    public synchronized List<TorrentItem> favorites() {
        List<TorrentItem> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString("favorites", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject j = a.optJSONObject(i);
                if (j != null) list.add(TorrentItem.from(j));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public synchronized boolean saved(TorrentItem t) {
        for (TorrentItem x : favorites()) if (x.key().equals(t.key())) return true;
        return false;
    }

    public synchronized void toggle(TorrentItem t) {
        LinkedHashMap<String,TorrentItem> map = new LinkedHashMap<>();
        for (TorrentItem x : favorites()) map.put(x.key(), x);
        if (map.containsKey(t.key())) map.remove(t.key()); else map.put(t.key(), t);
        JSONArray a = new JSONArray();
        for (TorrentItem x : map.values()) a.put(x.json());
        prefs.edit().putString("favorites", a.toString()).apply();
    }
}
