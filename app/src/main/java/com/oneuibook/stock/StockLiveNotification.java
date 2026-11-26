package com.oneuibook.stock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.os.Bundle;

import java.lang.reflect.Method;
import java.util.Locale;

/**
 * Live Activity / Now Bar ongoing notification & price-target alert controller for Stock Android:
 * - Promoted ongoing Live Update notification (Android 16+ EXTRA_REQUEST_PROMOTED_ONGOING + setShortCriticalText)
 *   compatible with Now Bar lockscreen/status-bar live pill & rich sparkline card.
 * - Quick actions: Refresh quote & Dismiss live bar (handled by StockLiveReceiver).
 * - Per-ticker price target alerts delivered as high-priority live notifications.
 */
final class StockLiveNotification {
    private StockLiveNotification() {}

    static final String CH_LIVE = "stock_live_activity";
    static final String CH_ALERT = "stock_price_alerts";
    static final int ID_LIVE = 4101;
    static final int ID_ALERT_BASE = 4200;

    static final String ACTION_REFRESH = "com.oneuibook.stock.ACTION_LIVE_REFRESH";
    static final String ACTION_DISMISS = "com.oneuibook.stock.ACTION_LIVE_DISMISS";

    static void ensureChannels(Context c) {
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        if (nm.getNotificationChannel(CH_LIVE) == null) {
            NotificationChannel ch = new NotificationChannel(CH_LIVE, "실시간 시세 라이브 바", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("상태바와 잠금화면 실시간 바에 선택한 종목의 시세를 표시합니다.");
            ch.setSound(null, null);
            ch.enableVibration(false);
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
        if (nm.getNotificationChannel(CH_ALERT) == null) {
            NotificationChannel ch = new NotificationChannel(CH_ALERT, "목표가 도달 알림", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("설정한 목표가에 도달하면 라이브 알림으로 알려줍니다.");
            ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            nm.createNotificationChannel(ch);
        }
    }

    static boolean isLiveEnabled(Context c) {
        return !Market.liveSymbol.isEmpty();
    }

    static void pinLive(Context c, String symbol) {
        Market.liveSymbol = symbol == null ? "" : symbol;
        Market.saveWatch(c);
        if (Market.liveSymbol.isEmpty()) cancelLive(c);
        else updateLive(c);
    }

    static void cancelLive(Context c) {
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.cancel(ID_LIVE);
        } catch (Throwable ignored) {}
    }

    static void updateLive(Context c) {
        if (Market.liveSymbol == null || Market.liveSymbol.isEmpty()) {
            cancelLive(c);
            return;
        }
        Market.Quote q = Market.findExact(Market.liveSymbol);
        if (q == null) return;
        ensureChannels(c);
        Notification n = buildLiveNotification(c, q, null);
        try {
            NotificationManager nm = c.getSystemService(NotificationManager.class);
            if (nm != null) nm.notify(ID_LIVE, n);
        } catch (Throwable ignored) {}
    }

    /** Evaluates user price targets across watchlist and triggers live alert notifications when crossed. */
    static void checkTargets(Context c) {
        if (c == null) return;
        ensureChannels(c);
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        if (nm == null) return;
        boolean dirty = false;
        int idx = 0;
        for (Market.Quote q : Market.watch) {
            idx++;
            if (!q.alertEnabled || q.targetPrice <= 0 || q.price <= 0) continue;
            boolean hit = q.alertAbove ? (q.price >= q.targetPrice) : (q.price <= q.targetPrice);
            if (hit) {
                q.alertEnabled = false;
                dirty = true;
                String dir = q.alertAbove ? "이상 도달" : "이하 도달";
                String note = "목표가 " + Market.fmtVal(q.targetPrice, q.currency) + " " + dir;
                Notification n = buildLiveNotification(c, q, note);
                try { nm.notify(ID_ALERT_BASE + idx, n); } catch (Throwable ignored) {}
            }
        }
        if (dirty) Market.saveWatch(c);
    }

    /** Builds an Android Live Updates / Now Bar compatible Live Activity notification. */
    static Notification buildLiveNotification(Context c, Market.Quote q, String alertNote) {
        boolean isAlert = alertNote != null && !alertNote.isEmpty();
        int col = Market.deltaColor(q.change());
        String arrow = q.change() >= 0 ? "▲" : "▼";
        String pctStr = Market.fmtPct(q.changePct());
        String priceStr = Market.fmtPrice(q);
        String shortChip = compactName(q.name) + " " + arrow + String.format(Locale.US, "%.1f%%", Math.abs(q.changePct()));

        Intent openIn = new Intent(c, MainActivity.class);
        openIn.putExtra("open_symbol", q.symbol);
        openIn.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent openPi = PendingIntent.getActivity(c, 10, openIn,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent refIn = new Intent(c, StockLiveReceiver.class).setAction(ACTION_REFRESH);
        PendingIntent refPi = PendingIntent.getBroadcast(c, 11, refIn,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent disIn = new Intent(c, StockLiveReceiver.class).setAction(ACTION_DISMISS);
        PendingIntent disPi = PendingIntent.getBroadcast(c, 12, disIn,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = q.name + "  " + priceStr;
        String body = isAlert
                ? (alertNote + " · 현재 " + priceStr + " (" + pctStr + ")")
                : (arrow + " " + Market.fmtDelta(q) + " (" + pctStr + ")  ·  고 "
                    + Market.fmtVal(q.dayHigh > 0 ? q.dayHigh : q.price, q.currency)
                    + " / 저 " + Market.fmtVal(q.dayLow > 0 ? q.dayLow : q.price, q.currency));

        Notification.Builder b = new Notification.Builder(c, isAlert ? CH_ALERT : CH_LIVE)
                .setSmallIcon(android.R.drawable.ic_menu_sort_by_size)
                .setContentTitle(title)
                .setContentText(body)
                .setSubText(isAlert ? "목표가 알림 · " + q.symbol : "실시간 시세 · " + q.symbol)
                .setContentIntent(openPi)
                .setColor(col)
                .setColorized(true)
                .setOngoing(!isAlert)
                .setAutoCancel(isAlert)
                .setOnlyAlertOnce(!isAlert)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setCategory(isAlert ? Notification.CATEGORY_ALARM : Notification.CATEGORY_STATUS)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis())
                .setLargeIcon(renderSparkBadge(q, col))
                .setStyle(new Notification.BigTextStyle().bigText(body));

        if (!isAlert) {
            b.addAction(new Notification.Action.Builder(null, "새로고침", refPi).build());
            b.addAction(new Notification.Action.Builder(null, "실시간 바 끄기", disPi).build());
        }

        // Android 16+ Live Updates & One UI Now Bar promotion metadata
        Bundle extras = new Bundle();
        extras.putBoolean("android.requestPromotedOngoing", true);
        extras.putString("android.shortCriticalText", shortChip);
        extras.putString("stock.live.symbol", q.symbol);
        extras.putString("stock.live.chip", shortChip);
        // Samsung Now Bar / Ongoing Activity compatibility keys
        extras.putBoolean("android.ongoingActivity", true);
        extras.putString("ongoing_primary_text", q.name + " " + priceStr);
        extras.putString("ongoing_secondary_text", arrow + " " + pctStr);
        extras.putInt("ongoing_chip_bg_color", col);
        b.addExtras(extras);

        // Reflectively invoke API 36+ setShortCriticalText when running on Android 16+ / One UI 8+
        try {
            Method m = Notification.Builder.class.getMethod("setShortCriticalText", String.class);
            m.invoke(b, shortChip);
        } catch (Throwable ignored) {}

        return b.build();
    }

    private static String compactName(String name) {
        if (name == null || name.isEmpty()) return "주식";
        return name.length() > 6 ? name.substring(0, 6) : name;
    }

    /** Renders a crisp rounded mini-sparkline card bitmap as notification largeIcon. */
    private static Bitmap renderSparkBadge(Market.Quote q, int col) {
        int w = 168, h = 112;
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF141822);
        cv.drawRoundRect(new RectF(0, 0, w, h), 24, 24, p);

        double[] pts = q.series;
        if (pts != null && pts.length >= 2) {
            double min = pts[0], max = pts[0];
            for (double v : pts) { if (v < min) min = v; if (v > max) max = v; }
            double span = Math.max(1e-6, max - min);
            Path line = new Path(), fill = new Path();
            float padX = 14, padT = 18, padB = 34;
            for (int i = 0; i < pts.length; i++) {
                float x = padX + (w - padX * 2f) * i / (pts.length - 1);
                float y = (h - padB) - (float) ((pts[i] - min) / span) * (h - padT - padB);
                if (i == 0) { line.moveTo(x, y); fill.moveTo(x, h - padB); fill.lineTo(x, y); }
                else { line.lineTo(x, y); fill.lineTo(x, y); }
            }
            fill.lineTo(w - padX, h - padB);
            fill.close();
            p.setShader(new LinearGradient(0, padT, 0, h - padB, (col & 0x00FFFFFF) | 0x55000000, 0x00000000, Shader.TileMode.CLAMP));
            cv.drawPath(fill, p);
            p.setShader(null);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4f);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(col);
            cv.drawPath(line, p);
        }

        p.setStyle(Paint.Style.FILL);
        p.setColor(col);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(22f);
        p.setFakeBoldText(true);
        cv.drawText(Market.fmtPct(q.changePct()), w / 2f, h - 10f, p);
        return bmp;
    }
}
