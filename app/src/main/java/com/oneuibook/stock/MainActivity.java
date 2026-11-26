package com.oneuibook.stock;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;

import java.util.ArrayList;
import java.util.List;

/** 주식 (9.5.0) — 관심 종목, 차트, 포트폴리오. */
public final class MainActivity extends Activity implements Dock.Listener, Header.Listener, Market.Listener {
    static final String VERSION = "9.5.4";

    FrameLayout root;
    Glass.Backdrop backdrop;
    private FrameLayout pageHost;
    Header header;
    Dock dock;

    private final NavStack[] tabs = new NavStack[4];
    int tab;
    private Pages.SearchPage searchPage;
    private Sidebar sidebar;
    private float sidebarP;
    private static boolean sidebarWanted;

    static SharedPreferences prefs(Context c) { return Market.prefs(c); }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences p = prefs(this);
        Ui.init(this, p);
        Market.init(this);

        Window w = getWindow();
        w.setDecorFitsSystemWindows(false);
        w.setStatusBarColor(Color.TRANSPARENT);
        w.setNavigationBarColor(Color.TRANSPARENT);
        w.setNavigationBarContrastEnforced(false);
        w.getDecorView().setBackgroundColor(Ui.groupedBg());

        root = new FrameLayout(this);
        backdrop = new Glass.Backdrop(this);
        backdrop.setBackgroundColor(Ui.groupedBg());
        root.addView(backdrop, match());
        pageHost = new FrameLayout(this);
        backdrop.addView(pageHost, match());

        header = new Header(this, backdrop);
        header.setListener(this);
        root.addView(header, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, headerHeight()));
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> fitHeader());

        dock = new Dock(this, backdrop, new String[]{
                getString(R.string.tab_watch), getString(R.string.tab_chart),
                getString(R.string.tab_portfolio), getString(R.string.tab_settings)});
        dock.setListener(this);
        root.addView(dock, match());
        setContentView(root);

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets sys = insets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            int ime = insets.getInsets(WindowInsets.Type.ime()).bottom;
            boolean changed = sys.top != Ui.statusInset || sys.bottom != Ui.navInset;
            Ui.statusInset = sys.top; Ui.navInset = sys.bottom;
            Ui.cutoutLeft = sys.left; Ui.cutoutRight = sys.right; Ui.imeInset = ime;
            dock.setImeOffset(ime);
            fitHeader();
            if (changed) {
                for (NavStack s : tabs) if (s != null) s.applyInsets();
                if (searchPage != null) searchPage.applyInsets();
            }
            return WindowInsets.CONSUMED;
        });

        root.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override public boolean onPreDraw() {
                updateScrollChrome(currentPage());
                boolean dirty = backdrop.isDirty();
                if (dirty || header.animating()) header.invalidate();
                if (dirty || dock.animating()) dock.invalidate();
                for (int i = 0; i < root.getChildCount(); i++) {
                    View c = root.getChildAt(i);
                    if (c instanceof GlassMenu) c.invalidate();
                }
                return true;
            }
        });

        tabs[0] = new NavStack(new Pages.WatchPage(this));
        tabs[1] = new NavStack(new Pages.ChartPage(this));
        tabs[2] = new NavStack(new Pages.PortfolioPage(this));
        tabs[3] = new NavStack(new Settings.SettingsPage(this));
        for (NavStack s : tabs) { s.setVisibility(View.GONE); pageHost.addView(s, match()); }
        tab = state == null ? 0 : state.getInt("tab", 0);
        tabs[tab].setVisibility(View.VISIBLE);
        tabs[tab].top().show();
        dock.select(tab, false);
        syncHeader();
        setLightStatus(!Ui.dark);
        if (sidebarWanted) root.post(() -> { if (tabletUi()) openSidebar(false); });
        Market.listeners.add(this);
        Market.refreshAll(this);
        StockLiveNotification.updateLive(this);
        handleOpenSymbol(getIntent());
    }

    @Override protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent);
        handleOpenSymbol(intent);
    }

    private void handleOpenSymbol(android.content.Intent in) {
        if (in == null) return;
        String sym = in.getStringExtra("open_symbol");
        if (sym != null && !sym.isEmpty()) openChart(sym);
    }

    void requestNotificationsIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 501);
        }
    }

    @Override protected void onDestroy() { Market.listeners.remove(this); super.onDestroy(); }
    @Override public void onMarketUpdated() { reloadAll(); if (sidebar != null) sidebar.refresh(); }

    void openChart(String symbol) {
        Market.selectedSymbol = symbol;
        dock.select(1, true);
        onTab(1, false);
        tabs[1].top().rebuild();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        Ui.init(this, prefs(this));
        fitHeader();
        dock.requestLayout();
        for (NavStack s : tabs) if (s != null) { s.applyInsets(); s.rebuildAll(); }
        if (tabletUi() && sidebarWanted && sidebar == null) openSidebar(false);
        else if (!tabletUi() && sidebar != null) closeSidebar();
    }

    void setLightStatus(boolean light) {
        WindowInsetsController ctrl = getWindow().getInsetsController();
        if (ctrl == null) return;
        int flag = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        ctrl.setSystemBarsAppearance(light ? flag : 0, flag);
    }

    int rootW() { return root != null && root.getWidth() > 0 ? root.getWidth() : Ui.screenW; }
    int rootH() { return root != null && root.getHeight() > 0 ? root.getHeight() : Ui.screenH; }
    boolean tabletUi() { return Dock.tabletFor(rootW(), rootH()); }
    boolean railUi() { return Dock.railFor(rootW(), rootH()); }
    int sideInset() { return railUi() ? Ui.dp(92) : 0; }
    int contentInsetPx() { return sidebar != null ? Math.round(Sidebar.widthFor(rootW()) * sidebarP) : 0; }
    int bottomPadding() { return Ui.navInset + Ui.dp(tabletUi() ? 92 : 148); }
    private int headerHeight() { return Ui.statusInset + Ui.dp(railUi() ? 172 : 88); }

    private void fitHeader() {
        if (header == null) return;
        int h = headerHeight();
        if (header.getLayoutParams().height == h) return;
        header.getLayoutParams().height = h;
        header.post(header::requestLayout);
    }

    private static FrameLayout.LayoutParams match() {
        return new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    void openSidebar(boolean animate) {
        if (!tabletUi()) return;
        sidebarWanted = true;
        if (sidebar == null) {
            sidebar = new Sidebar(this);
            sidebar.setSelected(tab);
            root.addView(sidebar, root.indexOfChild(dock), new FrameLayout.LayoutParams(Math.round(Sidebar.widthFor(rootW())), ViewGroup.LayoutParams.MATCH_PARENT));
        }
        float w = Sidebar.widthFor(rootW());
        if (animate) Ui.animate(sidebarP, 1f, 320, Ui.EMPHASIZED, a -> applySidebar((float) a.getAnimatedValue(), w));
        else applySidebar(1f, w);
    }

    void closeSidebar() {
        sidebarWanted = false;
        if (sidebar == null) return;
        float w = Sidebar.widthFor(rootW());
        Ui.animate(sidebarP, 0f, 260, Ui.EMPHASIZED, a -> {
            float v = (float) a.getAnimatedValue();
            applySidebar(v, w);
            if (v <= 0.01f && sidebar != null) { root.removeView(sidebar); sidebar = null; }
        });
    }

    private void applySidebar(float p, float w) {
        sidebarP = p;
        if (sidebar != null) sidebar.setTranslationX(-w * (1f - p));
        pageHost.setPadding(Math.round(w * p), 0, 0, 0);
        header.setPadding(Math.round(w * p), 0, 0, 0);
        dock.setSidebar(w, p);
    }

    void sidebarTab(int idx) { dock.select(idx, true); onTab(idx, idx == tab); }
    void reloadAll() { for (NavStack s : tabs) if (s != null) s.rebuildAll(); }

    void refreshAppearance() {
        getWindow().getDecorView().setBackgroundColor(Ui.groupedBg());
        backdrop.setBackgroundColor(Ui.groupedBg());
        setLightStatus(!Ui.dark);
        if (sidebar != null) { root.removeView(sidebar); sidebar = null; if (sidebarWanted) openSidebar(false); }
        reloadAll(); header.invalidate(); dock.requestLayout(); dock.invalidate();
    }

    Page currentPage() { return searchPage != null && searchPage.getVisibility() == View.VISIBLE ? searchPage : tabs[tab].top(); }
    void syncHeader() { Page p = currentPage(); header.configure(p.title, p.headerIcons, p.back); updateScrollChrome(p); }

    void onPageScroll(Page p, int y, int dy) {
        if (p != currentPage()) return;
        updateScrollChrome(p);
        if (y <= Ui.dp(8)) dock.setCollapsed(false);
        else if (dy > Ui.dp(10) && y > Ui.dp(36)) dock.setCollapsed(true);
        else if (dy < -Ui.dp(10)) dock.setCollapsed(false);
    }

    private void updateScrollChrome(Page p) { if (p != null) header.setScroll(p.titleProgress(), p.blurProgress()); }

    @Override public void onTab(int index, boolean reselect) {
        if (searchPage != null && searchPage.getVisibility() == View.VISIBLE) onExitSearch();
        if (reselect) { tabs[tab].top().scrollToTop(); return; }
        tabs[tab].animate().cancel();
        tabs[tab].setVisibility(View.GONE);
        tab = index;
        NavStack next = tabs[tab];
        next.animate().cancel();
        next.setVisibility(View.VISIBLE);
        next.top().show();
        next.setAlpha(0.45f);
        next.setScaleX(0.985f);
        next.setScaleY(0.985f);
        next.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).setInterpolator(Ui.EMPHASIZED).start();
        if (sidebar != null) sidebar.setSelected(tab);
        syncHeader();
    }

    @Override public void onMini() {}
    @Override public void onSearch() {
        if (searchPage == null) { searchPage = new Pages.SearchPage(this); pageHost.addView(searchPage, match()); }
        searchPage.setVisibility(View.VISIBLE); searchPage.show(); dock.setSearchMode(true); syncHeader();
    }
    @Override public void onExitSearch() {
        if (searchPage != null) searchPage.setVisibility(View.GONE); dock.setSearchMode(false); syncHeader();
    }
    @Override public void onQuery(String q, boolean submit) { if (searchPage != null) searchPage.setQuery(q); }
    @Override public void onSidebar() { if (sidebar == null) openSidebar(true); else closeSidebar(); }

    @Override public void onHeaderButton(int icon, RectF anchor) {
        if (icon == Icons.PLUS) Pages.openAddTickerSheet(this);
        else if (icon == Icons.REFRESH) Market.refreshAll(this);
        else if (icon == Icons.BELL) {
            Market.Quote q = Market.find(Market.selectedSymbol);
            if (q != null) Pages.openAlertSheet(this, q);
        }
    }

    @Override public void onBack() { onBackPressed(); }
    @Override public void onBackPressed() {
        if (GlassMenu.dismissAny(root)) return;
        if (sidebar != null) { closeSidebar(); return; }
        if (searchPage != null && searchPage.getVisibility() == View.VISIBLE) { onExitSearch(); return; }
        if (tab != 0) { dock.select(0, true); onTab(0, false); return; }
        super.onBackPressed();
    }

    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putInt("tab", tab); }

    final class NavStack extends FrameLayout {
        private final List<Page> stack = new ArrayList<>();
        NavStack(Page r) { super(MainActivity.this); stack.add(r); addView(r, match()); }
        Page top() { return stack.get(stack.size() - 1); }
        void applyInsets() { for (Page p : stack) p.applyInsets(); }
        void rebuildAll() { for (Page p : stack) p.rebuild(); }
    }
}
