package com.torrentscout;

import android.net.Uri;
import android.util.Xml;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SearchService {
    private SearchService() {}
    private static final int MAX_BYTES = 4_000_000;
    private static final Pattern HASH = Pattern.compile("(?i)(?:btih:|/)([a-f0-9]{40})(?:$|[&/?#])");

    private static InputStream open(String url, String mime) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setRequestProperty("User-Agent", "TorrentScout/0.1 (Android; torrent metadata search)");
        c.setRequestProperty("Accept", mime);
        c.setConnectTimeout(9000);
        c.setReadTimeout(12000);
        c.setInstanceFollowRedirects(true);
        if (c.getResponseCode() < 200 || c.getResponseCode() >= 300) {
            int code = c.getResponseCode();
            c.disconnect();
            throw new Exception("Server returned HTTP " + code);
        }
        return new java.io.FilterInputStream(c.getInputStream()) {
            @Override public void close() throws java.io.IOException {
                try { super.close(); } finally { c.disconnect(); }
            }
        };
    }

    private static String readText(InputStream in) throws Exception {
        try (InputStream input = in; BufferedReader br = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            char[] b = new char[8192];
            int n;
            while ((n = br.read(b)) != -1) {
                sb.append(b, 0, n);
                if (sb.length() > MAX_BYTES) throw new Exception("Search response too large");
            }
            return sb.toString();
        }
    }

    public static List<TorrentItem> archive(String userQuery) throws Exception {
        String clean = userQuery.trim().replaceAll("[^\\p{L}\\p{N}\\s._-]", " ").replaceAll("\\s+", " ").trim();
        if (clean.isEmpty()) return new ArrayList<>();
        String escaped = clean.replace(" ", " AND ");
        String query = "(" + escaped + ") AND format:\"Archive BitTorrent\"";
        Uri.Builder uri = Uri.parse("https://archive.org/advancedsearch.php").buildUpon()
            .appendQueryParameter("q", query)
            .appendQueryParameter("fl[]", "identifier")
            .appendQueryParameter("fl[]", "title")
            .appendQueryParameter("fl[]", "mediatype")
            .appendQueryParameter("fl[]", "item_size")
            .appendQueryParameter("rows", "45")
            .appendQueryParameter("page", "1")
            .appendQueryParameter("output", "json");
        JSONObject json = new JSONObject(readText(open(uri.build().toString(), "application/json")));
        JSONArray docs = json.getJSONObject("response").getJSONArray("docs");
        List<TorrentItem> found = new ArrayList<>();
        for (int i = 0; i < docs.length(); i++) {
            JSONObject o = docs.optJSONObject(i);
            if (o == null) continue;
            String id = o.optString("identifier").trim();
            if (id.isEmpty()) continue;
            String title = o.optString("title", id);
            if (title.trim().isEmpty()) title = id;
            TorrentItem t = new TorrentItem();
            t.title = title;
            t.source = "Internet Archive";
            t.category = categoryFromText(o.optString("mediatype"));
            t.size = o.optLong("item_size", -1L);
            t.link = "https://archive.org/download/" + Uri.encode(id) + "/" + Uri.encode(id + "_archive.torrent");
            t.details = "https://archive.org/details/" + Uri.encode(id);
            found.add(t);
        }
        return found;
    }

    public static boolean isValidEndpoint(String url) {
        try {
            Uri u = Uri.parse(url.trim());
            String scheme = u.getScheme();
            String host = u.getHost();
            if (host == null || host.isEmpty() || u.getUserInfo() != null) return false;
            if ("https".equalsIgnoreCase(scheme)) return true;
            if (!"http".equalsIgnoreCase(scheme)) return false;
            String h = host.toLowerCase(Locale.ROOT);
            if (h.equals("localhost") || h.equals("127.0.0.1") || h.equals("::1")
                || h.startsWith("192.168.") || h.startsWith("10.")) return true;
            Matcher m = Pattern.compile("^172\\.(\\d+)\\.").matcher(h);
            if (m.find()) {
                int subnet = Integer.parseInt(m.group(1));
                return subnet >= 16 && subnet <= 31;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    public static List<TorrentItem> torznab(SettingsStore.Indexer indexer, String query) throws Exception {
        if (!isValidEndpoint(indexer.url)) throw new Exception("Source URL requires HTTPS or a private LAN host");
        Uri.Builder url = Uri.parse(indexer.url.trim()).buildUpon()
            .appendQueryParameter("t", "search")
            .appendQueryParameter("q", query)
            .appendQueryParameter("limit", "60");
        if (!indexer.apiKey.trim().isEmpty()) url.appendQueryParameter("apikey", indexer.apiKey.trim());
        try (InputStream input = open(url.build().toString(), "application/rss+xml, application/xml, text/xml")) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(input, "UTF-8");
            int event = parser.getEventType();
            TorrentItem active = null;
            String tag = "";
            String categoryRaw = "";
            String fallbackLink = "";
            List<TorrentItem> items = new ArrayList<>();
            int count = 0;
            while (event != XmlPullParser.END_DOCUMENT && count < 3000) {
                count++;
                if (event == XmlPullParser.START_TAG) {
                    tag = parser.getName();
                    if ("item".equalsIgnoreCase(tag)) {
                        active = new TorrentItem();
                        active.source = indexer.name;
                        fallbackLink = "";
                        categoryRaw = "";
                    } else if (active != null && "enclosure".equalsIgnoreCase(tag)) {
                        String candidate = parser.getAttributeValue(null, "url");
                        if (usableLink(candidate)) active.link = candidate;
                        long length = toLong(parser.getAttributeValue(null, "length"));
                        if (length > 0) active.size = length;
                    } else if (active != null && "attr".equalsIgnoreCase(tag)) {
                        String key = parser.getAttributeValue(null, "name");
                        String val = parser.getAttributeValue(null, "value");
                        if (key != null && val != null) {
                            switch (key.toLowerCase(Locale.ROOT)) {
                                case "seeders": active.seeders = (int) toLong(val); break;
                                case "size": if (toLong(val) > 0) active.size = toLong(val); break;
                                case "category": if (categoryRaw.isEmpty()) categoryRaw = val; break;
                                case "infohash": active.infoHash = val; break;
                                case "magneturl": if (usableLink(val)) active.link = val; break;
                            }
                        }
                    }
                } else if (event == XmlPullParser.TEXT && active != null) {
                    String value = parser.getText().trim();
                    if (!value.isEmpty()) {
                        switch (tag.toLowerCase(Locale.ROOT)) {
                            case "title": active.title += value; break;
                            case "link": fallbackLink += value; break;
                            case "guid":
                                if (active.details.isEmpty() && usableLink(value)) active.details = value;
                                break;
                            case "category":
                                if (categoryRaw.isEmpty()) categoryRaw = value;
                                break;
                            case "size":
                                if (active.size < 0 && toLong(value) > 0) active.size = toLong(value);
                                break;
                        }
                    }
                } else if (event == XmlPullParser.END_TAG) {
                    String end = parser.getName();
                    if ("item".equalsIgnoreCase(end) && active != null) {
                        if (active.link.isEmpty() && usableLink(fallbackLink)) active.link = fallbackLink;
                        if (active.infoHash.isEmpty()) active.infoHash = hashFrom(active.link);
                        if (active.link.isEmpty() && active.infoHash.matches("(?i)[a-f0-9]{40}"))
                            active.link = "magnet:?xt=urn:btih:" + active.infoHash + "&dn=" + Uri.encode(active.title);
                        active.category = categoryFromCode(categoryRaw);
                        if ("Other".equals(active.category)) active.category = categoryFromText(categoryRaw + " " + active.title);
                        if (!active.title.trim().isEmpty() && !active.link.isEmpty()) items.add(active);
                        active = null;
                    }
                    tag = "";
                }
                event = parser.next();
            }
            return items;
        }
    }

    private static boolean usableLink(String link) {
        if (link == null) return false;
        String l = link.trim().toLowerCase(Locale.ROOT);
        return l.startsWith("magnet:?xt=urn:btih:") || l.startsWith("https://") || l.startsWith("http://");
    }

    private static String hashFrom(String text) {
        if (text == null) return "";
        Matcher m = HASH.matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private static long toLong(String s) {
        try { return Long.parseLong(s); } catch (Exception e) { return -1; }
    }

    public static String categoryFromCode(String raw) {
        try {
            int num = Integer.parseInt(raw.trim());
            switch (num / 1000) {
                case 1: return "Games";
                case 2: case 5: return "Video";
                case 3: return "Audio";
                case 4: return "Software";
                case 7: return "Books";
                default: return "Other";
            }
        } catch (Exception e) { return "Other"; }
    }

    public static String categoryFromText(String raw) {
        String s = raw.toLowerCase(Locale.ROOT);
        if (s.contains("movie") || s.contains("video") || s.contains("tv") || s.contains("film")) return "Video";
        if (s.contains("audio") || s.contains("music") || s.contains("flac") || s.contains("mp3")) return "Audio";
        if (s.contains("book") || s.contains("text") || s.contains("epub") || s.contains("pdf")) return "Books";
        if (s.contains("game") || s.contains("console")) return "Games";
        if (s.contains("software") || s.contains("app") || s.contains("iso") || s.contains("linux") || s.contains("program")) return "Software";
        return "Other";
    }
}
