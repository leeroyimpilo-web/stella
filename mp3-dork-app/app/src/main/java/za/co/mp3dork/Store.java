package za.co.mp3dork;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;

public final class Store {
    private final SharedPreferences preferences;
    public Store(Context context) { preferences = context.getSharedPreferences("mp3dork_v1", Context.MODE_PRIVATE); }
    public SharedPreferences settings() { return preferences; }
    public synchronized JSONArray list(String key) { try { return new JSONArray(preferences.getString(key, "[]")); } catch (Exception e) { return new JSONArray(); } }
    public synchronized void save(String key, JSONArray items) { preferences.edit().putString(key, items.toString()).apply(); }
    public synchronized void put(String key, JSONObject item, String identity, int limit) {
        JSONArray previous = list(key), next = new JSONArray();
        next.put(item);
        for (int i = 0; i < previous.length() && next.length() < limit; i++) {
            JSONObject old = previous.optJSONObject(i);
            if (old != null && !old.optString(identity).equals(item.optString(identity))) next.put(old);
        }
        save(key, next);
    }
    public synchronized void remove(String key, String identity, String value) {
        JSONArray previous = list(key), next = new JSONArray();
        for (int i = 0; i < previous.length(); i++) { JSONObject item = previous.optJSONObject(i); if (item != null && !item.optString(identity).equals(value)) next.put(item); }
        save(key, next);
    }
    public static JSONObject object(Object... values) {
        JSONObject object = new JSONObject();
        for (int i = 0; i + 1 < values.length; i += 2) try { object.put(String.valueOf(values[i]), values[i + 1]); } catch (Exception ignored) {}
        return object;
    }
}
