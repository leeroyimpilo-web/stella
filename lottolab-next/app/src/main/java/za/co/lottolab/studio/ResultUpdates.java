package za.co.lottolab.studio;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.*;

public final class ResultUpdates extends JobService {
  private static final int PERIODIC_JOB = 2202;
  private static final int CONNECTED_JOB = 2203;
  private static final long SIX_HOURS = 6 * 60 * 60 * 1000L;
  private volatile boolean stopped;

  public static void schedule(Context c) {
    ArchiveStore store = new ArchiveStore(c);
    android.content.SharedPreferences prefs = store.preferences();
    // Initial installation is automatic. Existing explicit choices always take precedence.
    if (!prefs.contains("feed_url"))
      prefs.edit().putString("feed_url", OperatorResults.ENDPOINT).apply();
    if (!prefs.contains("background_updates"))
      prefs.edit().putBoolean("background_updates", true).apply();
    boolean enabled =
        !prefs.getString("feed_url", "").isEmpty()
            && prefs.getBoolean("background_updates", true);
    JobScheduler jobs = (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);
    ComponentName service = new ComponentName(c, ResultUpdates.class);
    if (enabled) {
      jobs.schedule(
          new JobInfo.Builder(PERIODIC_JOB, service)
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .setPeriodic(SIX_HOURS)
              .setPersisted(true)
              .build());
      // Network-constrained catch-up job on an app launch after prolonged offline use.
      long last = prefs.getLong("last_attempt", 0);
      if (System.currentTimeMillis() - last > SIX_HOURS)
        jobs.schedule(
            new JobInfo.Builder(CONNECTED_JOB, service)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(1000L)
                .build());
    } else {
      jobs.cancel(PERIODIC_JOB);
      jobs.cancel(CONNECTED_JOB);
    }
    store.close();
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    stopped = false;
    new Thread(
            () -> {
              ArchiveStore store = new ArchiveStore(this);
              try {
                store.preferences().edit().putLong("last_attempt", System.currentTimeMillis()).apply();
                List<Draw> draws = FeedClient.fetch(store.preferences().getString("feed_url", ""));
                if (!stopped) {
                  int changed = store.importDraws(draws);
                  store
                      .preferences()
                      .edit()
                      .putLong("last_sync", System.currentTimeMillis())
                      .putString("sync_error", "")
                      .apply();
                  if (changed > 0) notifyAvailable(this, store);
                }
              } catch (Exception e) {
                store.preferences().edit().putString("sync_error", e.getMessage()).apply();
              } finally {
                store.close();
                if (!stopped) jobFinished(params, false);
              }
            },
            "lottolab-results")
        .start();
    return true;
  }

  @Override
  public boolean onStopJob(JobParameters params) {
    stopped = true;
    return true;
  }

  public static void notifyAvailable(Context context, ArchiveStore store) {
    if (Build.VERSION.SDK_INT >= 33
        && context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            != PackageManager.PERMISSION_GRANTED) return;
    NotificationManager manager =
        (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
    manager.createNotificationChannel(
        new NotificationChannel(
            "results", "Result updates", NotificationManager.IMPORTANCE_DEFAULT));
    Set<String> ticketKeys = new HashSet<>();
    for (TicketRecord t : store.tickets()) ticketKeys.add(t.entry.game.name() + ":" + t.date);
    for (Game g : Game.values()) {
      List<Draw> draws = store.draws(g, 0);
      for (Draw d : draws) {
        if (d != draws.get(0) && !ticketKeys.contains(d.key())) continue;
        if (!store.preferences().getBoolean("notify_" + g.name(), false)
            && !ticketKeys.contains(d.key())) continue;
        if (!store.preferences().getBoolean("notifications", false)) continue;
        String key = "notified_" + d.key();
        String fingerprint = ArchiveStore.numbers(d.numbers) + ":" + d.bonus + ":" + d.powerBall;
        if (fingerprint.equals(store.preferences().getString(key, ""))) continue;
        Intent launch =
            new Intent(context, MainActivity.class)
                .putExtra("game", g.name())
                .putExtra("date", d.date.toString());
        PendingIntent pi =
            PendingIntent.getActivity(
                context,
                d.key().hashCode(),
                launch,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String title = g.shortName + " · " + d.date;
        String message =
            (d.verification.equals("feed") ? "New feed result" : "New imported result")
                + ". Tap to check your numbers; confirm with the official result.";
        manager.notify(
            d.key().hashCode(),
            new Notification.Builder(context, "results")
                .setSmallIcon(za.co.lottolab.studio.R.drawable.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build());
        store.preferences().edit().putString(key, fingerprint).apply();
      }
    }
  }
}
