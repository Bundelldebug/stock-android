package com.oneuibook.stock;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RecordingCanvas;
import android.graphics.RectF;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.text.Editable;
import android.text.InputType;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

/**
 * Floating adaptive chrome (matching Prism):
 * - Phone: bottom glass tab pill with draggable refracting lens + detached search bubble
 * - Foldable unfolded: vertical glass tab rail on the right (or tablet mode per preference)
 * - Tablet: top-center glass tab pill with sidebar toggle
 */
final class Dock extends FrameLayout {
    interface Listener {
        void onTab(int index, boolean reselect);
        void onMini();
        void onSearch();
        void onExitSearch();
        void onQuery(String q, boolean submit);
        default void onSidebar() {}
    }

    static final int[] TAB_ICONS = {Icons.HOME, Icons.CHART, Icons.WALLET, Icons.SETTINGS};

    private final Glass.Backdrop backdrop;
    private Listener listener;
    private final String[] labels;
    int selected;

    boolean rail, tablet;
    private float sideW, sideP;

    float collapse, search;
    private ValueAnimator collapseAnim, searchAnim, miniShowAnim, hintTypeAnim, hintShimmerAnim;
    private float hintType = 0f, hintShimmer = 0f;
    private final TextPaint hintPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final String searchHintText;
    private int imeOffset;

    final RectF pill = new RectF(), circle = new RectF(), lens = new RectF(), mini = new RectF();
    private final RectF tmp = new RectF();

    private final Glass.Surface barGlass = new Glass.Surface();
    private final Glass.Surface lensGlass = new Glass.Surface();
    private final Glass.Surface miniGlass = new Glass.Surface();
    private final RenderNode contentNode = new RenderNode("dock-content");

    private final Ui.Spring lensX = new Ui.Spring(380f, 0.98f, 0.4f);
    private final Ui.Spring lift = new Ui.Spring(460f, 1.0f, 0.002f);
    private final Ui.Spring panelRecoil = new Ui.Spring(340f, 0.96f, 0.1f);
    private final Ui.Spring miniPress = new Ui.Spring(700f, 0.9f, 0.002f);
    private final Ui.Spring searchPress = new Ui.Spring(700f, 0.9f, 0.002f);
    private final Ui.Spring barPress = new Ui.Spring(520f, 0.85f, 0.002f);
    private float touchX, touchY, dragVel;
    private VelocityTracker vt;

    private final TextPaint label = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint title = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint sub = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint art = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Matrix artM = new Matrix();
    private Bitmap thumbBmp;
    private String thumbKey;

    final EditText field;
    private boolean miniVisible;
    float miniShow;

    private static final int T_NONE = 0, T_TAB = 1, T_SEARCH = 2, T_MINI = 3, T_MINI_PLAY = 4, T_MINI_CLOSE = 5, T_SIDE = 6;
    private int touch = T_NONE;
    private float downX, downY;
    private boolean dragging;

    Dock(Context c, Glass.Backdrop backdrop, String[] labels) {
        super(c);
        this.backdrop = backdrop;
        this.labels = labels;
        setWillNotDraw(false);
        setClipChildren(false);
        label.setTypeface(Ui.semibold);
        label.setTextSize(Ui.sp(10.5f));
        label.setTextAlign(Paint.Align.CENTER);
        title.setTypeface(Ui.semibold);
        title.setTextSize(Ui.sp(14f));
        sub.setTypeface(Ui.regular);
        sub.setTextSize(Ui.sp(12f));
        hintPaint.setTypeface(Ui.medium);
        hintPaint.setTextSize(Ui.sp(15.5f));
        searchHintText = c.getString(R.string.search_hint);

        barGlass.blurDp = 2.5f;
        barGlass.refractDp = 17f;
        barGlass.edgeDp = 26f;
        barGlass.outerDp = 7f;
        barGlass.outerRefractDp = 12f;
        barGlass.chroma = 0f;
        barGlass.vibrance = 1.35f;
        barGlass.rim = 0.32f;

        miniGlass.blurDp = 2f;
        miniGlass.refractDp = 17f;
        miniGlass.edgeDp = 24f;
        miniGlass.outerDp = 7f;
        miniGlass.outerRefractDp = 12f;
        miniGlass.chroma = 0f;
        miniGlass.vibrance = 1.5f;
        miniGlass.rim = 0.32f;

        lensGlass.blur = false;
        lensGlass.refractDp = 16f;
        lensGlass.edgeDp = 24f;
        lensGlass.outerDp = 7.5f;
        lensGlass.outerRefractDp = 13f;
        lensGlass.depthEffect = 0.65f;
        lensGlass.chroma = 0.95f;
        lensGlass.rim = 0.56f;
        lensGlass.tint = Ui.dark ? 0x10FFFFFF : 0x0AFFFFFF;
        lensGlass.shadowAlpha = 0.18f;
        lensGlass.strokeColor = Ui.dark ? 0x42FFFFFF : 0x1C000000;

        Runnable inv = this::invalidate;
        lensX.onUpdate = inv;
        lift.onUpdate = inv;
        panelRecoil.onUpdate = inv;
        miniPress.onUpdate = inv;
        searchPress.onUpdate = inv;
        barPress.onUpdate = inv;

        field = new EditText(c);
        field.setBackground(null);
        field.setSingleLine(true);
        field.setHint(null);
        field.setIncludeFontPadding(false);
        field.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15.5f);
        field.setTextColor(Ui.text());
        field.setHintTextColor(Ui.text2());
        field.setTypeface(Ui.regular);
        field.setInputType(InputType.TYPE_CLASS_TEXT);
        field.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        field.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        field.setPadding(0, 0, 0, 0);
        field.setVisibility(GONE);
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int d) {}
            @Override public void afterTextChanged(Editable e) {
                invalidate();
                if (listener != null) listener.onQuery(e.toString(), false);
            }
        });
        field.setOnEditorActionListener((v, id, ev) -> {
            if (id == EditorInfo.IME_ACTION_SEARCH || (ev != null && ev.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                if (listener != null) listener.onQuery(field.getText().toString(), true);
                hideIme();
                return true;
            }
            return false;
        });
        addView(field, new LayoutParams(1, 1));
    }

    static boolean tabletFor(int W, int H) {
        float wd = W / Ui.density, hd = H / Ui.density;
        if (Math.min(wd, hd) < 600) return false;
        float ratio = Math.max(wd, hd) / Math.max(1f, Math.min(wd, hd));
        return Ui.unfoldTablet || ratio >= 1.35f;
    }

    static boolean railFor(int W, int H) {
        float wd = W / Ui.density, hd = H / Ui.density;
        if (Math.min(wd, hd) < 600) return false;
        return !tabletFor(W, H);
    }

    void setListener(Listener l) { listener = l; }

    void setSidebar(float widthPx, float progress) {
        sideW = widthPx;
        sideP = progress;
        invalidate();
    }

    private float contentLeft() { return tablet ? sideW * sideP : 0f; }
    private float lead() { return tablet ? Ui.dpf(46) * (1f - search) : 0f; }
    private boolean unifiedG() { return Ui.gStyle && !rail && !tablet; }
    private int slotCount() { return unifiedG() ? 5 : 4; }
    private float pillK() { return Ui.clamp(collapse / 0.65f, 0f, 1f); }
    private float miniK() { return Ui.clamp((collapse - 0.12f) / 0.88f, 0f, 1f); }

    void select(int i, boolean animate) {
        selected = Ui.clamp(i, 0, 3) == i ? i : 0;
        if (getWidth() > 0) {
            layoutRects();
            float target = tabCenter(selected);
            if (animate) lensX.to(target); else lensX.snap(target);
        }
        invalidate();
    }

    void setCollapsed(boolean c) {
        if (rail || tablet || search > 0.1f) c = false;
        float to = c ? 1f : 0f;
        if (collapseAnim != null) collapseAnim.cancel();
        collapseAnim = Ui.animate(collapse, to, 320, Ui.EMPHASIZED, a -> { collapse = (float) a.getAnimatedValue(); invalidate(); });
    }

    void setSearchMode(boolean on) {
        float to = on ? 1f : 0f;
        if (searchAnim != null) searchAnim.cancel();
        if (hintTypeAnim != null) hintTypeAnim.cancel();
        if (hintShimmerAnim != null) hintShimmerAnim.cancel();
        if (on) {
            field.setVisibility(VISIBLE);
            field.setCursorVisible(true);
            field.requestFocus();
            showIme();
            hintType = 0f;
            // Typing-out hint animation ("무엇을 찾고 있나요?")
            hintTypeAnim = ValueAnimator.ofFloat(0f, 1f);
            hintTypeAnim.setStartDelay(140);
            hintTypeAnim.setDuration(720);
            hintTypeAnim.setInterpolator(Ui.DECEL);
            hintTypeAnim.addUpdateListener(a -> { hintType = (float) a.getAnimatedValue(); invalidate(); });
            hintTypeAnim.start();
            // Subtle continuous gradient shimmer across the hint text
            hintShimmerAnim = ValueAnimator.ofFloat(0f, 1f);
            hintShimmerAnim.setDuration(2600);
            hintShimmerAnim.setRepeatCount(ValueAnimator.INFINITE);
            hintShimmerAnim.setInterpolator(new android.view.animation.LinearInterpolator());
            hintShimmerAnim.addUpdateListener(a -> {
                if (field.getText().length() == 0 && search > 0.2f) {
                    hintShimmer = (float) a.getAnimatedValue();
                    invalidate();
                }
            });
            hintShimmerAnim.start();
        } else {
            field.setText("");
            field.setCursorVisible(false);
            field.clearFocus();
            hideIme();
            hintType = 0f;
        }
        searchAnim = Ui.animate(search, to, 360, Ui.EMPHASIZED, a -> {
            search = (float) a.getAnimatedValue();
            if (search <= 0.01f && !on) field.setVisibility(GONE);
            placeField();
            invalidate();
        });
    }

    void setImeOffset(int px) { imeOffset = px; placeField(); invalidate(); }

    boolean animating() {
        return lensX.running || lift.running || miniPress.running || searchPress.running || barPress.running
                || (collapseAnim != null && collapseAnim.isRunning())
                || (searchAnim != null && searchAnim.isRunning())
                || (miniShowAnim != null && miniShowAnim.isRunning())
                || (hintTypeAnim != null && hintTypeAnim.isRunning());
    }

    float[] snapshot() { return new float[]{getWidth(), getHeight()}; }

    private void showIme() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(field, InputMethodManager.SHOW_IMPLICIT);
    }

    private void hideIme() {
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(field.getWindowToken(), 0);
    }

    private void layoutRects() {
        int W = getWidth(), H = getHeight();
        if (W <= 0 || H <= 0) return;
        tablet = tabletFor(W, H);
        rail = railFor(W, H);
        float bottomY = H - Ui.navInset - Ui.dpf(14) - imeOffset;

        if (tablet) {
            // Top-center floating glass tab pill + search circle, clamped cleanly away from right header pill
            float top = Ui.statusInset + Ui.dpf(14);
            float h = Ui.dpf(48);
            float cl = contentLeft();
            float rightReserve = Ui.dpf(132);
            float usableW = Math.max(Ui.dpf(200), W - cl - rightReserve);
            float gap = Ui.dpf(10);
            float maxPillW = Math.max(Ui.dpf(180), usableW - (search > 0.5f ? Ui.dpf(16) : h + gap + Ui.dpf(16)));
            float pillW = Math.min(maxPillW, Ui.lerp(Ui.dpf(400), Ui.dpf(460), search));
            float totalW = search > 0.5f ? pillW : pillW + gap + h;
            float left = cl + Math.max(Ui.dpf(12), (usableW - totalW) / 2f);
            pill.set(left, top, left + pillW, top + h);
            circle.set(pill.right + gap, top, pill.right + gap + h, top + h);
            if (search > 0.5f) circle.setEmpty();
            float availW = W - cl;
            float cx = cl + availW / 2f;
            float mw = Math.min(availW - Ui.dpf(48), Ui.dpf(780));
            float mh = Ui.dpf(64);
            mini.set(cx - mw / 2f, bottomY - mh, cx + mw / 2f, bottomY);
        } else if (rail) {
            // Foldable unfolded: vertical rail on the right
            float rw = Ui.dpf(60);
            float right = W - Ui.dpf(20);
            float rh = Ui.dpf(256);
            float cy = H / 2f;
            pill.set(right - rw, cy - rh / 2f, right, cy + rh / 2f);
            circle.set(right - rw, pill.bottom + Ui.dpf(12), right, pill.bottom + Ui.dpf(12) + rw);
            if (search > 0.01f) {
                float sw = Math.min(W - Ui.dpf(40), Ui.dpf(460));
                circle.set(Ui.lerp(circle.left, (W - sw) / 2f, search),
                        Ui.lerp(circle.top, bottomY - Ui.dpf(52), search),
                        Ui.lerp(circle.right, (W + sw) / 2f, search),
                        Ui.lerp(circle.bottom, bottomY, search));
            }
            float mw = Math.min(W - Ui.dpf(120), Ui.dpf(480));
            float mh = Ui.dpf(56);
            mini.set(Ui.dpf(20), bottomY - mh, Ui.dpf(20) + mw, bottomY);
        } else if (unifiedG()) {
            // G style phone: unified floating pill containing 4 tabs + search slot (5 slots total).
            float pad = Ui.dpf(16);
            float pk = pillK(), mk = miniK();
            float h = Ui.lerp(Ui.dpf(64), Ui.dpf(52), pk);
            float bottom = bottomY + Ui.dpf(4) * pk;
            float top = bottom - h;
            float fullW = Math.min(W - pad * 2, Ui.dpf(420));
            float expLeft = (W - fullW) / 2f, expRight = expLeft + fullW;
            if (search > 0.01f) {
                float sh = Ui.dpf(54);
                pill.set(Ui.lerp(expLeft, pad, search), Ui.lerp(top, bottomY - sh, search),
                        Ui.lerp(expRight, W - pad, search), Ui.lerp(bottom, bottomY, search));
            } else {
                pill.set(Ui.lerp(expLeft, pad, pk), top, Ui.lerp(expRight, pad + h, pk), bottom);
            }
            circle.setEmpty();
            float mh = Ui.dpf(58);
            float expBottom = top - Ui.dpf(10);
            float cl = pad + h + Ui.dpf(10), cr = W - pad;
            mini.set(Ui.lerp(expLeft, cl, mk), Ui.lerp(expBottom - mh, top, mk),
                    Ui.lerp(expRight, cr, mk), Ui.lerp(expBottom, bottom, mk));
            if (search > 0.01f) mini.offset(0, Ui.dpf(26) * search);
        } else {
            // A style phone: bottom tab pill + detached search bubble + mini player bar.
            float pad = Ui.dpf(16);
            float pk = pillK(), mk = miniK();
            float h = Ui.lerp(Ui.dpf(62), Ui.dpf(52), pk);
            float bottom = bottomY + Ui.dpf(4) * pk;
            float top = bottom - h;
            float gap = Ui.dpf(10);
            float expandedRight = W - pad - Ui.dpf(62) - gap;
            if (search > 0.01f) {
                float sLeft = Ui.lerp(W - pad - h, pad, search);
                circle.set(sLeft, bottomY - Ui.dpf(54), W - pad, bottomY);
                pill.set(pad, top, Ui.lerp(expandedRight, pad, search), bottom);
            } else {
                float pillRight = Ui.lerp(expandedRight, pad + h, pk);
                pill.set(pad, top, pillRight, bottom);
                circle.set(W - pad - h, top, W - pad, bottom);
            }
            float mh = Ui.dpf(56);
            float expBottom = bottomY - Ui.dpf(62) - Ui.dpf(10);
            float eLeft = pad, eTop = expBottom - mh, eRight = W - pad, eBot = expBottom;
            float cl = pad + h + gap, cr = W - pad - h - gap;
            mini.set(Ui.lerp(eLeft, cl, mk), Ui.lerp(eTop, top, mk), Ui.lerp(eRight, cr, mk), Ui.lerp(eBot, bottom, mk));
            if (search > 0.01f) mini.offset(0, Ui.dpf(26) * search);
        }

        if (!lensX.running && !dragging) lensX.snap(tabCenter(selected));
        placeField();
    }

    private float tabCenter(int i) {
        if (rail) return pill.top + (i + 0.5f) * (pill.height() / 4f);
        float start = pill.left + lead();
        float w = Math.max(1f, pill.width() - lead());
        return start + (i + 0.5f) * (w / slotCount());
    }

    private void placeField() {
        if (field.getVisibility() != VISIBLE) return;
        RectF box = (tablet || unifiedG()) ? pill : circle;
        int l = Math.round(box.left + Ui.dpf(44));
        int t = Math.round(box.top);
        int r = Math.round(box.right - Ui.dpf(44));
        int b = Math.round(box.bottom);
        int w = r - l, h = b - t;
        if (w > 0 && h > 0) {
            field.measure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY));
            field.layout(l, t, r, b);
            field.setAlpha(Ui.clamp((search - 0.35f) / 0.65f, 0f, 1f));
        }
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        layoutRects();
        if (changed) lensX.snap(tabCenter(selected));
    }

    @Override protected void dispatchDraw(Canvas c) {
        if (search <= 0.05f && field.getVisibility() == VISIBLE && (searchAnim == null || !searchAnim.isRunning())) {
            field.setVisibility(GONE);
        }
        layoutRects();
        int fg = Ui.text(), fg2 = Ui.text2();

        // 2. Main tab pill + search bubble
        barGlass.tint = Ui.barTint();
        barGlass.blurDp = Ui.barBlur();
        barGlass.smoothK = Ui.dpf(18) * lift.value;
        barGlass.pressX = touchX; barGlass.pressY = touchY;
        barGlass.pressR = Ui.dpf(48);
        barGlass.pressAmt = (touch == T_TAB || touch == T_SEARCH) ? Math.max(barPress.value, searchPress.value) : 0f;

        boolean showPill = pill.width() > Ui.dpf(24);
        boolean showCircle = !circle.isEmpty() && circle.width() > Ui.dpf(24);
        if (showPill && showCircle) {
            barGlass.shapes(pill, Math.min(pill.width(), pill.height()) / 2f, circle, circle.height() / 2f, null, 0);
        } else if (showPill) {
            barGlass.shapes(pill, Math.min(pill.width(), pill.height()) / 2f, null, 0, null, 0);
        } else if (showCircle) {
            barGlass.shapes(circle, circle.height() / 2f, null, 0, null, 0);
        } else {
            barGlass.shapes(new RectF(), 0, null, 0, null, 0);
        }
        barGlass.alpha = 1f;
        barGlass.draw(c, this, backdrop);

        // 3. Selection capsule / Kyant0 + MeiloX dual-pass refracting lens
        if (showPill && search < 0.8f && collapse < 0.8f) {
            float l = lift.value;
            float vNorm = Ui.clamp(dragVel / 1800f, -0.22f, 0.22f);
            float stretchX = 1f / (1f - Math.abs(vNorm) * 0.72f);
            float squashY = 1f - Math.abs(vNorm) * 0.24f;
            float recoil = panelRecoil.value;
            if (rail) {
                float slotH = pill.height() / 4f;
                float lh = Ui.lerp(slotH - Ui.dpf(8), slotH + Ui.dpf(6), l) * stretchX;
                float lw = Ui.lerp(pill.width() - Ui.dpf(10), pill.width() + Ui.dpf(4), l) * squashY;
                lens.set(pill.centerX() - lw / 2f, lensX.value + recoil - lh / 2f, pill.centerX() + lw / 2f, lensX.value + recoil + lh / 2f);
            } else {
                float slotW = (pill.width() - lead()) / slotCount();
                float lw = Ui.lerp(slotW - Ui.dpf(8), slotW + Ui.dpf(12), l) * stretchX;
                float lh = Ui.lerp(pill.height() - Ui.dpf(10), pill.height() + Ui.dpf(4), l) * squashY;
                lens.set(lensX.value + recoil - lw / 2f, pill.centerY() - lh / 2f, lensX.value + recoil + lw / 2f, pill.centerY() + lh / 2f);
            }
            if (Ui.gStyle) {
                fill.setColor(Ui.alpha(Ui.ACCENT, Ui.dark ? 0.24f : 0.14f));
                c.drawRoundRect(lens, lens.height() / 2f, lens.height() / 2f, fill);
            } else if (l > 0.02f && c.isHardwareAccelerated()) {
                contentNode.setPosition(0, 0, getWidth(), getHeight());
                RecordingCanvas rc = contentNode.beginRecording(getWidth(), getHeight());
                try { drawTabItems(rc, Ui.ACCENT, Ui.ACCENT, true); } finally { contentNode.endRecording(); }
                lensGlass.extra = contentNode;
                lensGlass.shapes(lens, Math.min(lens.width(), lens.height()) / 2f, null, 0, null, 0);
                lensGlass.mag = Ui.lerp(1f, 1.20f, l);
                lensGlass.magX = lens.centerX();
                lensGlass.magY = lens.centerY();
                lensGlass.innerShadow = l;
                lensGlass.alpha = 1f;
                drawTabItems(c, fg, fg2, false);
                lensGlass.draw(c, this, backdrop);
                drawSearchAndLead(c, fg);
                super.dispatchDraw(c);
                return;
            } else {
                fill.setColor(Ui.dark ? 0x26FFFFFF : 0x14000000);
                c.drawRoundRect(lens, Math.min(lens.width(), lens.height()) / 2f, Math.min(lens.width(), lens.height()) / 2f, fill);
            }
            drawTabItems(c, fg, fg2, false);
        } else if (showPill && collapse >= 0.8f) {
            Icons.draw(c, TAB_ICONS[selected], pill.centerX(), pill.centerY(), Ui.dpf(22), Ui.ACCENT);
        }

        drawSearchAndLead(c, fg);
        super.dispatchDraw(c);
    }

    private void drawTabItems(Canvas c, int fg, int fg2, boolean forceActive) {
        float alpha = 1f - Math.max(collapse, search);
        if (alpha <= 0.01f) return;
        int slots = slotCount();
        for (int i = 0; i < slots; i++) {
            boolean isSearchSlot = i == 4;
            boolean on = forceActive || (!isSearchSlot && i == selected);
            int col = Ui.alpha(on ? Ui.ACCENT : fg2, alpha);
            int ic = isSearchSlot ? Icons.SEARCH : TAB_ICONS[i];
            String lb = isSearchSlot ? getContext().getString(R.string.search) : labels[i];
            if (rail) {
                float cy = tabCenter(i);
                Icons.draw(c, ic, pill.centerX(), cy, Ui.dpf(23), col);
            } else if (tablet) {
                float cx = tabCenter(i);
                label.setColor(col);
                label.setTextSize(Ui.sp(13.5f));
                label.setTypeface(on ? Ui.bold : Ui.medium);
                c.drawText(lb, cx, pill.centerY() + label.getTextSize() * 0.36f, label);
            } else {
                float cx = tabCenter(i);
                Icons.draw(c, ic, cx, pill.centerY() - Ui.dpf(7), Ui.dpf(21), col);
                label.setColor(col);
                label.setTextSize(Ui.sp(10f));
                label.setTypeface(on ? Ui.bold : Ui.medium);
                c.drawText(lb, cx, pill.centerY() + Ui.dpf(15), label);
            }
        }
    }

    private void drawSearchAndLead(Canvas c, int fg) {
        if (tablet && search < 0.8f) {
            Icons.draw(c, Icons.SIDEBAR, pill.left + Ui.dpf(26), pill.centerY(), Ui.dpf(21), Ui.alpha(fg, 1f - search));
        }
        RectF sb = (tablet || unifiedG()) && search > 0.5f ? pill : circle;
        if (!sb.isEmpty()) {
            float sx = search > 0.2f ? sb.left + Ui.dpf(24) : sb.centerX();
            Icons.draw(c, Icons.SEARCH, sx, sb.centerY(), Ui.dpf(21), search > 0.5f ? Ui.text2() : fg);
            if (search > 0.5f) {
                float closeA = (search - 0.5f) * 2f;
                Icons.draw(c, Icons.CLOSE, sb.right - Ui.dpf(24), sb.centerY(), Ui.dpf(18), Ui.alpha(fg, closeA));
                // Animated typing gradient hint ("무엇을 찾고 있나요?")
                if (field.getText().length() == 0 && searchHintText != null && !searchHintText.isEmpty()) {
                    int total = searchHintText.length();
                    int shown = Math.max(1, Math.min(total, Math.round(total * (hintType <= 0f ? 1f : hintType))));
                    String visible = searchHintText.substring(0, shown);
                    float hx = sb.left + Ui.dpf(44);
                    Paint.FontMetrics fm = hintPaint.getFontMetrics();
                    float hy = sb.centerY() - (fm.ascent + fm.descent) / 2f;
                    float tw = Math.max(Ui.dpf(140), hintPaint.measureText(searchHintText));
                    float shift = (hintShimmer * 2f - 0.5f) * tw;
                    hintPaint.setShader(new LinearGradient(hx + shift, hy, hx + shift + tw, hy,
                            new int[]{0xFF7B5CFF, 0xFF387AFF, 0xFF18B8D8, 0xFF7B5CFF},
                            new float[]{0f, 0.38f, 0.72f, 1f}, Shader.TileMode.MIRROR));
                    hintPaint.setAlpha((int) (225 * closeA));
                    c.drawText(visible, hx, hy, hintPaint);
                    hintPaint.setShader(null);
                }
            }
        }
    }

    private int slotAt(float x, float y) {
        if (rail) {
            int i = (int) ((y - pill.top) / (pill.height() / 4f));
            return Math.max(0, Math.min(3, i));
        }
        int slots = slotCount();
        float start = pill.left + lead();
        float w = Math.max(1f, pill.width() - lead());
        int i = (int) ((x - start) / (w / slots));
        return Math.max(0, Math.min(slots - 1, i));
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX(), y = e.getY();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = touchX = x; downY = touchY = y;
                dragging = false;
                if (miniShow > 0.5f && search < 0.2f && mini.contains(x, y)) {
                    boolean compact = !tablet && !rail && collapse > 0.35f;
                    if (compact) {
                        if (x > mini.right - Ui.dpf(48)) touch = T_MINI_PLAY;
                        else touch = T_MINI;
                    } else {
                        if (x > mini.right - Ui.dpf(44)) touch = T_MINI_CLOSE;
                        else if (x > mini.right - Ui.dpf(86)) touch = T_MINI_PLAY;
                        else touch = T_MINI;
                    }
                    miniPress.to(1f);
                    return true;
                }
                RectF sb = (tablet || unifiedG()) && search > 0.5f ? pill : circle;
                if (!sb.isEmpty() && sb.contains(x, y)) {
                    if (search > 0.5f && x > sb.right - Ui.dpf(48)) {
                        if (listener != null) listener.onExitSearch();
                        return true;
                    }
                    touch = T_SEARCH;
                    searchPress.to(1f);
                    return true;
                }
                if (pill.contains(x, y) && search < 0.5f) {
                    if (tablet && x < pill.left + lead()) {
                        touch = T_SIDE;
                        barPress.to(1f);
                        return true;
                    }
                    if (unifiedG() && collapse < 0.5f && slotAt(x, y) == 4) {
                        touch = T_SEARCH;
                        searchPress.to(1f);
                        return true;
                    }
                    touch = T_TAB;
                    barPress.to(1f);
                    if (collapse < 0.5f) {
                        lift.to(1f);
                        vt = VelocityTracker.obtain();
                        vt.addMovement(e);
                    }
                    return true;
                }
                touch = T_NONE;
                return false;

            case MotionEvent.ACTION_MOVE:
                touchX = x; touchY = y;
                if (touch == T_TAB && collapse < 0.5f) {
                    if (vt != null) {
                        vt.addMovement(e);
                        vt.computeCurrentVelocity(1000);
                        float v = rail ? vt.getYVelocity() : vt.getXVelocity();
                        dragVel = dragVel * 0.65f + v * 0.35f;
                    }
                    float d = rail ? Math.abs(y - downY) : Math.abs(x - downX);
                    if (d > Ui.dpf(8)) dragging = true;
                    if (dragging) {
                        float pos = rail
                                ? Ui.clamp(y, pill.top + pill.height() / 8f, pill.bottom - pill.height() / 8f)
                                : Ui.clamp(x, tabCenter(0), tabCenter(3));
                        lensX.snap(pos);
                        float span = Math.max(1f, rail ? pill.height() : pill.width());
                        float frac = Ui.clamp((rail ? (y - downY) : (x - downX)) / span, -1f, 1f);
                        panelRecoil.snap(Ui.dpf(4f) * Math.signum(frac) * (1f - (1f - Math.abs(frac)) * (1f - Math.abs(frac))));
                    }
                    return true;
                }
                return touch != T_NONE;

            case MotionEvent.ACTION_UP: {
                int t = touch;
                touch = T_NONE;
                dragVel = 0f;
                panelRecoil.to(0f);
                miniPress.to(0f); searchPress.to(0f); barPress.to(0f); lift.to(0f);
                if (vt != null) { vt.recycle(); vt = null; }
                if (t == T_MINI && listener != null) listener.onMini();


                else if (t == T_SIDE && listener != null) listener.onSidebar();
                else if (t == T_SEARCH && listener != null) {
                    if (search < 0.5f) listener.onSearch();
                } else if (t == T_TAB && listener != null) {
                    if (collapse >= 0.5f) {
                        setCollapsed(false);
                    } else {
                        int idx = Math.min(3, slotAt(x, y));
                        boolean re = idx == selected;
                        selected = idx;
                        lensX.to(tabCenter(idx));
                        listener.onTab(idx, re);
                    }
                }
                dragging = false;
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                touch = T_NONE;
                dragging = false;
                dragVel = 0f;
                panelRecoil.to(0f);
                miniPress.to(0f); searchPress.to(0f); barPress.to(0f); lift.to(0f);
                if (vt != null) { vt.recycle(); vt = null; }
                lensX.to(tabCenter(selected));
                return true;
        }
        return false;
    }
}
