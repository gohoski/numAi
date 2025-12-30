# numAi
**English** / [русский](README.ru.md) 

A simple AI app compatible with **Android 1.0+**. Access ChatGPT, DeepSeek, Gemini, Grok, Qwen, GLM, and other LLMs in one simple app on your legacy device.

![numAi](img/logo.png "AI client for legacy Android devices")

![Screenshot](img/scr1.png) ![Screenshot](img/scr2.png) ![Screenshot](img/scr3.png)
* **Telegram channel with updates**: [English @AppDataEN](https://t.me/AppDataEN) / [русский @AppDataApps](https://t.me/AppDataApps)
* **[Retro Android Group](https://t.me/retroandroidgroup)** on Telegram

## 📥 Download
* [GitHub Releases](https://github.com/gohoski/numAi/releases) *(Recommended)*
* Telegram (link at the top of the README)

> [!IMPORTANT]  
> Please note that the app was developed for Android <10, so it may work unstably or display incorrectly on newer versions. However, you can still report issues for newer versions of Android.

## Features
* Support of various APIs and models that support the OpenAI format (i.e. most LLM APIs)
* Thinking mode (switch between chat and thinking model)
* Vision (image attachments)
* Ability to change the system prompt
### TODO
* Markdown formatting support
* File attachments
* Import API key from file

## Reporting bugs
**Report bugs in the [Issues](https://github.com/gohoski/numAi/issues) tab!** Don't forget to specify which version of Android you encountered the bug on.

## Recommended models
<small>As of December 2025 for the Ollama API</small>
* Chat model — `deepseek-v3.1`, or `qwen3-vl-235b-instruct` if you need vision
* Thinking model — `deepseek-v3.2` or `gemini-3-flash`/`gemini-3-pro` (has heavy rate limits) or `qwen3-vl-235b` if you need vision

<small>As of December 2025 for VoidAI</small>
* Chat model — `deepseek-v3.2` or `gemini-3-flash`
* Thinking model — `deepseek-v3.2` or `gemini-3-flash` or `glm-4.7`
### Notes
* Only 'vision' models support images — if an image is not processed by the AI, try switching the model to Qwen3-VL-235B, GPT-5 mini, Gemini 3, or any other vision model.
* Models with `Instruct` in the name do not support Thinking mode, and models with `Thinking` only support Thinking mode. Some models only support either thinking (e.g., MiniMax M2 and Gemini 3 Pro) or chat, so if you choose the wrong one, there may be display issues—please enter correct models.
* Gemini outputs text slowly and does not show the thought process. This is a quirk of API due to Google's decisions.

## API key setup guide
All of the following APIs have free quotas—no payment is required.
### VoidAI (Android 2.3+)
> [!TIP]  
> If you have a Discord account, it is recommended to try [NavyAI](https://api.navy) as it has more models. However, it also uses Cloudflare (see below).

> [!WARNING]  
> This API uses Cloudflare, which makes it unavailable for Android 1.0–2.2. For more details, see ["Why are there limitations on Android 2.2 and below?"](#why-are-there-limitations-on-android-22-and-below).

1. On a modern browser, go to [voidai.app/register](https://voidai.app/register) and create an account.
2. After logging in, navigate to the **API Keys** section in your dashboard.
3. Click **Generate New API Key**.
4. Copy the key that appears and transfer it to your device.

### Ollama Cloud
> [!TIP]  
> This API doesn't use Cloudflare and is **recommended to use** on Android 1.0–2.2. This provider also has vision models (models that can see images; `Qwen3-VL-235B`).

1. On a modern browser, go to [signin.ollama.com/sign-up](https://signin.ollama.com/sign-up) and create an account.
2. After logging in, go to [ollama.com/settings/keys](https://ollama.com/settings/keys).
3. Click **Add API Key**, then **Generate API Key**.
4. Copy the key and transfer it to your device. Instead of VoidAI, choose Ollama in the dropdown menu.

### Baseten
*This API doesn't use Cloudflare and is available on all Android versions.* However, it could be tougher to sign up for due to possible requirement of **manual approval** of your account by the Baseten team. This provider does not have vision models.
1. On a modern browser, go to [app.baseten.co/signup](https://app.baseten.co/signup?next=/) and create an account. **It is recommended to sign up via social** (GitHub/Google) to make it less likely for the "We need more information to approve your account" popup to show—however, if it does popup, try to submit the application with your information and wait for a response.
2. After logging in, go to [app.baseten.co/model-apis/create](https://app.baseten.co/model-apis/create), select any model and click **Add new Model API**
3. On the next screen, click **View API endpoint**, click **Generate API key**
4. Copy the key and transfer it to your device. Instead of VoidAI, choose Baseten in the dropdown menu.

### Why are there limitations on Android 2.2 and below?
Cloudflare and some other network services block Java 5 TLS 1.0 requests due to their TLS fingerprinting systems seeing them as suspicious. Since Android 1.0–2.2 use Java 5 and most AI services use such network services, you may not be able to connect to them. Android 2.3–4.4 use Java 6, so there are no problems there. The same blocks happen with Java 5 on PC, so this is not an Android problem specifically. Strangely, you either get a *403 Forbidden* page or the Client Hello handshake is reset. The issue is not in certificates, as we already ignore them in our code *(while it is unsafe, I doubt anyone would specifically target a free LLM API service)*.

The exact reason of why Java 5 requests are detected is unknown to me, since TLS 1.0 support is present and works with cURL and Java 6. It is known that not all network services perform such blocks yet, so Ollama, Baseten and Upstage, which are hosted on Google Cloud and Amazon Web Services, work on Android <=2.2.

There are only two ways to fix this problem:
1. Try to compile OpenSSL/wolfSSL/??? for TLS 1.2 support. However, this would rapidly increase the difficulty of compiling this project and is unneeded as there are still AI services that work on Java 5. *I will be rejecting any PRs implementing this for now.*
2. **Setup an HTTPS -> HTTP reverse proxy, e.g. using nginx.** This is a recommended and feasible solution to do for anyone with a VPS. However, what to do for people that don't have a VPS is uncertain. *If anyone decides to host one, feel free to contact me so I can add your proxy server, but it should be stable.*

## Build
The project is developed under the following build environment.
* Android Studio 2.3.2 [`Download`](https://developer.android.com/studio/archive)
  * Android Studio 1.0–3.1.2 may support Android <2.2, but 2.3.2 is recommended for development as it's simultaneously old and supported.
  * Latest AS versions still support Android 2.2 and later (though they are made with 4.1+ in mind)—you can use them if you don't prioritize old Android versions.
* Android SDK of any version *(25 recommended)*
  * It is not required to use an old SDK for developing legacy apps.
* Android 1.0 emulator from the SDK [`Download`](https://developer.android.com/sdk/older_releases#release-1.0-r1)

It is recommended to use AS while contributing; however, you may use another IDE as long as you make the project still usable in AS.

## Acknowledgments
* [How-to-develop-and-backport-for-Android-2.1-in-2020](https://github.com/Mik-el/How-to-develop-and-backport-for-Android-2.1-in-2020) project template by Michele
* [NNJSON](https://github.com/shinovon/NNJSON) library by nnproject
* The simplified [Base64](app/src/main/java/io/github/gohoski/numai/Base64.java) implementation is adapted from [Robert Harder's](https://iharder.sourceforge.net/current/java/base64/) public domain code
* [ReOldAI by YMP Yuri](https://github.com/YMP-CO/ReOldAi) — Although not used as inspiration or a codebase, this similar app, which utilizes the Gemini API, provided motivation for the project
## License
The **numAi** project is licensed under the Do What The Fuck You Want To Public License, Version 2. See [LICENSE](LICENSE) for details. *If you want, you may credit me in the README of your project.*  

HOWEVER, the NNJSON library is licensed under the MIT license. See [LICENSE-NNJSON](LICENSE-NNJSON) for details.