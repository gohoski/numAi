package io.github.gohoski.numai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Gleb on 27.09.2025.
 *
 * First-time setup for the API key, which contains a user-friendly guide.
 */

public class FirstTimeActivity extends Activity {
    private ViewFlipper viewFlipper;
    private String apiKey="";
    private ApiService apiService;
    private ConfigManager apiConfig;
    private Context context;
    private Spinner spinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_firsttime);

        apiService = new ApiService(this);
        apiConfig = ConfigManager.getInstance(this);
        SSLDisabler.disableSSLCertificateChecking();
        context = this;

        viewFlipper = (ViewFlipper) findViewById(R.id.view_flipper);
        setupNavigation();
        runScreenSpecificCode(); // Initialize first screen
    }

    private void setupNavigation() {
        // Screen 1 (index 0)
        View screen1 = viewFlipper.getChildAt(0);
        Button skipButton = (Button) screen1.findViewById(R.id.skip);
        Button nextButton1 = (Button) screen1.findViewById(R.id.next);

        skipButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                forceShowScreen(2); //go to API key entering
            }
        });

        nextButton1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showNextScreen();
            }
        });

        // Screen 2 (index 1)
        View screen2 = viewFlipper.getChildAt(1);
        Button backButton2 = (Button) screen2.findViewById(R.id.back);
        Button nextButton2 = (Button) screen2.findViewById(R.id.next);

        backButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showPreviousScreen();
            }
        });
        nextButton2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showNextScreen();
            }
        });


        // Screen 3 (index 2)
        View screen3 = viewFlipper.getChildAt(2);
        Button backButton3 = (Button) screen3.findViewById(R.id.back);
        Button nextButton3 = (Button) screen3.findViewById(R.id.next);
        final EditText keyText = (EditText) screen3.findViewById(R.id.apiKey);
        spinner = (Spinner) findViewById(R.id.provider_spinner);
        SpinnerHelper.setupApiSpinner(context, spinner, apiConfig, new SpinnerHelper.ApiSelectionCallback() {
            @Override
            public void onApiSelected(String api) {
                System.out.println(api);
            }
        });

        backButton3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showPreviousScreen();
            }
        });
        nextButton3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                keyText.clearFocus();
                if (Integer.parseInt(Build.VERSION.SDK) >= 3) {
                    // Use reflection to get the InputMethodManager and hide the keyboard
                    // without reflection, java.lang.VerifyError is thrown on Android 1.0/1.1
                    try {
                        Class<?> immClass = Class.forName("android.view.inputmethod.InputMethodManager");
                        Object imm = getSystemService(Context.INPUT_METHOD_SERVICE);
                        Method hideMethod = immClass.getMethod(
                                "hideSoftInputFromWindow",
                                android.os.IBinder.class, //type of the first arg
                                Integer.TYPE // type of the second arg
                        );
                        hideMethod.invoke(imm, keyText.getWindowToken(), 0);
                    } catch (Exception e) {
                        Log.e("FirstTimeActivity", "Failed to hide keyboard via reflection", e);
                    }
                }
                showNextScreen();
            }
        });
        keyText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                apiKey = keyText.getText().toString();
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void showNextScreen() {
        viewFlipper.showNext();
        runScreenSpecificCode();
    }

    private void showPreviousScreen() {
        viewFlipper.showPrevious();
        runScreenSpecificCode();
    }

    private void forceShowScreen(int screen) {
        viewFlipper.setDisplayedChild(screen);
        runScreenSpecificCode();
    }

    private void runScreenSpecificCode() {
        switch (viewFlipper.getDisplayedChild()) {
            case 0:
            case 2:
                break;
            case 1:
                // On Android <=2.2, Cloudflare returns a 403 error, needs some research
                // We need to use the old String var because SDK_INT got to exist only in Android 1.6
                if (Integer.parseInt(Build.VERSION.SDK) < 9) {
                    new AlertDialog.Builder(this)
                        .setTitle(R.string.cloudflare_title)
                        .setMessage(R.string.cloudflare_msg)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {}
                        })
                        .show();
                }
                break;
            case 3:
                apiConfig.updateApiKey(apiKey);
                apiConfig.updateBaseUrl(ApiManager.getUrlByName(spinner.getSelectedItem().toString()));
                //let's test the key by loading models
                apiService.getModels(new ApiCallback<ArrayList<String>>() {
                    @Override
                    public void onSuccess(ArrayList<String> models) {
                        try {
                            //key good!
                            //since we loaded the models, let's select the best model automatically
                            apiConfig.updateChatModel(ModelSelector.selectChatModel(models));
                            //Dirty fix but DeepSeek doesn't support thinking on VoidAI for some reason...
                            apiConfig.updateThinkingModel(spinner.getSelectedItem().toString().equals("VoidAI") ? "gemini-3-flash-preview" : ModelSelector.selectThinkingModel(models));

                            // Then report to the user to start chatting
                            ProgressBar loading = (ProgressBar) findViewById(R.id.progress_loader);
                            loading.setVisibility(View.GONE);

                            TextView title = (TextView) findViewById(R.id.title);
                            title.setText(R.string.hello_user);
                            title.setTextSize(25f);
                            ImageView bugdroid = (ImageView) findViewById(R.id.bugdroid);
                            bugdroid.setVisibility(View.VISIBLE);

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Button startButton = (Button) findViewById(R.id.startChatting);
                                    Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
                                    fadeIn.setDuration(1000);
                                    fadeIn.setInterpolator(new AccelerateInterpolator());
                                    startButton.setVisibility(View.VISIBLE);
                                    startButton.startAnimation(fadeIn);
                                    startButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            Intent intent = new Intent(context, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        }
                                    });
                                }
                            }, 1000);
                        } catch(Exception e) {
                            e.printStackTrace();
                            finish();
                        }
                    }

                    @Override
                    public void onError(ApiError error) {
                        //api is down/key is invalid, remove the key for stability
                        apiConfig.updateApiKey("");
                        error.printStackTrace();
                        Toast.makeText(context, getString(R.string.api_key_error) + " " + error.getMessage(), Toast.LENGTH_LONG).show();
                        showPreviousScreen();
                    }
                });
                break;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            switch (viewFlipper.getDisplayedChild()) {
                case 0: finish(); break;
                case 3: break;
                default: viewFlipper.showPrevious();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
