package com.oneuibook.stock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Clean, zero-slop Watchlist, Detailed Interactive Chart, Portfolio, and Live Search pages (v9.5.4):
 * - Interactive scrubbable chart with crosshair, horizontal gridlines, right price axis, volume bars,
 *   period baseline, bottom timestamps, 6 timeframes (1D/1W/1M/3M/1Y/ALL), and 6-cell key metrics card.
 * - Instant KR/US popular catalog + Yahoo Finance online search for adding watchlist tickers.
 * - Per-ticker Live Activity Bar pinning & target-price live alert configuration.
 */
final class Pages {
    private Pages() {}

    static LinearLayout cardBox(Context c) {
        LinearLayout card = new LinearLayout(c);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = Ui.round(Ui.card(), Ui.cardRadius());
        if (Ui.gStyle) bg.setStroke(Ui.dp(0.8f), Ui.gStroke());
        card.setBackground(bg);
        card.setClipToOutline(true);
        return card;
    }

    static LinearLayout.LayoutParams cardLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Ui.dp(16), Ui.dp(8), Ui.dp(16), Ui.dp(8));
        return lp;
    }

    // ---- 1. Watchlist Page ----
    static final class WatchPage extends Page {
        WatchPage(MainActivity m) {
            super(m, m.getString(R.string.app_name), true);
            setGrouped(true);
            headerIcons = new int[]{Icons.PLUS, Icons.REFRESH};
        }

        @Override void build() {
            Context c = getContext();

            // Compact Market Indices Strip (clean, crisp typography + inline sparkline)
            LinearLayout rail = Widgets.rail(content, 10);
            for (Market.Quote q : Market.indices) {
                LinearLayout card = cardBox(c);
                card.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
                int col = Market.deltaColor(q.change());

                LinearLayout top = new LinearLayout(c);
                top.setGravity(Gravity.CENTER_VERTICAL);
                top.addView(Ui.text(c, q.name, 12f, Ui.semibold, Ui.text2()),
                        new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                top.addView(new SparkView(c, q.series, col), new LinearLayout.LayoutParams(Ui.dp(36), Ui.dp(16)));
                card.addView(top, Widgets.MW);

                TextView px = Ui.text(c, Market.fmtPrice(q), 15.5f, Ui.bold, Ui.text());
                px.setSingleLine(true);
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                plp.topMargin = Ui.dp(6);
                card.addView(px, plp);
                card.addView(Ui.text(c, Market.fmtPct(q.changePct()), 12f, Ui.semibold, col));

                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(144), ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(10);
                card.setLayoutParams(lp);
                card.setOnClickListener(v -> main.openChart(q.symbol));
                Ui.press(card, 0.96f);
                rail.addView(card);
            }

            // Live Activity Pinned Status Strip (shown only when a stock is actively pinned to Now Bar / Live Bar)
            if (!Market.liveSymbol.isEmpty()) {
                Market.Quote lq = Market.findExact(Market.liveSymbol);
                if (lq != null) {
                    LinearLayout liveBar = cardBox(c);
                    liveBar.setOrientation(LinearLayout.HORIZONTAL);
                    liveBar.setGravity(Gravity.CENTER_VERTICAL);
                    liveBar.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(14), Ui.dp(12));
                    int col = Market.deltaColor(lq.change());
                    LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(18), Ui.dp(18));
                    ilp.rightMargin = Ui.dp(10);
                    liveBar.addView(new Icons.IconView(c, Icons.BELL, 17, col), ilp);
                    TextView ltv = Ui.text(c, "실시간 바 · " + lq.name + " " + Market.fmtPrice(lq) + " (" + Market.fmtPct(lq.changePct()) + ")",
                            13f, Ui.semibold, Ui.text());
                    ltv.setSingleLine(true);
                    ltv.setEllipsize(TextUtils.TruncateAt.END);
                    liveBar.addView(ltv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    TextView off = Ui.text(c, "해제", 12f, Ui.semibold, Ui.text2());
                    off.setPadding(Ui.dp(10), Ui.dp(4), Ui.dp(6), Ui.dp(4));
                    off.setOnClickListener(v -> { StockLiveNotification.pinLive(main, ""); main.reloadAll(); });
                    liveBar.addView(off);
                    liveBar.setOnClickListener(v -> main.openChart(lq.symbol));
                    content.addView(liveBar, cardLp());
                }
            }

            // Watchlist Section Header + inline "+ 종목 추가" pill
            LinearLayout secRow = new LinearLayout(c);
            secRow.setOrientation(LinearLayout.HORIZONTAL);
            secRow.setGravity(Gravity.CENTER_VERTICAL);
            secRow.setPadding(Ui.dp(20), Ui.dp(20), Ui.dp(18), Ui.dp(8));
            secRow.addView(Ui.text(c, "관심 종목", 20, Ui.bold, Ui.text()),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            TextView addChip = Ui.text(c, "+ 추가", 13f, Ui.semibold, Ui.ACCENT);
            addChip.setPadding(Ui.dp(12), Ui.dp(6), Ui.dp(12), Ui.dp(6));
            addChip.setBackground(Ui.round(Ui.alpha(Ui.ACCENT, Ui.dark ? 0.20f : 0.12f), 100));
            addChip.setOnClickListener(v -> openAddTickerSheet(main));
            Ui.press(addChip, 0.94f);
            secRow.addView(addChip);
            content.addView(secRow, Widgets.MW);

            Widgets.Group g = new Widgets.Group(c, null);
            for (Market.Quote q : Market.watch) g.addCustom(quoteRow(c, main, q));
            content.addView(g, Widgets.MW);
        }
    }

    static View quoteRow(Context c, MainActivity m, Market.Quote q) {
        LinearLayout r = new LinearLayout(c);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(16), Ui.dp(13), Ui.dp(16), Ui.dp(13));
        r.setBackground(Widgets.ripple());

        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);
        LinearLayout nameLine = new LinearLayout(c);
        nameLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView nm = Ui.text(c, q.name, 15.5f, Ui.semibold, Ui.text());
        nm.setMaxLines(1); nm.setEllipsize(TextUtils.TruncateAt.END);
        nameLine.addView(nm, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        boolean isLive = q.symbol.equalsIgnoreCase(Market.liveSymbol);
        if (isLive || q.alertEnabled) {
            TextView dot = Widgets.pillBadge(c, isLive ? "LIVE" : "알림",
                    Ui.alpha(Ui.ACCENT, Ui.dark ? 0.25f : 0.13f), Ui.ACCENT);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.leftMargin = Ui.dp(6);
            nameLine.addView(dot, dlp);
        }
        left.addView(nameLine);
        left.addView(Ui.text(c, q.symbol + " · " + q.market, 12, Ui.regular, Ui.text2()));
        r.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int col = Market.deltaColor(q.change());
        SparkView sv = new SparkView(c, q.series, col);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(Ui.dp(62), Ui.dp(28));
        slp.rightMargin = Ui.dp(12);
        r.addView(sv, slp);

        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        right.setMinimumWidth(Ui.dp(94));
        TextView priceTv = Ui.text(c, Market.fmtPrice(q), 15f, Ui.bold, Ui.text());
        priceTv.setSingleLine(true);
        right.addView(priceTv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView pct = Widgets.pillBadge(c, Market.fmtPct(q.changePct()), Ui.alpha(col, Ui.dark ? 0.24f : 0.13f), col);
        right.addView(pct);
        r.addView(right, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        r.setOnClickListener(v -> m.openChart(q.symbol));
        r.setOnLongClickListener(v -> {
            showQuoteActionSheet(m, v, q);
            return true;
        });
        return r;
    }

    static void showQuoteActionSheet(MainActivity m, View anchorView, Market.Quote q) {
        boolean isLive = q.symbol.equalsIgnoreCase(Market.liveSymbol);
        List<GlassMenu.Entry> es = new ArrayList<>();
        es.add(new GlassMenu.Entry("상세 차트 보기", Icons.CHART, false, () -> m.openChart(q.symbol)));
        es.add(new GlassMenu.Entry(isLive ? "실시간 알림 바 해제" : "실시간 알림 바에 고정", Icons.BELL, isLive, () -> {
            m.requestNotificationsIfNeeded();
            StockLiveNotification.pinLive(m, isLive ? "" : q.symbol);
            Toast.makeText(m, isLive ? "실시간 알림 바를 해제했습니다" : q.name + " 실시간 알림 바 고정됨", Toast.LENGTH_SHORT).show();
            m.reloadAll();
        }));
        es.add(new GlassMenu.Entry("목표가 알림 설정", Icons.TAG, q.alertEnabled, () -> openAlertSheet(m, q)));
        es.add(new GlassMenu.Entry("보유 수량 · 평단가 편집", Icons.WALLET, q.shares > 0, () -> openHoldingSheet(m, q)));
        if (Market.isWatched(q.symbol)) {
            es.add(new GlassMenu.Entry("관심 종목에서 삭제", Icons.TRASH, false, () -> {
                Market.removeFromWatchlist(m, q.symbol);
                m.reloadAll();
            }));
        }
        RectF anchor = anchorOf(anchorView);
        GlassMenu.sheet(m, anchor, q.name + " (" + q.symbol + ")", es, null);
    }

    private static RectF anchorOf(View v) {
        if (v != null && v.getWidth() > 0) {
            float[] o = Glass.abs(v);
            return new RectF(o[0], o[1], o[0] + v.getWidth(), o[1] + v.getHeight());
        }
        return new RectF(Ui.screenW / 2f - Ui.dpf(40), Ui.screenH - Ui.dpf(80), Ui.screenW / 2f + Ui.dpf(40), Ui.screenH - Ui.dpf(40));
    }

    // ---- 2. Detailed Interactive Chart Page ----
    static final class ChartPage extends Page {
        ChartPage(MainActivity m) {
            super(m, m.getString(R.string.tab_chart), true);
            setGrouped(true);
            headerIcons = new int[]{Icons.BELL, Icons.REFRESH};
        }

        @Override void build() {
            Context c = getContext();
            Market.Quote q = Market.find(Market.selectedSymbol);
            if (q == null) return;
            q.fillDerivedDefaults();
            int col = Market.deltaColor(q.change());

            LinearLayout card = cardBox(c);
            card.setPadding(Ui.dp(18), Ui.dp(18), Ui.dp(18), Ui.dp(16));

            // Top row: Name + Symbol + Live Bar / Alert action pills
            LinearLayout hdr = new LinearLayout(c);
            hdr.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout titleCol = new LinearLayout(c);
            titleCol.setOrientation(LinearLayout.VERTICAL);
            titleCol.addView(Ui.text(c, q.name, 18f, Ui.bold, Ui.text()));
            titleCol.addView(Ui.text(c, q.symbol + " · " + q.market, 12f, Ui.regular, Ui.text2()));
            hdr.addView(titleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            boolean isLive = q.symbol.equalsIgnoreCase(Market.liveSymbol);
            TextView liveBtn = Ui.text(c, isLive ? "실시간 바 ON" : "실시간 바", 11.5f, Ui.semibold, isLive ? 0xFFFFFFFF : Ui.ACCENT);
            liveBtn.setPadding(Ui.dp(11), Ui.dp(6), Ui.dp(11), Ui.dp(6));
            liveBtn.setBackground(Ui.round(isLive ? Ui.ACCENT : Ui.alpha(Ui.ACCENT, Ui.dark ? 0.22f : 0.12f), 100));
            liveBtn.setOnClickListener(v -> {
                main.requestNotificationsIfNeeded();
                StockLiveNotification.pinLive(main, isLive ? "" : q.symbol);
                Toast.makeText(main, isLive ? "실시간 바 해제됨" : q.name + " 실시간 알림 바 고정됨", Toast.LENGTH_SHORT).show();
                rebuild();
            });
            Ui.press(liveBtn, 0.94f);
            hdr.addView(liveBtn);
            card.addView(hdr, Widgets.MW);

            // Live scrubbable Price & Delta Readout
            final TextView priceTv = Ui.text(c, Market.fmtPrice(q), 31f, Ui.heavy, Ui.text());
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.topMargin = Ui.dp(10);
            card.addView(priceTv, plp);

            final String baseDeltaText = Market.fmtDelta(q) + " (" + Market.fmtPct(q.changePct()) + ")";
            final TextView deltaTv = Ui.text(c, baseDeltaText, 14f, Ui.semibold, col);
            card.addView(deltaTv);

            // High-precision Interactive Detailed Chart (gridlines, right price axis, volume histogram, touch scrub crosshair)
            DetailedChartView chart = new DetailedChartView(c, q, col, (scrubIdx) -> {
                if (scrubIdx < 0 || q.series == null || scrubIdx >= q.series.length) {
                    priceTv.setText(Market.fmtPrice(q));
                    deltaTv.setText(baseDeltaText);
                    deltaTv.setTextColor(col);
                } else {
                    double v = q.series[scrubIdx];
                    double base = q.series[0] > 0 ? q.series[0] : q.prevClose;
                    double diff = v - base;
                    double pct = base > 0 ? diff / base * 100.0 : 0;
                    int scCol = Market.deltaColor(diff);
                    priceTv.setText(Market.fmtVal(v, q.currency));
                    String ts = (q.timestamps != null && scrubIdx < q.timestamps.length && q.timestamps[scrubIdx] > 0)
                            ? formatChartTime(q.timestamps[scrubIdx], Market.chartRange) + " · " : "";
                    deltaTv.setText(ts + Market.fmtPct(pct));
                    deltaTv.setTextColor(scCol);
                }
            });
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(264));
            clp.topMargin = Ui.dp(14);
            card.addView(chart, clp);
            content.addView(card, cardLp());

            // 6 Timeframe Selector Chips
            String[] rLabels = {"1일", "1주", "1개월", "3개월", "1년", "전체"};
            String[] rKeys = {"1d", "5d", "1mo", "3mo", "1y", "5y"};
            int rIdx = 2;
            for (int i = 0; i < rKeys.length; i++) if (rKeys[i].equals(Market.chartRange)) rIdx = i;
            content.addView(new Widgets.Chips(c, rLabels, rIdx, idx -> {
                Market.chartRange = rKeys[idx];
                Market.refreshSingle(c, q);
                rebuild();
            }), Widgets.MW);

            // Key Metrics 2x3 Grid Card
            LinearLayout metrics = cardBox(c);
            metrics.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14));
            metrics.addView(metricRow(c,
                    "시가", Market.fmtVal(q.open, q.currency),
                    "전일 종가", Market.fmtVal(q.prevClose, q.currency)));
            metrics.addView(metricSep(c));
            metrics.addView(metricRow(c,
                    "당일 고가", Market.fmtVal(q.dayHigh, q.currency),
                    "당일 저가", Market.fmtVal(q.dayLow, q.currency)));
            metrics.addView(metricSep(c));
            metrics.addView(metricRow(c,
                    "52주 최고", Market.fmtVal(q.week52High, q.currency),
                    "52주 최저", Market.fmtVal(q.week52Low, q.currency)));
            metrics.addView(metricSep(c));
            String alertVal = (q.alertEnabled && q.targetPrice > 0)
                    ? Market.fmtVal(q.targetPrice, q.currency) + (q.alertAbove ? " 이상" : " 이하")
                    : "설정 안 됨";
            View bottomMetric = metricRow(c, "거래량", Market.fmtVolume(q.volume), "목표가 알림", alertVal);
            bottomMetric.setOnClickListener(v -> openAlertSheet(main, q));
            metrics.addView(bottomMetric);
            content.addView(metrics, cardLp());

            // Quick switch watchlist
            content.addView(Widgets.section(c, "종목 전환"));
            Widgets.Group g = new Widgets.Group(c, null);
            for (Market.Quote item : Market.watch) {
                g.row(Icons.CHART, Market.deltaColor(item.change()), item.name,
                        Market.fmtPrice(item) + " · " + Market.fmtPct(item.changePct()), true,
                        v -> { Market.selectedSymbol = item.symbol; Market.refreshSingle(c, item); rebuild(); });
            }
            content.addView(g, Widgets.MW);
        }

        private View metricRow(Context c, String k1, String v1, String k2, String v2) {
            LinearLayout row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, Ui.dp(8), 0, Ui.dp(8));
            row.addView(metricCell(c, k1, v1), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            View div = new View(c);
            div.setBackgroundColor(Ui.separator());
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Math.max(1, Ui.dp(0.6f)), ViewGroup.LayoutParams.MATCH_PARENT);
            dlp.setMargins(Ui.dp(12), Ui.dp(2), Ui.dp(12), Ui.dp(2));
            row.addView(div, dlp);
            row.addView(metricCell(c, k2, v2), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            return row;
        }

        private View metricCell(Context c, String label, String val) {
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(Ui.text(c, label, 12f, Ui.regular, Ui.text2()));
            TextView tv = Ui.text(c, val, 14.5f, Ui.semibold, Ui.text());
            tv.setSingleLine(true);
            tv.setPadding(0, Ui.dp(3), 0, 0);
            col.addView(tv);
            return col;
        }

        private View metricSep(Context c) {
            View v = new View(c);
            v.setBackgroundColor(Ui.separator());
            v.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(0.6f))));
            return v;
        }
    }

    static String formatChartTime(long epochSec, String range) {
        Date d = new Date(epochSec * 1000L);
        String pat = ("1d".equals(range) || "5d".equals(range)) ? "MM/dd HH:mm" : "yyyy.MM.dd";
        return new SimpleDateFormat(pat, Locale.KOREA).format(d);
    }

    // ---- 3. Portfolio Page ----
    static final class PortfolioPage extends Page {
        PortfolioPage(MainActivity m) {
            super(m, m.getString(R.string.tab_portfolio), true);
            setGrouped(true);
            headerIcons = new int[]{Icons.PLUS};
        }

        @Override void build() {
            Context c = getContext();
            double totalVal = 0, totalCost = 0;
            for (Market.Quote q : Market.watch) {
                if (q.shares > 0) {
                    totalVal += Market.toKrw(q, q.price * q.shares);
                    totalCost += Market.toKrw(q, q.avgCost * q.shares);
                }
            }
            double pnl = totalVal - totalCost;
            double pnlPct = totalCost > 0 ? pnl / totalCost * 100.0 : 0;
            int col = Market.deltaColor(pnl);

            LinearLayout hero = cardBox(c);
            hero.setPadding(Ui.dp(20), Ui.dp(18), Ui.dp(20), Ui.dp(18));
            hero.addView(Ui.text(c, "총 평가금액 (원화 환산)", 13.5f, Ui.semibold, Ui.text2()));
            TextView tv = Ui.text(c, String.format(Locale.KOREA, "%,d원", Math.round(totalVal)), 30, Ui.heavy, Ui.text());
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            tlp.topMargin = Ui.dp(4);
            hero.addView(tv, tlp);
            String pnlStr = String.format(Locale.KOREA, "%s%,d원 (%s)", pnl >= 0 ? "+" : "", Math.round(pnl), Market.fmtPct(pnlPct));
            hero.addView(Ui.text(c, pnlStr, 14.5f, Ui.semibold, col));
            content.addView(hero, cardLp());

            content.addView(Widgets.section(c, "보유 종목"));
            Widgets.Group g = new Widgets.Group(c, null);
            int count = 0;
            for (Market.Quote q : Market.watch) {
                if (q.shares <= 0) continue;
                count++;
                double diffPct = q.avgCost > 0 ? (q.price - q.avgCost) / q.avgCost * 100.0 : 0;
                String shStr = Math.abs(q.shares - Math.rint(q.shares)) < 1e-6
                        ? String.format(Locale.US, "%.0f", q.shares)
                        : String.format(Locale.US, "%.4f", q.shares).replaceAll("0+$", "").replaceAll("\\.$", "");
                String sub = shStr + "주 · " + Market.fmtPct(diffPct);
                g.row(Icons.WALLET, Market.deltaColor(diffPct), q.name, sub, true, v -> openHoldingSheet(main, q));
            }
            if (count == 0) content.addView(Widgets.caption(c, "관심 종목을 길게 눌러 보유 수량을 입력하세요."));
            else content.addView(g, Widgets.MW);
        }
    }

    // ---- 4. Live Search & Quick-Add Page ----
    static final class SearchPage extends Page {
        private String q = "";
        private List<Market.CatalogItem> results = new ArrayList<>();

        SearchPage(MainActivity m) {
            super(m, m.getString(R.string.search), true);
            setGrouped(true);
            headerIcons = new int[0];
            results = Market.searchCatalog("");
        }

        void setQuery(String s) {
            q = s == null ? "" : s.trim();
            results = Market.searchCatalog(q);
            rebuild();
            if (q.length() >= 2) {
                final String want = q;
                Market.searchOnline(want, list -> {
                    if (want.equals(q)) {
                        results = list;
                        rebuild();
                    }
                });
            }
        }

        @Override void build() {
            Context c = getContext();
            Widgets.Group g = new Widgets.Group(c, q.isEmpty() ? "인기 종목 · 바로 추가" : "검색 결과 (" + results.size() + ")");
            int limit = Math.min(20, results.size());
            for (int i = 0; i < limit; i++) {
                g.addCustom(catalogAddRow(c, main, results.get(i), this::rebuild));
            }
            if (limit == 0 && !q.isEmpty()) {
                String sym = q.toUpperCase(Locale.ROOT);
                boolean kr = sym.endsWith(".KS") || sym.endsWith(".KQ");
                g.addCustom(catalogAddRow(c, main, new Market.CatalogItem(sym, sym, kr ? "KRX" : "US", kr ? "KRW" : "USD", "직접 입력"), this::rebuild));
            }
            content.addView(g, Widgets.MW);
        }
    }

    static View catalogAddRow(Context c, MainActivity m, Market.CatalogItem it, Runnable onChanged) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(16), Ui.dp(12), Ui.dp(14), Ui.dp(12));
        row.setBackground(Widgets.ripple());

        LinearLayout info = new LinearLayout(c);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView nm = Ui.text(c, it.name, 15f, Ui.semibold, Ui.text());
        nm.setMaxLines(1); nm.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(nm);
        info.addView(Ui.text(c, it.symbol + " · " + it.market, 12f, Ui.regular, Ui.text2()));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        boolean added = Market.isWatched(it.symbol);
        TextView btn = Ui.text(c, added ? "추가됨" : "+ 추가", 12.5f, Ui.semibold, added ? Ui.text2() : 0xFFFFFFFF);
        btn.setPadding(Ui.dp(12), Ui.dp(6), Ui.dp(12), Ui.dp(6));
        btn.setBackground(Ui.round(added ? Ui.chip() : Ui.ACCENT, 100));
        btn.setOnClickListener(v -> {
            if (added) {
                Market.removeFromWatchlist(m, it.symbol);
                Toast.makeText(m, it.name + " 삭제됨", Toast.LENGTH_SHORT).show();
            } else {
                Market.addToWatchlist(m, it.symbol, it.name, it.market, it.currency);
                Toast.makeText(m, it.name + " 관심 종목에 추가됨", Toast.LENGTH_SHORT).show();
            }
            m.reloadAll();
            if (onChanged != null) onChanged.run();
        });
        row.addView(btn);
        return row;
    }

    /** Upgraded Watchlist Ticker Search & Add Sheet (popular KR/US/ETF catalog + live Yahoo search + custom ticker). */
    static void openAddTickerSheet(MainActivity m) {
        LinearLayout body = new LinearLayout(m);
        body.setOrientation(LinearLayout.VERTICAL);

        EditText searchEt = Widgets.formField(m, null, "종목명 또는 티커 검색 (예: 카카오, NVDA, SCHD)", "", InputType.TYPE_CLASS_TEXT, body);
        final LinearLayout listBox = new LinearLayout(m);
        listBox.setOrientation(LinearLayout.VERTICAL);

        ScrollView sv = new ScrollView(m);
        sv.addView(listBox, Widgets.MW);
        body.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(280)));

        final Runnable[] render = new Runnable[1];
        final List<Market.CatalogItem>[] curItems = new List[]{Market.searchCatalog("")};
        render[0] = () -> {
            listBox.removeAllViews();
            String q = searchEt.getText().toString().trim();
            int max = Math.min(12, curItems[0].size());
            for (int i = 0; i < max; i++) {
                listBox.addView(catalogAddRow(m, m, curItems[0].get(i), render[0]));
            }
            if (!q.isEmpty()) {
                String sym = q.toUpperCase(Locale.ROOT);
                boolean dup = false;
                for (int i = 0; i < max; i++) if (curItems[0].get(i).symbol.equalsIgnoreCase(sym)) { dup = true; break; }
                if (!dup) {
                    boolean kr = sym.endsWith(".KS") || sym.endsWith(".KQ");
                    listBox.addView(catalogAddRow(m, m, new Market.CatalogItem(sym, sym, kr ? "KRX" : "US", kr ? "KRW" : "USD", "직접 입력"), render[0]));
                }
            }
        };
        render[0].run();

        searchEt.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable e) {
                String q = e.toString().trim();
                curItems[0] = Market.searchCatalog(q);
                render[0].run();
                if (q.length() >= 2) {
                    Market.searchOnline(q, res -> {
                        if (q.equals(searchEt.getText().toString().trim())) {
                            curItems[0] = res;
                            render[0].run();
                        }
                    });
                }
            }
        });

        Widgets.showFormSheet(m, "관심 종목 추가", body, "완료", m::reloadAll);
    }

    /** Per-ticker Price Target Live Alert & Now Bar Pinning Sheet. */
    static void openAlertSheet(MainActivity m, Market.Quote q) {
        LinearLayout form = new LinearLayout(m);
        form.setOrientation(LinearLayout.VERTICAL);

        final boolean[] above = {q.alertAbove};
        String[] dirs = {"목표가 이상 도달 시 알림", "목표가 이하 도달 시 알림"};
        form.addView(new Widgets.Chips(m, dirs, above[0] ? 0 : 1, idx -> above[0] = (idx == 0)), Widgets.MW);

        EditText targetEt = Widgets.formField(m, "목표가 (" + q.currency + ")",
                "현재가 " + Market.fmtPrice(q),
                q.targetPrice > 0 ? String.valueOf(q.targetPrice) : "",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, form);

        LinearLayout pinRow = new LinearLayout(m);
        pinRow.setGravity(Gravity.CENTER_VERTICAL);
        pinRow.setPadding(Ui.dp(4), Ui.dp(10), Ui.dp(4), Ui.dp(6));
        pinRow.addView(Ui.text(m, "실시간 알림 바에 고정", 14.5f, Ui.semibold, Ui.text()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final Widgets.Toggle pinTg = new Widgets.Toggle(m, q.symbol.equalsIgnoreCase(Market.liveSymbol));
        pinRow.addView(pinTg);
        form.addView(pinRow, Widgets.MW);

        Widgets.showFormSheet(m, q.name + " 라이브 알림", form, m.getString(R.string.save), () -> {
            m.requestNotificationsIfNeeded();
            String raw = targetEt.getText().toString().trim();
            if (!raw.isEmpty()) {
                try {
                    double v = Double.parseDouble(raw);
                    if (v > 0) {
                        q.targetPrice = v;
                        q.alertAbove = above[0];
                        q.alertEnabled = true;
                    }
                } catch (Exception ignored) {}
            } else {
                q.alertEnabled = false;
            }
            if (pinTg.on) StockLiveNotification.pinLive(m, q.symbol);
            else if (q.symbol.equalsIgnoreCase(Market.liveSymbol)) StockLiveNotification.pinLive(m, "");
            Market.saveWatch(m);
            StockLiveNotification.checkTargets(m);
            Toast.makeText(m, "라이브 알림 설정을 저장했습니다", Toast.LENGTH_SHORT).show();
            m.reloadAll();
        });
    }

    static void openHoldingSheet(MainActivity m, Market.Quote q) {
        LinearLayout form = new LinearLayout(m);
        form.setOrientation(LinearLayout.VERTICAL);
        String shInit = Math.abs(q.shares - Math.rint(q.shares)) < 1e-6
                ? String.valueOf((long) Math.rint(q.shares))
                : String.format(Locale.US, "%.4f", q.shares).replaceAll("0+$", "").replaceAll("\\.$", "");
        EditText shEt = Widgets.formField(m, "보유 수량", "0", shInit, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, form);
        EditText avgEt = Widgets.formField(m, "평균 매입가 (" + q.currency + ")", "0", String.valueOf(q.avgCost), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, form);
        Widgets.showFormSheet(m, q.name + " 보유 편집", form, m.getString(R.string.save), () -> {
            try { q.shares = Math.max(0, Double.parseDouble(shEt.getText().toString().trim())); } catch (Exception ignored) {}
            try { q.avgCost = Math.max(0, Double.parseDouble(avgEt.getText().toString().trim())); } catch (Exception ignored) {}
            Market.saveWatch(m);
            m.reloadAll();
        });
    }

    static final class SparkView extends View {
        private final double[] pts;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        SparkView(Context c, double[] pts, int color) {
            super(c); this.pts = pts;
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Ui.dpf(1.9f));
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override protected void onDraw(Canvas cv) {
            if (pts == null || pts.length < 2) return;
            double min = pts[0], max = pts[0];
            for (double v : pts) { if (v < min) min = v; if (v > max) max = v; }
            double span = Math.max(1e-6, max - min);
            float w = getWidth(), h = getHeight(), pad = Ui.dpf(2.5f);
            path.reset();
            for (int i = 0; i < pts.length; i++) {
                float x = pad + (w - pad * 2) * i / (pts.length - 1);
                float y = h - pad - (float) ((pts[i] - min) / span) * (h - pad * 2);
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            cv.drawPath(path, p);
        }
    }

    /** High-precision Interactive Chart: gridlines, right Y-axis labels, reference baseline, volume bars, X-axis dates, finger crosshair scrub. */
    static final class DetailedChartView extends View {
        interface ScrubListener { void onScrub(int index); }

        private final Market.Quote q;
        private final int color;
        private final ScrubListener scrubCb;
        private int scrubIdx = -1;

        private final Paint lineP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint fillP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint gridP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint refP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint volP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint crossP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final TextPaint axisP = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        private final Path line = new Path(), fill = new Path();

        DetailedChartView(Context c, Market.Quote q, int color, ScrubListener scrubCb) {
            super(c);
            this.q = q;
            this.color = color;
            this.scrubCb = scrubCb;

            lineP.setColor(color);
            lineP.setStyle(Paint.Style.STROKE);
            lineP.setStrokeWidth(Ui.dpf(2.3f));
            lineP.setStrokeCap(Paint.Cap.ROUND);
            lineP.setStrokeJoin(Paint.Join.ROUND);

            gridP.setStyle(Paint.Style.STROKE);
            gridP.setStrokeWidth(Math.max(1f, Ui.dpf(0.7f)));
            gridP.setColor(Ui.separator());

            refP.setStyle(Paint.Style.STROKE);
            refP.setStrokeWidth(Ui.dpf(1f));
            refP.setColor(Ui.alpha(Ui.text2(), 0.45f));
            refP.setPathEffect(new DashPathEffect(new float[]{Ui.dpf(4), Ui.dpf(4)}, 0));

            volP.setStyle(Paint.Style.FILL);
            crossP.setStyle(Paint.Style.STROKE);
            crossP.setStrokeWidth(Ui.dpf(1.1f));
            crossP.setColor(Ui.alpha(Ui.text(), 0.55f));

            axisP.setTextSize(Ui.sp(10.5f));
            axisP.setTypeface(Ui.medium);
            axisP.setColor(Ui.text2());
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            double[] pts = q.series;
            if (pts == null || pts.length < 2) return false;
            float plotL = Ui.dpf(4), plotR = getWidth() - Ui.dpf(66);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    float frac = Ui.clamp((e.getX() - plotL) / Math.max(1f, plotR - plotL), 0f, 1f);
                    int idx = Math.round(frac * (pts.length - 1));
                    if (idx != scrubIdx) {
                        scrubIdx = idx;
                        if (scrubCb != null) scrubCb.onScrub(scrubIdx);
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    scrubIdx = -1;
                    if (scrubCb != null) scrubCb.onScrub(-1);
                    invalidate();
                    return true;
            }
            return super.onTouchEvent(e);
        }

        @Override protected void onDraw(Canvas cv) {
            double[] pts = q.series;
            if (pts == null || pts.length < 2) return;
            float W = getWidth(), H = getHeight();
            if (W <= 0 || H <= 0) return;

            float axisW = Ui.dpf(64);
            float plotL = Ui.dpf(4);
            float plotR = W - axisW - Ui.dpf(4);
            float plotT = Ui.dpf(12);
            float volH = Ui.dpf(36);
            float xLabelH = Ui.dpf(20);
            float plotB = H - volH - xLabelH - Ui.dpf(6);
            float volTop = plotB + Ui.dpf(6);
            float volBot = H - xLabelH;

            double min = pts[0], max = pts[0];
            for (double v : pts) { if (v < min) min = v; if (v > max) max = v; }
            if (q.prevClose > 0) {
                min = Math.min(min, q.prevClose);
                max = Math.max(max, q.prevClose);
            }
            double padSpan = Math.max(max * 0.004, (max - min) * 0.08);
            min -= padSpan; max += padSpan;
            double span = Math.max(1e-6, max - min);

            // 1. Horizontal gridlines + right-aligned Y-axis price labels
            axisP.setTextAlign(Paint.Align.RIGHT);
            Paint.FontMetrics fm = axisP.getFontMetrics();
            int steps = 3;
            for (int i = 0; i <= steps; i++) {
                float f = i / (float) steps;
                float y = Ui.lerp(plotT, plotB, f);
                cv.drawLine(plotL, y, plotR, y, gridP);
                double val = max - span * f;
                cv.drawText(shortAxisVal(val, q.currency), W - Ui.dpf(2), y - (fm.ascent + fm.descent) / 2f, axisP);
            }

            // 2. Dotted Previous-Close Reference Baseline
            if (q.prevClose >= min && q.prevClose <= max) {
                float refY = plotB - (float) ((q.prevClose - min) / span) * (plotB - plotT);
                cv.drawLine(plotL, refY, plotR, refY, refP);
            }

            // 3. Volume Histogram Bars along bottom
            if (q.volumes != null && q.volumes.length == pts.length) {
                long maxV = 1L;
                for (long v : q.volumes) if (v > maxV) maxV = v;
                float barW = Math.max(Ui.dpf(2.2f), (plotR - plotL) / pts.length * 0.62f);
                for (int i = 0; i < pts.length; i++) {
                    float cx = plotL + (plotR - plotL) * i / (pts.length - 1);
                    float bh = Math.max(Ui.dpf(2f), (float) (q.volumes[i] / (double) maxV) * (volBot - volTop));
                    boolean up = i == 0 ? (pts[i] >= q.prevClose) : (pts[i] >= pts[i - 1]);
                    int vc = Market.deltaColor(up ? 1 : -1);
                    volP.setColor(Ui.alpha(vc, i == scrubIdx ? 0.85f : 0.28f));
                    cv.drawRoundRect(cx - barW / 2f, volBot - bh, cx + barW / 2f, volBot, Ui.dpf(1.2f), Ui.dpf(1.2f), volP);
                }
            }

            // 4. Price Curve + Gradient Fill
            line.reset(); fill.reset();
            for (int i = 0; i < pts.length; i++) {
                float x = plotL + (plotR - plotL) * i / (pts.length - 1);
                float y = plotB - (float) ((pts[i] - min) / span) * (plotB - plotT);
                if (i == 0) { line.moveTo(x, y); fill.moveTo(x, plotB); fill.lineTo(x, y); }
                else { line.lineTo(x, y); fill.lineTo(x, y); }
            }
            fill.lineTo(plotR, plotB);
            fill.close();
            fillP.setShader(new LinearGradient(0, plotT, 0, plotB, Ui.alpha(color, 0.26f), Ui.alpha(color, 0f), Shader.TileMode.CLAMP));
            cv.drawPath(fill, fillP);
            cv.drawPath(line, lineP);

            // 5. Latest Price Live Endpoint Dot (when not scrubbing)
            int activeIdx = Math.max(0, Math.min(pts.length - 1, scrubIdx >= 0 ? scrubIdx : (pts.length - 1)));
            float ax = plotL + (plotR - plotL) * activeIdx / (pts.length - 1);
            float ay = plotB - (float) ((pts[activeIdx] - min) / span) * (plotB - plotT);
            if (scrubIdx >= 0) {
                cv.drawLine(ax, plotT, ax, volBot, crossP);
                cv.drawLine(plotL, ay, plotR, ay, crossP);
            }
            dotP.setColor(Ui.alpha(color, 0.24f));
            cv.drawCircle(ax, ay, Ui.dpf(scrubIdx >= 0 ? 9f : 6.5f), dotP);
            dotP.setColor(color);
            cv.drawCircle(ax, ay, Ui.dpf(scrubIdx >= 0 ? 4.5f : 3.5f), dotP);

            // 6. X-axis Timestamps (start / mid / end)
            if (q.timestamps != null && q.timestamps.length == pts.length) {
                float ty = H - Ui.dpf(3);
                axisP.setTextAlign(Paint.Align.LEFT);
                cv.drawText(shortTimeLabel(q.timestamps[0]), plotL, ty, axisP);
                axisP.setTextAlign(Paint.Align.CENTER);
                cv.drawText(shortTimeLabel(q.timestamps[pts.length / 2]), (plotL + plotR) / 2f, ty, axisP);
                axisP.setTextAlign(Paint.Align.RIGHT);
                cv.drawText(shortTimeLabel(q.timestamps[pts.length - 1]), plotR, ty, axisP);
            }
        }

        private String shortAxisVal(double v, String cur) {
            if ("KRW".equals(cur)) return String.format(Locale.KOREA, "%,d", Math.round(v));
            if ("PT".equals(cur)) return String.format(Locale.US, "%,.0f", v);
            return String.format(Locale.US, "$%.1f", v);
        }

        private String shortTimeLabel(long epochSec) {
            if (epochSec <= 0) return "";
            String pat = "1d".equals(Market.chartRange) ? "HH:mm" : ("5y".equals(Market.chartRange) ? "yy.MM" : "MM/dd");
            return new SimpleDateFormat(pat, Locale.KOREA).format(new Date(epochSec * 1000L));
        }
    }
}
