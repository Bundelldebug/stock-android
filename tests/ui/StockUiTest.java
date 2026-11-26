package com.oneuibook.stock;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 34)
public class StockUiTest {
    private File outDir;

    @Before public void setup() {
        outDir = new File(System.getProperty("stock.output", "."));
        outDir.mkdirs();
    }

    private void saveShot(View v, int w, int h, String name) throws Exception {
        v.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY));
        v.layout(0, 0, w, h);
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        v.draw(c);
        try (FileOutputStream fos = new FileOutputStream(new File(outDir, name))) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        bmp.recycle();
    }

    @Test public void launcherIconRenders() throws Exception {
        Context ctx = RuntimeEnvironment.getApplication();
        Drawable icon = ctx.getDrawable(R.mipmap.ic_launcher);
        assertNotNull(icon);
        Bitmap bmp = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        android.graphics.Path sq = new android.graphics.Path();
        float r = 122f, cx = 128f, cy = 128f, n = 2.76f;
        for (int i = 0; i <= 120; i++) {
            double a = i * Math.PI * 2.0 / 120.0;
            double ca = Math.cos(a), sa = Math.sin(a);
            float x = cx + (float) (Math.signum(ca) * Math.pow(Math.abs(ca), 2.0 / n) * r);
            float y = cy + (float) (Math.signum(sa) * Math.pow(Math.abs(sa), 2.0 / n) * r);
            if (i == 0) sq.moveTo(x, y); else sq.lineTo(x, y);
        }
        sq.close();
        c.clipPath(sq);
        icon.setBounds(0, 0, 256, 256);
        icon.draw(c);
        try (FileOutputStream fos = new FileOutputStream(new File(outDir, "icon.png"))) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos);
        }
        bmp.recycle();
    }

    @Test public void watchlistAddCatalogAndLiveActivityNowBarNotificationWork() {
        Context ctx = RuntimeEnvironment.getApplication();
        Market.init(ctx);
        assertTrue(Market.searchCatalog("카카오").size() >= 1);
        assertTrue(Market.searchCatalog("NVDA").size() >= 1);
        assertTrue(Market.addToWatchlist(ctx, "GOOGL", "Alphabet", "NASDAQ", "USD"));
        assertTrue(Market.isWatched("GOOGL"));
        Market.Quote q = Market.findExact("GOOGL");
        assertNotNull(q);
        assertTrue(q.series.length >= 20);
        assertEquals(q.series.length, q.timestamps.length);
        assertEquals(q.series.length, q.volumes.length);

        android.app.Notification n = StockLiveNotification.buildLiveNotification(ctx, q, null);
        assertNotNull(n);
        assertTrue((n.flags & android.app.Notification.FLAG_ONGOING_EVENT) != 0);
        assertTrue(n.extras.getBoolean("android.requestPromotedOngoing", false));
        assertTrue(n.extras.getBoolean("android.ongoingActivity", false));
        assertTrue(n.extras.getString("stock.live.chip", "").contains("Alphab"));
        Market.removeFromWatchlist(ctx, "GOOGL");
    }

    @Test @Config(qualifiers = "ko-rKR-w412dp-h915dp-xxhdpi")
    public void phoneScreens() throws Exception {
        ActivityController<MainActivity> ctrl = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity act = ctrl.get();
        assertEquals("주식", act.getString(R.string.app_name));
        assertEquals("9.5.4", MainActivity.VERSION);
        saveShot(act.root, 1236, 2745, "phone-watch.png");
        act.dock.select(1, false); act.onTab(1, false);
        org.robolectric.shadows.ShadowLooper.idleMainLooper(350, java.util.concurrent.TimeUnit.MILLISECONDS);
        saveShot(act.root, 1236, 2745, "phone-chart.png");
        act.onBackPressed();
        assertEquals(0, act.tab);
        assertFalse(act.isFinishing());
        act.dock.select(2, false); act.onTab(2, false);
        org.robolectric.shadows.ShadowLooper.idleMainLooper(350, java.util.concurrent.TimeUnit.MILLISECONDS);
        saveShot(act.root, 1236, 2745, "phone-portfolio.png");
    }

    @Test @Config(qualifiers = "ko-rKR-w840dp-h600dp-xhdpi")
    public void tabletSidebarNoOverlap() throws Exception {
        ActivityController<MainActivity> ctrl = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity act = ctrl.get();
        assertTrue(act.tabletUi());
        act.openSidebar(false);
        saveShot(act.root, 1680, 1200, "tablet-sidebar.png");
        assertTrue(act.dock.circle.right <= act.header.pill.left + 2f);
    }
}
