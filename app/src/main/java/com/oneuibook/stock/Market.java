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

/** Watchlist, market indices, portfolio holdings, ticker catalog/search, and live Yahoo Finance chart fetcher. */
final class Market {
    private Market() {}

    static final class Quote {
        String symbol, name, market, currency;
        double price, prevClose;
        double open, dayHigh, dayLow, week52High, week52Low;
        long volume;
        double[] series;
        long[] timestamps;
        long[] volumes;
        double shares, avgCost;
        double targetPrice;
        boolean alertAbove = true;
        boolean alertEnabled;

        Quote(String sym, String nm, String mkt, String cur, double px, double prev, double[] pts) {
            symbol = sym; name = nm; market = mkt; currency = cur;
            price = px; prevClose = prev;
            series = (pts != null && pts.length >= 2) ? pts : synthesizeSeries(prev, px, sym.hashCode());
            fillDerivedDefaults();
        }

        void fillDerivedDefaults() {
            if (series == null || series.length < 2) series = synthesizeSeries(prevClose, price, symbol.hashCode());
            double mn = series[0], mx = series[0];
            for (double v : series) { if (v < mn) mn = v; if (v > mx) mx = v; }
            if (open <= 0) open = series[0];
            if (dayHigh <= 0) dayHigh = Math.max(mx, price);
            if (dayLow <= 0) dayLow = Math.min(mn, price);
            if (week52High <= 0) week52High = dayHigh * 1.18;
            if (week52Low <= 0) week52Low = dayLow * 0.78;
            if (volume <= 0) volume = 1_200_000L + (Math.abs(symbol.hashCode()) % 9_500_000L);
            if (timestamps == null || timestamps.length != series.length) {
                timestamps = new long[series.length];
                long nowSec = System.currentTimeMillis() / 1000L;
                long step = "1d".equals(chartRange) ? 900L : ("5d".equals(chartRange) ? 3600L : 86400L);
                for (int i = 0; i < series.length; i++) {
                    timestamps[i] = nowSec - (long) (series.length - 1 - i) * step;
                }
            }
            if (volumes == null || volumes.length != series.length) {
                volumes = new long[series.length];
                int h = Math.abs(symbol.hashCode());
                for (int i = 0; i < series.length; i++) {
                    double wave = 0.45 + 0.55 * Math.abs(Math.sin((i + 1) * 0.73 + (h % 13)));
                    volumes[i] = Math.round(volume * wave / Math.max(1, series.length));
                }
            }
        }

        double change() { return price - prevClose; }
        double changePct() { return prevClose > 0 ? (price - prevClose) / prevClose * 100.0 : 0.0; }
    }

    static final class CatalogItem {
        final String symbol, name, market, currency, category;
        CatalogItem(String s, String n, String m, String c, String cat) {
            symbol = s; name = n; market = m; currency = c; category = cat;
        }
    }

    interface Listener { void onMarketUpdated(); }
    interface SearchCb { void onResults(List<CatalogItem> results); }

    static final ExecutorService EXEC = Executors.newCachedThreadPool();
    static final Handler MAIN = new Handler(Looper.getMainLooper());
    static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    static final List<Quote> indices = new CopyOnWriteArrayList<>();
    static final List<Quote> watch = new CopyOnWriteArrayList<>();
    static String selectedSymbol = "005930.KS";
    static String liveSymbol = "";
    static String chartRange = "1mo"; // 1d, 5d, 1mo, 3mo, 1y, 5y
    static boolean krColors = true;

    static final CatalogItem[] CATALOG = {
            // KR popular
            new CatalogItem("005930.KS", "삼성전자", "KOSPI", "KRW", "국내"),
            new CatalogItem("000660.KS", "SK하이닉스", "KOSPI", "KRW", "국내"),
            new CatalogItem("035420.KS", "NAVER", "KOSPI", "KRW", "국내"),
            new CatalogItem("035720.KS", "카카오", "KOSPI", "KRW", "국내"),
            new CatalogItem("005380.KS", "현대차", "KOSPI", "KRW", "국내"),
            new CatalogItem("000270.KS", "기아", "KOSPI", "KRW", "국내"),
            new CatalogItem("373220.KS", "LG에너지솔루션", "KOSPI", "KRW", "국내"),
            new CatalogItem("068270.KS", "셀트리온", "KOSPI", "KRW", "국내"),
            new CatalogItem("005490.KS", "POSCO홀딩스", "KOSPI", "KRW", "국내"),
            new CatalogItem("105560.KS", "KB금융", "KOSPI", "KRW", "국내"),
            new CatalogItem("012450.KS", "한화에어로스페이스", "KOSPI", "KRW", "국내"),
            new CatalogItem("034020.KS", "두산에너빌리티", "KOSPI", "KRW", "국내"),
            // US popular
            new CatalogItem("AAPL", "Apple", "NASDAQ", "USD", "미국"),
            new CatalogItem("NVDA", "NVIDIA", "NASDAQ", "USD", "미국"),
            new CatalogItem("MSFT", "Microsoft", "NASDAQ", "USD", "미국"),
            new CatalogItem("GOOGL", "Alphabet", "NASDAQ", "USD", "미국"),
            new CatalogItem("AMZN", "Amazon", "NASDAQ", "USD", "미국"),
            new CatalogItem("META", "Meta", "NASDAQ", "USD", "미국"),
            new CatalogItem("TSLA", "Tesla", "NASDAQ", "USD", "미국"),
            new CatalogItem("AVGO", "Broadcom", "NASDAQ", "USD", "미국"),
            new CatalogItem("AMD", "AMD", "NASDAQ", "USD", "미국"),
            new CatalogItem("TSM", "TSMC", "NYSE", "USD", "미국"),
            new CatalogItem("NFLX", "Netflix", "NASDAQ", "USD", "미국"),
            new CatalogItem("PLTR", "Palantir", "NASDAQ", "USD", "미국"),
            // ETF
            new CatalogItem("SPY", "SPDR S&P 500 ETF", "NYSE", "USD", "ETF"),
            new CatalogItem("QQQ", "Invesco QQQ", "NASDAQ", "USD", "ETF"),
            new CatalogItem("SCHD", "Schwab US Dividend", "NYSE", "USD", "ETF"),
            new CatalogItem("SOXX", "iShares Semiconductor", "NASDAQ", "USD", "ETF"),
            new CatalogItem("069500.KS", "KODEX 200", "KOSPI", "KRW", "ETF"),
            new CatalogItem("360750.KS", "TIGER 미국S&P500", "KOSPI", "KRW", "ETF")
    };

    static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences("stock_android", Context.MODE_PRIVATE);
    }

    static double[] synthesizeSeries(double prev, double px, int seed) {
        int n = 24;
        double[] out = new double[n];
        double base = prev > 0 ? prev : (px > 0 ? px : 100.0);
        double end = px > 0 ? px : base;
        double amp = Math.max(base * 0.012, Math.abs(end - base) * 0.45);
        for (int i = 0; i < n; i++) {
            double t = i / (double) (n - 1);
            double wave = Math.sin(t * Math.PI * 3.2 + (seed % 7)) * amp * 0.45
                    + Math.cos(t * Math.PI * 6.1 + (seed % 11)) * amp * 0.22;
            double v = base + (end - base) * t + wave * Math.sin(t * Math.PI);
            out[i] = Math.max(0.01, v);
        }
        out[n - 1] = end;
        return out;
    }

    static void init(Context c) {
        SharedPreferences p = prefs(c);
        krColors = p.getBoolean("kr_colors", true);
        liveSymbol = p.getString("live_symbol", "");
        if (indices.isEmpty()) {
            indices.add(new Quote("^KS11", "KOSPI", "KRX", "PT", 2528.40, 2504.12, null));
            indices.add(new Quote("^KQ11", "KOSDAQ", "KRX", "PT", 714.65, 718.20, null));
            indices.add(new Quote("^GSPC", "S&P 500", "US", "PT", 5985.30, 5948.70, null));
            indices.add(new Quote("^IXIC", "NASDAQ", "US", "PT", 19120.45, 18980.10, null));
            indices.add(new Quote("KRW=X", "USD/KRW", "FX", "KRW", 1398.50, 1402.10, null));
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
                        o.optString("c", "USD"), o.optDouble("p", 100), o.optDouble("pc", 100), null);
                q.shares = o.optDouble("sh", 0);
                q.avgCost = o.optDouble("ac", 0);
                q.targetPrice = o.optDouble("tp", 0);
                q.alertAbove = o.optBoolean("aa", true);
                q.alertEnabled = o.optBoolean("ae", false);
                watch.add(q);
            }
        } catch (Exception ignored) {}
        if (watch.isEmpty()) {
            Quote s1 = new Quote("005930.KS", "삼성전자", "KOSPI", "KRW", 58400, 57600, null);
            s1.shares = 20; s1.avgCost = 55000;
            Quote s2 = new Quote("000660.KS", "SK하이닉스", "KOSPI", "KRW", 192500, 189000, null);
            s2.shares = 5; s2.avgCost = 178000;
            watch.add(s1);
            watch.add(s2);
            watch.add(new Quote("035420.KS", "NAVER", "KOSPI", "KRW", 198200, 200500, null));
            watch.add(new Quote("005380.KS", "현대차", "KOSPI", "KRW", 224500, 221000, null));
            Quote aapl = new Quote("AAPL", "Apple", "NASDAQ", "USD", 232.80, 229.90, null);
            aapl.shares = 4; aapl.avgCost = 215.0;
            watch.add(aapl);
            watch.add(new Quote("NVDA", "NVIDIA", "NASDAQ", "USD", 146.20, 142.50, null));
            watch.add(new Quote("TSLA", "Tesla", "NASDAQ", "USD", 338.40, 344.10, null));
            watch.add(new Quote("MSFT", "Microsoft", "NASDAQ", "USD", 424.60, 421.30, null));
            saveWatch(c);
        }
    }

    static void saveWatch(Context c) {
        try {
            JSONArray a = new JSONArray();
            for (Quote q : watch) {
                a.put(new JSONObject().put("s", q.symbol).put("n", q.name).put("m", q.market)
                        .put("c", q.currency).put("p", q.price).put("pc", q.prevClose)
                        .put("sh", q.shares).put("ac", q.avgCost)
                        .put("tp", q.targetPrice).put("aa", q.alertAbove).put("ae", q.alertEnabled));
            }
            prefs(c).edit()
                    .putString("watchlist", a.toString())
                    .putBoolean("kr_colors", krColors)
                    .putString("live_symbol", liveSymbol == null ? "" : liveSymbol)
                    .apply();
        } catch (Exception ignored) {}
    }

    static boolean isWatched(String sym) {
        if (sym == null) return false;
        for (Quote q : watch) if (q.symbol.equalsIgnoreCase(sym)) return true;
        return false;
    }

    static boolean addToWatchlist(Context c, String sym, String name, String market, String currency) {
        if (sym == null || sym.trim().isEmpty()) return false;
        String cleanSym = sym.trim().toUpperCase(Locale.ROOT);
        if (isWatched(cleanSym)) return false;
        boolean kr = cleanSym.endsWith(".KS") || cleanSym.endsWith(".KQ");
        String nm = (name == null || name.trim().isEmpty()) ? cleanSym : name.trim();
        String mkt = (market == null || market.isEmpty()) ? (kr ? "KRX" : "US") : market;
        String cur = (currency == null || currency.isEmpty()) ? (kr ? "KRW" : "USD") : currency;
        Quote q = new Quote(cleanSym, nm, mkt, cur, kr ? 50000 : 100, kr ? 50000 : 100, null);
        watch.add(q);
        saveWatch(c);
        refreshSingle(c, q);
        return true;
    }

    static void removeFromWatchlist(Context c, String sym) {
        if (sym == null) return;
        for (int i = 0; i < watch.size(); i++) {
            if (watch.get(i).symbol.equalsIgnoreCase(sym)) {
                watch.remove(i);
                break;
            }
        }
        if (sym.equalsIgnoreCase(liveSymbol)) StockLiveNotification.pinLive(c, "");
        saveWatch(c);
    }

    static List<CatalogItem> searchCatalog(String query) {
        String ql = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        List<CatalogItem> out = new ArrayList<>();
        for (CatalogItem it : CATALOG) {
            if (ql.isEmpty()
                    || it.symbol.toLowerCase(Locale.ROOT).contains(ql)
                    || it.name.toLowerCase(Locale.ROOT).contains(ql)
                    || it.category.toLowerCase(Locale.ROOT).contains(ql)) {
                out.add(it);
            }
        }
        return out;
    }

    /** Queries Yahoo Finance live symbol search and merges with local KR/US catalog. */
    static void searchOnline(String query, SearchCb cb) {
        List<CatalogItem> base = searchCatalog(query);
        if (query == null || query.trim().length() < 1 || "robolectric".equals(android.os.Build.FINGERPRINT)) {
            MAIN.post(() -> cb.onResults(base));
            return;
        }
        EXEC.execute(() -> {
            List<CatalogItem> merged = new ArrayList<>(base);
            try {
                String u = "https://query2.finance.yahoo.com/v1/finance/search?q="
                        + URLEncoder.encode(query.trim(), "UTF-8") + "&quotesCount=10&newsCount=0";
                HttpURLConnection conn = (HttpURLConnection) new URL(u).openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("User-Agent", "Mozilla/5.0");
                if (conn.getResponseCode() == 200) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    try (InputStream is = conn.getInputStream()) {
                        byte[] buf = new byte[4096]; int n;
                        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                    }
                    JSONArray qs = new JSONObject(bos.toString("UTF-8")).optJSONArray("quotes");
                    if (qs != null) {
                        for (int i = 0; i < qs.length(); i++) {
                            JSONObject o = qs.optJSONObject(i);
                            if (o == null) continue;
                            String sym = o.optString("symbol", "");
                            if (sym.isEmpty()) continue;
                            boolean dup = false;
                            for (CatalogItem ex : merged) if (ex.symbol.equalsIgnoreCase(sym)) { dup = true; break; }
                            if (dup) continue;
                            String nm = o.optString("shortname", o.optString("longname", sym));
                            String exch = o.optString("exchDisp", o.optString("exchange", "US"));
                            boolean kr = sym.endsWith(".KS") || sym.endsWith(".KQ");
                            merged.add(new CatalogItem(sym, nm, exch, kr ? "KRW" : "USD", kr ? "국내" : "해외"));
                        }
                    }
                }
            } catch (Throwable ignored) {}
            MAIN.post(() -> cb.onResults(merged));
        });
    }

    static Quote findExact(String sym) {
        if (sym == null) return null;
        for (Quote q : watch) if (q.symbol.equalsIgnoreCase(sym)) return q;
        for (Quote q : indices) if (q.symbol.equalsIgnoreCase(sym)) return q;
        return null;
    }

    static Quote find(String sym) {
        Quote exact = findExact(sym);
        if (exact != null) return exact;
        return watch.isEmpty() ? null : watch.get(0);
    }

    static int deltaColor(double delta) {
        if (Math.abs(delta) < 1e-6) return Ui.text2();
        if (krColors) return delta > 0 ? 0xFFE53935 : 0xFF2563EB;
        return delta > 0 ? 0xFF16A34A : 0xFFE53935;
    }

    static String fmtVal(double val, String currency) {
        if ("KRW".equals(currency)) return String.format(Locale.KOREA, "%,d원", Math.round(val));
        if ("PT".equals(currency)) return String.format(Locale.US, "%,.2f", val);
        if ("JPY".equals(currency)) return String.format(Locale.US, "¥%,.0f", val);
        if ("EUR".equals(currency)) return String.format(Locale.US, "€%,.2f", val);
        if ("GBP".equals(currency)) return String.format(Locale.US, "£%,.2f", val);
        if ("HKD".equals(currency)) return String.format(Locale.US, "HK$%,.2f", val);
        if (currency != null && !currency.isEmpty() && !"USD".equals(currency)) {
            return String.format(Locale.US, "%s %,.2f", currency, val);
        }
        return String.format(Locale.US, "$%,.2f", val);
    }

    static String fmtPrice(Quote q) { return fmtVal(q.price, q.currency); }

    static String fmtDelta(Quote q) {
        double d = q.change();
        String sgn = d >= 0 ? "+" : "-";
        double a = Math.abs(d);
        return sgn + fmtVal(a, q.currency);
    }

    static String fmtPct(double pct) {
        return String.format(Locale.US, "%s%.2f%%", pct >= 0 ? "+" : "", pct);
    }

    static String fmtVolume(long vol) {
        if (vol >= 100_000_000L) return String.format(Locale.US, "%.1f억", vol / 100_000_000.0);
        if (vol >= 10_000L) return String.format(Locale.US, "%.1f만", vol / 10_000.0);
        return String.format(Locale.KOREA, "%,d", vol);
    }

    static double usdKrw() {
        for (Quote q : indices) if ("KRW=X".equals(q.symbol)) return q.price;
        return 1400.0;
    }

    static double toKrw(Quote q, double amount) {
        return "USD".equals(q.currency) ? amount * usdKrw() : amount;
    }

    static void refreshSingle(Context c, Quote q) {
        if ("robolectric".equals(android.os.Build.FINGERPRINT) || q == null) return;
        Context app = c.getApplicationContext();
        EXEC.execute(() -> {
            if (fetchInto(q, chartRange)) {
                saveWatch(app);
                StockLiveNotification.checkTargets(app);
                if (q.symbol.equalsIgnoreCase(liveSymbol)) StockLiveNotification.updateLive(app);
                MAIN.post(() -> { for (Listener l : listeners) l.onMarketUpdated(); });
            }
        });
    }

    /** Refreshes quotes asynchronously from Yahoo Finance chart API. */
    static void refreshAll(Context c) { refreshAll(c, null); }

    static void refreshAll(Context c, Runnable onDone) {
        if ("robolectric".equals(android.os.Build.FINGERPRINT)) {
            if (onDone != null) MAIN.post(onDone);
            return;
        }
        Context app = c.getApplicationContext();
        EXEC.execute(() -> {
            try {
                List<Quote> all = new ArrayList<>(indices);
                all.addAll(watch);
                boolean any = false;
                for (Quote q : all) {
                    if (fetchInto(q, chartRange)) any = true;
                }
                if (any) {
                    saveWatch(app);
                    StockLiveNotification.checkTargets(app);
                    StockLiveNotification.updateLive(app);
                    MAIN.post(() -> { for (Listener l : listeners) l.onMarketUpdated(); });
                }
            } finally {
                if (onDone != null) MAIN.post(onDone);
            }
        });
    }

    static boolean fetchInto(Quote q, String range) {
        try {
            String interval;
            if ("1d".equals(range)) interval = "5m";
            else if ("5d".equals(range)) interval = "30m";
            else if ("5y".equals(range)) interval = "1wk";
            else interval = "1d";
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
            String metaCur = meta.optString("currency", "");
            if (!metaCur.isEmpty() && !"PT".equals(q.currency)) q.currency = metaCur;
            double newPrice = meta.optDouble("regularMarketPrice", q.price);
            double prevDay = meta.optDouble("regularMarketPreviousClose",
                    meta.optDouble("previousClose", Double.NaN));
            if (Double.isNaN(prevDay) || prevDay <= 0) {
                if ("1d".equals(range)) prevDay = meta.optDouble("chartPreviousClose", q.prevClose);
                else prevDay = q.prevClose;
            }
            q.price = newPrice;
            q.dayHigh = meta.optDouble("regularMarketDayHigh", q.dayHigh);
            q.dayLow = meta.optDouble("regularMarketDayLow", q.dayLow);
            q.week52High = meta.optDouble("fiftyTwoWeekHigh", q.week52High);
            q.week52Low = meta.optDouble("fiftyTwoWeekLow", q.week52Low);
            q.volume = meta.optLong("regularMarketVolume", q.volume);

            JSONArray tsArr = res.optJSONArray("timestamp");
            JSONObject quoteObj = res.getJSONObject("indicators").getJSONArray("quote").getJSONObject(0);
            JSONArray closes = quoteObj.optJSONArray("close");
            JSONArray opens = quoteObj.optJSONArray("open");
            JSONArray vols = quoteObj.optJSONArray("volume");
            if (opens != null && opens.length() > 0 && !opens.isNull(opens.length() - 1)) {
                q.open = opens.optDouble(opens.length() - 1, q.open);
            }
            if (closes != null) {
                List<Double> pts = new ArrayList<>();
                List<Long> tList = new ArrayList<>();
                List<Long> vList = new ArrayList<>();
                for (int i = 0; i < closes.length(); i++) {
                    if (!closes.isNull(i)) {
                        pts.add(closes.optDouble(i));
                        tList.add(tsArr != null && i < tsArr.length() ? tsArr.optLong(i, 0L) : 0L);
                        vList.add(vols != null && i < vols.length() && !vols.isNull(i) ? vols.optLong(i, 0L) : 0L);
                    }
                }
                if (pts.size() >= 2) {
                    if ("1d".equals(interval) && (Double.isNaN(meta.optDouble("regularMarketPreviousClose", Double.NaN))
                            && Double.isNaN(meta.optDouble("previousClose", Double.NaN)))) {
                        prevDay = pts.get(pts.size() - 2);
                    }
                    double[] nextSeries = new double[pts.size()];
                    long[] nextTs = new long[pts.size()];
                    long[] nextVols = new long[pts.size()];
                    for (int i = 0; i < pts.size(); i++) {
                        nextSeries[i] = pts.get(i);
                        nextTs[i] = tList.get(i);
                        nextVols[i] = vList.get(i);
                    }
                    q.timestamps = nextTs;
                    q.volumes = nextVols;
                    q.series = nextSeries;
                }
            }
            if (prevDay > 0) q.prevClose = prevDay;
            q.fillDerivedDefaults();
            return true;
        } catch (Throwable ignored) { return false; }
    }
}
