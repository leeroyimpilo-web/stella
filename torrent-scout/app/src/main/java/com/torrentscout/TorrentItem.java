package com.torrentscout;

import org.json.JSONObject;
import java.util.Locale;

public final class TorrentItem {
    public String title = "";
    public String source = "";
    public String category = "Other";
    public String link = "";
    public String details = "";
    public String infoHash = "";
    public long size = -1L;
    public int seeders = -1;

    public String key() {
        if (!infoHash.isEmpty()) return "hash:" + infoHash.toLowerCase(Locale.ROOT);
        return title.trim().toLowerCase(Locale.ROOT) + ":" + size;
    }

    public String sizeText() {
        if (size < 0) return "Size unknown";
        if (size < 1024) return size + " B";
        String[] units = {"KB","MB","GB","TB"};
        double d = size;
        int u = -1;
        do { d /= 1024; u++; } while (d >= 1024 && u < 3);
        return String.format(Locale.ROOT, "%.1f %s", d, units[u]);
    }

    public JSONObject json() {
        JSONObject o = new JSONObject();
        try {
            o.put("title", title);
            o.put("source", source);
            o.put("category", category);
            o.put("link", link);
            o.put("details", details);
            o.put("infoHash", infoHash);
            o.put("size", size);
            o.put("seeders", seeders);
        } catch (Exception ignored) {}
        return o;
    }

    public static TorrentItem from(JSONObject o) {
        TorrentItem t = new TorrentItem();
        t.title = o.optString("title");
        t.source = o.optString("source");
        t.category = o.optString("category", "Other");
        t.link = o.optString("link");
        t.details = o.optString("details");
        t.infoHash = o.optString("infoHash");
        t.size = o.optLong("size", -1);
        t.seeders = o.optInt("seeders", -1);
        return t;
    }
}
