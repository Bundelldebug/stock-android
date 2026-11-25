package com.oneuibook.stock;

import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** A scrolling screen with an optional large title that collapses into the fixed header. */
abstract class Page extends FrameLayout {
    final MainActivity main;
    final Scroll scroll;
    final LinearLayout content;
    TextView bigTitle;
    String title;
    int[] headerIcons = {Icons.PLUS};
    boolean back;
    boolean grouped;
    private boolean started;

    final class Scroll extends ScrollView {
        Scroll() {
            super(Page.this.getContext());
            setVerticalScrollBarEnabled(false);
            setOverScrollMode(OVER_SCROLL_IF_CONTENT_SCROLLS);
            setClipToPadding(false);
        }

        @Override protected void onScrollChanged(int l, int t, int ol, int ot) {
            super.onScrollChanged(l, t, ol, ot);
            main.onPageScroll(Page.this, t, t - ot);
            Page.this.onScrolled(t);
        }
    }

    Page(MainActivity m, String title, boolean largeTitle) {
        super(m);
        main = m;
        this.title = title;
        scroll = new Scroll();
        content = new LinearLayout(m);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(scroll, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        applyInsets();
        if (largeTitle) addBigTitle();
    }

    void applyInsets() {
        int top = Ui.statusInset + Ui.dp(largeTop() ? 64 : 60);
        content.setPadding(0, top, main.sideInset(), main.bottomPadding());
    }

    boolean largeTop() { return true; }

    void addBigTitle() {
        // G style sub-pages (except long-title detail lists like LocalListPage) show their title cleanly in the top bar
        boolean gRoot = Ui.gStyle && !back;
        boolean gSubClean = Ui.gStyle && back;
        bigTitle = Ui.text(getContext(), title, gRoot ? 34 : 36, Ui.bold, Ui.text());
        bigTitle.setLetterSpacing(Ui.gStyle ? -0.02f : -0.01f);
        if (gSubClean) {
            // keep bigTitle non-null for scroll helpers, collapsed invisibly so content starts right under "< Title"
            bigTitle.setVisibility(GONE);
            content.addView(bigTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
            return;
        }
        // long names (album titles…) shrink and wrap instead of being cut off: up to 3 lines, 36sp down to 24sp
        int avail = Ui.screenW - main.sideInset() - main.contentInsetPx() - Ui.dp(Ui.gStyle ? 44 : 34);
        float size = Math.min(gRoot ? 34f : 36f, fitTitleSize(title, avail));
        bigTitle.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, size);
        bigTitle.setSingleLine(false);
        bigTitle.setMaxLines(3);
        bigTitle.setLineSpacing(0, 1.05f);
        if (gRoot) {
            boolean wideScreen = main.sideInset() > 0 || main.tabletUi();
            // One UI 8.5/9.x Expanding App Bar: balanced top breathing room without excessive dead space;
            // unfolded foldable / tablet keeps compact left-aligned title so it never collides with rail or top tabs
            bigTitle.setGravity(wideScreen ? android.view.Gravity.START : android.view.Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            int topM = Ui.dp(wideScreen ? 10 : (Ui.cover() ? 16 : 30));
            int botM = Ui.dp(wideScreen ? 6 : (Ui.cover() ? 10 : 16));
            lp.setMargins(Ui.dp(22), topM, Ui.dp(22), botM);
            content.addView(bigTitle, lp);
            return;
        }
        boolean oneLine = lines(title, avail, size) <= 1;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                oneLine ? Ui.dp(50) : ViewGroup.LayoutParams.WRAP_CONTENT);
        // short 16:10 cover screens: tighter title spacing so more content fits
        lp.setMargins(Ui.dp(Ui.gStyle ? 24 : 18), Ui.dp(Ui.cover() ? 4 : 14), Ui.dp(Ui.gStyle ? 20 : 16), Ui.dp(Ui.cover() ? 0 : 6));
        content.addView(bigTitle, lp);
    }

    /** How many lines {@code s} takes at {@code sp} in {@code width} px with the big-title font. */
    private static int lines(String s, int width, float sp) {
        if (s == null || s.isEmpty() || width <= 0) return 1;
        android.text.TextPaint p = new android.text.TextPaint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Ui.bold);
        p.setTextSize(Ui.sp(sp));
        p.setLetterSpacing(-0.01f);
        return android.text.StaticLayout.Builder.obtain(s, 0, s.length(), p, width).build().getLineCount();
    }

    /** Largest size (36 → 24sp) at which the title fits in one line, else in two, else three lines at 24sp. */
    static float fitTitleSize(String s, int width) {
        for (int maxLines = 1; maxLines <= 2; maxLines++)
            for (float sp = 36f; sp >= 26f; sp -= 2f) if (lines(s, width, sp) <= maxLines) return sp;
        return 24f;
    }

    void setGrouped(boolean g) {
        grouped = g;
        setBackgroundColor(g ? Ui.groupedBg() : Ui.bg());
    }

    /** Called when the page becomes visible; builds content once. */
    void show() {
        if (!started) {
            started = true;
            if (Ui.gStyle && back && bigTitle != null && bigTitle.getVisibility() == VISIBLE) {
                bigTitle.setVisibility(GONE);
                bigTitle.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0));
            }
            if (getBackground() == null) setBackgroundColor(grouped ? Ui.groupedBg() : Ui.bg());
            build();
        }
    }

    /** Where {@link #slot()} puts new blocks (a side column in two-pane layouts); null = the page itself. */
    LinearLayout slotHost;

    void rebuild() {
        started = false;
        slotHost = null;
        content.removeAllViews();
        if (bigTitle != null) addBigTitle();
        show();
    }

    abstract void build();

    void onScrolled(int y) {}

    /** 0 → large title fully visible, 1 → small header title shown. */
    float titleProgress() {
        int y = scroll.getScrollY();
        if (Ui.gStyle && back) return 1f;
        if (y <= Ui.dp(8)) return 0f;
        if (bigTitle == null) return Ui.clamp((y - Ui.dp(140)) / (float) Ui.dp(24), 0f, 1f);
        float start = bigTitle.getTop() - Ui.statusInset - Ui.dp(Ui.gStyle ? 24 : 40);
        return Ui.clamp((y - start) / (float) Ui.dp(22), 0f, 1f);
    }

    float blurProgress() { return Ui.clamp(scroll.getScrollY() / (float) Ui.dp(20), 0f, 1f); }

    void scrollToTop() { scroll.smoothScrollTo(0, 0); }

    /** Adds a lazily-filled block so async sections keep their order. */
    LinearLayout slot() {
        LinearLayout s = new LinearLayout(getContext());
        s.setOrientation(LinearLayout.VERTICAL);
        (slotHost != null ? slotHost : content).addView(s,
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }
}
