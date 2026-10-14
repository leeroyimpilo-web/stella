package za.co.mp3dork;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.widget.Toast;
import java.util.HashMap;
import java.util.Map;

public final class PlaybackService extends Service {
    public static volatile PlaybackService instance;
    public static final String PLAY = "za.co.mp3dork.PLAY", TOGGLE = "za.co.mp3dork.TOGGLE", STOP = "za.co.mp3dork.STOP";
    private static final String CHANNEL = "mp3dork_playback";
    private static final int NOTIFICATION = 41;
    private MediaPlayer player;
    private MediaSession session;
    private AudioManager audioManager;
    private AudioFocusRequest focus;
    private boolean prepared, loading, resumeOnFocus;
    private String title = "MP3 Dork", url = "";
    private final BroadcastReceiver headphones = new BroadcastReceiver() { @Override public void onReceive(Context context, Intent intent) { pause(); } };

    public static void play(Context context, String url, String title, String cookie, String agent) {
        Intent intent = new Intent(context, PlaybackService.class).setAction(PLAY).putExtra("url", url).putExtra("title", title).putExtra("cookie", cookie).putExtra("agent", agent);
        context.startForegroundService(intent);
    }
    public static void toggle(Context context) { context.startForegroundService(new Intent(context, PlaybackService.class).setAction(TOGGLE)); }
    public static void stop(Context context) { context.startService(new Intent(context, PlaybackService.class).setAction(STOP)); }
    @Override public void onCreate() {
        super.onCreate(); instance = this;
        NotificationManager notifications = getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(CHANNEL, "Audio playback", NotificationManager.IMPORTANCE_LOW));
        audioManager = getSystemService(AudioManager.class);
        AudioAttributes attributes = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
        focus = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attributes).setOnAudioFocusChangeListener(change -> {
            if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) { resumeOnFocus = isPlaying(); pauseInternal(false); }
            else if (change == AudioManager.AUDIOFOCUS_LOSS) { resumeOnFocus = false; pauseInternal(false); }
            else if (change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK && player != null) player.setVolume(0.25f, 0.25f);
            else if (change == AudioManager.AUDIOFOCUS_GAIN) { if (player != null) player.setVolume(1, 1); if (resumeOnFocus) { resumeOnFocus = false; resume(); } }
        }).build();
        session = new MediaSession(this, "MP3Dork");
        session.setCallback(new MediaSession.Callback() {
            @Override public void onPlay() { resume(); }
            @Override public void onPause() { pause(); }
            @Override public void onStop() { stopSelf(); }
            @Override public void onSeekTo(long position) { seekTo(position); }
        });
        session.setActive(true);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(headphones, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(headphones, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }
    @Override public int onStartCommand(Intent intent, int flags, int id) {
        if (intent == null || STOP.equals(intent.getAction())) { stopSelf(); return START_NOT_STICKY; }
        startForeground(NOTIFICATION, notification());
        if (PLAY.equals(intent.getAction())) load(intent);
        else if (TOGGLE.equals(intent.getAction())) { if (isPlaying()) pause(); else if (prepared) resume(); else if (!loading) stopSelf(); }
        return START_NOT_STICKY;
    }
    private void load(Intent intent) {
        releasePlayer();
        url = intent.getStringExtra("url");
        title = intent.getStringExtra("title");
        if (title == null) title = "Audio preview";
        loading = true;
        publish();
        if (audioManager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { fail("Audio focus is unavailable. Try again after the other audio stops."); return; }
        player = new MediaPlayer();
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
        player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);
        player.setOnPreparedListener(mp -> { prepared = true; loading = false; mp.start(); publish(); });
        player.setOnCompletionListener(mp -> { publish(); stopForeground(STOP_FOREGROUND_DETACH); audioManager.abandonAudioFocusRequest(focus); });
        player.setOnErrorListener((mp, what, extra) -> { fail("Could not play this audio. Check the connection or try another link."); return true; });
        try {
            Map<String, String> headers = new HashMap<>();
            String cookie = intent.getStringExtra("cookie"), agent = intent.getStringExtra("agent");
            if (cookie != null && !cookie.isEmpty()) headers.put("Cookie", cookie);
            if (agent != null && !agent.isEmpty()) headers.put("User-Agent", agent);
            player.setDataSource(this, Uri.parse(url), headers);
            player.prepareAsync();
        } catch (Exception e) { fail("Could not open this audio file."); }
    }
    public void pause() { resumeOnFocus = false; pauseInternal(true); }
    private void pauseInternal(boolean abandon) {
        if (isPlaying()) player.pause();
        publish(); stopForeground(STOP_FOREGROUND_DETACH);
        if (abandon) audioManager.abandonAudioFocusRequest(focus);
    }
    public void resume() {
        if (!prepared || player == null) return;
        startForeground(NOTIFICATION, notification());
        if (audioManager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { stopForeground(STOP_FOREGROUND_DETACH); return; }
        player.start(); publish();
    }
    public void seekTo(long value) { if (prepared && player != null) { player.seekTo(Math.max(0, Math.min(value, duration())), MediaPlayer.SEEK_CLOSEST); publish(); } }
    public boolean isPlaying() { try { return prepared && player != null && player.isPlaying(); } catch (Exception e) { return false; } }
    public boolean isLoading() { return loading; }
    public String title() { return title; }
    public String url() { return url; }
    public int position() { try { return prepared && player != null ? player.getCurrentPosition() : 0; } catch (Exception e) { return 0; } }
    public int duration() { try { return prepared && player != null ? player.getDuration() : 0; } catch (Exception e) { return 0; } }
    private void publish() {
        if (session == null) return;
        session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE, title).putString(MediaMetadata.METADATA_KEY_ARTIST, "MP3 Dork").putLong(MediaMetadata.METADATA_KEY_DURATION, duration()).build());
        int state = loading ? PlaybackState.STATE_BUFFERING : isPlaying() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED;
        session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_STOP | PlaybackState.ACTION_SEEK_TO).setState(state, position(), isPlaying() ? 1 : 0).build());
        getSystemService(NotificationManager.class).notify(NOTIFICATION, notification());
    }
    private PendingIntent command(String action, int code) { return PendingIntent.getForegroundService(this, code, new Intent(this, PlaybackService.class).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); }
    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_app).setContentTitle(title).setContentText(loading ? "Loading audio…" : isPlaying() ? "Playing" : "Paused").setContentIntent(open).setOnlyAlertOnce(true).setVisibility(Notification.VISIBILITY_PUBLIC).setCategory(Notification.CATEGORY_TRANSPORT).setOngoing(isPlaying() || loading);
        builder.addAction(new Notification.Action.Builder(isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, isPlaying() ? "Pause" : "Play", command(TOGGLE, 1)).build());
        builder.addAction(new Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel, "Stop", command(STOP, 2)).build());
        if (session != null) builder.setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0, 1));
        return builder.build();
    }
    private void fail(String message) { loading = false; Toast.makeText(this, message, Toast.LENGTH_LONG).show(); stopSelf(); }
    private void releasePlayer() { if (player != null) { try { player.reset(); player.release(); } catch (Exception ignored) {} player = null; } prepared = false; loading = false; }
    @Override public void onTaskRemoved(Intent rootIntent) { if (!isPlaying() && !loading) stopSelf(); }
    @Override public void onDestroy() {
        releasePlayer(); audioManager.abandonAudioFocusRequest(focus); session.release();
        try { unregisterReceiver(headphones); } catch (Exception ignored) {}
        stopForeground(STOP_FOREGROUND_REMOVE); getSystemService(NotificationManager.class).cancel(NOTIFICATION); instance = null; super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
