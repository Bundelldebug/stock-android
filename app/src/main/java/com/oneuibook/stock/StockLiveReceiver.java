package com.oneuibook.stock;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Handles Live Activity / Now Bar notification quick actions (Refresh & Dismiss). */
public final class StockLiveReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) return;
        Market.init(context);
        String act = intent.getAction();
        if (StockLiveNotification.ACTION_DISMISS.equals(act)) {
            StockLiveNotification.pinLive(context, "");
        } else if (StockLiveNotification.ACTION_REFRESH.equals(act)) {
            PendingResult pr = goAsync();
            Market.refreshAll(context, () -> {
                StockLiveNotification.updateLive(context);
                if (pr != null) try { pr.finish(); } catch (Throwable ignored) {}
            });
        }
    }
}
