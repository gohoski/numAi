package io.github.gohoski.numai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Gleb on 15.10.2025.
 */

public class SettingsActivity extends Activity {
    Context context;
    ConfigManager config;
    ApiService api;
    Spinner apiSpinner, chatSpinner, thinkSpinner;
    EditText keyText;
    boolean fetched = false;
    CheckBox shrinkThink;
    String systemPrompt;
    EditText updateDelay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        context = this;
        config = ConfigManager.getInstance();
        Config conf = config.getConfig();
        api = new ApiService(this);
        systemPrompt = conf.getSystemPrompt();

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        apiSpinner = (Spinner) findViewById(R.id.api_spinner);
        SpinnerHelper.setupApiSpinner(context, apiSpinner, config, new SpinnerHelper.ApiSelectionCallback() {
            @Override
            public void onApiSelected(String api) {
                System.out.println(api);
            }
        });

        keyText = (EditText) findViewById(R.id.apiKey);
        keyText.setText(conf.getApiKey());

        chatSpinner = (Spinner) findViewById(R.id.chat_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{conf.getChatModel()}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        chatSpinner.setAdapter(adapter);

        thinkSpinner = (Spinner) findViewById(R.id.think_spinner);
        adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{conf.getThinkingModel()}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        thinkSpinner.setAdapter(adapter);

        chatSpinner.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    loadModels(chatSpinner);
                    return true;
                }
                return false;
            }
        });
        thinkSpinner.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    loadModels(thinkSpinner);
                    return true;
                }
                return false;
            }
        });

        shrinkThink = (CheckBox) findViewById(R.id.shrinkThinking);
        shrinkThink.setChecked(conf.getShrinkThink());
        updateDelay = (EditText) findViewById(R.id.update_delay);
        updateDelay.setText(conf.getUpdateDelay()+"");

        findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                config.setConfig(new Config(ApiManager.getUrlByName(apiSpinner.getSelectedItem().toString()),
                    keyText.getText().toString(), chatSpinner.getSelectedItem().toString(),
                    thinkSpinner.getSelectedItem().toString(),
                    shrinkThink.isChecked(), systemPrompt, Integer.parseInt(updateDelay.getText().toString())));
                Intent intent = new Intent(context, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        findViewById(R.id.changeSystemPrompt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText edittext = new EditText(context);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.FILL_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(20, 10, 20, 10);
                edittext.setLayoutParams(params);
                edittext.setSingleLine(false);
                edittext.setMinLines(4);
                edittext.setTextSize(14);
                edittext.setPadding(10, 10, 10, 10);
                edittext.setText(systemPrompt);
                new AlertDialog.Builder(context)
                    .setTitle(R.string.change_system)
                    .setView(edittext)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            systemPrompt = edittext.getText().toString();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            }
        });

    }

    private void loadModels(final Spinner spinner) {
        if (!keyText.getText().toString().equals(config.getConfig().getApiKey())) {
            Toast.makeText(this, R.string.change_key_pls, Toast.LENGTH_LONG).show();
            return;
        }
        if (fetched) {
            spinner.performClick();
            return;
        }
        final Loading loading = new Loading(context);
        api.getModels(new ApiCallback<ArrayList<String>>() {
            @Override
            public void onSuccess(ArrayList<String> result) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                        context,
                        android.R.layout.simple_spinner_item,
                        result
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                chatSpinner.setAdapter(adapter);
                thinkSpinner.setAdapter(adapter);
                Config conf = config.getConfig();
                chatSpinner.setSelection(result.indexOf(conf.getChatModel()));
                thinkSpinner.setSelection(result.indexOf(conf.getThinkingModel()));
                loading.hide();
                fetched = true;
                spinner.performClick();
            }

            @Override
            public void onError(ApiError error) {
                error.printStackTrace();
            }
        });
    }
}
