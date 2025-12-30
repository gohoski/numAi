package io.github.gohoski.numai;

import android.app.ProgressDialog;
import android.content.Context;

/**
 * Created by Gleb on 24.10.2025.
 * Loading popup
 */

class Loading extends ProgressDialog {
    private Loading(Context context, int message) {
        super(context);
        this.setMessage(context.getString(message));
        this.setCancelable(false);
        this.show();
    }

    Loading(Context context) {
        this(context, R.string.loading);
    }
}
