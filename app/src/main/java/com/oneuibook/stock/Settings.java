package com.oneuibook.stock;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

final class Settings {
    private Settings() {}

    static final class SettingsPage extends Page {
        SettingsPage(MainActivity m) {
            super(m, m.getString(R.string.tab_settings), true);
            setGrouped(true);
            headerIcons = new int[0];
        }

        @Override void build() {
            Context c = getContext();
            SharedPreferences p = MainActivity.prefs(c);

            Widgets.Group gMkt = new Widgets.Group(c, "시세 표시");
            LinearLayout row = new LinearLayout(c);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(Ui.dp(18), Ui.dp(14), Ui.dp(18), Ui.dp(14));
            row.addView(Ui.text(c, "한국식 등락 색상 (빨강 상승 · 파랑 하락)", 15, Ui.medium, Ui.text()),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            Widgets.Toggle tg = new Widgets.Toggle(c, Market.krColors);
            tg.change = on -> { Market.krColors = on; Market.saveWatch(main); main.reloadAll(); };
            row.addView(tg);
            gMkt.addCustom(row);
            content.addView(gMkt, Widgets.MW);

            Widgets.Group gApp = new Widgets.Group(c, "화면 및 글래스");
            String[] themes = {"시스템 설정", "라이트", "다크"};
            String[] themeKeys = {"system", "light", "dark"};
            String curTheme = p.getString("theme", "system");
            int tIdx = curTheme.equals("light") ? 1 : (curTheme.equals("dark") ? 2 : 0);
            gApp.row(Icons.APPEARANCE, 0xFF2563EB, "테마", themes[tIdx], true, v ->
                    Widgets.pickSheet(main, v, "테마", themes, tIdx, idx -> {
                        p.edit().putString("theme", themeKeys[idx]).apply();
                        Ui.init(main, p);
                        main.refreshAppearance();
                    }));
            String[] styles = {"글래스 (A)", "블러 (G)"};
            int stIdx = Ui.gStyle ? 1 : 0;
            gApp.row(Icons.INFO, 0xFF14B8A6, "글래스 스타일", styles[stIdx], true, v ->
                    Widgets.pickSheet(main, v, "글래스 스타일", styles, stIdx, idx -> {
                        p.edit().putString("ui_style", idx == 1 ? "oneui" : "prism").apply();
                        Ui.init(main, p);
                        main.refreshAppearance();
                    }));
            content.addView(gApp, Widgets.MW);

            Widgets.Group gAbout = new Widgets.Group(c, "앱 정보");
            gAbout.row(Icons.INFO, Ui.text2(), c.getString(R.string.app_name), MainActivity.VERSION, false, null);
            content.addView(gAbout, Widgets.MW);
        }
    }
}
