package za.co.mp3dork;

import java.util.ArrayList;
import java.util.List;

public final class QueryBuilder {
    public static final String[] PRESETS = {"Direct MP3", "Music folders", "Open audio", "Internet Archive"};
    private QueryBuilder() {}
    public static String build(String words, int preset, boolean exact, String domain, String exclusions) {
        String term = words == null ? "" : words.trim().replaceAll("\\s+", " ");
        if (exact && !term.isEmpty()) term = "\"" + term.replace("\"", "") + "\"";
        List<String> parts = new ArrayList<>();
        if (!term.isEmpty()) parts.add(term);
        switch (preset) {
            case 1: parts.add("intitle:\"index of\" mp3"); break;
            case 2: parts.add("filetype:mp3 (\"creative commons\" OR \"public domain\")"); break;
            case 3: parts.add("site:archive.org (mp3 OR audio)"); break;
            default: parts.add("filetype:mp3");
        }
        if (domain != null && !domain.trim().isEmpty()) {
            String site = domain.trim().replaceFirst("(?i)^https?://", "").split("/", 2)[0];
            if (!site.matches("(?i)[a-z0-9][a-z0-9.-]*\\.[a-z]{2,}")) throw new IllegalArgumentException("Enter a website domain, for example archive.org.");
            if (preset == 3 && !site.equalsIgnoreCase("archive.org")) throw new IllegalArgumentException("Choose another preset to search a different website.");
            if (preset != 3) parts.add("site:" + site.toLowerCase(java.util.Locale.ROOT));
        }
        if (exclusions != null) for (String word : exclusions.trim().split("[,\\s]+")) {
            String clean = word.replaceAll("[^\\p{L}\\p{N}_-]", "");
            if (!clean.isEmpty()) parts.add("-" + clean.replaceFirst("^-+", ""));
        }
        return String.join(" ", parts);
    }
}
