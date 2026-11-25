package com.oneuibook.stock;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Choreographer;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.PathInterpolator;
import android.widget.TextView;

/** Theme, metrics, typography and small animation helpers shared by every screen. */
final class Ui {
    private Ui() {}

    static float density = 3f;
    static float fontScale = 1f;
    static boolean dark;
    static boolean glass = true;
    static int screenW, screenH;
    static int statusInset, navInset, imeInset, cutoutLeft, cutoutRight;

    static final int ACCENT_DEFAULT = 0xFF19B36B;
    static int ACCENT = ACCENT_DEFAULT;
    static final String[] ACCENT_KEYS = {"green", "blue", "red", "violet", "teal", "orange", "pink", "dynamic"};
    static final int[] ACCENT_COLORS = {0xFF19B36B, 0xFF387AFF, 0xFFE92D44, 0xFF7B61FF, 0xFF0FB59D, 0xFFF26D21, 0xFFE84593, 0};
    static final int[] ACCENT_LABELS = {R.string.accent_green, R.string.accent_blue, R.string.accent_red, R.string.accent_violet, R.string.accent_teal, R.string.accent_orange, R.string.accent_pink, R.string.accent_dynamic};
    static final TimeInterpolator EMPHASIZED = new PathInterpolator(0.2f, 0f, 0f, 1f);
    static final TimeInterpolator DECEL = new PathInterpolator(0.05f, 0.7f, 0.1f, 1f);
    static final TimeInterpolator STANDARD = new PathInterpolator(0.2f, 0f, 0f, 1f);
    static final TimeInterpolator ACCEL = new PathInterpolator(0.3f, 0f, 0.8f, 0.15f);
    // One UI 9.x official motion curves (from SESL / memory & incallui reference APKs)
    static final TimeInterpolator ONEUI_RECOIL_PRESS = new PathInterpolator(0f, 0f, 1f, 1f);
    static final TimeInterpolator ONEUI_RECOIL_RELEASE = new PathInterpolator(0.22f, 0.25f, 0f, 1f);
    static final TimeInterpolator ONEUI_SINE_80 = new PathInterpolator(0.33f, 0f, 0.20f, 1f);
    static final TimeInterpolator ONEUI_SINE_90 = new PathInterpolator(0.33f, 0f, 0.10f, 1f);
    static final TimeInterpolator ONEUI_POPUP_ENTER = new PathInterpolator(0.365f, 0.483f, 0f, 1.215f);

    static Typeface regular, medium, semibold, bold, heavy;

    static String accentKey(SharedPreferences p) { return p.getString("accent_color", "blue"); }

    static int resolveAccent(Context c, String key) {
        if ("dynamic".equals(key)) {
            try {
                int col = c.getColor(dark ? android.R.color.system_accent1_300 : android.R.color.system_accent1_600);
                return (col & 0xFFFFFF) | 0xFF000000;
            } catch (Throwable ignored) {
                return ACCENT_DEFAULT;
            }
        }
        for (int i = 0; i < ACCENT_KEYS.length; i++)
            if (ACCENT_KEYS[i].equals(key) && ACCENT_COLORS[i] != 0) return ACCENT_COLORS[i];
        return ACCENT_DEFAULT;
    }

    static void init(Context c, SharedPreferences p) {
        density = c.getResources().getDisplayMetrics().density;
        fontScale = Math.min(1.3f, c.getResources().getConfiguration().fontScale);
        screenW = c.getResources().getDisplayMetrics().widthPixels;
        screenH = c.getResources().getDisplayMetrics().heightPixels;
        String theme = p.getString("theme", "system");
        if ("dark".equals(theme)) dark = true;
        else if ("light".equals(theme)) dark = false;
        else dark = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        glass = p.getBoolean("glass", true);
        glassT = readGlassT(p);
        unfoldTablet = "tablet".equals(p.getString("unfold_ui", "fold"));
        gStyle = "oneui".equals(p.getString("ui_style", p.getString("player_style", "prism")));
        ACCENT = resolveAccent(c, accentKey(p));
        Typeface base = Typeface.SANS_SERIF;
        regular = Typeface.create(base, 400, false);
        medium = Typeface.create(base, 500, false);
        semibold = Typeface.create(base, 600, false);
        bold = Typeface.create(base, 700, false);
        heavy = Typeface.create(base, 800, false);
    }

    static int dp(float v) { return Math.round(v * density); }

    /**
     * Galaxy Z Fold8 cover screen (16:10) and similar short-and-wide portrait phones: phone width class,
     * but much less height than a 19.5:9 phone, so cards are made shorter to keep content on screen.
     */
    static boolean cover() {
        float wd = screenW / density;
        float ratio = screenH / (float) Math.max(1, screenW);
        return wd < 600 && ratio > 1f && ratio <= 1.75f;
    }

    /** Wide foldable inner screen (4:3 aspect ratio, e.g. Galaxy Z Fold8 unfolded). */
    static boolean wideInner() {
        float wd = screenW / density, hd = screenH / density;
        float minD = Math.min(wd, hd), maxD = Math.max(wd, hd);
        return minD >= 600 && (maxD / Math.max(1f, minD)) <= 1.45f;
    }
    static float dpf(float v) { return v * density; }
    static float sp(float v) { return v * density * fontScale; }

    // ---- palette (G style matches One UI 8.5/9.x recording & reference APKs) ----
    static int bg() { return dark ? (gStyle ? 0xFF010102 : 0xFF000000) : (gStyle ? 0xFFF2F2F5 : 0xFFFFFFFF); }
    static int groupedBg() { return dark ? (gStyle ? 0xFF010102 : 0xFF000000) : (gStyle ? 0xFFF2F2F5 : 0xFFF2F2F7); }
    static int card() { return dark ? (gStyle ? 0xFF17171A : 0xFF1C1C1E) : (gStyle ? 0xFFFFFFFF : 0xFFFFFFFF); }
    static int text() { return dark ? (gStyle ? 0xFFFCFCFF : 0xFFFFFFFF) : (gStyle ? 0xFF010102 : 0xFF000000); }
    static int text2() { return dark ? (gStyle ? 0xFF99999E : 0x99EBEBF5) : (gStyle ? 0xFF77777A : 0x993C3C43); }
    static int text3() { return dark ? 0x66EBEBF5 : 0x4D3C3C43; }
    static int chip() { return dark ? (gStyle ? 0xFF252528 : 0xFF1C1C1E) : (gStyle ? 0xFFE8E8ED : 0xFFF2F1F6); }
    static int separator() { return dark ? (gStyle ? 0x1EFFFFFF : 0x33FFFFFF) : (gStyle ? 0x10000000 : 0x1F000000); }
    static int placeholder() { return dark ? 0xFF2C2C2E : 0xFFE9E9EE; }
    static int glassTint() { return dark ? 0x5C1C1C1E : 0x5CFAFAFC; }
    static int glassStroke() { return dark ? 0x33FFFFFF : 0x24000000; }
    static int gStroke() { return dark ? 0x1FFFFFFF : 0x12000000; }
    static int gOutlineTop() { return dark ? 0x24FFFFFF : 0xB2FFFFFF; }
    static float cardRadius() { return gStyle ? 26f : 16f; }
    /**
     * Glass level, one setting for every floating surface so the tab bar and the music bar always match.
     * "high" = high transparency (Prism's original look), "medium" = denser frost.
     */
    /**
     * Glass transparency 0..1, one setting for every floating surface so the tab bar and the music bar always match.
     * 1 = the old "high" level (Prism's original look), .5 = the default (the old "medium" frost),
     * 0 = opaque. Piecewise blend opaque → medium → high.
     */
    static float glassT = 0.5f;
    /** Unfolded foldables: true = the tablet UI (top tabs, sidebar) instead of the foldable right-hand rail. */
    static boolean unfoldTablet;
    /**
     * App style. A (false) = the glass look; G (true) = frosted translucent pills: same shapes and springy motion,
     * no refraction / lens / rim. Stored as player_style ("prism" = A, "oneui" = G).
     */
    static boolean gStyle;

    /** G style fill: One UI 9.x basic_token_effect_blur_surface (#CCFCFCFF light / #CC3A3A3D dark), responsive to glassT. */
    static int gTint(int t) {
        int a = t >>> 24;
        if (a >= 242) return t;
        int rgb = (dark ? 0xFF3A3A3D : 0xFFFCFCFF) & 0xFFFFFF;
        int minA = Math.round(lerp(dark ? 236f : 244f, dark ? 182f : 188f, glassT));
        int ga = Math.max(a, minA);
        return (ga << 24) | rgb;
    }
    static int glassBlend(int opaque, int medium, int high) {
        return glassT >= 0.5f ? mix(medium, high, (glassT - 0.5f) * 2f) : mix(opaque, medium, glassT * 2f);
    }
    static int barTint() {
        return dark ? glassBlend(0xFF1C1C1E, 0xA61C1C1E, 0x521C1C1E) : glassBlend(0xFFFAFAFC, 0xA8FAFAFC, 0x40FFFFFF);
    }
    static float barBlur() { return glassT >= 0.5f ? lerp(6f, 2f, (glassT - 0.5f) * 2f) : 6f; }
    static int menuTint() {
        return dark ? glassBlend(0xF51C1C1E, 0xB81C1C1E, 0x8C1C1C1E) : glassBlend(0xF5FAFAFC, 0xBDFAFAFC, 0x8CFAFAFC);
    }
    static float readGlassT(SharedPreferences p) {
        if (p.contains("glass_t")) return clamp(p.getFloat("glass_t", 0.5f), 0f, 1f);
        String old = p.getString("glass_level", null);      // migrate the old two-level choice
        return "high".equals(old) ? 1f : 0.5f;
    }

    static int pressed() { return dark ? (gStyle ? 0x33FFFFFF : 0x1FFFFFFF) : (gStyle ? 0x1A000000 : 0x0F000000); }

    static int alpha(int color, float a) {
        return (Math.round(Color.alpha(color) * Math.max(0f, Math.min(1f, a))) << 24) | (color & 0xFFFFFF);
    }

    static int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255, aa = a >>> 24;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255, ba = b >>> 24;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }

    static TextView text(Context c, CharSequence s, float sizeSp, Typeface tf, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp);
        t.setTypeface(tf);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }

    static GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dpf(radiusDp));
        return g;
    }

    /** Press feedback: in G style uses One UI 9.x SESL Recoil (100ms linear press, 350ms cubic 0.22,0.25,0,1 release). */
    static void press(View v, float scale) {
        v.setOnTouchListener((view, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    view.animate().scaleX(scale).scaleY(scale)
                            .setDuration(gStyle ? 100 : 140)
                            .setInterpolator(gStyle ? ONEUI_RECOIL_PRESS : STANDARD).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    view.animate().scaleX(1f).scaleY(1f)
                            .setDuration(gStyle ? 350 : 420)
                            .setInterpolator(gStyle ? ONEUI_RECOIL_RELEASE : new Overshoot(1.6f)).start();
                    break;
            }
            return false;
        });
    }

    static final class Overshoot implements TimeInterpolator {
        final float tension;
        Overshoot(float t) { tension = t; }
        @Override public float getInterpolation(float t) {
            t -= 1.0f;
            return t * t * ((tension + 1) * t + tension) + 1.0f;
        }
    }

    static ValueAnimator animate(float from, float to, long ms, TimeInterpolator in, ValueAnimator.AnimatorUpdateListener l) {
        ValueAnimator a = ValueAnimator.ofFloat(from, to);
        a.setDuration(ms);
        a.setInterpolator(in);
        a.addUpdateListener(l);
        a.start();
        return a;
    }

    /** Children appearing in a container (loaded sections, cards, rows) fade up into place. */
    static android.animation.LayoutTransition appear() {
        android.animation.LayoutTransition t = new android.animation.LayoutTransition();
        android.animation.ObjectAnimator in = android.animation.ObjectAnimator.ofPropertyValuesHolder((Object) null,
                android.animation.PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                android.animation.PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, dpf(14), 0f));
        in.setInterpolator(EMPHASIZED);
        t.setAnimator(android.animation.LayoutTransition.APPEARING, in);
        t.setDuration(android.animation.LayoutTransition.APPEARING, 340);
        t.setStagger(android.animation.LayoutTransition.APPEARING, 28);
        t.setStartDelay(android.animation.LayoutTransition.APPEARING, 0);
        // keep it cheap: no animated re-layout of siblings or parents
        t.disableTransitionType(android.animation.LayoutTransition.CHANGE_APPEARING);
        t.disableTransitionType(android.animation.LayoutTransition.CHANGE_DISAPPEARING);
        t.disableTransitionType(android.animation.LayoutTransition.DISAPPEARING);
        t.setAnimateParentHierarchy(false);
        return t;
    }

    static String time(long ms) {
        long s = Math.max(0, ms / 1000);
        return (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
    }

        static String size(long bytes) { if (bytes < 1024) return bytes + " B"; if (bytes < 1048576L) return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024f); if (bytes < 1073741824L) return String.format(java.util.Locale.US, "%.1f MB", bytes / 1048576f); return String.format(java.util.Locale.US, "%.2f GB", bytes / 1073741824f); }

    /** Critically-damped-ish spring driven by Choreographer (framework has no SpringAnimation without AndroidX). */
    static final class Spring implements Choreographer.FrameCallback {
        float value, target, velocity;
        float stiffness, damping;
        float precision;
        boolean running;
        private long last;
        Runnable onUpdate, onEnd;

        Spring(float stiffness, float dampingRatio, float precision) {
            this.stiffness = stiffness;
            this.damping = dampingRatio;
            this.precision = precision;
        }

        void to(float t) {
            target = t;
            if (!running) {
                running = true;
                last = 0;
                Choreographer.getInstance().postFrameCallback(this);
            }
        }

        void snap(float v) {
            if (running) Choreographer.getInstance().removeFrameCallback(this);
            running = false;
            value = target = v;
            velocity = 0;
            if (onUpdate != null) onUpdate.run();
        }

        @Override public void doFrame(long now) {
            float dt = last == 0 ? 1f / 120f : Math.min((now - last) / 1e9f, 1f / 30f);
            last = now;
            int steps = 6;
            float h = dt / steps;
            float w0 = (float) Math.sqrt(stiffness);
            float c = 2f * damping * w0;
            for (int i = 0; i < steps; i++) {
                float a = -stiffness * (value - target) - c * velocity;
                velocity += a * h;
                value += velocity * h;
            }
            if (Math.abs(velocity) < precision * 6 && Math.abs(value - target) < precision) {
                value = target;
                velocity = 0;
                running = false;
                if (onUpdate != null) onUpdate.run();
                if (onEnd != null) onEnd.run();
                return;
            }
            if (onUpdate != null) onUpdate.run();
            Choreographer.getInstance().postFrameCallback(this);
        }
    }
}
