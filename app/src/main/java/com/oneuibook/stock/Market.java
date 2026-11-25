package com.oneuibook.stock;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Watchlist, market indices, portfolio holdings, and live Yahoo Finance chart quote fetcher. */
final class Market {
    private Market() {}

    static final class Quote {
        String symbol, name, market, currency;
        double price, prevClose;
        double[] series;
        double shares, avgCost; // portfolio holding

        Quote(String sym, String nm, String mkt, String cur, double px, double prev, double[] pts) {
            symbol = sym; name = nm; market = mkt; currency = cur; price = px; prevClose = prev; series = pts;
        }
        double change() { return price - prevClose; }
        double changePct() { return prevClose > 0 ? (price - prevClose) / prevClose * 100.0 : 0.0; }
    }

    interface Listener { void onMarketUpdated(); }

    static final ExecutorService EXEC = Executors.newCachedThreadPool();
    static final Handler MAIN = new Handler(Looper.getMainLooper());
    static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    static final List<Quote> indices = new ArrayList<>();
    static final List<Quote> watch = new ArrayList<>();
    static String selectedSymbol = "005930.KS";
    static String chartRange = "1mo"; // 1d, 5d, 1mo, 1y
    static boolean krColors = true;   // true: red up / blue down; false: green up / red down

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("stock_android", Context.MODE_PRIVATE);
    }

    static void init(Context c) {
        SharedPreferences p = prefs(c);
        krColors = p.getBoolean("kr_colors", true);
        if (indices.isEmpty()) {
            indices.add(new Quote("^KS11", "KOSPI", "KRX", "PT", 2528.40, 2504.12, new double[]{2480, 2492, 2488, 2504, 2515, 2528.4}));
            indices.add(new Quote("^KQ11", "KOSDAQ", "KRX", "PT", 714.65, 718.20, new double[]{724, 721, 719, 718, 716, 714.65}));
            indices.add(new Quote("^GSPC", "S&P 500", "US", "PT", 5985.30, 5948.70, new double[]{5890, 5912, 5935, 5948, 5962, 5985.3}));
            indices.add(new Quote("^IXIC", "NASDAQ", "US", "PT", 19120.45, 18980.10, new double[]{18750, 18820, 18910, 18980, 19040, 19120.45}));
            indices.add(new Quote("KRW=X", "USD/KRW", "FX", "KRW", 1398.50, 1402.10, new double[]{1408, 1405, 1403, 1402, 1400, 1398.5}));
        }
        if (watch.isEmpty()) {
            loadWatch(c);
        }
    }

    static void loadWatch(Context c) {
        watch.clear();
        try {
            JSONArray a = new JSONArray(prefs(c).getString("watchlist", "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Quote q = new Quote(o.getString("s"), o.getString("n"), o.optString("m", "US"),
                        o.optString("c", "USD"), o.optDouble("p", 100), o.optDouble("pc", 100),
                        new double[]{o.optDouble("pc", 100), o.optDouble("p", 100)});
                q.shares = o.optDouble("sh", 0);
                q.avgCost = o.optDouble("ac", 0);
                watch.add(q);
            }
        } catch (Exception ignored) {}
        if (watch.isEmpty()) {
            Quote s1 = new Quote("005930.KS", "삼성전자", "KOSPI", "KRW", 58400, 57600, new double[]{56200, 56800, 57100, 57600, 58000, 58400});
            s1.shares = 20; s1.avgCost = 55000;
            Quote s2 = new Quote("000660.KS", "SK하이닉스", "KOSPI", "KRW", 192500, 189000, new double[]{183000, 186000, 188500, 189000, 191000, 192500});
            s2.shares = 5; s2.avgCost = 178000;
            watch.add(s1);
            watch.add(s2);
            watch.add(new Quote("035420.KS", "NAVER", "KOSPI", "KRW", 198200, 200500, new double[]{203000, 202000, 201000, 200500, 199000, 198200}));
            watch.add(new Quote("005380.KS", "현대차", "KOSPI", "KRW", 224500, 221000, new double[]{217000, 219000, 220500, 221000, 223000, 224500}));
            Quote aapl = new Quote("AAPL", "Apple", "NASDAQ", "USD", 232.80, 229.90, new double[]{225.1, 226.8, 228.4, 229.9, 231.2, 232.8});
            aapl.shares = 4; aapl.avgCost = 215.0;
            watch.add(aapl);
            watch.add(new Quote("NVDA", "NVIDIA", "NASDAQ", "USD", 146.20, 142.50, new double[]{136.0, 138.5, 141.0, 142.5, 144.8, 146.2}));
            watch.add(new Quote("TSLA", "Tesla", "NASDAQ", "USD", 338.40, 344.10, new double[]{351.0, 348.2, 345.0, 344.1, 340.5, 338.4}));
            watch.add(new Quote("MSFT", "Microsoft", "NASDAQ", "USD", 424.60, 421.30, new double[]{416.0, 418.2, 419.5, 421.3, 423.0, 424.6}));
            saveWatch(c);
        }
    }

    static void saveWatch(Context c) {
        try {
            JSONArray a = new JSONArray();
            for (Quote q : watch) {
                a.put(new JSONObject().put("s", q.symbol).put("n", q.name).put("m", q.market)
                        .put("c", q.currency).put("p", q.price).put("pc", q.prevClose)
                        .put("sh", q.shares).put("ac", q.avgCost));
            }
            prefs(c).edit().putString("watchlist", a.toString()).putBoolean("kr_colors", krColors).apply();
        } catch (Exception ignored) {}
    }

    static Quote find(String sym) {
        for (Quote q : watch) if (q.symbol.equalsIgnoreCase(sym)) return q;
        for (Quote q : indices) if (q.symbol.equalsIgnoreCase(sym)) return q;
        return watch.isEmpty() ? null : watch.get(0);
    }

    static int deltaColor(double delta) {
        if (Math.abs(delta) < 1e-6) return Ui.text2();
        if (krColors) return delta > 0 ? 0xFFE53935 : 0xFF2563EB;
        return delta > 0 ? 0xFF16A34A : 0xFFE53935;
    }

    static String fmtPrice(Quote q) {
        if ("KRW".equals(q.currency)) return String.format(Locale.KOREA, "%,d원", Math.round(q.price));
        if ("PT".equals(q.currency)) return String.format(Locale.US, "%,.2f", q.price);
        return String.format(Locale.US, "$%,.2f", q.price);
    }

    static String fmtPct(double pct) {
        return String.format(Locale.US, "%s%.2f%%", pct >= 0 ? "+" : "", pct);
    }

    static double usdKrw() {
        for (Quote q : indices) if ("KRW=X".equals(q.symbol)) return q.price;
        return 1400.0;
    }

    static double toKrw(Quote q, double amount) {
        return "USD".equals(q.currency) ? amount * usdKrw() : amount;
    }

    /** Refreshes quotes asynchronously from Yahoo Finance chart API. */
    static void refreshAll(Context c) {
        if ("robolectric".equals(android.os.Build.FINGERPRINT)) return;
        Context app = c.getApplicationContext();
        EXEC.execute(() -> {
            List<Quote> all = new ArrayList<>(indices);
            all.addAll(watch);
            boolean any = false;
            for (Quote q : all) {
                if (fetchInto(q, chartRange)) any = true;
            }
            if (any) {
                saveWatch(app);
                MAIN.post(() -> { for (Listener l : listeners) l.onMarketUpdated(); });
            }
        });
    }

    static boolean fetchInto(Quote q, String range) {
        try {
            String interval = "1d".equals(range) ? "15m" : ("5d".equals(range) ? "60m" : "1d");
            String url = "https://query1.finance.yahoo.com/v8/finance/chart/" + URLEncoder.encode(q.symbol, "UTF-8")
                    + "?range=" + range + "&interval=" + interval;
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");
            if (conn.getResponseCode() != 200) return false;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (InputStream is = conn.getInputStream()) {
                byte[] buf = new byte[4096]; int n;
                while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            }
            JSONObject res = new JSONObject(bos.toString("UTF-8")).getJSONObject("chart").getJSONArray("result").getJSONObject(0);
            JSONObject meta = res.getJSONObject("meta");
            double px = meta.optDouble("regularMarketPrice", q.price);
            double pc = meta.optDouble("chartPreviousClose", meta.optDouble("previousClose", q.prevClose));
            q.price = px;
            q.prevClose = pc;
            JSONArray closes = res.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0).optJSONArray("close");
            if (closes != null) {
                List<Double> pts = new ArrayList<>();
                for (int i = 0; i < closes.length(); i++) {
                    if (!closes.isNull(i)) pts.add(closes.optDouble(i));
                }
                if (pts.size() >= 2) {
                    q.series = new double[pts.size()];
                    for (int i = 0; i < pts.size(); i++) q.series[i] = pts.get(i);
                }
            }
            return true;
        } catch (Throwable ignored) { return false; }
    }
}
