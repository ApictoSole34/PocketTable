package com.fizzycoyote.pockettable;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

/**
 * Base Activity for true edge-to-edge immersive mode: content draws behind
 * the status bar and navigation bar, and both bars are hidden until the
 * user swipes them into view briefly (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE).
 *
 * <p>All Activities in the app extend this instead of AppCompatActivity
 * directly. Because the system navigation bar is hidden, every screen that
 * needs a "back" action must provide its own visible leave/back button in
 * its layout - relying on the (hidden) system back button/gesture alone is
 * not reliable on every device in this mode.</p>
 */
public abstract class BaseImmersiveActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        enableImmersiveMode();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            enableImmersiveMode();
        }
    }

    private void enableImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.hide(WindowInsetsCompat.Type.systemBars());
            controller.setSystemBarsBehavior(
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            );
        }
    }

    /**
     * Pads {@code view} on top by the height of the status bar / notch, so
     * on-screen controls placed at the top of the screen (e.g. the "Leave"
     * button) are never drawn underneath the system bar area when it's
     * swiped into view, or on devices with a display cutout.
     */
    protected void applyTopInsetPadding(View view) {
        int baseLeft = view.getPaddingLeft();
        int baseRight = view.getPaddingRight();
        int baseBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(baseLeft, systemBars.top, baseRight, baseBottom);
            return insets;
        });
    }
}