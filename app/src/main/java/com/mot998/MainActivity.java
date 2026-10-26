package com.mot998;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    private static class TabModel {
        String title;
        String url;
        WebView webView;

        TabModel(String title, String url, WebView webView) {
            this.title = title;
            this.url = url;
            this.webView = webView;
        }
    }

    private final List<TabModel> tabList = new ArrayList<>();
    private int currentTabPosition = -1;

    private EditText urlInput;
    private ProgressBar progressBar;
    private FrameLayout container;
    private LinearLayout homeView;
    private TextView blockedCounterTv;
    private TextView tabCounterBtn;

    private int adsBlockedCount = 0;
    private boolean isDesktopMode = false;
    private boolean isIncognito = false;

    private static final String[] AD_FILTERS = {
            "doubleclick.net", "googleadservices.com", "googlesyndication.com",
            "adnxs.com", "facebook.com/tr/", "analytics.google.com", "adservice.google.com"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#121212"));

        // 1. شريط البحث العلوي
        LinearLayout topBar = createTopBar();
        rootLayout.addView(topBar);

        // 2. شريط التقدم (ProgressBar)
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgressDrawable(new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.parseColor("#FF5722"), Color.parseColor("#FF9800")}
        ));
        progressBar.setVisibility(View.GONE);
        rootLayout.addView(progressBar);

        // 3. حاوي المحتوى
        container = new FrameLayout(this);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1.0f);
        container.setLayoutParams(containerParams);

        homeView = createBraveHomeDashboard();
        container.addView(homeView);
        rootLayout.addView(container);

        // 4. شريط التحكم السفلي
        LinearLayout bottomBar = createBottomBar();
        rootLayout.addView(bottomBar);

        setContentView(rootLayout);

        // إنشاء التبويب الأول
        createNewTab("https://duckduckgo.com", false);
        showHomeScreen();
    }

    private void createNewTab(String url, boolean openImmediately) {
        WebView newWebView = new WebView(this);
        newWebView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        newWebView.setVisibility(View.GONE);
        setupBraveSettings(newWebView);

        TabModel tab = new TabModel("تبويب جديد", url, newWebView);
        tabList.add(tab);
        container.addView(newWebView);

        updateTabCounter();

        if (openImmediately) {
            switchToTab(tabList.size() - 1);
            if (url != null && !url.isEmpty()) {
                loadUrl(url);
            }
        }
    }

    private void switchToTab(int position) {
        if (position < 0 || position >= tabList.size()) return;

        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            tabList.get(currentTabPosition).webView.setVisibility(View.GONE);
        }

        currentTabPosition = position;
        TabModel currentTab = tabList.get(currentTabPosition);

        homeView.setVisibility(View.GONE);
        currentTab.webView.setVisibility(View.VISIBLE);

        if (currentTab.webView.getUrl() != null) {
            urlInput.setText(currentTab.webView.getUrl());
        } else {
            urlInput.setText(currentTab.url);
        }
    }

    private void closeTab(int position) {
        if (tabList.size() <= 1) {
            Toast.makeText(this, "لا يمكن إغلاق التبويب الأخير", Toast.LENGTH_SHORT).show();
            return;
        }

        TabModel tabToRemove = tabList.get(position);
        container.removeView(tabToRemove.webView);
        tabToRemove.webView.destroy();
        tabList.remove(position);

        if (currentTabPosition >= tabList.size()) {
            currentTabPosition = tabList.size() - 1;
        }

        updateTabCounter();
        switchToTab(currentTabPosition);
    }

    private void updateTabCounter() {
        if (tabCounterBtn != null) {
            tabCounterBtn.setText(String.valueOf(tabList.size()));
        }
    }

    private void showTabsManagerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout dialogLayout = new LinearLayout(this);
        dialogLayout.setOrientation(LinearLayout.VERTICAL);
        dialogLayout.setPadding(30, 30, 30, 30);
        dialogLayout.setBackgroundColor(Color.parseColor("#1C1C24"));

        LinearLayout topHeader = new LinearLayout(this);
        topHeader.setOrientation(LinearLayout.HORIZONTAL);
        topHeader.setGravity(Gravity.CENTER_VERTICAL);
        topHeader.setPadding(0, 0, 0, 20);

        TextView title = new TextView(this);
        title.setText("التبويبات المفتوحة (" + tabList.size() + ")");
        title.setTextColor(Color.WHITE);
        title.setTextSize(18);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        Button newTabBtn = createStyledButton("➕ تبويب جديد", Color.parseColor("#FF5722"));
        topHeader.addView(title);
        topHeader.addView(newTabBtn);
        dialogLayout.addView(topHeader);

        final AlertDialog dialog = builder.setView(scrollView).create();

        newTabBtn.setOnClickListener(v -> {
            dialog.dismiss();
            createNewTab("", true);
            showHomeScreen();
        });

        for (int i = 0; i < tabList.size(); i++) {
            final int index = i;
            TabModel tab = tabList.get(i);

            LinearLayout tabCard = new LinearLayout(this);
            tabCard.setOrientation(LinearLayout.HORIZONTAL);
            tabCard.setPadding(25, 25, 25, 25);
            tabCard.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(i == currentTabPosition ? Color.parseColor("#2C2C3A") : Color.parseColor("#23232D"));
            cardBg.setCornerRadius(15);
            tabCard.setBackground(cardBg);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 10, 0, 10);
            tabCard.setLayoutParams(cardParams);

            TextView tabTitle = new TextView(this);
            String titleText = tab.webView.getTitle();
            if (titleText == null || titleText.isEmpty()) titleText = tab.title;
            tabTitle.setText((i + 1) + ". " + titleText);
            tabTitle.setTextColor(Color.WHITE);
            tabTitle.setTextSize(14);
            tabTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

            TextView closeBtn = new TextView(this);
            closeBtn.setText(" ❌ ");
            closeBtn.setTextSize(16);
            closeBtn.setTextColor(Color.RED);
            closeBtn.setOnClickListener(v -> {
                dialog.dismiss();
                closeTab(index);
            });

            tabCard.addView(tabTitle);
            tabCard.addView(closeBtn);

            tabCard.setOnClickListener(v -> {
                dialog.dismiss();
                switchToTab(index);
            });

            dialogLayout.addView(tabCard);
        }

        scrollView.addView(dialogLayout);
        dialog.show();
    }

    private LinearLayout createTopBar() {
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setPadding(20, 15, 20, 15);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setBackgroundColor(Color.parseColor("#1E1E24"));

        TextView lionIcon = new TextView(this);
        lionIcon.setText("🦁 ");
        lionIcon.setTextSize(18);
        topBar.addView(lionIcon);

        urlInput = new EditText(this);
        urlInput.setHint("بحث في Google أو اكتب عنوانًا");
        urlInput.setHintTextColor(Color.GRAY);
        urlInput.setTextColor(Color.WHITE);
        urlInput.setSingleLine(true);
        urlInput.setTextSize(14);
        urlInput.setPadding(30, 20, 30, 20);
        urlInput.setImeOptions(EditorInfo.IME_ACTION_GO);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(Color.parseColor("#2B2B36"));
        inputBg.setCornerRadius(30);
        urlInput.setBackground(inputBg);

        LinearLayout.LayoutParams urlParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f);
        urlParams.setMargins(10, 0, 10, 0);
        urlInput.setLayoutParams(urlParams);

        // إصلاح حدث البحث من الكيبورد
        urlInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH 
                        || actionId == EditorInfo.IME_ACTION_DONE 
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                    loadUrlFromInput();
                    return true;
                }
                return false;
            }
        });

        topBar.addView(urlInput);
        return topBar;
    }

    private LinearLayout createBraveHomeDashboard() {
        LinearLayout mainHome = new LinearLayout(this);
        mainHome.setOrientation(LinearLayout.VERTICAL);
        mainHome.setPadding(30, 40, 30, 30);
        mainHome.setBackgroundColor(Color.parseColor("#18181C"));

        LinearLayout statsCard = new LinearLayout(this);
        statsCard.setOrientation(LinearLayout.VERTICAL);
        statsCard.setPadding(35, 35, 35, 35);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Color.parseColor("#25252E"));
        cardBg.setCornerRadius(30);
        statsCard.setBackground(cardBg);

        TextView statsTitle = new TextView(this);
        statsTitle.setText("🛡️ إحصائيات الخصوصية");
        statsTitle.setTextColor(Color.WHITE);
        statsTitle.setTextSize(16);
        statsCard.addView(statsTitle);

        LinearLayout numbersLayout = new LinearLayout(this);
        numbersLayout.setOrientation(LinearLayout.HORIZONTAL);
        numbersLayout.setPadding(0, 30, 0, 10);
        numbersLayout.setWeightSum(3);

        blockedCounterTv = createStatItem(numbersLayout, "3.6K", "تم حظر أدوات\nالتتبع والإعلانات");
        createStatItem(numbersLayout, "79 MB", "البيانات التقديرية\nالمحفوظة");
        createStatItem(numbersLayout, "3m", "الوقت المقدر\nالذي تم توفيره");

        statsCard.addView(numbersLayout);
        mainHome.addView(statsCard);

        LinearLayout shortcutsLayout = new LinearLayout(this);
        shortcutsLayout.setOrientation(LinearLayout.HORIZONTAL);
        shortcutsLayout.setPadding(0, 40, 0, 0);
        shortcutsLayout.setGravity(Gravity.CENTER);

        addShortcutItem(shortcutsLayout, "🌐", "Google", "https://google.com");
        addShortcutItem(shortcutsLayout, "🦆", "DuckDuckGo", "https://duckduckgo.com");
        addShortcutItem(shortcutsLayout, "🤖", "Arena AI", "https://chat.lmsys.org");
        addShortcutItem(shortcutsLayout, "✉️", "Temp Mail", "https://tempmail.com");

        mainHome.addView(shortcutsLayout);
        return mainHome;
    }

    private TextView createStatItem(LinearLayout parent, String number, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView numTv = new TextView(this);
        numTv.setText(number);
        numTv.setTextColor(Color.parseColor("#FF5722"));
        numTv.setTextSize(20);
        numTv.setGravity(Gravity.CENTER);

        TextView lblTv = new TextView(this);
        lblTv.setText(label);
        lblTv.setTextColor(Color.GRAY);
        lblTv.setTextSize(10);
        lblTv.setGravity(Gravity.CENTER);

        item.addView(numTv);
        item.addView(lblTv);
        parent.addView(item);
        return numTv;
    }

    private void addShortcutItem(LinearLayout parent, String icon, String name, final String url) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        item.setPadding(20, 10, 20, 10);

        TextView iconTv = new TextView(this);
        iconTv.setText(icon);
        iconTv.setTextSize(24);
        iconTv.setPadding(20, 20, 20, 20);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#2B2B36"));
        bg.setCornerRadius(50);
        iconTv.setBackground(bg);

        TextView nameTv = new TextView(this);
        nameTv.setText(name);
        nameTv.setTextColor(Color.WHITE);
        nameTv.setTextSize(11);
        nameTv.setPadding(0, 10, 0, 0);

        item.addView(iconTv);
        item.addView(nameTv);

        item.setOnClickListener(v -> loadUrl(url));
        parent.addView(item);
    }

    private LinearLayout createBottomBar() {
        LinearLayout bottomBar = new LinearLayout(this);
        bottomBar.setOrientation(LinearLayout.HORIZONTAL);
        bottomBar.setPadding(10, 20, 10, 20);
        bottomBar.setGravity(Gravity.CENTER);
        bottomBar.setBackgroundColor(Color.parseColor("#18181C"));
        bottomBar.setWeightSum(5);

        addBottomNavItem(bottomBar, "⋮", v -> showBraveMenu());

        tabCounterBtn = createBottomNavTextView("1", v -> showTabsManagerDialog());
        bottomBar.addView(tabCounterBtn);

        addBottomNavItem(bottomBar, "🔍", v -> urlInput.requestFocus());
        addBottomNavItem(bottomBar, "🔖", v -> Toast.makeText(this, "الإشارات المرجعية", Toast.LENGTH_SHORT).show());
        addBottomNavItem(bottomBar, "🏠", v -> showHomeScreen());

        return bottomBar;
    }

    private TextView createBottomNavTextView(String text, View.OnClickListener listener) {
        TextView btn = new TextView(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(18);
        btn.setGravity(Gravity.CENTER);
        btn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        btn.setOnClickListener(listener);
        return btn;
    }

    private void addBottomNavItem(LinearLayout parent, String text, View.OnClickListener listener) {
        parent.addView(createBottomNavTextView(text, listener));
    }

    private void showBraveMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_NoActionBar);
        ScrollView scrollView = new ScrollView(this);
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        menuLayout.setPadding(40, 40, 40, 40);
        menuLayout.setBackgroundColor(Color.parseColor("#23232C"));

        addMenuItem(menuLayout, "➕  علامة تبويب جديدة", v -> {
            createNewTab("", true);
            showHomeScreen();
        });
        addMenuItem(menuLayout, "🕶️  علامة تبويب خاصة جديدة", v -> toggleIncognito());
        addMenuItem(menuLayout, "🧹  حذف بيانات الموقع", v -> clearCurrentSiteData());
        addMenuItem(menuLayout, "🖥️  موقع مصمَّم للكمبيوتر", v -> toggleDesktopMode());
        addMenuItem(menuLayout, "⚙️  الإعدادات", v -> {});

        scrollView.addView(menuLayout);
        builder.setView(scrollView);
        builder.show();
    }

    private void addMenuItem(LinearLayout parent, String title, View.OnClickListener listener) {
        TextView item = new TextView(this);
        item.setText(title);
        item.setTextColor(Color.WHITE);
        item.setTextSize(15);
        item.setPadding(20, 25, 20, 25);
        item.setOnClickListener(listener);
        parent.addView(item);
    }

    private void setupBraveSettings(WebView webViewInstance) {
        WebSettings settings = webViewInstance.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setSavePassword(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webViewInstance, false);

        webViewInstance.setWebViewClient(new SecureWebViewClient());

        // إضافة WebChromeClient للتحكم بشريط التقدم الحقيقي (ProgressBar)
        webViewInstance.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void loadUrlFromInput() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) return;

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.contains(".") && !url.contains(" ")) {
                url = "https://" + url;
            } else {
                url = "https://duckduckgo.com/?q=" + url;
            }
        }
        loadUrl(url);
    }

    private void loadUrl(String url) {
        if (currentTabPosition < 0 || currentTabPosition >= tabList.size()) {
            createNewTab(url, true);
            return;
        }

        homeView.setVisibility(View.GONE);
        WebView activeWeb = tabList.get(currentTabPosition).webView;
        activeWeb.setVisibility(View.VISIBLE);
        activeWeb.loadUrl(url);
    }

    private void showHomeScreen() {
        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            tabList.get(currentTabPosition).webView.setVisibility(View.GONE);
        }
        homeView.setVisibility(View.VISIBLE);
        urlInput.setText("");
    }

    private void clearCurrentSiteData() {
        if (currentTabPosition < 0 || homeView.getVisibility() == View.VISIBLE) {
            Toast.makeText(this, "قم بفتح موقع أولاً لمسح بياناته", Toast.LENGTH_SHORT).show();
            return;
        }

        WebView activeWeb = tabList.get(currentTabPosition).webView;
        String currentUrl = activeWeb.getUrl();
        if (currentUrl == null) return;

        try {
            Uri uri = Uri.parse(currentUrl);
            String host = uri.getHost();

            if (host != null) {
                WebStorage.getInstance().deleteOrigin(uri.getScheme() + "://" + host);
                activeWeb.clearCache(true);
                activeWeb.reload();
                Toast.makeText(this, "تم مسح بيانات: " + host, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "خطأ أثناء الحذف: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleDesktopMode() {
        isDesktopMode = !isDesktopMode;
        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            WebView activeWeb = tabList.get(currentTabPosition).webView;
            WebSettings settings = activeWeb.getSettings();
            if (isDesktopMode) {
                settings.setUserAgentString("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
                Toast.makeText(this, "تم تفعيل عرض الكمبيوتر", Toast.LENGTH_SHORT).show();
            } else {
                settings.setUserAgentString(null);
                Toast.makeText(this, "تم تفعيل عرض الهاتف", Toast.LENGTH_SHORT).show();
            }
            activeWeb.reload();
        }
    }

    private void toggleIncognito() {
        isIncognito = !isIncognito;
        if (isIncognito) {
            CookieManager.getInstance().removeAllCookies(null);
            Toast.makeText(this, "تم التبديل للتصفح الخفي", Toast.LENGTH_SHORT).show();
        }
        createNewTab("", true);
        showHomeScreen();
    }

    private class SecureWebViewClient extends WebViewClient {
        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            progressBar.setVisibility(View.VISIBLE);
            urlInput.setText(url);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            progressBar.setVisibility(View.GONE);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString().toLowerCase();
            for (String adDomain : AD_FILTERS) {
                if (url.contains(adDomain)) {
                    runOnUiThread(() -> {
                        adsBlockedCount++;
                        blockedCounterTv.setText(String.valueOf(adsBlockedCount));
                    });
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream("".getBytes()));
                }
            }
            return super.shouldInterceptRequest(view, request);
        }
    }

    @Override
    public void onBackPressed() {
        if (currentTabPosition >= 0 && currentTabPosition < tabList.size()) {
            WebView activeWeb = tabList.get(currentTabPosition).webView;
            if (activeWeb.getVisibility() == View.VISIBLE && activeWeb.canGoBack()) {
                activeWeb.goBack();
                return;
            } else if (activeWeb.getVisibility() == View.VISIBLE) {
                showHomeScreen();
                return;
            }
        }
        super.onBackPressed();
    }

    private Button createStyledButton(String text, int bgColor) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(11);

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(15);
        shape.setColor(bgColor);
        button.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(4, 0, 4, 0);
        button.setLayoutParams(params);

        return button;
    }
}
