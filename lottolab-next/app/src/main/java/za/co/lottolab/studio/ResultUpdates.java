package za.co.lottolab.studio;

import android.app.*;
import android.app.job.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.*;

public final class ResultUpdates extends JobService {
  private volatile boolean stopped;

  public static void schedule(Context c) {
    ArchiveStore store = new ArchiveStore(c);
    boolean enabled =
        !store.preferences().getString("feed_url", "").isEmpty()
            && store.preferences().getBoolean("background_updates", false);
    JobScheduler jobs = (JobScheduler) c.getSystemService(JOB_SCHEDULER_SERVICE);
    if (enabled)
      jobs.schedule(
          new JobInfo.Builder(2202, new ComponentName(c, ResultUpdates.class))
              .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
              .setPeriodic(6 * 60 * 60 * 1000L)
              .setPersisted(true)
              .build());
    else jobs.cancel(2202);
    store.close();
  }

  @Override
  public boolean onStartJob(JobParameters params) {
    stopped = false;
    new Thread(
            () -> {
              boolean retry = false;
              ArchiveStore store = new ArchiveStore(this);
              try {
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
                retry = true;
                store.preferences().edit().putString("sync_error", e.getMessage()).apply();
              } finally {
                store.close();
                if (!stopped) jobFinished(params, retry);
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
