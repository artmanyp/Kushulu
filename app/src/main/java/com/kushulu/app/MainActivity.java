package com.kushulu.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final class Target {
        final String name;
        final String pkg;
        Target(String name, String pkg) {
            this.name = name;
            this.pkg = pkg;
        }
    }

    private final List<Target> targets = new ArrayList<>();
    private final Map<String, TextView> versionViews = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(getString(R.string.app_name));

        // Edit names here if you prefer different labels. The list is sorted alphabetically.
        targets.add(new Target("Android System Intelligence", "com.google.android.as"));
        targets.add(new Target("Android System Key Verifier", "com.google.android.contactkeys"));
        targets.add(new Target("Android System SafetyCore", "com.google.android.safetycore"));
        targets.add(new Target("Android System WebView", "com.google.android.webview"));
        targets.add(new Target("Android Switch", "com.google.android.apps.restore"));
        targets.add(new Target("Google Play Services", "com.google.android.gms"));
        targets.add(new Target("Google Play Services for AR", "com.google.ar.core"));
        Collections.sort(targets, new Comparator<Target>() {
            @Override
            public int compare(Target a, Target b) {
                return a.name.toLowerCase(Locale.ROOT).compareTo(b.name.toLowerCase(Locale.ROOT));
            }
        });

        boolean round = Build.VERSION.SDK_INT >= 23
                && getResources().getConfiguration().isScreenRound();
        int side = dp(round ? 36 : 16);

        // Page content
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(side, dp(round ? 40 : 16), side, dp(8));

        TextView brandEn = new TextView(this);
        brandEn.setText(R.string.brand_en);
        brandEn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f);
        brandEn.setTypeface(brandEn.getTypeface(), Typeface.BOLD);
        brandEn.setGravity(Gravity.CENTER_HORIZONTAL);
        list.addView(brandEn, matchWrap());

        TextView brandFa = new TextView(this);
        brandFa.setText(R.string.brand_fa);
        brandFa.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);
        brandFa.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams faParams = matchWrap();
        faParams.bottomMargin = dp(24);
        list.addView(brandFa, faParams);

        for (final Target t : targets) {
            TextView name = new TextView(this);
            name.setText(t.name);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f);
            name.setTypeface(name.getTypeface(), Typeface.BOLD);
            name.setGravity(Gravity.CENTER_HORIZONTAL);

            Button button = new Button(this);
            button.setText(R.string.view_in_play_store);
            button.setAllCaps(false);
            button.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_play_store, 0, 0, 0);
            button.setCompoundDrawablePadding(dp(8));
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openLink(t.pkg);
                }
            });

            TextView version = new TextView(this);
            version.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
            version.setAlpha(0.75f);
            version.setGravity(Gravity.CENTER_HORIZONTAL);
            versionViews.put(t.pkg, version);

            list.addView(name, matchWrap());
            list.addView(button, matchWrap());
            LinearLayout.LayoutParams vp = matchWrap();
            vp.bottomMargin = dp(24);
            list.addView(version, vp);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(list);

        // Footer: always visible, below the scrolling list. Must stay.
        TextView footer = new TextView(this);
        footer.setText(R.string.copyright);
        footer.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
        footer.setAlpha(0.7f);
        footer.setGravity(Gravity.CENTER_HORIZONTAL);
        footer.setPadding(side, dp(8), side, dp(round ? 24 : 8));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.addView(scroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(footer, matchWrap());

        // Keep content clear of system bars (newer Android draws edge-to-edge)
        root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            @SuppressWarnings("deprecation")
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets bars = insets.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                } else {
                    v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                            insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
                }
                return insets;
            }
        });

        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh after returning from the Play Store, in case an update was installed
        for (Target t : targets) {
            TextView tv = versionViews.get(t.pkg);
            if (tv != null) tv.setText(installedText(t.pkg));
        }
    }

    @SuppressWarnings("deprecation")
    private String installedText(String pkg) {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(pkg, 0);
            long code = Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
            return getString(R.string.installed_fmt,
                    info.versionName != null ? info.versionName : "?", code);
        } catch (PackageManager.NameNotFoundException e) {
            return getString(R.string.not_installed);
        }
    }

    // Play Store handles this link when present; otherwise a browser opens it.
    private void openLink(String pkg) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.cannot_open, Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
