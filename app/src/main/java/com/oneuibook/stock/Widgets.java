package com.oneuibook.stock;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Reusable cards, groups, toggles, sliders, chips and glass sheets. */
final class Widgets {
    private Widgets() {}

    static final LinearLayout.LayoutParams MW = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

    static RippleDrawable ripple() {
        return new RippleDrawable(ColorStateList.valueOf(Ui.pressed()), null, Ui.round(0xFFFFFFFF, 16));
    }

    static TextView section(Context c, String title) {
        TextView t = Ui.text(c, title, 21, Ui.bold, Ui.text());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Ui.dp(20), Ui.dp(26), Ui.dp(18), Ui.dp(12));
        t.setLayoutParams(lp);
        return t;
    }

    static TextView caption(Context c, String text) {
        TextView t = Ui.text(c, text, 13, Ui.regular, Ui.text2());
        t.setSingleLine(false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(Ui.dp(22), Ui.dp(4), Ui.dp(22), Ui.dp(10));
        t.setLayoutParams(lp);
        return t;
    }

    static LinearLayout rail(LinearLayout parent, int spacingDp) {
        HorizontalScrollView hs = new HorizontalScrollView(parent.getContext());
        hs.setHorizontalScrollBarEnabled(false);
        hs.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hs.setClipToPadding(false);
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(Ui.dp(18), 0, Ui.dp(18 - spacingDp), 0);
        hs.addView(row);
        parent.addView(hs, MW);
        row.setTag(spacingDp);
        return row;
    }

    static int contentW(MainActivity m) {
        return Math.max(Ui.dp(260), Ui.screenW - (m == null ? 0 : m.sideInset() + m.contentInsetPx()));
    }

    static TextView pillBadge(Context c, String text, int bg, int fg) {
        TextView t = Ui.text(c, text, 10, Ui.bold, fg);
        t.setPadding(Ui.dp(6), Ui.dp(2), Ui.dp(6), Ui.dp(2));
        t.setBackground(Ui.round(bg, 6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = Ui.dp(4);
        t.setLayoutParams(lp);
        return t;
    }

    /** Primary pill button. */
    static TextView button(Context c, String label, boolean primary, View.OnClickListener click) {
        TextView b = Ui.text(c, label, 15, Ui.semibold, primary ? 0xFFFFFFFF : Ui.text());
        b.setSingleLine(false);
        b.setEllipsize(null);
        b.setGravity(Gravity.CENTER);
        b.setPadding(Ui.dp(22), Ui.dp(12), Ui.dp(22), Ui.dp(12));
        b.setBackground(Ui.round(primary ? Ui.ACCENT : Ui.chip(), 100));
        b.setOnClickListener(click);
        Ui.press(b, 0.95f);
        return b;
    }

    /** Grouped inset card (26dp radius in G style). */
    static final class Group extends LinearLayout {
        private final LinearLayout card;
        private int count;

        Group(Context c, String header) {
            super(c);
            setOrientation(VERTICAL);
            setPadding(Ui.dp(16), Ui.dp(10), Ui.dp(16), Ui.dp(6));
            if (header != null && !header.isEmpty()) {
                TextView h = Ui.text(c, header, 13, Ui.semibold, Ui.text2());
                h.setPadding(Ui.dp(14), Ui.dp(6), Ui.dp(14), Ui.dp(8));
                addView(h, MW);
            }
            card = new LinearLayout(c);
            card.setOrientation(VERTICAL);
            GradientDrawable bg = Ui.round(Ui.card(), Ui.cardRadius());
            if (Ui.gStyle) bg.setStroke(Ui.dp(0.8f), Ui.gStroke());
            card.setBackground(bg);
            card.setClipToOutline(true);
            addView(card, MW);
        }

        View row(int icon, int iconTint, String title, String value, boolean chevron, OnClickListener click) {
            if (count > 0) {
                View sep = new View(getContext());
                sep.setBackgroundColor(Ui.separator());
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(0.6f)));
                slp.leftMargin = Ui.dp(icon != 0 ? 58 : 18);
                slp.rightMargin = Ui.dp(18);
                card.addView(sep, slp);
            }
            count++;
            LinearLayout r = new LinearLayout(getContext());
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14));
            if (click != null) {
                r.setBackground(ripple());
                r.setOnClickListener(click);
            }
            if (icon != 0) {
                if (Ui.gStyle) {
                    FrameLayout badge = new FrameLayout(getContext());
                    badge.setBackground(Ui.round(Ui.alpha(iconTint, Ui.dark ? 0.22f : 0.13f), 17));
                    badge.addView(new Icons.IconView(getContext(), icon, 18, iconTint),
                            new FrameLayout.LayoutParams(Ui.dp(34), Ui.dp(34)));
                    LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(Ui.dp(34), Ui.dp(34));
                    blp.rightMargin = Ui.dp(14);
                    r.addView(badge, blp);
                } else {
                    LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(Ui.dp(24), Ui.dp(24));
                    ilp.rightMargin = Ui.dp(14);
                    r.addView(new Icons.IconView(getContext(), icon, 21, iconTint), ilp);
                }
            }
            TextView t = Ui.text(getContext(), title, 16, Ui.medium, Ui.text());
            r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (value != null && !value.isEmpty()) {
                TextView v = Ui.text(getContext(), value, 14.5f, Ui.regular, Ui.text2());
                LinearLayout.LayoutParams vlp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                vlp.leftMargin = Ui.dp(8);
                r.addView(v, vlp);
            }
            if (chevron) {
                LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(Ui.dp(18), Ui.dp(18));
                clp.leftMargin = Ui.dp(6);
                r.addView(new Icons.IconView(getContext(), Icons.CHEVRON, 15, Ui.text3()), clp);
            }
            card.addView(r, MW);
            return r;
        }

        View option(String title, boolean checked, OnClickListener click) {
            if (count > 0) {
                View sep = new View(getContext());
                sep.setBackgroundColor(Ui.separator());
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(0.6f)));
                slp.leftMargin = Ui.dp(18); slp.rightMargin = Ui.dp(18);
                card.addView(sep, slp);
            }
            count++;
            LinearLayout r = new LinearLayout(getContext());
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.setPadding(Ui.dp(18), Ui.dp(14), Ui.dp(18), Ui.dp(14));
            if (click != null) {
                r.setBackground(ripple());
                r.setOnClickListener(click);
            }
            TextView t = Ui.text(getContext(), title, 16, checked ? Ui.semibold : Ui.regular, checked ? Ui.ACCENT : Ui.text());
            r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            if (checked) {
                r.addView(new Icons.IconView(getContext(), Icons.CHECK, 18, Ui.ACCENT),
                        new LinearLayout.LayoutParams(Ui.dp(20), Ui.dp(20)));
            }
            card.addView(r, MW);
            return r;
        }

        void addCustom(View v) {
            if (count > 0) {
                View sep = new View(getContext());
                sep.setBackgroundColor(Ui.separator());
                LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, Ui.dp(0.6f)));
                slp.leftMargin = Ui.dp(18); slp.rightMargin = Ui.dp(18);
                card.addView(sep, slp);
            }
            count++;
            card.addView(v, MW);
        }
    }

    /** Kyant0 AndroidLiquidGlass LiquidToggle (64×28dp track, 40×24dp refractive capsule thumb, critically damped). */
    static final class Toggle extends View {
        interface OnChange { void onChange(boolean on); }
        boolean on;
        OnChange change;
        private final Ui.Spring p = new Ui.Spring(380f, 1.0f, 0.002f);
        private final Ui.Spring press = new Ui.Spring(460f, 1.0f, 0.002f);
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rimP = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF(), knob = new RectF();
        private float downX;
        private boolean dragged;

        Toggle(Context c, boolean initial) {
            super(c);
            on = initial;
            p.snap(initial ? 1f : 0f);
            press.snap(0f);
            p.onUpdate = this::invalidate;
            press.onUpdate = this::invalidate;
            rimP.setStyle(Paint.Style.STROKE);
            setLayoutParams(new LinearLayout.LayoutParams(Ui.dp(64), Ui.dp(36)));
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            float dragW = Math.max(1f, Ui.dpf(20));
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    downX = e.getX(); dragged = false; press.to(1f); return true;
                case MotionEvent.ACTION_MOVE: {
                    float dx = e.getX() - downX;
                    if (Math.abs(dx) > Ui.dpf(3)) dragged = true;
                    if (dragged) p.to(Ui.clamp((on ? 1f : 0f) + dx / dragW, 0f, 1f));
                    return true;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    boolean next = dragged ? p.value >= 0.5f : !on;
                    if (e.getActionMasked() == MotionEvent.ACTION_CANCEL) next = on;
                    boolean changed = next != on;
                    on = next;
                    p.to(on ? 1f : 0f);
                    postDelayed(() -> press.to(0f), 110);
                    if (changed && change != null) change.onChange(on);
                    return true;
                }
            }
            return true;
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float k = Ui.clamp(p.value, 0f, 1f);
            float pr = Ui.clamp(press.value, 0f, 1f);
            float trackH = Ui.dpf(28), top = (h - trackH) * 0.5f;
            int offCol = Ui.dark ? 0x5C787880 : 0x33787878;
            int onCol = Ui.gStyle ? Ui.ACCENT : (Ui.dark ? 0xFF30D158 : 0xFF34C759);
            paint.setColor(Ui.mix(offCol, onCol, k));
            r.set(0f, top, w, top + trackH);
            c.drawRoundRect(r, trackH * 0.5f, trackH * 0.5f, paint);
            float pad = Ui.dpf(2), baseW = Ui.dpf(40), baseH = Ui.dpf(24);
            float cx = pad + baseW * 0.5f + Math.max(0f, w - baseW - pad * 2f) * k;
            float sc = Ui.lerp(1f, 1.38f, pr);
            float vel = Ui.clamp(p.velocity * 0.04f, -0.2f, 0.2f);
            float kw = baseW * sc / (1f - Math.abs(vel) * 0.75f);
            float kh = baseH * sc * (1f - Math.abs(vel) * 0.25f);
            knob.set(cx - kw * 0.5f, h * 0.5f - kh * 0.5f, cx + kw * 0.5f, h * 0.5f + kh * 0.5f);
            paint.setColor(Ui.gStyle ? 0xFFFFFFFF : Ui.mix(0xFFFFFFFF, 0x55FFFFFF, pr));
            paint.setShadowLayer(Ui.dpf(3.5f), 0, Ui.dpf(1.2f), 0x24000000);
            c.drawRoundRect(knob, kh * 0.5f, kh * 0.5f, paint);
            paint.clearShadowLayer();
            if (pr > 0.02f && !Ui.gStyle) {
                rimP.setStrokeWidth(Ui.dpf(1.2f));
                rimP.setColor(Ui.alpha(0xFFFFFFFF, 0.78f * pr));
                c.drawRoundRect(knob, kh * 0.5f, kh * 0.5f, rimP);
            }
        }
    }

    /** Smooth pill slider for glass transparency, subtitle size, audio boost, EQ, etc. */
    static final class Slider extends View {
        interface OnChange { void onChange(float value, boolean fromUser); }
        float value;
        OnChange change;
        boolean darkSurface;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        Slider(Context c, float initial) {
            super(c);
            value = Ui.clamp(initial, 0f, 1f);
            setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(38)));
        }

        void setValue(float v) { value = Ui.clamp(v, 0f, 1f); invalidate(); }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float pad = Ui.dpf(12);
            float th = Ui.dpf(6);
            float cy = h / 2f;
            float usable = Math.max(1f, w - pad * 2);
            p.setColor(darkSurface ? 0x33FFFFFF : Ui.separator());
            c.drawRoundRect(pad, cy - th / 2f, w - pad, cy + th / 2f, th / 2f, th / 2f, p);
            float fillX = pad + usable * value;
            p.setColor(Ui.ACCENT);
            c.drawRoundRect(pad, cy - th / 2f, fillX, cy + th / 2f, th / 2f, th / 2f, p);
            p.setColor(0xFFFFFFFF);
            p.setShadowLayer(Ui.dpf(4), 0, Ui.dpf(1.5f), 0x44000000);
            c.drawCircle(fillX, cy, Ui.dpf(10.5f), p);
            p.clearShadowLayer();
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getActionMasked() == MotionEvent.ACTION_DOWN || e.getActionMasked() == MotionEvent.ACTION_MOVE) {
                getParent().requestDisallowInterceptTouchEvent(true);
                float pad = Ui.dpf(12);
                float nv = Ui.clamp((e.getX() - pad) / Math.max(1f, getWidth() - pad * 2), 0f, 1f);
                if (Math.abs(nv - value) > 0.002f) {
                    value = nv;
                    invalidate();
                    if (change != null) change.onChange(value, true);
                }
                return true;
            }
            return super.onTouchEvent(e);
        }
    }

    /** Interactive glass slider with spring-lifted refracting thumb (matching Prism GlassSlider). */
    static final class GlassSlider extends View {
        interface OnChange { void onChange(float value); }
        private float v;
        private final OnChange cb;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF knob = new RectF();
        private final Ui.Spring lift = new Ui.Spring(520f, 0.72f, 0.002f);

        GlassSlider(Context c, float initial, OnChange cb) {
            super(c);
            this.v = Ui.clamp(initial, 0f, 1f);
            this.cb = cb;
            lift.onUpdate = this::invalidate;
            setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(40)));
        }

        void setValue(float nv) { v = Ui.clamp(nv, 0f, 1f); invalidate(); }

        @Override protected void onDraw(Canvas c) {
            float l = lift.value;
            float pad = Ui.dpf(18);
            float cy = getHeight() / 2f;
            float th = Ui.gStyle ? Ui.lerp(Ui.dpf(6), Ui.dpf(10), l) : Ui.dpf(6);
            float usable = Math.max(1f, getWidth() - pad * 2);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Ui.separator());
            c.drawRoundRect(pad, cy - th / 2f, getWidth() - pad, cy + th / 2f, th / 2f, th / 2f, p);
            float kx = pad + usable * v;
            p.setColor(Ui.ACCENT);
            c.drawRoundRect(pad, cy - th / 2f, kx, cy + th / 2f, th / 2f, th / 2f, p);
            float kw = Ui.lerp(Ui.dpf(26), Ui.dpf(34), l);
            float kh = Ui.lerp(Ui.dpf(18), Ui.dpf(23), l);
            knob.set(kx - kw / 2f, cy - kh / 2f, kx + kw / 2f, cy + kh / 2f);
            p.setColor(Ui.alpha(0xFF000000, Ui.lerp(0.16f, 0.24f, l)));
            c.save();
            c.translate(0, Ui.dpf(1.5f + l));
            c.drawRoundRect(knob, kh / 2f, kh / 2f, p);
            c.restore();
            p.setColor(0xFFFFFFFF);
            c.drawRoundRect(knob, kh / 2f, kh / 2f, p);
            if (Ui.gStyle && l > 0.02f) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Ui.dpf(2f) * l);
                p.setColor(Ui.alpha(Ui.ACCENT, l));
                c.drawRoundRect(knob, kh / 2f, kh / 2f, p);
                p.setStyle(Paint.Style.FILL);
            }
        }

        @Override public boolean onTouchEvent(MotionEvent e) {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_MOVE) {
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                if (a == MotionEvent.ACTION_DOWN) lift.to(1f);
                float pad = Ui.dpf(18);
                v = Ui.clamp((e.getX() - pad) / Math.max(1f, getWidth() - pad * 2), 0f, 1f);
                invalidate();
                if (cb != null) cb.onChange(v);
                return true;
            }
            if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) {
                lift.to(0f);
                return true;
            }
            return super.onTouchEvent(e);
        }
    }

    /** Horizontal scrollable filter chips bar. */
    static final class Chips extends HorizontalScrollView {
        interface Listener { void onSelect(int index); }
        private final LinearLayout row;
        private final String[] items;
        private int selected;

        Chips(Context c, String[] items, int initial, Listener l) {
            super(c);
            this.items = items;
            this.selected = initial;
            setHorizontalScrollBarEnabled(false);
            setOverScrollMode(OVER_SCROLL_NEVER);
            setClipToPadding(false);
            row = new LinearLayout(c);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(Ui.dp(18), Ui.dp(4), Ui.dp(18), Ui.dp(8));
            addView(row);
            rebuild(l);
        }

        private void rebuild(Listener l) {
            row.removeAllViews();
            for (int i = 0; i < items.length; i++) {
                final int idx = i;
                boolean on = i == selected;
                TextView ch = Ui.text(getContext(), items[i], 13.5f, on ? Ui.semibold : Ui.medium, on ? 0xFFFFFFFF : Ui.text());
                ch.setPadding(Ui.dp(15), Ui.dp(8), Ui.dp(15), Ui.dp(8));
                GradientDrawable bg = Ui.round(on ? Ui.ACCENT : Ui.chip(), 100);
                if (Ui.gStyle && !on) bg.setStroke(Ui.dp(0.8f), Ui.gStroke());
                ch.setBackground(bg);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.rightMargin = Ui.dp(8);
                ch.setOnClickListener(v -> {
                    if (selected != idx) {
                        selected = idx;
                        rebuild(l);
                        if (l != null) l.onSelect(idx);
                    }
                });
                Ui.press(ch, 0.95f);
                row.addView(ch, lp);
            }
        }
    }

    /** Spacious rounded input field for bottom sheets. */
    static android.widget.EditText formField(Context c, String label, String hint, String initial, int inputType, LinearLayout parent) {
        if (label != null && !label.isEmpty()) {
            TextView lb = Ui.text(c, label, 13, Ui.semibold, Ui.text2());
            LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            llp.setMargins(Ui.dp(4), Ui.dp(10), Ui.dp(4), Ui.dp(6));
            parent.addView(lb, llp);
        }
        android.widget.EditText et = new android.widget.EditText(c);
        et.setHint(hint);
        if (initial != null) et.setText(initial);
        et.setInputType(inputType);
        et.setSingleLine(true);
        et.setIncludeFontPadding(false);
        et.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15.5f);
        et.setTextColor(Ui.text());
        et.setHintTextColor(Ui.text3());
        et.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        et.setPadding(Ui.dp(16), Ui.dp(14), Ui.dp(16), Ui.dp(14));
        GradientDrawable bg = Ui.round(Ui.dark ? 0xFF28282E : 0xFFF2F3F7, 16);
        bg.setStroke(Ui.dp(1), Ui.separator());
        et.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(52));
        lp.bottomMargin = Ui.dp(6);
        parent.addView(et, lp);
        return et;
    }

    /** Spacious bottom sheet with primary + cancel buttons. */
    static void showFormSheet(MainActivity m, String title, LinearLayout formBody, String okLabel, Runnable onOk) {
        LinearLayout wrap = new LinearLayout(m);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(Ui.dp(20), Ui.dp(4), Ui.dp(20), Ui.dp(14));
        wrap.addView(formBody, MW);

        LinearLayout btns = new LinearLayout(m);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blp.topMargin = Ui.dp(14);

        TextView cancel = Ui.text(m, m.getString(R.string.cancel), 15, Ui.semibold, Ui.text());
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(0, Ui.dp(14), 0, Ui.dp(14));
        cancel.setBackground(Ui.round(Ui.chip(), 100));
        cancel.setOnClickListener(v -> GlassMenu.dismissAny(m.root));
        Ui.press(cancel, 0.96f);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        clp.rightMargin = Ui.dp(10);
        btns.addView(cancel, clp);

        TextView ok = Ui.text(m, okLabel, 15, Ui.semibold, 0xFFFFFFFF);
        ok.setGravity(Gravity.CENTER);
        ok.setPadding(0, Ui.dp(14), 0, Ui.dp(14));
        ok.setBackground(Ui.round(Ui.ACCENT, 100));
        ok.setOnClickListener(v -> {
            GlassMenu.dismissAny(m.root);
            if (onOk != null) onOk.run();
        });
        Ui.press(ok, 0.96f);
        btns.addView(ok, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        wrap.addView(btns, blp);
        RectF anchor = new RectF(Ui.screenW / 2f - Ui.dpf(40), Ui.screenH - Ui.dpf(80), Ui.screenW / 2f + Ui.dpf(40), Ui.screenH - Ui.dpf(40));
        GlassMenu.customSheet(m, anchor, title, wrap, null);
    }

    interface PickCb { void onPicked(int index); }

    /** Unified single-choice glass sheet replacing stock AlertDialog across all settings & pickers. */
    static void pickSheet(MainActivity m, View anchorView, String title, String[] labels, int selectedIdx, PickCb cb) {
        RectF anchor;
        if (anchorView != null && anchorView.getWidth() > 0) {
            float[] o = Glass.abs(anchorView);
            anchor = new RectF(o[0], o[1], o[0] + anchorView.getWidth(), o[1] + anchorView.getHeight());
        } else {
            anchor = new RectF(Ui.screenW / 2f - Ui.dpf(40), Ui.screenH - Ui.dpf(80), Ui.screenW / 2f + Ui.dpf(40), Ui.screenH - Ui.dpf(40));
        }
        java.util.List<GlassMenu.Entry> es = new java.util.ArrayList<>();
        for (int i = 0; i < labels.length; i++) {
            final int idx = i;
            GlassMenu.Entry e = new GlassMenu.Entry(labels[i], 0, i == selectedIdx, () -> {
                if (cb != null) cb.onPicked(idx);
            });
            e.checkable = true;
            es.add(e);
        }
        GlassMenu.sheet(m, anchor, title, es, null);
    }
}
