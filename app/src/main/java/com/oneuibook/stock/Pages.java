package com.oneuibook.stock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

/** Watchlist, Interactive Chart, Portfolio, and Search pages for Stock Android (9.5.0). */
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
            // Market indices rail
            LinearLayout rail = Widgets.rail(content, 10);
            for (Market.Quote q : Market.indices) {
                LinearLayout card = cardBox(c);
                card.setPadding(Ui.dp(14), Ui.dp(12), Ui.dp(14), Ui.dp(12));
                int col = Market.deltaColor(q.change());
                card.addView(Ui.text(c, q.name, 12.5f, Ui.semibold, Ui.text2()));
                TextView px = Ui.text(c, Market.fmtPrice(q), 16, Ui.bold, Ui.text());
                LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                plp.topMargin = Ui.dp(4);
                card.addView(px, plp);
                card.addView(Ui.text(c, Market.fmtPct(q.changePct()), 12.5f, Ui.semibold, col));
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(136), ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(10);
                card.setLayoutParams(lp);
                card.setOnClickListener(v -> main.openChart(q.symbol));
                Ui.press(card, 0.96f);
                rail.addView(card);
            }

            content.addView(Widgets.section(c, "관심 종목"));
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
        left.addView(Ui.text(c, q.name, 15.5f, Ui.semibold, Ui.text()));
        left.addView(Ui.text(c, q.symbol + " · " + q.market, 12, Ui.regular, Ui.text2()));
        r.addView(left, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        int col = Market.deltaColor(q.change());
        SparkView sv = new SparkView(c, q.series, col);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(Ui.dp(64), Ui.dp(28));
        slp.rightMargin = Ui.dp(12);
        r.addView(sv, slp);

        LinearLayout right = new LinearLayout(c);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END);
        right.setMinimumWidth(Ui.dp(92));
        TextView priceTv = Ui.text(c, Market.fmtPrice(q), 15f, Ui.bold, Ui.text());
        priceTv.setSingleLine(true);
        priceTv.setEllipsize(null);
        right.addView(priceTv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView pct = Widgets.pillBadge(c, Market.fmtPct(q.changePct()), Ui.alpha(col, Ui.dark ? 0.25f : 0.14f), col);
        right.addView(pct);
        r.addView(right, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        r.setOnClickListener(v -> m.openChart(q.symbol));
        return r;
    }

    // ---- 2. Chart Page ----
    static final class ChartPage extends Page {
        ChartPage(MainActivity m) {
            super(m, m.getString(R.string.tab_chart), true);
            setGrouped(true);
            headerIcons = new int[]{Icons.REFRESH};
        }

        @Override void build() {
            Context c = getContext();
            Market.Quote q = Market.find(Market.selectedSymbol);
            if (q == null) return;
            int col = Market.deltaColor(q.change());

            LinearLayout card = cardBox(c);
            card.setPadding(Ui.dp(20), Ui.dp(18), Ui.dp(20), Ui.dp(18));
            card.addView(Ui.text(c, q.name + " (" + q.symbol + ")", 14, Ui.semibold, Ui.text2()));
            TextView px = Ui.text(c, Market.fmtPrice(q), 30, Ui.heavy, Ui.text());
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.topMargin = Ui.dp(4);
            card.addView(px, plp);
            card.addView(Ui.text(c, Market.fmtPct(q.changePct()) + " · " + q.market, 14, Ui.semibold, col));

            AreaChartView chart = new AreaChartView(c, q.series, col);
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(210));
            clp.topMargin = Ui.dp(16);
            card.addView(chart, clp);
            content.addView(card, cardLp());

            String[] rLabels = {"1일", "1주", "1개월", "1년"};
            String[] rKeys = {"1d", "5d", "1mo", "1y"};
            int rIdx = 2;
            for (int i = 0; i < rKeys.length; i++) if (rKeys[i].equals(Market.chartRange)) rIdx = i;
            content.addView(new Widgets.Chips(c, rLabels, rIdx, idx -> {
                Market.chartRange = rKeys[idx];
                Market.refreshAll(c);
                rebuild();
            }), Widgets.MW);

            // Quick switch watchlist
            content.addView(Widgets.section(c, "종목 선택"));
            Widgets.Group g = new Widgets.Group(c, null);
            for (Market.Quote item : Market.watch) {
                g.row(Icons.CHART, Market.deltaColor(item.change()), item.name, Market.fmtPrice(item) + " · " + Market.fmtPct(item.changePct()), true,
                        v -> { Market.selectedSymbol = item.symbol; rebuild(); });
            }
            content.addView(g, Widgets.MW);
        }
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
                double val = q.price * q.shares;
                double diffPct = q.avgCost > 0 ? (q.price - q.avgCost) / q.avgCost * 100.0 : 0;
                String sub = String.format(Locale.US, "%.0f주 · %s", q.shares, Market.fmtPct(diffPct));
                g.row(Icons.WALLET, Market.deltaColor(diffPct), q.name, sub, true, v -> openHoldingSheet(main, q));
            }
            if (count == 0) content.addView(Widgets.caption(c, "보유 종목 없음"));
            else content.addView(g, Widgets.MW);
        }
    }

    // ---- 4. Search Page ----
    static final class SearchPage extends Page {
        private String q = "";
        SearchPage(MainActivity m) { super(m, m.getString(R.string.search), true); setGrouped(true); headerIcons = new int[0]; }
        void setQuery(String s) { q = s == null ? "" : s.trim().toLowerCase(Locale.ROOT); rebuild(); }
        @Override void build() {
            Context c = getContext();
            Widgets.Group g = new Widgets.Group(c, "종목");
            for (Market.Quote item : Market.watch) {
                if (q.isEmpty() || item.name.toLowerCase(Locale.ROOT).contains(q) || item.symbol.toLowerCase(Locale.ROOT).contains(q)) {
                    g.addCustom(quoteRow(c, main, item));
                }
            }
            content.addView(g, Widgets.MW);
        }
    }

    static void openAddTickerSheet(MainActivity m) {
        LinearLayout form = new LinearLayout(m);
        form.setOrientation(LinearLayout.VERTICAL);
        EditText symEt = Widgets.formField(m, "티커", "예: GOOGL, 035720.KS", "", InputType.TYPE_CLASS_TEXT, form);
        EditText nameEt = Widgets.formField(m, "종목명", "예: Alphabet, 카카오", "", InputType.TYPE_CLASS_TEXT, form);
        Widgets.showFormSheet(m, "관심 종목 추가", form, m.getString(R.string.save), () -> {
            String sym = symEt.getText().toString().trim().toUpperCase(Locale.ROOT);
            String nm = nameEt.getText().toString().trim();
            if (sym.isEmpty()) return;
            if (nm.isEmpty()) nm = sym;
            boolean kr = sym.endsWith(".KS") || sym.endsWith(".KQ");
            Market.watch.add(new Market.Quote(sym, nm, kr ? "KRX" : "US", kr ? "KRW" : "USD", 0, 0, new double[]{0, 0}));
            Market.saveWatch(m);
            Market.refreshAll(m);
            m.reloadAll();
        });
    }

    static void openHoldingSheet(MainActivity m, Market.Quote q) {
        LinearLayout form = new LinearLayout(m);
        form.setOrientation(LinearLayout.VERTICAL);
        EditText shEt = Widgets.formField(m, "보유 수량", "0", String.valueOf((long) q.shares), InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL, form);
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
            p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(Ui.dpf(2));
            p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override protected void onDraw(Canvas cv) {
            if (pts == null || pts.length < 2) return;
            double min = pts[0], max = pts[0];
            for (double v : pts) { if (v < min) min = v; if (v > max) max = v; }
            double span = Math.max(1e-6, max - min);
            float w = getWidth(), h = getHeight(), pad = Ui.dpf(3);
            path.reset();
            for (int i = 0; i < pts.length; i++) {
                float x = pad + (w - pad * 2) * i / (pts.length - 1);
                float y = h - pad - (float) ((pts[i] - min) / span) * (h - pad * 2);
                if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
            }
            cv.drawPath(path, p);
        }
    }

    static final class AreaChartView extends View {
        private final double[] pts;
        private final int color;
        private final Paint lp = new Paint(Paint.ANTI_ALIAS_FLAG), fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path line = new Path(), fill = new Path();
        AreaChartView(Context c, double[] pts, int color) {
            super(c); this.pts = pts; this.color = color;
            lp.setColor(color); lp.setStyle(Paint.Style.STROKE); lp.setStrokeWidth(Ui.dpf(2.6f));
            lp.setStrokeCap(Paint.Cap.ROUND); lp.setStrokeJoin(Paint.Join.ROUND);
        }
        @Override protected void onDraw(Canvas cv) {
            if (pts == null || pts.length < 2) return;
            double min = pts[0], max = pts[0];
            for (double v : pts) { if (v < min) min = v; if (v > max) max = v; }
            double span = Math.max(1e-6, max - min);
            float w = getWidth(), h = getHeight(), pad = Ui.dpf(10);
            line.reset(); fill.reset();
            for (int i = 0; i < pts.length; i++) {
                float x = pad + (w - pad * 2) * i / (pts.length - 1);
                float y = h - pad - (float) ((pts[i] - min) / span) * (h - pad * 2);
                if (i == 0) { line.moveTo(x, y); fill.moveTo(x, h); fill.lineTo(x, y); }
                else { line.lineTo(x, y); fill.lineTo(x, y); }
            }
            fill.lineTo(w - pad, h); fill.close();
            fp.setShader(new LinearGradient(0, 0, 0, h, Ui.alpha(color, 0.28f), Ui.alpha(color, 0f), Shader.TileMode.CLAMP));
            cv.drawPath(fill, fp);
            cv.drawPath(line, lp);
        }
    }
}
