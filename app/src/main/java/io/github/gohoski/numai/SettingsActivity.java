package io.github.gohoski.numai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

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
        final Config conf = config.getConfig();
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
        SettingsHelper.setupApiSpinner(context, apiSpinner, config, new SettingsHelper.ApiSelectionCallback() {
            @Override
            public void onApiSelected(String api) {
                fetched = false;
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
                final String urlByName = ApiManager.getUrlByName(apiSpinner.getSelectedItem().toString());
                if (conf.getBaseUrl().equals(urlByName)) {
                    config.setConfig(new Config(urlByName,
                            keyText.getText().toString(), chatSpinner.getSelectedItem().toString(),
                            thinkSpinner.getSelectedItem().toString(),
                            shrinkThink.isChecked(), systemPrompt, Integer.parseInt(updateDelay.getText().toString())));
                    Intent intent = new Intent(context, MainActivity.class);
                    startActivity(intent);
                    finish();
                } else if (!fetched) {
                    final Loading loading = new Loading(context);
                    final String orig = config.getConfig().getBaseUrl();
                    config.updateBaseUrl(urlByName);
                    api.getModels(new ApiCallback<ArrayList<String>>() {
                        @Override
                        public void onSuccess(ArrayList<String> models) {
                            config.setConfig(new Config(urlByName,
                                    keyText.getText().toString(), ModelSelector.selectChatModel(models),
                                    ModelSelector.selectThinkingModel(models),
                                    shrinkThink.isChecked(), systemPrompt, Integer.parseInt(updateDelay.getText().toString())));
                            loading.dismiss();
                            Intent intent = new Intent(context, MainActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onError(ApiError error) {
                            error.printStackTrace();
                            Toast.makeText(context, error.getMessage(), Toast.LENGTH_LONG).show();loading.dismiss();
                            config.updateBaseUrl(orig);
                        }
                    });
                }
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

        findViewById(R.id.from_file).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("text/plain");
                startActivityForResult(Intent.createChooser(intent, getString(R.string.select_txt)), 2);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                try {
                    InputStream is = getContentResolver().openInputStream(uri);
                    keyText.setText(new Scanner(is, "UTF-8").useDelimiter("\\A").next());
                    Toast.makeText(this, R.string.key_success, Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void loadModels(final Spinner spinner) {
//        if (!keyText.getText().toString().equals(config.getConfig().getApiKey())) {
//            Toast.makeText(this, R.string.change_key_pls, Toast.LENGTH_LONG).show();
//            return;
//        }
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
                loading.dismiss();
                fetched = true;
                spinner.performClick();
            }

            @Override
            public void onError(ApiError error) {
                error.printStackTrace();
                loading.dismiss();
                Toast.makeText(context, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
