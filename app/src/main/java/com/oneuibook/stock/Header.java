package com.oneuibook.stock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RecordingCanvas;
import android.graphics.RectF;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

/** Fixed top chrome: progressive blur under the status bar, collapsing small title, glass buttons. */
final class Header extends View {
    interface Listener {
        void onHeaderButton(int icon, RectF anchor);
        void onBack();
    }

    private final Glass.Backdrop backdrop;
    private Listener listener;
    private final Glass.Surface pillGlass = new Glass.Surface();
    private final Glass.Surface backGlass = new Glass.Surface();
    private final RenderNode blurNode = new RenderNode("header-blur");
    private final Paint mask = new Paint();
    private final Paint wash = new Paint();
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private String title = "";
    private int[] icons = {Icons.PLUS};
    private boolean back;
    float titleAlpha, blurAlpha;
    private float contentAlpha = 1f;
    boolean onDarkContent;

    final RectF pill = new RectF(), backRect = new RectF();
    private int pressed = -1; // index into icons, or 99 for back
    private final Ui.Spring press = new Ui.Spring(700f, 0.9f, 0.002f);

    Header(Context c, Glass.Backdrop backdrop) {
        super(c);
        this.backdrop = backdrop;
        titlePaint.setTypeface(Ui.bold);
        titlePaint.setTextSize(Ui.sp(17));
        mask.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        pillGlass.blurDp = 10;
        pillGlass.refractDp = 15;
        pillGlass.edgeDp = 20;
        pillGlass.outerRefractDp = 11;
        pillGlass.vibrance = 1.35f;
        pillGlass.rim = 0.32f;
        pillGlass.shadowAlpha = 0.07f;
        backGlass.blurDp = 10;
        backGlass.refractDp = 6;
        backGlass.edgeDp = 12;
        backGlass.rim = 0.25f;
        backGlass.shadowAlpha = 0.07f;
        press.onUpdate = this::invalidate;
        morph.onUpdate = this::invalidate;
        backP.onUpdate = this::invalidate;
        morph.snap(1f);
    }

    void setListener(Listener l) { listener = l; }

    // ---- morph between pages: pill reshapes (2 icons <-> 1 <-> none), back button pops in/out ----
    private int[] prevIcons = new int[0];
    private final RectF fromPill = new RectF(), drawn = new RectF();
    private final Ui.Spring morph = new Ui.Spring(340f, 0.74f, 0.002f);
    private final Ui.Spring backP = new Ui.Spring(420f, 0.72f, 0.002f);
    private boolean configured;

    void configure(String t, int[] ic, boolean b) {
        title = t == null ? "" : t;
        int[] next = ic == null ? new int[0] : ic;
        if (!java.util.Arrays.equals(next, icons)) {
            prevIcons = icons;
            fromPill.set(drawn);
            icons = next;
            if (configured) { morph.snap(0f); morph.to(1f); } else morph.snap(1f);
        }
        if (b != back || !configured) {
            back = b;
            if (configured) backP.to(b ? 1f : 0f); else backP.snap(b ? 1f : 0f);
        }
        configured = true;
        invalidate();
    }

    // ---- fold / unfold / rotate: the pill reshapes from where it was on the previous screen ----
    private float[] pendingFrom;

    /** Pill position across an activity re-creation: x as a screen fraction, y in px (the top inset doesn't change). */
    float[] snapshot() {
        float W = Math.max(1, getWidth());
        if (drawn.width() < 1) return null;
        return new float[]{drawn.left / W, drawn.top, drawn.right / W, drawn.bottom};
    }

    void morphFrom(float[] g) {
        if (g == null || g.length < 4) return;
        pendingFrom = g;
        invalidate();
    }

    private void startPendingMorph() {
        if (pendingFrom == null || getWidth() <= 0 || icons.length == 0) return;
        float W = getWidth();
        fromPill.set(pendingFrom[0] * W, pendingFrom[1], pendingFrom[2] * W, pendingFrom[3]);
        pendingFrom = null;
        prevIcons = icons;          // the same icons fold away and come back laid out for the new shape
        morph.snap(0f);
        morph.to(1f);
    }

    void setScroll(float titleA, float blurA) {
        if (Math.abs(titleA - titleAlpha) < 0.003f && Math.abs(blurA - blurAlpha) < 0.003f) return;
        titleAlpha = titleA;
        blurAlpha = blurA;
        invalidate();
    }

    void setContentAlpha(float a) { contentAlpha = a; invalidate(); }

    private RenderEffect blurFx;

    /** True while any of the header's own springs is moving (it must redraw even if the page is idle). */
    boolean animating() { return morph.running || backP.running || press.running; }

    private boolean pillHidden;
    boolean vertical;
    private float touchX, touchY;

    /** While a menu grows out of the pill, the menu *is* the pill: hide the original. */
    void setPillHidden(boolean h) { pillHidden = h; invalidate(); }

    int barBottom() { return Ui.statusInset + Ui.dp(60); }

    /** Right-hand header pill bounds in root coordinates (ensuring layout is up to date). */
    RectF pillInRoot() {
        layoutRects();
        float[] o = Glass.abs(this);
        RectF a = new RectF(pill);
        a.offset(o[0], o[1]);
        return a;
    }

    private void layoutRects() {
        float top = Ui.statusInset + Ui.dpf(14);
        // foldable inner screen: the pill stands upright, matching the vertical tab rail
        boolean rail = Dock.railFor(getRootView().getWidth() > 0 ? getRootView().getWidth() : Ui.screenW, getRootView().getHeight() > 0 ? getRootView().getHeight() : Ui.screenH);
        vertical = rail && icons.length > 1;
        // on the foldable rail the buttons share the right-hand control column (60dp wide, 20dp margin)
        float h = Ui.dpf(rail ? 60 : 46);
        float len = icons.length <= 1 ? h : Ui.dpf(rail ? 112 : 100);
        float right = getWidth() - Ui.dpf(rail ? 20 : 16);
        if (vertical) pill.set(right - h, top, right, top + len);
        else pill.set(right - len, top, right, top + h);
        if (icons.length == 0) pill.setEmpty();
        // Align backRect vertically dead-centre with the 46dp header pill row, respecting sidebar left padding
        float padL = getPaddingLeft();
        backRect.set(padL + Ui.dpf(16), top + Ui.dpf(1), padL + Ui.dpf(16) + Ui.dpf(44), top + Ui.dpf(45));
    }

    @Override protected void onDraw(Canvas c) {
        layoutRects();
        if (pendingFrom != null) startPendingMorph();
        int w = getWidth(), h = getHeight();
        // progressive blur strip
        if (blurAlpha > 0.01f && c.isHardwareAccelerated() && backdrop.recorded && Ui.glass) {
            int sc = c.saveLayer(0, 0, w, h, null);
            blurNode.setPosition(0, 0, w, h);
            if (blurFx == null) blurFx = RenderEffect.createBlurEffect(Ui.dpf(22), Ui.dpf(22), Shader.TileMode.CLAMP);
            blurNode.setRenderEffect(blurFx);
            RecordingCanvas rc = blurNode.beginRecording(w, h);
            try {
                float[] off = Glass.offsetIn(this, backdrop);
                rc.translate(-off[0], -off[1]);
                rc.drawRenderNode(backdrop.node);
            } finally { blurNode.endRecording(); }
            c.drawRenderNode(blurNode);
            int bg = onDarkContent ? 0xFF000000 : Ui.bg();
            wash.setShader(new LinearGradient(0, 0, 0, h, new int[]{Ui.alpha(bg, 0.72f), Ui.alpha(bg, 0.35f), Ui.alpha(bg, 0f)},
                    new float[]{0f, 0.55f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, wash);
            mask.setShader(new LinearGradient(0, 0, 0, h, new int[]{Ui.alpha(0xFF000000, blurAlpha), Ui.alpha(0xFF000000, blurAlpha), 0},
                    new float[]{0f, 0.6f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, mask);
            c.restoreToCount(sc);
        } else if (blurAlpha > 0.01f) {
            wash.setShader(new LinearGradient(0, 0, 0, h, new int[]{Ui.alpha(Ui.bg(), 0.95f * blurAlpha), Ui.alpha(Ui.bg(), 0f)},
                    null, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, w, h, wash);
        }
        if (contentAlpha <= 0.01f) return;
        int fg = Ui.text();
        // back button: in G style a clean borderless chevron (<) like One UI 9.x; in A style a glass circle
        float bp = backP.value;
        float padL = getPaddingLeft();
        float titleX = Ui.lerp(padL + Ui.dpf(Ui.gStyle ? 22 : 16), backRect.right + Ui.dpf(Ui.gStyle ? 6 : 12), Ui.clamp(bp, 0f, 1f));
        if (bp > 0.01f) {
            float s = Ui.lerp(0.35f, 1f, bp), ba = Ui.clamp(bp, 0f, 1f) * contentAlpha;
            RectF br = tmpRect;
            br.set(backRect.centerX() - backRect.width() / 2f * s, backRect.centerY() - backRect.height() / 2f * s,
                    backRect.centerX() + backRect.width() / 2f * s, backRect.centerY() + backRect.height() / 2f * s);
            if (!Ui.gStyle) {
                backGlass.shapes(br, br.height() / 2f, null, 0, null, 0);
                backGlass.alpha = ba;
                backGlass.tint = Ui.barTint();
                backGlass.blurDp = Ui.barBlur();
                backGlass.draw(c, this, backdrop);
            }
            Icons.draw(c, Icons.BACK, br.centerX() - Ui.dpf(1), br.centerY(), Ui.dpf(Ui.gStyle ? 24 : 22) * s, Ui.alpha(fg, ba));
            if (pressed == 99) {
                fill.setColor(Ui.alpha(Ui.pressed(), press.value));
                c.drawOval(br, fill);
            }
        }
        // small title (20sp Bold in G style, matching One UI 9.x collapsed header / sub-page bar)
        if (titleAlpha > 0.01f && title.length() > 0) {
            titlePaint.setColor(Ui.alpha(fg, titleAlpha * contentAlpha));
            int rw = getRootView().getWidth() > 0 ? getRootView().getWidth() : Ui.screenW;
            int rh = getRootView().getHeight() > 0 ? getRootView().getHeight() : Ui.screenH;
            boolean onTablet = Dock.tabletFor(rw, rh);
            float rightBound = pill.isEmpty() ? w - Ui.dpf(16) : pill.left - Ui.dpf(12);
            if (onTablet) {
                // Keep header title cleanly to the left of the centred tablet top tab pill (zero overlap!)
                float maxTabLeft = (w + padL - Math.min(rw * 0.42f, Ui.dpf(440)) - Ui.dpf(92)) / 2f - Ui.dpf(10);
                rightBound = Math.min(rightBound, maxTabLeft);
            }
            float avail = rightBound - titleX;
            if (avail >= Ui.dpf(40)) {
                float rowCy = vertical ? pill.top + Ui.dpf(23) : (pill.isEmpty() ? backRect.centerY() : pill.centerY());
                // long names get smaller before anything is cut off
                float ts = Ui.gStyle ? (onTablet ? 18f : 20f) : 17f;
                titlePaint.setTextSize(Ui.sp(ts));
                while (ts > 13f && titlePaint.measureText(title) > avail) { ts -= 1f; titlePaint.setTextSize(Ui.sp(ts)); }
                CharSequence t = TextUtils.ellipsize(title, titlePaint, avail, TextUtils.TruncateAt.END);
                float baseline = rowCy + titlePaint.getTextSize() * 0.36f;
                c.drawText(t, 0, t.length(), titleX, baseline - Ui.dpf(3) * (1f - titleAlpha), titlePaint);
            }
        }
        // pill action buttons: in G style, keep a crisp frosted pill surface even at top so icons always pop cleanly
        float k = morph.value;
        RectF from = fromPill.isEmpty() ? pointAt(pill.isEmpty() ? fromPill : pill) : fromPill;
        RectF to = pill.isEmpty() ? pointAt(fromPill) : pill;
        drawn.set(Ui.lerp(from.left, to.left, k), Ui.lerp(from.top, to.top, k), Ui.lerp(from.right, to.right, k), Ui.lerp(from.bottom, to.bottom, k));
        float pillA = pill.isEmpty() ? 1f - Ui.clamp(k, 0f, 1f) : (fromPill.isEmpty() ? Ui.clamp(k * 1.5f, 0f, 1f) : 1f);
        if (drawn.width() > 1 && pillA > 0.01f && !pillHidden) {
            float surfaceA = Ui.gStyle ? Ui.clamp(0.68f + blurAlpha * 0.32f, 0f, 1f) : 1f;
            pillGlass.shapes(drawn, Math.min(drawn.width(), drawn.height()) / 2f, null, 0, null, 0);
            pillGlass.alpha = contentAlpha * pillA * surfaceA;
            pillGlass.tint = Ui.barTint();
            pillGlass.blurDp = Ui.barBlur();
            // pressing the pill makes it glow white under the finger before the menu drops out of it
            pillGlass.pressX = touchX; pillGlass.pressY = touchY;
            pillGlass.pressR = Ui.dpf(40);
            pillGlass.pressAmt = pressed >= 0 && pressed != 99 ? press.value : 0f;
            if (pillGlass.alpha > 0.02f) pillGlass.draw(c, this, backdrop);
            // outgoing icons shrink away during the first half, incoming ones pop in during the second
            if (k < 0.5f && prevIcons.length > 0) drawIcons(c, prevIcons, drawn, 1f - k * 2f, fg);
            float inA = prevIcons.length == 0 && fromPill.isEmpty() ? Ui.clamp(k, 0f, 1f) : Ui.clamp(k * 2f - 1f, 0f, 1f);
            drawIcons(c, icons, drawn, inA, fg);
        }
    }

    private final RectF tmpRect = new RectF(), pointRect = new RectF();

    private RectF pointAt(RectF r) {
        pointRect.set(r.centerX(), r.centerY(), r.centerX(), r.centerY());
        return pointRect;
    }

    /** The pill's icons, laid out in {@code box} (any canvas), for a menu that grows out of the pill. */
    void drawGlyphs(Canvas c, RectF box, float a) {
        float keep = contentAlpha;
        contentAlpha = 1f;
        int pr = pressed;
        pressed = -1;
        drawIcons(c, icons, box, a, Ui.text());
        pressed = pr;
        contentAlpha = keep;
    }

    private void drawIcons(Canvas c, int[] set, RectF box, float a, int fg) {
        if (a <= 0.01f) return;
        int n = set.length;
        boolean vert = box.height() > box.width() * 1.2f;
        float s = 0.6f + 0.4f * a;
        {
            for (int i = 0; i < n; i++) {
                float along = n == 1 ? 0.5f : (i * 2 + 1) / (2f * n);
                float nudge = n == 1 ? 0 : (i == 0 ? Ui.dpf(3) : -Ui.dpf(3));
                float cx = vert ? box.centerX() : box.left + box.width() * along + nudge;
                float cy = vert ? box.top + box.height() * along + nudge : box.centerY();
                if (pressed == i && press.value > 0.01f && !Glass.shadersOk) {
                    fill.setColor(Ui.alpha(Ui.pressed(), press.value));
                    float r = Math.min(pill.width(), pill.height()) / 2f - Ui.dpf(3);
                    c.drawCircle(cx, cy, r, fill);
                }
                Icons.draw(c, set[i], cx, cy, Ui.dpf(23) * s, Ui.alpha(fg, contentAlpha * a));
            }
        }
    }

    private int hit(float x, float y) {
        if (back && x <= backRect.right + Ui.dpf(8) && y <= backRect.bottom + Ui.dpf(8) && y >= backRect.top - Ui.dpf(8)) return 99;
        if (!pill.isEmpty() && x >= pill.left - Ui.dpf(6) && y >= pill.top - Ui.dpf(6) && y <= pill.bottom + Ui.dpf(6)) {
            int n = icons.length;
            int i = vertical ? (int) ((y - pill.top) / (pill.height() / n)) : (int) ((x - pill.left) / (pill.width() / n));
            return Math.max(0, Math.min(n - 1, i));
        }
        return -1;
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (contentAlpha < 0.5f) return false;
                layoutRects();
                pressed = hit(e.getX(), e.getY());
                if (pressed < 0) return false;
                touchX = e.getX(); touchY = e.getY();
                press.to(1f);
                return true;
            case MotionEvent.ACTION_UP: {
                int p = pressed;
                press.to(0f);
                if (p >= 0 && hit(e.getX(), e.getY()) == p && listener != null) {
                    if (p == 99) listener.onBack();
                    else {
                        float[] o = Glass.abs(this);
                        RectF a = new RectF(pill);
                        a.offset(o[0], o[1]);
                        listener.onHeaderButton(icons[p], a);
                    }
                }
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                press.to(0f);
                return true;
        }
        return pressed >= 0;
    }
}
