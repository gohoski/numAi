package io.github.gohoski.numai;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gleb on 11.11.2025.
 * Helper class to not duplicate code.
 */

class SettingsHelper {
    static void setupApiSpinner(
            final Context context,
            final Spinner spinner,
            final ConfigManager config,
            final ApiSelectionCallback callback) {

        final List<String> apiNames = new ArrayList<String>(ApiManager.getAllApiNames());
        Config conf = config.getConfig();
        String nameOfUrl = ApiManager.getNameByUrl(conf.getBaseUrl());

        if (nameOfUrl == null) {
            nameOfUrl = conf.getBaseUrl();
            apiNames.add(nameOfUrl);
        }
        apiNames.add(context.getString(R.string.other));

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context,
                android.R.layout.simple_spinner_item,
                apiNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setSelection(apiNames.indexOf(nameOfUrl));

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String selected = adapterView.getItemAtPosition(i).toString();
                System.out.println(selected);
                if (selected.equals(context.getString(R.string.other))) {
                    showCustomUrlDialog(context, apiNames, adapter, spinner, callback);
                } else if (callback != null) {
                    callback.onApiSelected(selected);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Log.w("ApiSpinnerHelper", "No API selected");
            }
        });
    }

    private static void showCustomUrlDialog(final Context context,
            final List<String> apiNames,
            final ArrayAdapter<String> adapter,
            final Spinner spinner,final ApiSelectionCallback callback) {
        final EditText edittext = new EditText(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(20, 10, 20, 10);
        edittext.setLayoutParams(params);
        edittext.setSingleLine();
        edittext.setTextSize(14);
        edittext.setPadding(10, 10, 10, 10);
        new AlertDialog.Builder(context)
                .setTitle(R.string.other)
                .setMessage(R.string.base_url)
                .setView(edittext)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String customUrl = edittext.getText().toString();
                        if (customUrl.endsWith("/")) {
                            customUrl = customUrl.substring(0, customUrl.length() - 1);
                        }
                        if (isValidUrl(customUrl)) {
                            apiNames.add(customUrl);
                            adapter.notifyDataSetChanged();
                            spinner.setSelection(apiNames.size() - 1);
                            if (callback != null) {
                                callback.onApiSelected(customUrl);
                            }
                        } else {
                            Toast.makeText(context, R.string.bad_url, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    interface ApiSelectionCallback {
        void onApiSelected(String api);
    }

    private static boolean isValidUrl(String urlString) {
        try {
            new URL(urlString);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
