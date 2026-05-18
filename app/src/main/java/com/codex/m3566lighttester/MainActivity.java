package com.codex.m3566lighttester;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private final M3566Lights lights = M3566Lights.getInstance();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    private TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startApiService();
        setContentView(createContentView());
        log("Ready. Android " + android.os.Build.VERSION.RELEASE + " / " + android.os.Build.MODEL);
        log("Home Assistant API: http://" + NetworkAddress.getLanIpAddress() + ":" + LightApiServer.PORT);
    }

    @Override
    protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private View createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(246, 248, 250));

        TextView title = new TextView(this);
        title.setText("M3566 RGB Controller");
        title.setTextSize(24);
        title.setTextColor(Color.rgb(20, 25, 31));
        title.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(title, matchWrap());

        TextView subtitle = new TextView(this);
        subtitle.setText("API: http://" + NetworkAddress.getLanIpAddress() + ":" + LightApiServer.PORT);
        subtitle.setTextSize(14);
        subtitle.setTextColor(Color.rgb(74, 85, 98));
        subtitle.setPadding(0, dp(4), 0, dp(16));
        root.addView(subtitle, matchWrap());

        root.addView(section("Colour"));
        root.addView(row(
                actionButton("Red", Color.rgb(186, 43, 43), () -> lights.setColorName("red")),
                actionButton("Green", Color.rgb(34, 130, 75), () -> lights.setColorName("green")),
                actionButton("Blue", Color.rgb(44, 89, 190), () -> lights.setColorName("blue"))
        ));
        root.addView(row(
                actionButton("White", Color.rgb(68, 78, 88), () -> lights.setColorName("white")),
                actionButton("Yellow", Color.rgb(154, 117, 17), () -> lights.setColorName("yellow")),
                actionButton("Cyan", Color.rgb(28, 119, 142), () -> lights.setColorName("cyan"))
        ));
        root.addView(row(
                actionButton("Magenta", Color.rgb(126, 61, 151), () -> lights.setColorName("magenta")),
                actionButton("Off", Color.rgb(32, 38, 46), () -> lights.setColorName("off"))
        ));

        root.addView(section("Tools"));
        root.addView(row(
                actionButton("Test", Color.rgb(11, 122, 117), lights::testSequence),
                actionButton("Status", Color.rgb(68, 78, 88), () -> lights.getState().toJson())
        ));

        logView = new TextView(this);
        logView.setTextSize(12);
        logView.setTextColor(Color.rgb(25, 30, 36));
        logView.setPadding(dp(12), dp(12), dp(12), dp(12));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(Color.WHITE);
        scrollView.addView(logView);
        LinearLayout.LayoutParams logParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        logParams.topMargin = dp(16);
        root.addView(scrollView, logParams);
        return root;
    }

    private TextView section(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(Color.rgb(20, 25, 31));
        view.setPadding(0, dp(14), 0, dp(8));
        return view;
    }

    private LinearLayout row(Button... buttons) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        for (Button button : buttons) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            row.addView(button, params);
        }
        return row;
    }

    private Button actionButton(String label, int color, LightAction action) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setBackgroundColor(color);
        button.setOnClickListener(v -> runAction(label, action));
        return button;
    }

    private void runAction(String label, LightAction action) {
        log("Running " + label + "...");
        executor.execute(() -> {
            String result = action.run();
            mainHandler.post(() -> log(result));
        });
    }

    private void startApiService() {
        Intent service = new Intent(this, LightApiService.class);
        service.setAction(LightApiService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }

    private void log(String message) {
        String line = timeFormat.format(new Date()) + "  " + message;
        String current = logView == null ? "" : logView.getText().toString();
        logView.setText(line + "\n" + current);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface LightAction {
        String run();
    }
}
