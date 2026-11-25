package com.oneuibook.stock;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

final class Sidebar extends FrameLayout {
    private final MainActivity m;
    private final List<LinearLayout> tabRows = new ArrayList<>();
    private final LinearLayout extra;

    static float widthFor(int W) { return Ui.clamp(W * 0.27f, Ui.dpf(240), Ui.dpf(320)); }

    Sidebar(MainActivity m) {
        super(m);
        this.m = m;
        setBackgroundColor(Ui.gStyle ? Ui.groupedBg() : (Ui.dark ? 0xFF161618 : 0xFFE9E9EE));
        setClickable(true);
        ScrollView sv = new ScrollView(m);
        sv.setVerticalScrollBarEnabled(false);
        addView(sv, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout col = new LinearLayout(m);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(Ui.dp(12), Ui.statusInset + Ui.dp(12), Ui.dp(12), m.bottomPadding());
        sv.addView(col, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout top = new LinearLayout(m);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(Ui.dp(8), 0, 0, Ui.dp(14));
        top.addView(Ui.text(m, m.getString(R.string.app_name), 21, Ui.bold, Ui.text()), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Icons.IconView close = new Icons.IconView(m, Icons.SIDEBAR, 22, Ui.text());
        GradientDrawable cb = Ui.round(Ui.card(), 100);
        cb.setStroke(Ui.dp(1), Ui.dark ? 0x33FFFFFF : 0x1F000000);
        close.setBackground(cb);
        close.setOnClickListener(v -> m.closeSidebar());
        top.addView(close, new LinearLayout.LayoutParams(Ui.dp(46), Ui.dp(46)));
        col.addView(top, Widgets.MW);

        int[] titles = {R.string.tab_watch, R.string.tab_chart, R.string.tab_portfolio, R.string.tab_settings};
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            LinearLayout r = row(Dock.TAB_ICONS[i], m.getString(titles[i]), null, v -> m.sidebarTab(idx));
            tabRows.add(r);
            col.addView(r, rowLp());
        }
        extra = new LinearLayout(m);
        extra.setOrientation(LinearLayout.VERTICAL);
        col.addView(extra, Widgets.MW);
        refresh();
    }

    void refresh() {
        extra.removeAllViews();
        TextView h = Ui.text(getContext(), "주요 지수", 13, Ui.medium, Ui.text2());
        h.setPadding(Ui.dp(10), Ui.dp(18), Ui.dp(10), Ui.dp(6));
        extra.addView(h, Widgets.MW);
        for (Market.Quote q : Market.indices) {
            extra.addView(row(Icons.CHART, q.name, Market.fmtPrice(q) + " (" + Market.fmtPct(q.changePct()) + ")",
                    v -> m.openChart(q.symbol)), rowLp());
        }
    }

    private static LinearLayout.LayoutParams rowLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(48));
        lp.bottomMargin = Ui.dp(2);
        return lp;
    }

    private LinearLayout row(int icon, String title, String sub, View.OnClickListener click) {
        LinearLayout r = new LinearLayout(getContext());
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Ui.dp(12), 0, Ui.dp(12), 0);
        r.addView(new Icons.IconView(getContext(), icon, 20, Ui.ACCENT), new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24)));
        LinearLayout tx = new LinearLayout(getContext());
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(Ui.dp(14), 0, 0, 0);
        tx.addView(Ui.text(getContext(), title, sub == null ? 16.5f : 14.5f, Ui.regular, Ui.text()));
        if (sub != null) tx.addView(Ui.text(getContext(), sub, 12f, Ui.regular, Ui.text2()));
        r.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.setOnClickListener(click);
        r.setBackground(new RippleDrawable(ColorStateList.valueOf(Ui.pressed()), null, Ui.round(0xFFFFFFFF, 100)));
        return r;
    }

    void setSelected(int tab) {
        for (int i = 0; i < tabRows.size(); i++) {
            LinearLayout r = tabRows.get(i);
            boolean on = i == tab;
            int selCol = Ui.gStyle ? Ui.alpha(Ui.ACCENT, Ui.dark ? 0.22f : 0.13f) : (Ui.dark ? 0x33FFFFFF : 0x17000000);
            Drawable base = on ? Ui.round(selCol, 100) : null;
            r.setBackground(new RippleDrawable(ColorStateList.valueOf(Ui.pressed()), base, Ui.round(0xFFFFFFFF, 100)));
            TextView t = (TextView) ((LinearLayout) r.getChildAt(1)).getChildAt(0);
            t.setTypeface(on ? Ui.semibold : Ui.regular);
            t.setTextColor(on && Ui.gStyle ? Ui.ACCENT : Ui.text());
        }
    }
}
