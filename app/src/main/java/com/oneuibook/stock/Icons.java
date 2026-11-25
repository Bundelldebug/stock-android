package com.oneuibook.stock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.CornerPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/** Hand-crafted 24x24 vector icons for Play Video Player. */
final class Icons {
    private Icons() {}

    static final int HOME = 1, FOLDER = 2, LIBRARY = 3, SETTINGS = 4, SEARCH = 5,
            PLAY = 6, PAUSE = 7, NEXT = 8, PREV = 9, BACK = 10, CHEVRON = 11,
            CHECK = 12, MORE = 13, CLOSE = 14, PLUS = 15, SIDEBAR = 16,
            VIDEO = 17, CLOCK = 18, HEART = 19, HEART_FILL = 20, PLAYLIST = 21,
            LINK = 22, SUBTITLE = 23, AI = 24, SPEED = 25, ASPECT = 26,
            LOCK = 27, UNLOCK = 28, ROTATE = 29, PIP = 30, BOOKMARK = 31,
            REPEAT = 32, REPEAT_ONE = 33, TIMER = 34, CAMERA = 35,
            VOLUME = 36, VOLUME_MUTE = 37, BRIGHTNESS = 38, EQ = 39,
            HEADPHONES = 40, REWIND = 41, FORWARD = 42, SORT = 43,
            GRID = 44, LIST = 45, INFO = 46, STORAGE = 47, APPEARANCE = 48,
            GLOBE = 49, REFRESH = 50, AB = 51, AUDIO = 52,
            WAVE = 53, DOWNLOAD = 54, DEVICE = 55, DAC = 56, EARBUDS = 57,
            BUDS = 58, BUDS2 = 59, BUDS4 = 60, BUDS_LIVE = 61, BUDS_ON = 62,
            CHEVRON_UP = 63, YOUTUBE = 64, PROSCALER = 65,
            CALENDAR = 70, CHART = 71, WALLET = 72, BELL = 73, CARD = 74, TAG = 75,
            TRASH = 76, EDIT = 77, PIE = 78, SHARE = 79, CHEVRON_L = 80, BELL_OFF = 81;

    private static final Paint P = new Paint(Paint.ANTI_ALIAS_FLAG);
    private static final CornerPathEffect ROUND1 = new CornerPathEffect(1.3f);
    private static final Path T = new Path();
    private static final RectF R = new RectF();

    static void draw(Canvas c, int id, float cx, float cy, float size, int color) {
        c.save();
        c.translate(cx - size / 2f, cy - size / 2f);
        c.scale(size / 24f, size / 24f);
        P.reset();
        P.setAntiAlias(true);
        P.setColor(color);
        P.setStrokeCap(Paint.Cap.ROUND);
        P.setStrokeJoin(Paint.Join.ROUND);
        render(c, id);
        c.restore();
    }

    private static void stroke(float w) { P.setStyle(Paint.Style.STROKE); P.setStrokeWidth(w); P.setPathEffect(null); }
    private static void fill() { P.setStyle(Paint.Style.FILL); P.setPathEffect(null); }

    private static void render(Canvas c, int id) {
        Path p = T;
        p.reset();
        switch (id) {
            case HOME:
                fill(); P.setPathEffect(ROUND1);
                p.moveTo(12f, 2.8f); p.lineTo(22.2f, 11.5f); p.lineTo(19.4f, 11.5f); p.lineTo(19.4f, 21f);
                p.lineTo(14.3f, 21f); p.lineTo(14.3f, 15.2f); p.lineTo(9.7f, 15.2f); p.lineTo(9.7f, 21f);
                p.lineTo(4.6f, 21f); p.lineTo(4.6f, 11.5f); p.lineTo(1.8f, 11.5f); p.close();
                c.drawPath(p, P);
                break;
            case FOLDER:
                fill(); P.setPathEffect(ROUND1);
                p.moveTo(2.5f, 6.2f); p.lineTo(9.6f, 6.2f); p.lineTo(11.6f, 8.6f); p.lineTo(21.5f, 8.6f);
                p.lineTo(21.5f, 19.4f); p.lineTo(2.5f, 19.4f); p.close();
                c.drawPath(p, P);
                break;
            case LIBRARY:
                fill();
                R.set(3.5f, 7.8f, 20.5f, 20.8f);
                c.drawRoundRect(R, 3.2f, 3.2f, P);
                R.set(6.2f, 4.8f, 17.8f, 6.3f); c.drawRoundRect(R, 0.8f, 0.8f, P);
                R.set(8.6f, 2.2f, 15.4f, 3.6f); c.drawRoundRect(R, 0.8f, 0.8f, P);
                break;
            case SETTINGS: {
                stroke(1.8f);
                int teeth = 12;
                for (int i = 0; i < teeth * 2; i++) {
                    double a = Math.PI * 2 * i / (teeth * 2) - Math.PI / 2;
                    float r = (i % 2 == 0) ? 10.2f : 8.6f;
                    double a1 = a - Math.PI / (teeth * 2) * 0.55, a2 = a + Math.PI / (teeth * 2) * 0.55;
                    float x1 = 12 + (float) Math.cos(a1) * r, y1 = 12 + (float) Math.sin(a1) * r;
                    float x2 = 12 + (float) Math.cos(a2) * r, y2 = 12 + (float) Math.sin(a2) * r;
                    if (i == 0) p.moveTo(x1, y1); else p.lineTo(x1, y1);
                    p.lineTo(x2, y2);
                }
                p.close();
                c.drawPath(p, P);
                for (int i = 0; i < 3; i++) {
                    double a = -Math.PI / 2 + i * Math.PI * 2 / 3 + Math.PI / 3;
                    c.drawLine(12, 12, 12 + (float) Math.cos(a) * 8.2f, 12 + (float) Math.sin(a) * 8.2f, P);
                }
                break;
            }
            case SEARCH:
                stroke(2.1f);
                c.drawCircle(10.5f, 10.5f, 6.6f, P);
                c.drawLine(15.4f, 15.4f, 20.6f, 20.6f, P);
                break;
            case PLAY:
                fill(); P.setPathEffect(ROUND1);
                p.moveTo(7.2f, 4.5f); p.lineTo(19.6f, 12f); p.lineTo(7.2f, 19.5f); p.close();
                c.drawPath(p, P);
                break;
            case PAUSE:
                fill();
                R.set(6.2f, 4.8f, 10.2f, 19.2f); c.drawRoundRect(R, 1.5f, 1.5f, P);
                R.set(13.8f, 4.8f, 17.8f, 19.2f); c.drawRoundRect(R, 1.5f, 1.5f, P);
                break;
            case NEXT:
                fill(); P.setPathEffect(ROUND1);
                p.moveTo(5.2f, 5.5f); p.lineTo(14.5f, 12f); p.lineTo(5.2f, 18.5f); p.close();
                c.drawPath(p, P);
                P.setPathEffect(null);
                R.set(16.2f, 5.5f, 18.8f, 18.5f); c.drawRoundRect(R, 1.2f, 1.2f, P);
                break;
            case PREV:
                fill(); P.setPathEffect(ROUND1);
                p.moveTo(18.8f, 5.5f); p.lineTo(9.5f, 12f); p.lineTo(18.8f, 18.5f); p.close();
                c.drawPath(p, P);
                P.setPathEffect(null);
                R.set(5.2f, 5.5f, 7.8f, 18.5f); c.drawRoundRect(R, 1.2f, 1.2f, P);
                break;
            case BACK:
                stroke(2.2f);
                p.moveTo(15f, 5.5f); p.lineTo(8.5f, 12f); p.lineTo(15f, 18.5f);
                c.drawPath(p, P);
                break;
            case CHEVRON:
                stroke(2.0f);
                p.moveTo(9.2f, 6.2f); p.lineTo(15f, 12f); p.lineTo(9.2f, 17.8f);
                c.drawPath(p, P);
                break;
            case CHECK:
                stroke(2.3f);
                p.moveTo(5.2f, 12.6f); p.lineTo(9.8f, 17.2f); p.lineTo(19f, 7.4f);
                c.drawPath(p, P);
                break;
            case MORE:
                fill();
                c.drawCircle(6f, 12f, 1.8f, P);
                c.drawCircle(12f, 12f, 1.8f, P);
                c.drawCircle(18f, 12f, 1.8f, P);
                break;
            case CLOSE:
                stroke(2.2f);
                c.drawLine(6.5f, 6.5f, 17.5f, 17.5f, P);
                c.drawLine(17.5f, 6.5f, 6.5f, 17.5f, P);
                break;
            case PLUS:
                stroke(2.2f);
                c.drawLine(12f, 5.5f, 12f, 18.5f, P);
                c.drawLine(5.5f, 12f, 18.5f, 12f, P);
                break;
            case SIDEBAR:
                stroke(1.9f);
                R.set(2.8f, 4.6f, 21.2f, 19.4f); c.drawRoundRect(R, 3.6f, 3.6f, P);
                c.drawLine(9.2f, 5f, 9.2f, 19f, P);
                break;
            case VIDEO:
                stroke(1.9f);
                R.set(2.8f, 5.2f, 21.2f, 18.8f); c.drawRoundRect(R, 3.4f, 3.4f, P);
                fill(); P.setPathEffect(ROUND1);
                p.moveTo(10f, 8.8f); p.lineTo(16f, 12f); p.lineTo(10f, 15.2f); p.close();
                c.drawPath(p, P);
                break;
            case CLOCK:
                stroke(1.9f);
                c.drawCircle(12, 12, 8.6f, P);
                c.drawLine(12, 7.2f, 12, 12.2f, P);
                c.drawLine(12, 12.2f, 15.6f, 14.2f, P);
                break;
            case HEART:
            case HEART_FILL:
                if (id == HEART_FILL) fill(); else stroke(1.9f);
                p.moveTo(12f, 19.6f);
                p.cubicTo(4.5f, 14.5f, 3f, 9.8f, 5.2f, 6.8f);
                p.cubicTo(7.2f, 4.2f, 10.5f, 4.8f, 12f, 7.5f);
                p.cubicTo(13.5f, 4.8f, 16.8f, 4.2f, 18.8f, 6.8f);
                p.cubicTo(21f, 9.8f, 19.5f, 14.5f, 12f, 19.6f);
                c.drawPath(p, P);
                break;
            case PLAYLIST:
            case LIST:
                stroke(2.0f);
                c.drawLine(4.5f, 7f, 19.5f, 7f, P);
                c.drawLine(4.5f, 12f, 19.5f, 12f, P);
                c.drawLine(4.5f, 17f, 14.5f, 17f, P);
                break;
            case LINK:
                stroke(1.9f);
                R.set(3.2f, 8.2f, 13f, 15.8f); c.drawRoundRect(R, 3.8f, 3.8f, P);
                R.set(11f, 8.2f, 20.8f, 15.8f); c.drawRoundRect(R, 3.8f, 3.8f, P);
                break;
            case SUBTITLE:
                stroke(1.9f);
                R.set(2.8f, 5.2f, 21.2f, 18.8f); c.drawRoundRect(R, 3f, 3f, P);
                c.drawLine(6.5f, 13.5f, 12.5f, 13.5f, P);
                c.drawLine(14.8f, 13.5f, 17.5f, 13.5f, P);
                c.drawLine(6.5f, 16f, 10f, 16f, P);
                c.drawLine(12.2f, 16f, 17.5f, 16f, P);
                break;
            case AI:
                fill();
                // 4-point sparkle + mini sparkle
                p.moveTo(11f, 2.5f); p.quadTo(11.6f, 10.4f, 19.5f, 11f);
                p.quadTo(11.6f, 11.6f, 11f, 19.5f); p.quadTo(10.4f, 11.6f, 2.5f, 11f);
                p.quadTo(10.4f, 10.4f, 11f, 2.5f); p.close();
                c.drawPath(p, P);
                p.reset();
                p.moveTo(18.5f, 14.5f); p.quadTo(18.8f, 17.7f, 22f, 18f);
                p.quadTo(18.8f, 18.3f, 18.5f, 21.5f); p.quadTo(18.2f, 18.3f, 15f, 18f);
                p.quadTo(18.2f, 17.7f, 18.5f, 14.5f); p.close();
                c.drawPath(p, P);
                break;
            case SPEED:
                stroke(1.9f);
                R.set(3.5f, 5f, 20.5f, 22f);
                c.drawArc(R, 195, 150, false, P);
                c.drawLine(12f, 14.5f, 16.5f, 9.2f, P);
                fill();
                c.drawCircle(12f, 14.5f, 1.8f, P);
                break;
            case ASPECT:
                stroke(1.9f);
                R.set(3f, 5.5f, 21f, 18.5f); c.drawRoundRect(R, 2.5f, 2.5f, P);
                c.drawLine(6.5f, 9f, 9.5f, 9f, P); c.drawLine(6.5f, 9f, 6.5f, 12f, P);
                c.drawLine(17.5f, 15f, 14.5f, 15f, P); c.drawLine(17.5f, 15f, 17.5f, 12f, P);
                break;
            case LOCK:
            case UNLOCK:
                stroke(1.9f);
                R.set(5.2f, 10.5f, 18.8f, 20.2f); c.drawRoundRect(R, 2.6f, 2.6f, P);
                R.set(8f, 4.2f, 16f, 12.2f);
                c.drawArc(R, 180, id == LOCK ? 180 : 125, false, P);
                c.drawLine(8f, 8.2f, 8f, 10.5f, P);
                if (id == LOCK) c.drawLine(16f, 8.2f, 16f, 10.5f, P);
                break;
            case ROTATE:
                stroke(1.9f);
                R.set(4.5f, 4.5f, 19.5f, 19.5f);
                c.drawArc(R, -40, 270, false, P);
                p.moveTo(16.8f, 4.5f); p.lineTo(19.2f, 7.6f); p.lineTo(15.2f, 8.2f);
                c.drawPath(p, P);
                break;
            case PIP:
                stroke(1.9f);
                R.set(3f, 5f, 21f, 19f); c.drawRoundRect(R, 2.6f, 2.6f, P);
                fill();
                R.set(12f, 11.2f, 18.5f, 16.5f); c.drawRoundRect(R, 1.4f, 1.4f, P);
                break;
            case BOOKMARK:
                stroke(1.9f);
                p.moveTo(6.5f, 4.2f); p.lineTo(17.5f, 4.2f); p.lineTo(17.5f, 20f);
                p.lineTo(12f, 16.2f); p.lineTo(6.5f, 20f); p.close();
                c.drawPath(p, P);
                break;
            case REPEAT:
            case REPEAT_ONE:
                stroke(1.9f);
                p.moveTo(17f, 4.5f); p.lineTo(20f, 7.5f); p.lineTo(17f, 10.5f);
                c.drawPath(p, P);
                p.reset();
                p.moveTo(4.5f, 11.5f); p.lineTo(4.5f, 9f); p.quadTo(4.5f, 7.5f, 6.5f, 7.5f); p.lineTo(19.5f, 7.5f);
                c.drawPath(p, P);
                p.reset();
                p.moveTo(7f, 19.5f); p.lineTo(4f, 16.5f); p.lineTo(7f, 13.5f);
                c.drawPath(p, P);
                p.reset();
                p.moveTo(19.5f, 12.5f); p.lineTo(19.5f, 15f); p.quadTo(19.5f, 16.5f, 17.5f, 16.5f); p.lineTo(4.5f, 16.5f);
                c.drawPath(p, P);
                if (id == REPEAT_ONE) {
                    fill();
                    c.drawCircle(12f, 12f, 1.8f, P);
                }
                break;
            case TIMER:
                stroke(1.9f);
                c.drawCircle(12f, 13f, 7.2f, P);
                c.drawLine(9.5f, 3.2f, 14.5f, 3.2f, P);
                c.drawLine(12f, 13f, 15f, 10f, P);
                break;
            case CAMERA:
                stroke(1.9f);
                R.set(3f, 6.5f, 21f, 19f); c.drawRoundRect(R, 2.8f, 2.8f, P);
                c.drawCircle(12f, 12.8f, 3.3f, P);
                c.drawLine(8.5f, 6.5f, 10f, 4.5f, P); c.drawLine(10f, 4.5f, 14f, 4.5f, P); c.drawLine(14f, 4.5f, 15.5f, 6.5f, P);
                break;
            case VOLUME:
            case VOLUME_MUTE:
                fill(); P.setPathEffect(ROUND1);
                p.moveTo(3f, 9f); p.lineTo(7f, 9f); p.lineTo(12f, 4.6f); p.lineTo(12f, 19.4f); p.lineTo(7f, 15f); p.lineTo(3f, 15f); p.close();
                c.drawPath(p, P);
                stroke(1.9f);
                if (id == VOLUME) {
                    R.set(10.5f, 8.5f, 17.5f, 15.5f); c.drawArc(R, -50, 100, false, P);
                    R.set(10f, 5f, 21.2f, 19f); c.drawArc(R, -52, 104, false, P);
                } else {
                    c.drawLine(15.5f, 9f, 20.5f, 15f, P);
                    c.drawLine(20.5f, 9f, 15.5f, 15f, P);
                }
                break;
            case BRIGHTNESS:
                stroke(1.9f);
                c.drawCircle(12f, 12f, 4.2f, P);
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    c.drawLine(12 + (float) Math.cos(a) * 6.8f, 12 + (float) Math.sin(a) * 6.8f,
                            12 + (float) Math.cos(a) * 9.5f, 12 + (float) Math.sin(a) * 9.5f, P);
                }
                break;
            case EQ:
                stroke(2.2f);
                c.drawLine(6f, 6f, 6f, 18f, P);
                c.drawLine(12f, 6f, 12f, 18f, P);
                c.drawLine(18f, 6f, 18f, 18f, P);
                fill();
                c.drawCircle(6f, 10f, 2.2f, P);
                c.drawCircle(12f, 15f, 2.2f, P);
                c.drawCircle(18f, 9f, 2.2f, P);
                break;
            case HEADPHONES:
                stroke(1.9f);
                R.set(4.5f, 4.5f, 19.5f, 19.5f); c.drawArc(R, 180, 180, false, P);
                fill();
                R.set(3.5f, 12f, 7.2f, 19f); c.drawRoundRect(R, 1.8f, 1.8f, P);
                R.set(16.8f, 12f, 20.5f, 19f); c.drawRoundRect(R, 1.8f, 1.8f, P);
                break;
            case REWIND:
                fill(); P.setPathEffect(ROUND1);
                p.moveTo(11.5f, 6.2f); p.lineTo(3.8f, 12f); p.lineTo(11.5f, 17.8f); p.close();
                c.drawPath(p, P);
                p.reset();
                p.moveTo(20.2f, 6.2f); p.lineTo(12.5f, 12f); p.lineTo(20.2f, 17.8f); p.close();
                c.drawPath(p, P);
                break;
            case FORWARD:
                fill(); P.setPathEffect(ROUND1);
                p.moveTo(3.8f, 6.2f); p.lineTo(11.5f, 12f); p.lineTo(3.8f, 17.8f); p.close();
                c.drawPath(p, P);
                p.reset();
                p.moveTo(12.5f, 6.2f); p.lineTo(20.2f, 12f); p.lineTo(12.5f, 17.8f); p.close();
                c.drawPath(p, P);
                break;
            case SORT:
                stroke(2.0f);
                c.drawLine(4.5f, 7f, 19.5f, 7f, P);
                c.drawLine(4.5f, 12f, 15.5f, 12f, P);
                c.drawLine(4.5f, 17f, 11.5f, 17f, P);
                break;
            case GRID:
                stroke(1.9f);
                R.set(4f, 4f, 10.5f, 10.5f); c.drawRoundRect(R, 1.8f, 1.8f, P);
                R.set(13.5f, 4f, 20f, 10.5f); c.drawRoundRect(R, 1.8f, 1.8f, P);
                R.set(4f, 13.5f, 10.5f, 20f); c.drawRoundRect(R, 1.8f, 1.8f, P);
                R.set(13.5f, 13.5f, 20f, 20f); c.drawRoundRect(R, 1.8f, 1.8f, P);
                break;
            case INFO:
                stroke(1.9f);
                c.drawCircle(12f, 12f, 8.8f, P);
                c.drawLine(12f, 11f, 12f, 16.2f, P);
                fill(); c.drawCircle(12f, 8.2f, 1.1f, P);
                break;
            case STORAGE:
                stroke(1.9f);
                R.set(4f, 5f, 20f, 19f); c.drawRoundRect(R, 2.8f, 2.8f, P);
                c.drawLine(4f, 13.5f, 20f, 13.5f, P);
                fill(); c.drawCircle(16.8f, 16.2f, 1.1f, P);
                break;
            case APPEARANCE:
                stroke(1.9f);
                c.drawCircle(12f, 12f, 8.6f, P);
                fill();
                R.set(12f, 3.4f, 20.6f, 20.6f);
                c.drawArc(R, -90, 180, true, P);
                break;
            case GLOBE:
                stroke(1.8f);
                c.drawCircle(12f, 12f, 8.6f, P);
                R.set(8.2f, 3.4f, 15.8f, 20.6f); c.drawOval(R, P);
                c.drawLine(3.8f, 12f, 20.2f, 12f, P);
                break;
            case REFRESH:
                stroke(2.0f);
                R.set(4.5f, 4.5f, 19.5f, 19.5f); c.drawArc(R, 30, 290, false, P);
                p.moveTo(19f, 5.2f); p.lineTo(19.5f, 9.5f); p.lineTo(15.4f, 8.6f);
                c.drawPath(p, P);
                break;
            case AB:
                stroke(1.9f);
                c.drawLine(4.5f, 12f, 19.5f, 12f, P);
                c.drawLine(6.5f, 7f, 6.5f, 17f, P);
                c.drawLine(17.5f, 7f, 17.5f, 17f, P);
                fill();
                c.drawCircle(6.5f, 12f, 2.2f, P);
                c.drawCircle(17.5f, 12f, 2.2f, P);
                break;
            case AUDIO:
            case WAVE:
                stroke(2.0f);
                c.drawLine(5f, 10f, 5f, 14f, P);
                c.drawLine(9f, 7f, 9f, 17f, P);
                c.drawLine(13f, 5f, 13f, 19f, P);
                c.drawLine(17f, 8f, 17f, 16f, P);
                c.drawLine(21f, 10.5f, 21f, 13.5f, P);
                break;
            case DOWNLOAD:
                stroke(2.0f);
                c.drawLine(12f, 4f, 12f, 15.5f, P);
                p.moveTo(7.5f, 11.5f); p.lineTo(12f, 16f); p.lineTo(16.5f, 11.5f);
                c.drawPath(p, P);
                c.drawLine(5f, 19.5f, 19f, 19.5f, P);
                break;
            case CHEVRON_UP:
                stroke(2f);
                c.drawLine(12f, 20f, 12f, 5f, P);
                p.moveTo(5.5f, 11f); p.lineTo(12f, 4.5f); p.lineTo(18.5f, 11f);
                c.drawPath(p, P);
                break;
            case DEVICE:
                stroke(1.8f);
                R.set(6f, 3.2f, 18f, 20.8f); c.drawRoundRect(R, 2.4f, 2.4f, P);
                c.drawLine(10f, 18.2f, 14f, 18.2f, P);
                break;
            case DAC:
                stroke(1.7f);
                R.set(5.5f, 7.2f, 18.5f, 17.8f); c.drawRoundRect(R, 2.8f, 2.8f, P);
                R.set(9.4f, 3.2f, 14.6f, 7.2f); c.drawRoundRect(R, 1.1f, 1.1f, P);
                c.drawLine(12f, 17.8f, 12f, 21f, P);
                fill(); c.drawCircle(12f, 12.5f, 1.5f, P);
                break;
            case EARBUDS:
                stroke(1.75f);
                c.drawCircle(7.2f, 8.0f, 3.4f, P);
                c.drawLine(8.6f, 11.2f, 8.6f, 19.2f, P);
                c.drawCircle(16.8f, 8.0f, 3.4f, P);
                c.drawLine(15.4f, 11.2f, 15.4f, 19.2f, P);
                break;
            case BUDS:
                stroke(1.55f);
                R.set(3.95f, 5.15f, 10.85f, 12.05f); c.drawArc(R, 64f, -320f, false, P);
                p.moveTo(6.5f, 9.7f); p.lineTo(6.5f, 19.25f); p.lineTo(7.65f, 20.2f); p.lineTo(8.8f, 19.25f); p.lineTo(8.8f, 11.75f);
                c.drawPath(p, P);
                p.reset();
                p.moveTo(4.05f, 8.2f); p.cubicTo(1.75f, 8.7f, 1.75f, 11.85f, 3.65f, 12.75f); p.cubicTo(4.65f, 13.2f, 5.65f, 12.65f, 6.15f, 11.9f);
                c.drawPath(p, P);
                R.set(13.15f, 5.15f, 20.05f, 12.05f); c.drawArc(R, 116f, 320f, false, P);
                p.reset();
                p.moveTo(17.5f, 9.7f); p.lineTo(17.5f, 19.25f); p.lineTo(16.35f, 20.2f); p.lineTo(15.2f, 19.25f); p.lineTo(15.2f, 11.75f);
                c.drawPath(p, P);
                p.reset();
                p.moveTo(19.95f, 8.2f); p.cubicTo(22.25f, 8.7f, 22.25f, 11.85f, 20.35f, 12.75f); p.cubicTo(19.35f, 13.2f, 18.35f, 12.65f, 17.85f, 11.9f);
                c.drawPath(p, P);
                break;
            case BUDS4:
                stroke(1.6f);
                p.moveTo(9.7f, 12.75f);
                p.cubicTo(7.45f, 11.35f, 7.35f, 7.35f, 9.45f, 5.45f);
                p.cubicTo(11.65f, 3.55f, 16.45f, 3.65f, 18.35f, 6.55f);
                p.cubicTo(19.85f, 8.85f, 18.95f, 12.25f, 16.05f, 13.45f);
                c.drawPath(p, P);
                c.save(); c.rotate(24.5f, 11.4f, 14.0f);
                R.set(9.4f, 6.25f, 13.4f, 21.15f); c.drawRoundRect(R, 2.0f, 2.0f, P);
                c.restore();
                break;
            case BUDS2:
            case BUDS_LIVE:
            case BUDS_ON:
                stroke(1.6f);
                R.set(4.15f, 7.25f, 10.65f, 16.75f); c.drawRoundRect(R, 3.25f, 3.85f, P);
                R.set(13.35f, 7.25f, 19.85f, 16.75f); c.drawRoundRect(R, 3.25f, 3.85f, P);
                break;
            case YOUTUBE:
                fill();
                R.set(2.4f, 5.2f, 21.6f, 18.8f); c.drawRoundRect(R, 4.4f, 4.4f, P);
                P.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
                p.moveTo(10f, 8.8f); p.lineTo(15.8f, 12f); p.lineTo(10f, 15.2f); p.close();
                c.drawPath(p, P);
                P.setXfermode(null);
                break;
            case PROSCALER:
                stroke(1.9f);
                R.set(3.2f, 4.8f, 20.8f, 19.2f); c.drawRoundRect(R, 3.2f, 3.2f, P);
                p.moveTo(7.5f, 15.5f); p.lineTo(16.5f, 8.5f);
                p.moveTo(12.5f, 8.5f); p.lineTo(16.5f, 8.5f); p.lineTo(16.5f, 12.5f);
                c.drawPath(p, P);
                break;
            case CALENDAR:
                stroke(1.9f);
                R.set(3.5f, 5f, 20.5f, 20.5f); c.drawRoundRect(R, 3.4f, 3.4f, P);
                c.drawLine(8f, 3.2f, 8f, 6.6f, P);
                c.drawLine(16f, 3.2f, 16f, 6.6f, P);
                c.drawLine(3.8f, 9.6f, 20.2f, 9.6f, P);
                fill();
                c.drawCircle(8.4f, 13.6f, 1.15f, P); c.drawCircle(12f, 13.6f, 1.15f, P); c.drawCircle(15.6f, 13.6f, 1.15f, P);
                c.drawCircle(8.4f, 17f, 1.15f, P); c.drawCircle(12f, 17f, 1.15f, P);
                break;
            case CHART:
                fill();
                R.set(4f, 12.5f, 7.6f, 20f); c.drawRoundRect(R, 1.4f, 1.4f, P);
                R.set(10.2f, 7.5f, 13.8f, 20f); c.drawRoundRect(R, 1.4f, 1.4f, P);
                R.set(16.4f, 3.8f, 20f, 20f); c.drawRoundRect(R, 1.4f, 1.4f, P);
                break;
            case WALLET:
                stroke(1.9f);
                R.set(3f, 6f, 21f, 19.5f); c.drawRoundRect(R, 3.2f, 3.2f, P);
                p.moveTo(5f, 6f); p.lineTo(16f, 3.4f); p.lineTo(17f, 6f);
                c.drawPath(p, P);
                R.set(14.5f, 10.6f, 21f, 15f); c.drawRoundRect(R, 2f, 2f, P);
                fill(); c.drawCircle(17.2f, 12.8f, 1f, P);
                break;
            case BELL:
            case BELL_OFF:
                stroke(1.9f);
                p.moveTo(6f, 16.8f); p.lineTo(6f, 11f); p.cubicTo(6f, 7.4f, 8.6f, 4.6f, 12f, 4.6f);
                p.cubicTo(15.4f, 4.6f, 18f, 7.4f, 18f, 11f); p.lineTo(18f, 16.8f);
                c.drawPath(p, P);
                c.drawLine(4.2f, 16.8f, 19.8f, 16.8f, P);
                R.set(9.6f, 17.6f, 14.4f, 21.2f); c.drawArc(R, 0, 180, false, P);
                c.drawLine(12f, 2.6f, 12f, 4.6f, P);
                if (id == BELL_OFF) c.drawLine(4f, 4f, 20f, 20f, P);
                break;
            case CARD:
                stroke(1.9f);
                R.set(2.8f, 5.5f, 21.2f, 18.5f); c.drawRoundRect(R, 2.8f, 2.8f, P);
                c.drawLine(3f, 9.6f, 21f, 9.6f, P);
                c.drawLine(6f, 15f, 10f, 15f, P);
                break;
            case TAG:
                stroke(1.9f); P.setPathEffect(ROUND1);
                p.moveTo(3.5f, 4.5f); p.lineTo(11.5f, 4.5f); p.lineTo(20.2f, 13.2f); p.lineTo(13.2f, 20.2f); p.lineTo(4.5f, 11.5f); p.close();
                c.drawPath(p, P);
                P.setPathEffect(null); fill(); c.drawCircle(8.2f, 8.2f, 1.4f, P);
                break;
            case TRASH:
                stroke(1.9f);
                c.drawLine(4f, 6.5f, 20f, 6.5f, P);
                p.moveTo(9f, 6.5f); p.lineTo(9.6f, 3.8f); p.lineTo(14.4f, 3.8f); p.lineTo(15f, 6.5f);
                c.drawPath(p, P);
                p.reset();
                p.moveTo(6f, 6.5f); p.lineTo(7f, 20f); p.lineTo(17f, 20f); p.lineTo(18f, 6.5f);
                c.drawPath(p, P);
                c.drawLine(10.2f, 10f, 10.4f, 16.5f, P); c.drawLine(13.8f, 10f, 13.6f, 16.5f, P);
                break;
            case EDIT:
                stroke(1.9f); P.setPathEffect(ROUND1);
                p.moveTo(4.5f, 19.5f); p.lineTo(5.2f, 15.4f); p.lineTo(15.8f, 4.8f); p.lineTo(19.2f, 8.2f); p.lineTo(8.6f, 18.8f); p.close();
                c.drawPath(p, P);
                P.setPathEffect(null);
                c.drawLine(13.4f, 7.2f, 16.8f, 10.6f, P);
                break;
            case PIE:
                fill();
                R.set(3f, 4f, 19f, 20f); c.drawArc(R, -60, 300, true, P);
                R.set(5f, 2.5f, 21f, 18.5f); c.drawArc(R, -90, 30, true, P);
                break;
            case SHARE:
                stroke(1.9f);
                c.drawLine(12f, 3.8f, 12f, 14.5f, P);
                p.moveTo(7.8f, 8f); p.lineTo(12f, 3.8f); p.lineTo(16.2f, 8f);
                c.drawPath(p, P);
                p.reset();
                p.moveTo(7.5f, 11f); p.lineTo(5f, 11f); p.lineTo(5f, 20f); p.lineTo(19f, 20f); p.lineTo(19f, 11f); p.lineTo(16.5f, 11f);
                c.drawPath(p, P);
                break;
            case CHEVRON_L:
                stroke(2.0f);
                p.moveTo(14.8f, 6.2f); p.lineTo(9f, 12f); p.lineTo(14.8f, 17.8f);
                c.drawPath(p, P);
                break;
        }
    }

    static final class IconView extends View {
        int id, color;
        float sizeDp;

        IconView(Context c, int id, float sizeDp, int color) {
            super(c);
            this.id = id;
            this.sizeDp = sizeDp;
            this.color = color;
        }

        void set(int id, int color) { this.id = id; this.color = color; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            Icons.draw(c, id, getWidth() / 2f, getHeight() / 2f, Ui.dpf(sizeDp), color);
        }
    }
}
