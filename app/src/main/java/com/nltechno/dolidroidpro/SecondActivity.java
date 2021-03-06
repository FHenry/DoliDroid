/* Copyright (C) 2013 Laurent Destailleur <eldy@users.sourceforge.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * or see http://www.gnu.org/
 */
package com.nltechno.dolidroidpro;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import com.nltechno.dolidroidpro.DoliDroid;
import com.nltechno.utils.MySSLSocketFactory;
import com.nltechno.utils.Utils;
/*
import com.nltechno.inapp.IabHelper;
import com.nltechno.inapp.IabResult;
import com.nltechno.inapp.Inventory;
import com.nltechno.inapp.Purchase;
*/

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.DownloadManager.Request;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.CookieManager;
import android.webkit.HttpAuthHandler;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.CookieSyncManager;
import android.webkit.WebViewDatabase;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.app.AlertDialog;


/**
 * Second activity class
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
@SuppressLint("SetJavaScriptEnabled")
public class SecondActivity extends Activity {

	private static final String LOG_TAG = "DoliDroidActivity";
	public static final String VERSION_RESOURCES = "12.0";

	private WebView myWebView;
	private WebViewClientDoliDroid myWebViewClientDoliDroid;
	private WebChromeClientDoliDroid myWebChromeClientDoliDroid;
	
	private ValueCallback<Uri[]> mFilePathCallback;
	private ValueCallback<Uri> mUploadMessage;
	
	private String savedDolRootUrl;
	private String savedDolRootUrlRel;
	private String savedDolScheme;
	private int savedDolPort;
	private String savedDolHost;
	private String savedDolBasedUrl;
	private String savedDolBasedUrlWithSForced;
	private String savedAuthuser=null;
	private String savedAuthpass=null;
	private String savedUserAgent=null;

	private String saveQueryForonRequestPermissionsResult;
	private String saveUrlForonRequestPermissionsResult;
	private String saveListOfCookiesForonRequestPermissionsResult;

	private boolean prefAlwaysUseLocalResources=true;
	public boolean sslErrorWasAccepted=false;

	private String lastversionfound;
	private String lastversionfoundforasset;


	// Variables used to manage cache and error retry
	private boolean tagToOverwriteLoginPass=true;
	private boolean tagLastLoginPassToSavedLoginPass=false;	// This is set to true after submitting login form
	private boolean tagToLogout=false;
	private String tagToShowInterruptMessage="";
	private int tagToShowInterruptCounter=0;
	private String tagToShowMessage="";
	private int tagToShowCounter=0;
	private String cacheForMenu;
	private String cacheForQuickAccess;
	private String lastLoadUrl;
	private boolean	isMulticompanyOn=false;

	private String menuAre="hardwareonly";
	private Menu savMenu;
	private boolean noPreviousPageShown=false;
	String listOfCookiesAfterLogon=null;
	private String mCameraPhotoPath;
	
    final Activity activity = this;
    private ProgressBar progress;
    static final int REQUEST_ABOUT = 0;
    static final int REQUEST_WEBVIEW = 1;
    static final int REQUEST_INPUTFILE = 2;
    static final int RESULT_LOGOUT =   RESULT_FIRST_USER+0; // We don't want to quit completely
    static final int RESULT_WEBVIEW =  RESULT_FIRST_USER+1;
    static final int RESULT_ABOUT =    RESULT_FIRST_USER+2;

    static final int REQUEST_CODE_ASK_PERMISSIONS_WRITE_EXTERNAL_STORAGE = 123;

    // For inapp purchases (public key is found into menu "Services and API" for application into Google play publish center).
    //IabHelper iabHelper;
    final String PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtKWPkZ1rys0aYT9qQ7gHytljus58x9ZNwFUabsXgRAua2RwVkHnFfc8L2p68ojIb2tNHiRvMV6hYH2qViylftEMSYLFoKnuHzpL4tc+Ic+cTv/KtubP+ehUfISPQfYrZrukp3E8y0zM795Agsy8mefc2mmuOFJny/IZFLNyM5J+vjhoE6mO2l3jBmo08zu/3tz8Mbo/VYqJSs+P9UTppwF8ovB6u3fGPFeqblAdGize9WQ1L4SXNYblIjCklYj0rbXHFN3aJCjV9sSo0U+qdi6i+mT+CZgj09W1+U7RpkNJ6OczspTwhFh7/1nEev3Zci17TIFXNyP2v5aGMoBuCPwIDAQAB";   // key dolidroid pro
    public static final String ITEM_SKU = "android.test.purchased";

    private final Pattern patternLoginHomePageForVersion = Pattern.compile(" (?:Doli[a-zA-Z]+|@) (\\d+)\\.(\\d+)\\.([^\\s]+)");     // Regex to extract version
    private final Pattern patternLoginHomePageForMulticompany = Pattern.compile("multicompany");                                    // Regex to know if multicompany module is on
    private final Pattern patternLoginPage = Pattern.compile("Login Doli[a-zA-Z]+ (\\d+)\\.(\\d+)\\.([^\\s]+)");                    // To know page is login page with dolibarr <= 3.6
    private final Pattern patternLoginPage2 = Pattern.compile("@ (?:Doli[a-zA-Z]+ |)(\\d+)\\.(\\d+)\\.([^\\s]+)");                  // To know page is login page with dolibarr >= 3.7
    
    private String nextAltHistoryStack = "";
    private String nextAltHistoryStackBis = "";
    ArrayList<String> altHistoryStack = new ArrayList<String>();

    // To store data for the download manager
    SharedPreferences preferenceManager;
    final String strPref_Download_ID = "PREF_DOWNLOAD_ID";


    // This is a UI Thread
    //@SuppressWarnings("deprecation")
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);

        Log.i(LOG_TAG, "onCreate savedInstanceState="+savedInstanceState);

        // This is to allow IO (Like HTTPGet into main thread).
        /*if (android.os.Build.VERSION.SDK_INT > 9) 
        {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }*/
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean prefAlwaysShowBar = sharedPrefs.getBoolean("prefAlwaysShowBar", true);
        boolean prefAlwaysAutoFill = sharedPrefs.getBoolean("prefAlwaysAutoFill", true);
        prefAlwaysUseLocalResources = sharedPrefs.getBoolean("prefAlwaysUseLocalResources", true);
        Log.d(LOG_TAG, "onCreate prefAlwaysShowBar="+prefAlwaysShowBar+" prefAlwaysAutoFill="+prefAlwaysAutoFill+" prefAlwaysUseLocResouces="+prefAlwaysUseLocalResources);
        
        tagToOverwriteLoginPass=prefAlwaysAutoFill;

        // Define kind of menu we want to use
        boolean hasMenuHardware = Utils.hasMenuHardware(this);
        if (! hasMenuHardware || prefAlwaysShowBar)
        {
            this.menuAre="actionbar";
        }
        Log.d(LOG_TAG, "onCreate hasMenuHardware="+hasMenuHardware+" menuAre="+this.menuAre);

        // menuAre is defined to buttonsbar, actionbar or hardwareonly
        if (this.menuAre.equals("actionbar"))
        {
            //getActionBar().setHomeButtonEnabled(true);
            //getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        else 
        {   // We choose menu using hardware
            // Hide actionbar without hiding title (no requestFeature(Window.FEATURE_NO_TITLE) because there is no way to restore actionbar after 
            ActionBar actionBar = getActionBar();
            if (actionBar != null) actionBar.hide();
        }
        //this.savWindow.requestFeature(Window.FEATURE_PROGRESS);
        //this.savWindow.setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);

        Intent intent = getIntent();
        String dolRootUrl = intent.getStringExtra("dolRootUrl");        
        String dolRequestUrl = intent.getStringExtra("dolRequestUrl");        
        this.savedDolRootUrl = dolRootUrl;                                                                                                                      // Example: http://testldr1.with.dolicloud.com/
        this.savedDolScheme=Uri.parse(this.savedDolRootUrl).getScheme();                                                                                        // Example: http
        this.savedDolPort=Uri.parse(this.savedDolRootUrl).getPort();
        this.savedDolHost=Uri.parse(this.savedDolRootUrl).getHost();
        this.savedDolBasedUrl = this.savedDolScheme+"://"+this.savedDolHost+((this.savedDolPort > 0 && this.savedDolPort != 80) ? ":"+this.savedDolPort : "");	// Example: http://testldr1.with.dolicloud.com
		this.savedDolBasedUrlWithSForced = "https:"+this.savedDolBasedUrl.replace("http:", "").replace("https:", "");
        this.savedDolRootUrlRel = dolRootUrl.replace(this.savedDolBasedUrl, "");	// rest of url														// Example: /

        try {
            URL uri=new URL(dolRootUrl);
            String userInfo=uri.getUserInfo();
            if (userInfo != null)
            {
                String[] credentials = userInfo.split(":");
                //view.setHttpAuthUsernamePassword(getBaseDomain(url), "Restricted", credentials[0], credentials[1]);
                savedAuthuser=credentials[0];
                savedAuthpass=credentials[1];
                Log.d(LOG_TAG, "onCreate Saving basic authentication found into URL authuser="+savedAuthuser+" authpass="+savedAuthpass);
            }
            else
            {
                Log.d(LOG_TAG, "onCreate No basic authentication info into URL");
                savedAuthuser=null;
                savedAuthpass=null;
            }
        }
        catch (MalformedURLException e) {
            Log.w(LOG_TAG, e.getMessage());
        }
        Log.d(LOG_TAG, "onCreate We have root URL : savedDolRootUrl=" + dolRootUrl + " => savedDolBasedUrl=" + this.savedDolBasedUrl + " savedDolBasedUrlWithSForced=" + this.savedDolBasedUrlWithSForced + " + savedDolRootUrlRel=" + this.savedDolRootUrlRel);

        String urlToGo = ""; 
        if (! dolRequestUrl.contains("?") && ! dolRequestUrl.contains(".php")) urlToGo = dolRequestUrl+"index.php?dol_hide_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile="+(DoliDroid.useJMobileAjax?'2':'1');
        else if (dolRequestUrl.contains("?")) urlToGo = dolRequestUrl+"&dol_hide_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile="+(DoliDroid.useJMobileAjax?'2':'1');
        else urlToGo = dolRequestUrl+"?dol_hide_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile="+(DoliDroid.useJMobileAjax?'2':'1');
        
        Log.d(LOG_TAG, "onCreate isDownloadManagerAvailable="+Utils.isDownloadManagerAvailable(this));
        Log.d(LOG_TAG, "onCreate We are in onCreate and will load URL urlToGo=" + urlToGo);

        // To have the view SecondActivity with WebView included:
        setContentView(R.layout.activity_second);

        progress = (ProgressBar) findViewById(R.id.progressBar1);
        progress.setMax(100);
        Drawable d=getResources().getDrawable(R.drawable.progressbar_style);
        ClipDrawable cd = new ClipDrawable(d, Gravity.START, ClipDrawable.HORIZONTAL);
        progress.setProgressDrawable(cd);
      
        myWebView = (WebView) findViewById(R.id.webViewContent);

        this.savedUserAgent = myWebView.getSettings().getUserAgentString() + " - " + getString(R.string.dolidroidUserAgent);

        myWebView.getSettings().setJavaScriptEnabled(true);
        myWebView.getSettings().setAllowFileAccess(true);
        //myWebView.getSettings().setLoadWithOverviewMode(true);
        //myWebView.getSettings().setUseWideViewPort(false);
        //myWebView.getSettings().setSavePassword(false);
        //myWebView.getSettings().setSaveFormData(false);
        myWebView.getSettings().setUserAgentString(this.savedUserAgent);
        // Cache for no network (we don't want it, so we use default)
        //myWebView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        //myWebView.getSettings().setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK); //LOAD_DEFAULT
        //myWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        myWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        //myWebView.getSettings().setRenderPriority(RenderPriority.HIGH);
        //myWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        final MyJavaScriptInterface myJavaScriptInterface = new MyJavaScriptInterface(activity);
        myWebView.addJavascriptInterface(myJavaScriptInterface, "HTMLOUT");
        myWebView.getSettings().setBuiltInZoomControls(true);
        Log.d(LOG_TAG, "onCreate Method setDisplayZoomControls exists. Set to false.");
        myWebView.getSettings().setDisplayZoomControls(false);          // Works with Android 3.0+ (level 11)
        this.myWebViewClientDoliDroid=new WebViewClientDoliDroid(this);
        myWebView.setWebViewClient(this.myWebViewClientDoliDroid);
        this.myWebChromeClientDoliDroid = new WebChromeClientDoliDroid();
        myWebView.setWebChromeClient(this.myWebChromeClientDoliDroid);
        
        lastLoadUrl=urlToGo;
        myWebView.loadUrl(urlToGo);
        
        CookieSyncManager.createInstance(this);
        
        // Init IabHelper for InApp purchase
        /*
        iabHelper = new IabHelper(this, PUBLIC_KEY);
        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (!result.isSuccess()) {
                    Log.d(LOG_TAG, "IAB setup failed : " + result);
                }
                else
                {
                    Log.d(LOG_TAG, "IAB setup OK");
                }
            }
        });
        */
    }


	
	/**
	 * Called when activity start
	 */
	@Override
    public void onStart() 
	{	
    	Log.i(LOG_TAG, "onStart");
    	super.onStart();

    	// We must reload menu (it may have been changed into other activities
		invalidateOptionsMenu();
	}


    /**
     *  Load SmartPhone menu
     *
     *  @param  Menu        menu    Object menu to initialize
     *  @return boolean             true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) 
    {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        getMenuInflater().inflate(R.menu.activity_second, menu);    // Deploy android menu
        Log.d(LOG_TAG, "onCreateOptionsMenu this.menuAre="+this.menuAre);

        // When there is hardware button and not using "actionbar", we remove the back from menu
        if (Utils.hasMenuHardware(activity) && ! this.menuAre.equals("actionbar"))
        {
            Log.d(LOG_TAG, "onCreateOptionsMenu Hide button back because there is hardware and this.menuAre="+this.menuAre);
            menu.findItem(R.id.menu_back).setVisible(false);
        }

        if (this.menuAre.equals("actionbar"))
        {
            menu.findItem(R.id.menu_menu).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.go_to).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.menu_back).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menu.findItem(R.id.menu_multicompany).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        if (this.menuAre.equals("hardwareonly"))
        {
            // Move entries from actionbar to list
            menu.findItem(R.id.menu_menu).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.findItem(R.id.go_to).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.findItem(R.id.menu_back).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            menu.findItem(R.id.menu_multicompany).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }

        // Hide menu show bar if there is no hardware, change label otherwise
        MenuItem menuItem = menu.findItem(R.id.always_show_bar);
        if (Utils.hasMenuHardware(activity))
        {
            boolean prefAlwaysShowBar = sharedPrefs.getBoolean("prefAlwaysShowBar", true);
            Log.d(LOG_TAG, "onCreateOptionsMenu prefAlwaysShowBar value is "+prefAlwaysShowBar);
            if (prefAlwaysShowBar) menuItem.setTitle(getString(R.string.menu_show_bar_on));
            else menuItem.setTitle(getString(R.string.menu_show_bar_off));
        }
        else
        {
            menuItem.setVisible(false);
        }

        // Hide menu show bar if phone too old, change label otherwise
        MenuItem menuItem2 = menu.findItem(R.id.always_autofill);
        boolean prefAlwaysAutoFill = sharedPrefs.getBoolean("prefAlwaysAutoFill", true);
        Log.d(LOG_TAG, "onCreateOptionsMenu prefAlwaysAutoFill value is "+prefAlwaysAutoFill);
        if (prefAlwaysAutoFill) menuItem2.setTitle(getString(R.string.menu_autofill_on));
        else menuItem2.setTitle(getString(R.string.menu_autofill_off));

        MenuItem menuItem4 = menu.findItem(R.id.always_uselocalresources);
        //boolean prefAlwaysUseLocalResources = sharedPrefs.getBoolean("prefAlwaysUseLocalResources", true);
        Log.d(LOG_TAG, "onCreateOptionsMenu prefAlwaysUseLocalResources value is "+prefAlwaysUseLocalResources);
        if (prefAlwaysUseLocalResources) menuItem4.setTitle(getString(R.string.menu_uselocalresources_on));
        else menuItem4.setTitle(getString(R.string.menu_uselocalresources_off));

        if (isMulticompanyOn) {
            Log.d(LOG_TAG, "onCreateOptionsMenu Module multicompany was found, we show picto");
            MenuItem menuItem5 = menu.findItem(R.id.menu_multicompany);
            if (menuItem5 != null) menuItem5.setVisible(true);
        } else {
            Log.d(LOG_TAG, "onCreateOptionsMenu Module multicompany was NOT found, we hide picto");
            MenuItem menuItem5 = menu.findItem(R.id.menu_multicompany);
            if (menuItem5 != null) menuItem5.setVisible(false);
        }

        this.savMenu = menu;
        
        return true;
    }

    /**
     *  Once we selected a menu option
     *
     *  @param  MenuItem    item    Menu item selected
     *  @return boolean             True if we selected a menu managed, False otherwise
     */
    public boolean onOptionsItemSelected(MenuItem item) 
    {
        Log.i(LOG_TAG, "SecondActivity::onOptionsItemSelected Click onto menu "+item.toString());

        SharedPreferences sharedPrefs = null;
        Editor editor = null;
        String urlToGo = ""; 
        
        switch (item.getItemId())
        {
            case R.id.menu_menu:
                return this.codeForMenu();
            case R.id.go_to:
                return this.codeForQuickAccess();
            case R.id.menu_back:
                return this.codeForBack();
            case R.id.menu_multicompany:
                return this.codeForMultiCompany();
            case R.id.always_show_bar:  // Switch menu bar on/off
                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                boolean prefAlwaysShowBar = sharedPrefs.getBoolean("prefAlwaysShowBar", true);
                Log.i(LOG_TAG, "Click onto switch show bar, prefAlwaysShowBar is "+prefAlwaysShowBar);
                prefAlwaysShowBar=!prefAlwaysShowBar;
                editor = sharedPrefs.edit();
                editor.putBoolean("prefAlwaysShowBar", prefAlwaysShowBar);
                editor.apply();
                Log.d(LOG_TAG, "Switched value is now "+prefAlwaysShowBar);
                // Update show bar or not
                if (prefAlwaysShowBar) 
                {
                    this.savMenu.findItem(R.id.always_show_bar).setTitle(getString(R.string.menu_show_bar_on));
                    this.menuAre="actionbar";
                    // Reload menu
                    invalidateOptionsMenu();
                    // Enable menu on screen
                    ActionBar actionBar = getActionBar();
                    if (actionBar != null) actionBar.show();
                }
                else
                {
                    this.savMenu.findItem(R.id.always_show_bar).setTitle(getString(R.string.menu_show_bar_off));
                    this.menuAre="hardwareonly";
                    // Disable menu from screen
                    ActionBar actionBar = getActionBar();
                    if (actionBar != null) actionBar.hide();
                    // Reload menu
                    invalidateOptionsMenu();
                }
                return true;
            case R.id.always_autofill:  // Switch menu bar on/off
                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                boolean prefAlwaysAutoFill = sharedPrefs.getBoolean("prefAlwaysAutoFill", true);
                Log.i(LOG_TAG, "Click onto switch autofill, prefAlwaysAutoFill is "+prefAlwaysAutoFill);
                prefAlwaysAutoFill=!prefAlwaysAutoFill;
                editor = sharedPrefs.edit();
                editor.putBoolean("prefAlwaysAutoFill", prefAlwaysAutoFill);
                editor.apply();
                Log.d(LOG_TAG, "Switched value is now "+prefAlwaysAutoFill);
                // Update show bar or not
                if (prefAlwaysAutoFill) 
                {
                    this.savMenu.findItem(R.id.always_autofill).setTitle(getString(R.string.menu_autofill_on));
                    invalidateOptionsMenu();
                }
                else
                {
                    this.savMenu.findItem(R.id.always_autofill).setTitle(getString(R.string.menu_autofill_off));
                    invalidateOptionsMenu();
                }
                return true;
            case R.id.always_uselocalresources:
                sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                //boolean prefAlwaysUseLocalResources = sharedPrefs.getBoolean("prefAlwaysUseLocalResources", true);
                Log.i(LOG_TAG, "Click onto switch uselocalresources, prefAlwaysUseLocalResources is "+prefAlwaysUseLocalResources);
                prefAlwaysUseLocalResources=!prefAlwaysUseLocalResources;
                editor = sharedPrefs.edit();
                editor.putBoolean("prefAlwaysUseLocalResources", prefAlwaysUseLocalResources);
                editor.apply();
                Log.d(LOG_TAG, "Switched value is now "+prefAlwaysUseLocalResources);
                //savedPrefAlwaysUseLocalResources=prefAlwaysUseLocalResources;
                // Update men label
                if (prefAlwaysUseLocalResources)
                {
                    this.savMenu.findItem(R.id.always_uselocalresources).setTitle(getString(R.string.menu_uselocalresources_on));
                    invalidateOptionsMenu();
                }
                else
                {
                    this.savMenu.findItem(R.id.always_uselocalresources).setTitle(getString(R.string.menu_uselocalresources_off));
                    invalidateOptionsMenu();
                }
                return true;
            case R.id.about:
                Log.i(LOG_TAG, "Start activity About");
                myWebView = (WebView) findViewById(R.id.webViewContent);
                Intent intent = new Intent(SecondActivity.this, AboutActivity.class);
                intent.putExtra("currentUrl", myWebView.getOriginalUrl());
                intent.putExtra("userAgent", myWebView.getSettings().getUserAgentString());
                intent.putExtra("savedDolRootUrl", this.savedDolRootUrl);
                intent.putExtra("lastversionfound", this.lastversionfound);
                intent.putExtra("lastversionfoundforasset", this.lastversionfoundforasset);
                intent.putExtra("title", myWebView.getTitle());
                intent.putExtra("savedAuthuser", this.savedAuthuser);
                intent.putExtra("savedAuthpass", this.savedAuthpass);
                Log.d(LOG_TAG, "startActivityForResult with requestCode="+REQUEST_ABOUT);
                startActivityForResult(intent,REQUEST_ABOUT);
                return true;
            case R.id.logout:
                tagToLogout=true;
                myWebView = (WebView) findViewById(R.id.webViewContent);
                urlToGo = this.savedDolRootUrl+"user/logout.php?noredirect=1&dol_hide_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile="+(DoliDroid.useJMobileAjax?'2':'1');
                Log.i(LOG_TAG, "LoadUrl after select Logout : "+urlToGo);
                lastLoadUrl=urlToGo;
                myWebView.loadUrl(urlToGo);             
                return true;
            case R.id.quit:
                Log.i(LOG_TAG, "Call finish activity, with setResult = "+RESULT_WEBVIEW);
                setResult(RESULT_WEBVIEW);
                finish();
                return true;    
            case R.id.refresh:
                myWebView = (WebView) findViewById(R.id.webViewContent);
                urlToGo = myWebView.getUrl();
                if (urlToGo != null)
                {
                    if (! urlToGo.contains("dol_hide_topmenu=")) urlToGo = urlToGo + (urlToGo.contains("?")?"&":"?") + "dol_hide_topmenu=1";
                    if (! urlToGo.contains("dol_hide_leftmenu=")) urlToGo = urlToGo + (urlToGo.contains("?")?"&":"?") + "dol_hide_leftmenu=1";
                    if (! urlToGo.contains("dol_optimize_smallscreen=")) urlToGo = urlToGo + (urlToGo.contains("?")?"&":"?") + "dol_optimize_smallscreen=1";
                    if (! urlToGo.contains("dol_no_mouse_hover=")) urlToGo = urlToGo + (urlToGo.contains("?")?"&":"?") + "dol_no_mouse_hover=1";
                    if (! urlToGo.contains("dol_use_jmobile=")) urlToGo = urlToGo + (urlToGo.contains("?")?"&":"?") + "dol_use_jmobile="+(DoliDroid.useJMobileAjax?'2':'1');
                    Log.d(LOG_TAG, "LoadUrl after select Refresh : Load url "+urlToGo);
                    lastLoadUrl=urlToGo;
                    myWebView.loadUrl(urlToGo);
                }
                return true;
            case R.id.clearcache:
                myWebView = (WebView) findViewById(R.id.webViewContent);
                Log.i(LOG_TAG, "Clear caches of webView");
                myWebView.clearCache(true);
                Log.d(LOG_TAG,"Clear also history of webview");
                myWebView.clearHistory();
                this.cacheForMenu=null;
                this.cacheForQuickAccess=null;
                //Log.d(LOG_TAG,"Clear also cookies");
                //this.myWebViewClientDoliDroid.deleteSessionCookies();
                return true;
        }
        
        return false;
    }

    
    /**
     * More info this method can be found at
     * http://developer.android.com/training/camera/photobasics.html
     *
     * @return
     * @throws IOException
     */
    /*
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "uploaded_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        return imageFile;
    }
    */
    
    
    /**
     * Return a DefaultHTTPClient with option to support untrusted HTTPS
     *  
     * @return HttpClient       Object derivated from DefaultHttpClient
     */
    public HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            MySSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(MySSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }
    
    /**
     * Class to load an URL in background
     * Used to load menu and quick search page (with Android 3+)
     */
    private class DownloadWebPageTask extends AsyncTask<String, Void, String> 
    {
        String mode;
        
        DownloadWebPageTask(String mode)
        {
            super();
            this.mode=mode;
        }
        
        /**
         * Launch download of url
         */
        @Override
        protected String doInBackground(String... urls) 
        {
          StringBuilder response = new StringBuilder();
          
          if (listOfCookiesAfterLogon != null)      // We do not try to load url if cookies are not yet set
          {
	          for (String url : urls)
	          {
	        	  //DefaultHttpClient client = new DefaultHttpClient();
	        	  HttpClient client = getNewHttpClient();
	        	  HttpGet httpGet = new HttpGet(url);
	        	  try {
	        		  Log.i(LOG_TAG, "doInBackground get url mode="+this.mode+" url="+url+" savedAuthuser="+savedAuthuser+" cookies="+listOfCookiesAfterLogon);
	
	        		  httpGet.setHeader("Cookie", listOfCookiesAfterLogon);
	        		  //httpGet.setHeader("Connection", "keep-alive");
	        		  httpGet.setHeader("User-Agent", savedUserAgent);
	        		  if (savedAuthuser != null) httpGet.setHeader("Authorization", "Basic " + Base64.encodeToString((savedAuthuser+":"+savedAuthpass).getBytes(), Base64.NO_WRAP));	// Add user/pass for basic authentication
	        		  String androlocale=Locale.getDefault().getLanguage();	
	        		  if (! "".equals(androlocale)) httpGet.setHeader("Accept-Language", androlocale);
	        		  
	        		  HttpResponse execute = client.execute(httpGet);
	        		  InputStream content = execute.getEntity().getContent();
	
	        		  BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
	        		  String s = "";
	        		  while ((s = buffer.readLine()) != null) {
	        			  response.append(s);
	        		  }
	        	  }
	        	  catch (Exception e) 
	        	  {
	        		  e.printStackTrace();
	        	  }
	          }
          }
          
          return response.toString();
        }
        
        /**
         * When url has been downloaded
         * 
         * @param   String      content downloaded
         */
        @Override
        protected void onPostExecute(String result) 
        {
            String historyUrl = null;
            
            Log.i(LOG_TAG, "onPostExecute mode="+this.mode+" result="+result+" result.length="+result.length());
            if ("menu".equals(this.mode)) 
            {
                cacheForMenu=result;
                historyUrl=savedDolRootUrl+"core/get_menudiv.php?dol_hide_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile="+(DoliDroid.useJMobileAjax?'2':'1');
            }

            if (result != null && ! "".equals(result))
            {
                Log.d(LOG_TAG, "onPostExecute Load content from result of doInBackground "+this.mode+" savedDolBasedUrl="+savedDolBasedUrl+" historyUrl="+historyUrl);
                if (this.mode != null) nextAltHistoryStack=this.mode;       // TODO Do not add same history url twice
                myWebView.loadDataWithBaseURL(savedDolBasedUrl, result, "text/html", "UTF-8", historyUrl);
                //myWebView.loadData(result, "text/html", "UTF-8");
            }
        }        
    }   
        
    
    /**
     * Once we click onto SmartPhone hardware key
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) 
    {
        if (event.getAction() == KeyEvent.ACTION_DOWN) 
        {
            // Check if the key event was the Back button
            if ((keyCode == KeyEvent.KEYCODE_BACK)) 
            {
                return this.codeForBack();
            }
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    } 
   
    /**
     * Click onto text Back
     * 
     * @param View v
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void onClickToBack(View v) 
    {
        this.codeForBack();
    }  

    /**
     * Click onto text Menu
     * 
     * @param View v
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void onClickToMenu(View v) 
    {
        this.codeForMenu();
    }      

    /**
     * Click onto text Search
     * 
     * @param View v
     */
    @SuppressLint("SetJavaScriptEnabled")
    public void onClickToSearch(View v) 
    {
        this.codeForQuickAccess();
    } 
    
    
    /**
     * Common code for Menu
     * codeForMenu is in a UI thread
     * 
     * @return  boolean     true
     */
    private boolean codeForMenu() 
    {
        String urlToGo = ""; 
        
        urlToGo = this.savedDolRootUrl+"core/get_menudiv.php?dol_hide_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile="+(DoliDroid.useJMobileAjax?'2':'1');
        //if (DoliDroid.useJMobileAjax) urlToGo=urlToGo.concat("&dol_basehref="+Uri.encode(this.savedDolBasedUrl));

        // If not found into cache, call URL
        Log.d(LOG_TAG, "We called codeForMenu after click on Menu : savedDolBasedUrl="+this.savedDolBasedUrl+" urlToGo="+urlToGo);
        myWebView = (WebView) findViewById(R.id.webViewContent);

        if (this.cacheForMenu != null && this.cacheForMenu.length() > 0)
        {
            String historyUrl = urlToGo;
            Log.d(LOG_TAG, "Got content from app cache this.cacheForMenu savedDolBasedUrl="+this.savedDolBasedUrl+" historyUrl="+historyUrl);
            //altHistoryStack.add("menu");  // TODO Do not add same history url twice
            nextAltHistoryStack="menu";
            myWebView.loadDataWithBaseURL(this.savedDolBasedUrl, this.cacheForMenu, "text/html", "UTF-8", historyUrl);
        }
        else
        {
            DownloadWebPageTask task = new DownloadWebPageTask("menu");
            task.execute(new String[] { urlToGo });
        }
        return true;
    }

    /**
     * Common code for Quick Access
     * codeForQuickAccess is in a UI thread
     *
     * @return  boolean     true
     */
    private boolean codeForQuickAccess() 
    {
        String urlToGo = ""; 
        
        urlToGo = this.savedDolRootUrl+"core/search_page.php?dol_hide_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile="+(DoliDroid.useJMobileAjax?'2':'1');

        // If not found into cache, call URL
        Log.d(LOG_TAG, "We called codeForQuickAccess after click on Search : savedDolBasedUrl="+this.savedDolBasedUrl+" urlToGo="+urlToGo);
        myWebView = (WebView) findViewById(R.id.webViewContent);

        if (this.cacheForQuickAccess != null && this.cacheForQuickAccess.length() > 0)
        {
            String historyUrl = urlToGo;
            Log.d(LOG_TAG, "Got content from app cache this.cacheForQuickAccess savedDolBasedUrl="+this.savedDolBasedUrl+" historyUrl="+historyUrl);
            //altHistoryStack.add("quickaccess");   // TODO Do not add same history url twice
            nextAltHistoryStack="quickaccess";
            myWebView.loadDataWithBaseURL(this.savedDolBasedUrl, this.cacheForQuickAccess, "text/html", "UTF-8", historyUrl);
        
            return true;
        }

        DownloadWebPageTask task = new DownloadWebPageTask("quickaccess");
        task.execute(new String[] { urlToGo });

        return true;
    }

    /**
     * Common code for Back
     * codeForBack is in a UI thread
     * 
     * @return  boolean             True
     */
    private boolean codeForBack() 
    {
        // Check if there is history
        String currentUrl = "";
        String previousUrl = "";
        myWebView = (WebView) findViewById(R.id.webViewContent);
        boolean b = myWebView.canGoBack();
        currentUrl = myWebView.getUrl();

        WebBackForwardList mWebBackForwardList = myWebView.copyBackForwardList();
        if (mWebBackForwardList.getCurrentIndex() > 0) 
            previousUrl = mWebBackForwardList.getItemAtIndex(mWebBackForwardList.getCurrentIndex()-1).getUrl();
        
        Log.d(LOG_TAG, "We called codeForBack. Can go back is "+b+", current URL is "+currentUrl+", previous URL is "+previousUrl);
        if (b) 
        {
            if (previousUrl.equals(savedDolBasedUrl+"/") || previousUrl.contains("&ui-page="))
            {
                Log.d(LOG_TAG, "Previous Url is a page with an history problem");
                if (currentUrl.contains("&ui-page="))
                {
                    if (altHistoryStack.size() > 0) // Should be true
                    {
                        nextAltHistoryStack=altHistoryStack.get(altHistoryStack.size() - 1);
                        Log.d(LOG_TAG, "Current page has &ui-page, we set nextAltHistoryStack to "+nextAltHistoryStack+" and consume the history stack");
                        altHistoryStack.remove(altHistoryStack.size() - 1);
                    }
                }
                else
                {
                    if (altHistoryStack.size() > 0) // Should be true
                    {
                        // later shouldInterceptRequest will take lasentry of history to knwo which cache to use
                        //nextAltHistoryStack=altHistoryStack.get(altHistoryStack.size() - 1);
                        Log.d(LOG_TAG, "We do nothing, we let shouldInterceptRequest consume and pop the history stack"); 
                        //altHistoryStack.remove(altHistoryStack.size() - 1);
                    }
                }
            }
            else
            {
                Log.d(LOG_TAG, "We clear nextAltHistoryStack"); 
                nextAltHistoryStack="";
            }
            Log.d(LOG_TAG, "We clear nextAltHistoryStackBis"); 
            nextAltHistoryStackBis="";
            myWebView.goBack(); // This will call shouldInterceptRequest
        }
        else
        {
            if (! this.noPreviousPageShown)
            {
                Toast.makeText(activity, getString(R.string.NoPreviousPageAgainToQuit), Toast.LENGTH_SHORT).show();
                this.noPreviousPageShown=true;
            }
            else
            {
                Log.d(LOG_TAG, "Second click on Previous when no previous available.");
                Log.i(LOG_TAG, "We finish activity resultCode="+RESULT_LOGOUT);
                this.noPreviousPageShown=false;
                setResult(RESULT_LOGOUT);   // We don't want to quit completely
                WebViewDatabase.getInstance(getBaseContext()).clearHttpAuthUsernamePassword();
                finish();
            }           
        }
        return true;
    }

    /**
     * Common code for MultiCompany
     * codeForMultiCompany is in a UI thread
     *
     * @return  boolean     true
     */
    private boolean codeForMultiCompany()
    {
        myWebView = (WebView) findViewById(R.id.webViewContent);

        String urlToGo = myWebView.getOriginalUrl();
        if (urlToGo.contains("?")) urlToGo=urlToGo+"&";
        else urlToGo=urlToGo+"?";
        urlToGo += "switchentityautoopen=1&dol_invisible_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile="+(DoliDroid.useJMobileAjax?'2':'1');

        // If not found into cache, call URL
        Log.d(LOG_TAG, "We called codeForMultiCompany after click on MultiCompany : savedDolBasedUrl="+this.savedDolBasedUrl+" urlToGo="+urlToGo);

        myWebView.loadUrl(urlToGo);

        return true;
    }


    /**
     * Dump content of backforward webview list
     */
    public void dumpBackForwardList(WebView myWebView) 
    {
        // FOR DEBUG ONLY
        /*
        try {
            WebBackForwardList mWebBackForwardList = myWebView.copyBackForwardList();
            int nbelem=mWebBackForwardList.getSize();
            if (nbelem > 0)
            {
                for (int i=0; i < nbelem; i++)
                {
                    Log.v(LOG_TAG, "BackForward i="+i+(mWebBackForwardList.getCurrentIndex()==i?"*":"")+" url="+mWebBackForwardList.getItemAtIndex(i).getUrl());
                }
            }
            
            for (int i=0; i < altHistoryStack.size(); i++)
            {
                Log.v(LOG_TAG, "altHistoryStack i="+i+" "+altHistoryStack.get(i));
            }
            Log.v(LOG_TAG, "nextAltHistoryStack="+nextAltHistoryStack);
            Log.v(LOG_TAG, "nextAltHistoryStackBis="+nextAltHistoryStackBis);
        }
        catch(Exception e)
        {
            Log.e(LOG_TAG, "Error in dumpBackForwardList "+e.getMessage());
        }
        */
    }

    /**
     * Method to generate an InApp purchase
     * 
     * @param View  v       view
     */
    /*
    public void buyMethod(View v) {
        iabHelper.launchPurchaseFlow(this, ITEM_SKU, REQUEST_CODE, mPurchaseFinishedListener, "");
    }*/
    /**
     * Listener for InApp purchase result
     */
    /*
    IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
        public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
            if (result.isFailure()) {
                Log.d(LOG_TAG, "Purchase failed");
                return;
            }
            else if (purchase.getSku().equals(ITEM_SKU)) {
                Log.d(LOG_TAG, "Purchase of item "+ITEM_SKU+" ok");
                iabHelper.queryInventoryAsync(mReceivedInventoryListener);
            }
        }
    };
    */
    /**
     * Listener for InApp purchase inventory result
     */
    /*
    IabHelper.QueryInventoryFinishedListener mReceivedInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
        @Override
        public void onQueryInventoryFinished(IabResult result, Inventory inventory) {
            if (result.isFailure()) {
                Log.d(LOG_TAG, "Inventory failed");
            }
            else {
                iabHelper.consumeAsync(inventory.getPurchase(ITEM_SKU), mConsumeFinishedListener);
                Log.d(LOG_TAG, "Consume is launched. Waiting for listener.");
            }
        }
    };
    */
    /**
     * Listener for InApp purchase consume result
     */
    /*
    IabHelper.OnConsumeFinishedListener mConsumeFinishedListener = new IabHelper.OnConsumeFinishedListener() {
        public void onConsumeFinished(Purchase purchase, IabResult result) {
            if (result.isSuccess()) {
                Log.d(LOG_TAG, "Consume of item ok");
            }
            else
            {
                Log.d(LOG_TAG, "Consume of item failed");
            }
        }
    };
    */


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d(LOG_TAG, "onRequestPermissionsResult override");
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the task we need to do.
                    putDownloadInQueue(saveQueryForonRequestPermissionsResult, saveUrlForonRequestPermissionsResult, saveListOfCookiesForonRequestPermissionsResult);
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d(LOG_TAG, "Sorry, permission was not granted by user to do so.");
                }
                return;
            }
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);

                // other 'case' lines to check for other
                // permissions this app might request
        }
    }


    public boolean putDownloadInQueue(String query, String url, String listOfCookies)
    {
        Log.d(LOG_TAG, "putDownloadInQueue url to download = " + url);
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription(query);
        request.setTitle(query);
        request.addRequestHeader("Cookie", listOfCookies);
        if (savedAuthuser != null) request.addRequestHeader("Authorization", "Basic " + Base64.encodeToString((savedAuthuser+":"+savedAuthpass).getBytes(), Base64.NO_WRAP));   // Add user/pass for basic authentication
        //request.setVisibleInDownloadsUi(isVisible);
        request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        request.setAllowedOverRoaming(true);
        request.setVisibleInDownloadsUi(true);
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        Log.d(LOG_TAG, "putDownloadInQueue Set output dirType=" + Environment.DIRECTORY_DOWNLOADS + " subPath="+query);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, query);
        //request.setDestinationInExternalFilesDir(getApplicationContext(), null, query);

        // Get download service and enqueue file
        // Complete tutorial on download manager on http://www.101apps.co.za/index.php/articles/using-the-downloadmanager-to-manage-your-downloads.html
        DownloadManager dmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long id = dmanager.enqueue(request);

        // Save the request id
            /*
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putLong(strPref_Download_ID, id);
            editor.commit();
            */
        return true;
    }



    /* ************************* */
    /* SUB-CLASSES WEBVIEW       */
    /* ************************* */ 
    
    /**
     * WebViewClientDoliDroid
     * 
     * Sequence of trigger called when we do myWebView.loadUrl(url):
     * 0) onPageStarted is called when we need to load a page (in cache or not, but not called by Ajax JMobile)
     * 0) shouldInterceptRequest is called for all HTTP requests of pages and images (.php, .css, .js, .png, but not called when cache is used)
     * 1) shouldOverrideUrlLoading is called for HTTP requests of pages only (.php, .css, .js, but not called when cache is used). Also note that a redirect triggers this method but not if redirect is done with javascript window.location. 
     * 2) onLoadResource is called for all HTTP requests, even Ajax (but not called when cache is used)
     * 3) onReceivedError or onReceivedSslError
     * 4) onPageFinished is called when page with its resources are loaded (in cache or not, even for call by Ajax JMobile)
     */
	class WebViewClientDoliDroid extends WebViewClient
	{
		int counthttpauth=0;

		private SecondActivity secondActivity;
		private String webViewtitle="";
		private final String jsInjectCodeForLoginSubmit = 
		        "function dolidroidParseFormAfterSubmit(event) {" +
		        "    var form = this;" +
		        "    if (this.tagName.toLowerCase() != 'form') form = this.form;" +    
		        "    var data = '';" +
		        "    if (!form.method)  form.method = 'get';" +
		        "    data += 'method=' + form.method;" +
		        "    data += '&action=' + form.action;" +        
		        "    var inputs = document.forms[0].getElementsByTagName('input');" +
		        "    for (var i = 0; i < inputs.length; i++) {" +
		        "         var field = inputs[i];" +
		        "         if (field.type != 'submit' && field.type != 'reset' && field.type != 'button')" +
		        "             data += '&' + field.name + '=' + field.value;" +
		        "    }" +
		        "    HTMLOUT.functionJavaCalledByJsProcessFormSubmit(data);" +
		        "}" +
		        "" +
		        "for (var form_idx = 0; form_idx < document.forms.length; ++form_idx) {" +
		        "    document.forms[form_idx].addEventListener('submit', dolidroidParseFormAfterSubmit, false);" +
		        "}" +
		        "var inputs = document.getElementsByTagName('input'); " +
		        "for (var i = 0; i < inputs.length; i++) {" +
		        "    if (inputs[i].getAttribute('type') == 'button')" +
		        "        inputs[i].addEventListener('click', dolidroidParseFormAfterSubmit, false);" +
		        "}";		
				
		public WebViewClientDoliDroid(SecondActivity secondActivity)
		{
			this.secondActivity = secondActivity;
		}
		
		/**
		 * onPageStarted
		 * 
		 * @param view		View
		 * @param url		URL
		 * @param favicon	Favicon
		 */
		@Override  
		public void onPageStarted(WebView view, String url, Bitmap favicon)
		{  
		    Log.d(LOG_TAG, "onPageStarted url="+url+" originalUrl="+view.getOriginalUrl()+" view.getUrl="+view.getUrl()+" savedDolBasedUrl="+savedDolBasedUrl);
			String urltotest = view.getOriginalUrl();
			if (urltotest == null) urltotest = view.getUrl();

		    if (urltotest.startsWith("http:") && urltotest.startsWith(savedDolBasedUrl)) {
				//Log.d(LOG_TAG, "https:" + view.getUrl().substring(5));
				//Log.d(LOG_TAG, url);
				if (("https:" + urltotest.substring(5)).equals(url)) {
					Log.w(LOG_TAG, "onPageStarted value of url is value of view.getUrl with a s added, we change the savedDolRootUrl");
					//Toast.makeText(activity, "Warning: It seems your server forced a redirect to HTTPS page. Please check your connection URL and use the https directly if you can.", Toast.LENGTH_SHORT).show();
					//savedDolBasedUrl = "http://"+savedDolBasedUrl.substring(4);

					// Use Dialog instead of Toast for a longer message
					AlertDialog alertDialog = new AlertDialog.Builder(activity).create();
					alertDialog.setTitle(getString(R.string.Warning));
					alertDialog.setMessage(getString(R.string.AlertHTTPS));
					alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							});
					alertDialog.show();
				}
			}
		    //super.onPageStarted(view, url, favicon);
		}


		/**
		 * Return if we must intercept HTTP Request for pages (not called when cache is used)
		 * This method is called into a non-UI Thread (Android >= 3.0) so UI Thread function are not allowed.
		 * 
		 * @param 	WebView		view
		 * @param 	String		url			For example "http://192.168.0.1/xxx" or "data:image/png;base64,..."
		 * @return	boolean					True or false if we must send request or not
		 */
		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view, String url)
		//public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest url)	from API21
		{
			// Get url relative to Dolibarr root.
			String host=null;
			String fileName=null;
			String version=null;
			try {
				if (url != null && ! url.startsWith("data:"))
				{
					Uri uri=Uri.parse(url);
					version=uri.getQueryParameter("version");
					if (version != null)
					{
						String[] split = version.split("\\.");
						version=split[0]+"."+split[1];
					}
					fileName=uri.getPath();	// Return relative path of page (withtout params)
					host=uri.getHost();
				}
				
				// Format fileName to have a relative URL from root
				if (fileName != null)
				{
					if (this.secondActivity.savedDolRootUrlRel.equals("/")) fileName=fileName.replaceFirst(this.secondActivity.savedDolRootUrlRel, "");
					else fileName=fileName.replaceFirst(this.secondActivity.savedDolRootUrlRel, "");
					if (fileName.startsWith("/")) fileName=fileName.substring(1);
				}
			}
			catch(Exception e)
			{
				Log.e(LOG_TAG, "shouldInterceptRequest Error into getting fileName or host");
				fileName=null;
			}
				
			
			Log.v(LOG_TAG, "shouldInterceptRequest url="+url+", host="+host+", fileName="+fileName+", savedDolBasedUrl="+savedDolBasedUrl+" version in url param (for js or css pages)="+version);

			if ("document.php".equals(fileName) && url != null && ! url.startsWith(savedDolBasedUrl) && url.startsWith(savedDolBasedUrlWithSForced)) {
				Log.w(LOG_TAG, "Bad savedDolBasedUrl that does not allow download");
				// Can't make interaction here
				//Toast.makeText(activity, R.string.AlertDownloadBadHTTPS, Toast.LENGTH_LONG).show();
			}

			if (fileName != null && url.startsWith(savedDolBasedUrl))
			{
				if (prefAlwaysUseLocalResources) {
					try {
						/* Example to return empty for files instead of some files */
						/*
						if (fileName.endsWith("datepicker.js.php")
								|| fileName.contains("viewimage.php") || fileName.contains("ajax-loader.gif") || fileName.contains("headbg2.jpg") || fileName.contains("lib_head.js")
								|| fileName.contains("js/dst.js") || fileName.contains("tipTip")|| fileName.contains("tablednd")|| fileName.contains("jnotify")|| fileName.contains("flot.")
								|| fileName.contains("jquery.mobile")
								//|| fileName.contains("jquery-latest")
								//|| fileName.contains("jquery-ui-latest")
								|| fileName.contains("button_")
								|| fileName.contains("dolibarr_logo.png")
								 )
						{
							Log.i(LOG_TAG, "Filename "+fileName+" discarded");
							return new WebResourceResponse("text/css", "UTF-8", new ByteArrayInputStream("".getBytes("UTF-8")));
						}*/

						String versionimg = VERSION_RESOURCES;                         // Set to the default value we want to use. Set "" to disable assets usage for img.
						//if (lastversionfoundforasset != null) versionimg = lastversionfoundforasset;
						String versionjscss = (version == null ? "" : version);        // Set to "" to disable assets usage for js and css

						// Check if file need to be replaced by an asset file (if open file fails, throw exception and load from web).
						if ((fileName.endsWith("favicon.ico") || fileName.startsWith("theme/") || fileName.startsWith("includes/") || fileName.startsWith("public/demo/"))) {
							if (!versionimg.equals("") && (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".gif") || fileName.endsWith(".ico"))) {
								Log.d(LOG_TAG, "shouldInterceptRequest Filename " + fileName + " intercepted. Replaced with image assets file into " + versionimg);
								return new WebResourceResponse(null, null, getAssets().open(versionimg + "/" + fileName));
							} else if (!versionjscss.equals("") && fileName.endsWith(".js")) {
								Log.d(LOG_TAG, "shouldInterceptRequest Filename " + fileName + " intercepted. Replaced with js assets file into " + versionjscss);
								return new WebResourceResponse("application/x-javascript", "UTF-8", getAssets().open(versionjscss + "/" + fileName));
							} else if (!versionjscss.equals("") && fileName.endsWith(".css")) {
								Log.d(LOG_TAG, "shouldInterceptRequest Filename " + fileName + " intercepted. Replaced with css assets file into " + versionjscss);
								return new WebResourceResponse("text/css", "UTF-8", getAssets().open(versionjscss + "/" + fileName));
							}
						} else if (fileName.startsWith("&ui-page=") || fileName.equals("")) {
							Log.d(LOG_TAG, "shouldInterceptRequest We make a back to go to a bad history url fileName=" + fileName);

							// Return last page that fails found into altHistoryStack
							if (altHistoryStack != null && altHistoryStack.size() > 0) {
								String lastelem = altHistoryStack.get(altHistoryStack.size() - 1);

								if ("menu".equals(lastelem) && cacheForMenu != null) {
									nextAltHistoryStack = "menu";
									altHistoryStack.remove(altHistoryStack.size() - 1);
									Log.d(LOG_TAG, "shouldInterceptRequest Return instead content of cacheForMenu");
									return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(cacheForMenu.getBytes("UTF-8")));
								}
								if ("quickaccess".equals(lastelem) && cacheForQuickAccess != null) {
									nextAltHistoryStack = "quickaccess";
									altHistoryStack.remove(altHistoryStack.size() - 1);
									Log.d(LOG_TAG, "shouldInterceptRequest Return instead content of cacheQuickAccess");
									return new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(cacheForQuickAccess.getBytes("UTF-8")));
								}
							}

							Log.w(LOG_TAG, "shouldInterceptRequest Nothing to return instead");
						}

					} catch (IOException e) {
						Log.w(LOG_TAG, "shouldInterceptRequest Filename " + fileName + " intercepted but failed to find/open it from assets, we do standard process (so use cache of webview browser if not expired or download).");
					}
				}
				else
				{
					Log.v(LOG_TAG, "shouldInterceptRequest option is off");
				}
			}
			
			return super.shouldInterceptRequest(view, url);
		}

		/**
		 * Handler to manage downloads
		 * 
		 * @param	WebView		view		Web view
		 * @param	String		url			Url
		 * @return	boolean					True to mean URL has been handled by code, False to ask webview to handle it.
		 */
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url)
		{
			Log.d(LOG_TAG, "shouldOverrideUrlLoading url="+url+" originalUrl="+view.getOriginalUrl()+" savedDolRootUrl="+savedDolRootUrl);

			// TODO Optimize performance by disabling loading of some url (ie: jquery plugin tipTip)
			
			if (url.startsWith("tel:")) {  // Intercept phone urls
				Log.d(LOG_TAG, "Launch dialer : " + url);
				Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
				startActivity(intent);
				return true;
			}
			else if (url.startsWith("geo:")) {  // Intercept geoloc url (map)
				Log.d(LOG_TAG, "Launch geo : " + url);
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
				startActivity(intent);
				return true;
			}
			else if (url.startsWith("mailto:")) {  // Intercept mailto urls
				Log.d(LOG_TAG, "Launch mailto : " + url);
				try {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND, Uri.parse(url));
                    emailIntent.setType("message/rfc822");
                    String recipient = url.substring( url.indexOf(":")+1 );
                    emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{recipient});
                    //emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, mContext.getString(R.string.email_subject));
                    //emailIntent.putExtra(android.content.Intent.EXTRA_TEXT, mContext.getString(R.string.email_message, " "));
                    startActivity(Intent.createChooser(emailIntent, "Send mail..."));
                }
                catch (Exception ex) {}
				return true;
			}
			else if (! url.startsWith(savedDolBasedUrl)) {	// This is an external url
				// Open in Chrome
				Log.d(LOG_TAG, "Launch external url : " + url);
				return false;
			}

			// Without this, we got "download not supported" ("telechargement non pris en charge")
			if (((url.endsWith(".pdf") || url.endsWith(".odt") || url.endsWith(".ods")) && ! url.contains("action=")) 	// Old way to detect a download (we do not make a download of link to delete or print or presend a file)
					|| url.startsWith(savedDolRootUrl+"document.php?")													// The default wrapper to download files
					|| url.contains("output=file"))																		// The new recommended parameter for files like export.php that generate file output
	        {
				String query=Uri.parse(url).getQuery().replaceAll(".*file=", "").replaceAll("&.*", "").replaceAll(".*/", "");
				Log.d(LOG_TAG, "shouldOverrideUrlLoading Start activity to download file="+query);
				/*Pattern p = Pattern.compile("file=(.*)\.(pdf|odt|ods)");
				Matcher m = p.matcher(urlquery);
				if (m.find()) {
				  url = m.group(1);  // The matched substring
				}*/

				if (! url.contains("dol_hide_topmenu=")) url = url + (url.contains("?")?"&":"?") + "dol_hide_topmenu=1";
    			if (! url.contains("dol_hide_leftmenu=")) url = url + (url.contains("?")?"&":"?") + "dol_hide_leftmenu=1";
    			if (! url.contains("dol_optimize_smallscreen=")) url = url + (url.contains("?")?"&":"?") + "dol_optimize_smallscreen=1";
    			if (! url.contains("dol_no_mouse_hover=")) url = url + (url.contains("?")?"&":"?") + "dol_no_mouse_hover=1";
    			if (! url.contains("dol_use_jmobile=")) url = url + (url.contains("?")?"&":"?") + "dol_use_jmobile=0";
    			
				String listOfCookies=this.listCookies();

				// This call url and save content into a file
				try {
					saveQueryForonRequestPermissionsResult = query;
					saveUrlForonRequestPermissionsResult = url;
					saveListOfCookiesForonRequestPermissionsResult = listOfCookies;

					// Test if Build.VERSION.SDK_INT (the version that run application is API level 23 or +)
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
					{
						// If API 23 or+, we ask permission to user
						int hasWriteContactsPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
						if (hasWriteContactsPermission != PackageManager.PERMISSION_GRANTED) {
							requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
									REQUEST_CODE_ASK_PERMISSIONS_WRITE_EXTERNAL_STORAGE);
							return false;
						}
					}

					Log.d(LOG_TAG, "shouldOverrideUrlLoading url to download = " + url);

					putDownloadInQueue(query, url, listOfCookies);
/*
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.setDescription(query);
                    request.setTitle(query);
                    request.addRequestHeader("Cookie", listOfCookies);
                    if (savedAuthuser != null) request.addRequestHeader("Authorization", "Basic " + Base64.encodeToString((savedAuthuser+":"+savedAuthpass).getBytes(), Base64.NO_WRAP));   // Add user/pass for basic authentication
                    //request.setVisibleInDownloadsUi(isVisible);
                    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
                    request.setAllowedOverRoaming(true);
                    request.setVisibleInDownloadsUi(true);
                    request.allowScanningByMediaScanner();
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);

                    Log.d(LOG_TAG, "shouldOverrideUrlLoading Set output dirType=" + Environment.DIRECTORY_DOWNLOADS + " subPath="+query);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, query);
                    //request.setDestinationInExternalFilesDir(getApplicationContext(), null, query);
                    
                    // Get download service and enqueue file
                    // Complete tutorial on download manager on http://www.101apps.co.za/index.php/articles/using-the-downloadmanager-to-manage-your-downloads.html
                    DownloadManager dmanager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    long id = dmanager.enqueue(request);
*/
					// Save the request id
					/*
					SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					SharedPreferences.Editor editor = sharedPrefs.edit();
					editor.putLong(strPref_Download_ID, id);
					editor.commit();
					*/

					Log.d(LOG_TAG, "shouldOverrideUrlLoading URI has been added in queue");
				}
				catch(IllegalArgumentException ie)
				{
					Log.e(LOG_TAG, ie.getMessage());
					Toast.makeText(activity, ie.getMessage(), Toast.LENGTH_LONG).show();
				}
				catch(Exception e)
				{
					Log.e(LOG_TAG, e.getMessage());
					Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
				}

				return true;
	        }

			return false;
		}
		
		/**
		 * Called when a HTTP request is done (not called when cache is used)
		 * 
		 * @param	WebView		view		Web view
		 * @param	String		url			Url
		 */
		@Override
		public void onLoadResource(WebView view, String url)
		{
			Log.v(LOG_TAG, "onLoadResource url="+url+" originalUrl="+view.getOriginalUrl());
			//super.onLoadResource(view, url);
		}

		/**
		 * onPageFinished: Execute code just after a page was completely loaded
		 * Note: When using jmobile ajax cache, this method may be called before or after JMobile has run, so before or after any onConsoleMessage of JMobile error.
		 */
		@Override
	    public void onPageFinished(WebView view, String url)
	    {
			// super.onPageFinished(view, url);
			
			if (listOfCookiesAfterLogon == null)
			{
				Log.d(LOG_TAG, "Save session cookies for the download manager into var listOfCookiesAfterLogon");
				listOfCookiesAfterLogon=this.listCookies();	// Save cookie for
			}

			myWebView = (WebView) findViewById(R.id.webViewContent);
			boolean b = myWebView.canGoBack();
			
			Log.d(LOG_TAG, "onPageFinished url="+url+" canGoBack="+b);
	        
		    if (tagToShowInterruptMessage.length() > 0 && tagToShowInterruptCounter > 0)	//onConsoleMessage is increased by onConsoleMessage function (javascript error)
			{
		    	// We should not go here. This manage loading errors when JMobile was not implemented correctly (no data-role defined into page). 
		    	String myErrorNum="JSERR001";
		    	String myMessage0="Error "+myErrorNum;
		    	String myMessage1="An error loading page was detected into some javascript part of your page.";
		    	String myMessage1b="This is commonly a problem on your server side, not into DoliDroid.";
		    	String myMessage2="DoliDroid will not work corrrectly until you solve this.";
		    	String myMessage3="-If you have installed external modules, try after disabling them (some external modules introduce bugs)";
		    	String myMessage4="-Try also to call login page from a standard PC by adding &dol_use_jmobile=1 as parameter of URL and check you don't have any javascript errors.";
		    	//String myMessage5="Go on <a href=\"http://wiki.dolibarr.org/index.php/Application_Android_-_DoliDroid\">here for online help to solve this</a>.";
		    	String myMessage5="For online help to solve this, go on page:<br><br>\n<strong><font size=\"-1\">http://wiki.dolibarr.org/index.php/Application_Android_-_DoliDroid#"+myErrorNum+"</font></strong>";
		    	String myMessage6="Note: Page you want to load is:<br><br>\n<font size=\"-1\">"+url+"</font>";
				Log.d(LOG_TAG, "Show user message "+myMessage0+"\n"+myMessage1+"\n"+myMessage1b+"\n"+myMessage2+"\n"+myMessage3+"\n"+myMessage4);

				tagToShowInterruptMessage="";
				
				myWebView.loadData(myMessage0+"<br><br>\n"+myMessage1+"<br>\n"+myMessage1b+"<br>\n"+myMessage2+"<br>\n<br>\n"+myMessage5+"<br><br><br>\n"+myMessage6, "text/html", "UTF-8");
			}
		    else if (tagToShowMessage.length() > 0 && tagToShowCounter > 0)	// tagToShowCounter is increased by onConsoleMessage function (javascript error)
			{
				Toast.makeText(activity, tagToShowMessage, Toast.LENGTH_LONG).show();

				tagToShowMessage="";
			}
			else
			{
				tagToShowInterruptCounter=0;		// Page was loaded, so we set count of number of try to 0

				// If we loaded page login.php, we check Dolibarr version
				// Title for login page is defined into login.tpl.php (with some part into dol_loginfunction in security2.lib.php)
				this.webViewtitle = myWebView.getTitle();
				if (this.webViewtitle != null)
				{
					Matcher m = patternLoginHomePageForVersion.matcher(this.webViewtitle);
				    Boolean foundVersion = m.find();
					if (foundVersion)	// if title ends with " Dolibarr x.y.z" or " Dolibarr x.y.z - multicompany or anytext from module hook setTitleHtml", this is login page or home page
				    {
				    	lastversionfound=m.group(1) + ", " + m.group(2) + ", " + m.group(3);
						lastversionfoundforasset=m.group(1) + "." + m.group(2);
						Log.i(LOG_TAG, "Title of page is: "+this.webViewtitle+" - url="+url+" - Found login or home page + version: " + lastversionfound+" - Suggest to use asset: "+lastversionfoundforasset);

                        Matcher multicompanyRegex = patternLoginHomePageForMulticompany.matcher(this.webViewtitle);
                        isMulticompanyOn = multicompanyRegex.find();
                        //isMulticompanyOn=true;
                        if (isMulticompanyOn)
                        {
                            savMenu.findItem(R.id.menu_multicompany).setVisible(true);
                            Log.d(LOG_TAG, "Module multicompany was found");
                        }
                        else
						{
							savMenu.findItem(R.id.menu_multicompany).setVisible(false);
							Log.d(LOG_TAG, "Module multicompany was NOT found");
						}
					}
				    
				    if (patternLoginPage.matcher(this.webViewtitle).find() || patternLoginPage2.matcher(this.webViewtitle).find())	// if title ends with "Login Dolixxx x.y.z", this is login page or home page
				    {
				    	if (url.equals(savedDolBasedUrl+"/"))
				    	{
							Log.w(LOG_TAG, "We ignore page since url is not a specific page");
				    	}
				    	else
				    	{
							synchronized (this) 
							{
								boolean versionOk=true;	// Will be false if Dolibarr is < 3.6.*
								if (foundVersion) {
									try {
										if (Integer.parseInt(m.group(1)) < 3) versionOk = false;
										if (Integer.parseInt(m.group(1)) < 3 && Integer.parseInt(m.group(2)) < 6)
											versionOk = false;
									}
									catch(Exception e)
									{
										versionOk = false;
									}
								}
								else {
									versionOk = false;
								}
								if (versionOk) Log.d(LOG_TAG, "Dolidroid is compatible with your Dolibarr "+lastversionfound);
								else 
								{
									Log.w(LOG_TAG, "Dolidroid is NOT compatible with your Dolibarr "+lastversionfound);
									final Toast aToast = Toast.makeText(activity, getString(R.string.notCompatibleWithVersion, (lastversionfound == null ? this.webViewtitle : lastversionfound), "3.4"), Toast.LENGTH_SHORT);
									new CountDownTimer(5000, 1000)	// 5 seconds
									{
									    public void onTick(long millisUntilFinished) {aToast.show();}
									    public void onFinish() {aToast.show();}
									}.start();								
								}
	
								String jsInjectCodeForSetForm="";
								if (tagToOverwriteLoginPass)	// If we are allowed to overwrite username/pass into fields
								{
							    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
							    	//SharedPreferences sharedPrefs = this.secondActivity.getSharedPreferences(FILENAME_INST_PARAM, Context.MODE_PRIVATE);
									String username=sharedPrefs.getString(savedDolRootUrl+"-username", "");
									String password=sharedPrefs.getString(savedDolRootUrl+"-password", "");
									if ((username != null && ! "".equals(username)) || (password != null && ! "".equals(password)))
									{
										tagToOverwriteLoginPass=false;
										Log.d(LOG_TAG, "Prepare js to autofill login form with username="+username+" password="+password.replaceAll(".", "*"));
										// This call inject JavaScript into the page which just finished loading.
										if (username != null && ! "".equals(username)) jsInjectCodeForSetForm+="document.getElementById('username').value='"+username+"';";	// Warning: This line makes Webkit fails with 2.3
										if (password != null && ! "".equals(password)) jsInjectCodeForSetForm+="document.getElementById('password').value='"+password+"';";	// Warning: This line makes Webkit fails with 2.3
									}
									else Log.d(LOG_TAG, "No predefined login/pass to autofill login form");
								}
								else Log.d(LOG_TAG, "Do not autofill login form with login/pass. tagToOverwriteLoginPass is false.");

								// Force inject value of mobile parameters. This is required when session expired and login is available
								jsInjectCodeForSetForm+="document.getElementById('dol_hide_topmenu').value='1';";	// Warning: This line makes Webkit fails with 2.3
								jsInjectCodeForSetForm+="document.getElementById('dol_hide_leftmenu').value='1';";	// Warning: This line makes Webkit fails with 2.3
								jsInjectCodeForSetForm+="document.getElementById('dol_optimize_smallscreen').value='1';";	// Warning: This line makes Webkit fails with 2.3
								jsInjectCodeForSetForm+="document.getElementById('dol_no_mouse_hover').value='1';";	// Warning: This line makes Webkit fails with 2.3
								jsInjectCodeForSetForm+="document.getElementById('dol_use_jmobile').value='"+(DoliDroid.useJMobileAjax?"2":"1")+"';";	// Warning: This line makes Webkit fails with 2.3
								jsInjectCodeForSetForm+=jsInjectCodeForLoginSubmit;
								// Now inject js to catch submission of login
								Log.d(LOG_TAG, "Inject js into page (to autofill form if allowed, to hook the submit of form, to fill submit params)");
								view.loadUrl("javascript:(function() { " + jsInjectCodeForSetForm + " })()");
							}
				    	}
					}
				    else	// This is not login page
				    {
				    	//Log.d(LOG_TAG, "Title of page is: "+myWebView.getTitle()+" - Login tag or Version not found");
				    	if (tagLastLoginPassToSavedLoginPass)
				    	{
				    		Log.i(LOG_TAG, "We have just received a page that is not Login page after submitting login form.");
				    		tagLastLoginPassToSavedLoginPass=false;
					    	SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
					    	//SharedPreferences sharedPrefs = this.secondActivity.getSharedPreferences(FILENAME_INST_PARAM, Context.MODE_PRIVATE);
					    	boolean prefAlwaysAutoFill = sharedPrefs.getBoolean("prefAlwaysAutoFill", true);
					    	if (prefAlwaysAutoFill)
					    	{
						    	Log.d(LOG_TAG, "We save form fields (prefAlwaysAutoFill is true).");
						    	String username=sharedPrefs.getString("lastsubmit-username", "");
								String password=sharedPrefs.getString("lastsubmit-password", "");
								if ((username != null && ! "".equals(username)) || (password != null && ! "".equals(password)))
								{
									SharedPreferences.Editor editor = sharedPrefs.edit();
									Log.d(LOG_TAG,"Save "+savedDolRootUrl+"-username="+username);
									editor.putString(savedDolRootUrl+"-username", username);
									Log.d(LOG_TAG,"Save "+savedDolRootUrl+"-password="+password);
									editor.putString(savedDolRootUrl+"-password", password);
									editor.apply();
								}
					    	}
					    	else Log.d(LOG_TAG, "We don't save form fields (prefAlwaysAutoFill is false).");
							tagToOverwriteLoginPass=prefAlwaysAutoFill;
								
				    		// Clear webview history
							Log.d(LOG_TAG,"We clear history of webview");
				    		myWebView.clearHistory();
				    	}
				    }
				}

				// If we loaded page logout.php, we finished activity
				if (url.contains("logout.php"))
				{
					synchronized (this) 
					{
						if (tagToLogout)
						{
							Log.d(LOG_TAG, "End of logout page, tagToLogout="+tagToLogout);
							tagToLogout=false;	// Set to false to avoid infinite loop
							tagToOverwriteLoginPass=true;
							Log.i(LOG_TAG, "We finish activity resultCode="+RESULT_LOGOUT);
							setResult(RESULT_LOGOUT);
					    	WebViewDatabase.getInstance(getBaseContext()).clearHttpAuthUsernamePassword();
							finish();	// End activity
						}
					}
				}

				// If we loaded page get_menudiv.php, we trigger code to save content into cache
				/*
				if (url.contains("get_menudiv.php")) 
				{
					synchronized (this) 
					{
						if (tagToGetCacheForMenu)
						{
							Log.d(LOG_TAG, "Inject js to get html content, tagToGetCacheMenu="+tagToGetCacheForMenu);
							tagToGetCacheForMenu=false;	// Set to false to avoid infinite loop
					        // This call inject JavaScript into the page which just finished loading.
							view.loadUrl("javascript:window.HTMLOUT.functionJavaCalledByJsProcessHTML('menu','<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");	// Warning: This line makes Webkit fails with 2.3, that's why there is a version check before
						}
					}
				}
				// If we loaded page search_page.php, we trigger code to save content into cache
				if (url.contains("search_page.php")) 
				{
					synchronized (this) 
					{
						if (tagToGetCacheForQuickAccess)
						{
							Log.d(LOG_TAG, "Inject js to get html content, tagToGetCacheQuickAccess="+tagToGetCacheForQuickAccess);
							tagToGetCacheForQuickAccess=false;	// Set to false to avoid infinite loop
					        // This call inject JavaScript into the page which just finished loading.
							view.loadUrl("javascript:window.HTMLOUT.functionJavaCalledByJsProcessHTML('quickaccess','<head>'+document.getElementsByTagName('html')[0].innerHTML+'</head>');");	// Warning: This line makes Webkit fails with 2.3, that's why there is a version check before
						}
					}
				}
				*/

				if (! "".equals(nextAltHistoryStackBis))
				{
					Log.d(LOG_TAG, "We add an entry into history stack because nextAltHistoryStackBis="+nextAltHistoryStackBis);
					altHistoryStack.add(nextAltHistoryStackBis);
					nextAltHistoryStackBis="";
					if (url.contains("&ui-page=")) nextAltHistoryStack="menu";
				}
				
				if (url.equals(savedDolBasedUrl+"/") || url.contains("&ui-page="))
				{
					Log.d(LOG_TAG, "We finished to load a page with a bad history "+url+".");
					// If we go from a back, nextAltHistoryStack is ""
					//nextAltHistoryStackBis=(("".equals(nextAltHistoryStack) && url.contains("&ui-page="))?"menu":nextAltHistoryStack);
					nextAltHistoryStackBis=nextAltHistoryStack;	
					nextAltHistoryStack="";
				}
				
				dumpBackForwardList(myWebView);
			}
	    }
		
		/**
		 * onReceivedHttpAuthRequest
		 */
	    @SuppressLint("AuthLeak")
		@Override
	    public void onReceivedHttpAuthRequest  (WebView view, HttpAuthHandler handler, String host, String realm)
	    { 
	    	Log.i(LOG_TAG, "A request to send http basic auth has been received");

	    	//String[] up = view.getHttpAuthUsernamePassword(host, realm); 

    		counthttpauth++;
            if (counthttpauth >= 3)
            {
                counthttpauth=0;
                Toast.makeText(getBaseContext(), "Server protected by Basic Authentication. Include login/pass into login URL:\nhttp://login:password@mydomain.com", Toast.LENGTH_LONG).show();
                handler.cancel();
            }
            if (counthttpauth == 1)
            {
                /*
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                //SharedPreferences sharedPrefs = this.secondActivity.getSharedPreferences(FILENAME_INST_PARAM, Context.MODE_PRIVATE);
                boolean prefAlwaysAutoFill = sharedPrefs.getBoolean("prefAlwaysAutoFill", true);
                if (prefAlwaysAutoFill)
                {
                    //String username=sharedPrefs.getString("lastsubmit-username", "");
                    //String password=sharedPrefs.getString("lastsubmit-password", "");
                    String username=sharedPrefs.getString(savedDolRootUrl+"-username", "");
                    String password=sharedPrefs.getString(savedDolRootUrl+"-password", "");
                    Log.d(LOG_TAG, "We try to proceed with username="+username+" password="+password);
                    handler.proceed(username, password);
                }
                else counthttpauth++;
                */
                counthttpauth++;
            }
            if (counthttpauth == 2) 
            {
                Log.d(LOG_TAG, "We try to proceed with info from URL username="+savedAuthuser+" password="+savedAuthpass);
                handler.proceed(savedAuthuser, savedAuthpass);
            }
	    	//webview.setHttpAuthUsernamePassword(host, realm, username, password);
	    }
	      
		/**
		 * onReceivedLoginRequest 
		 * 
		 * @return	void
		 */
	    /*@Override 
	    public void onReceivedLoginRequest(WebView view, String realm, String account, String args) 
	    { 
	    }*/

		/**
		 * onReceivedError
		 * This method is only called when network errors occur, but never when HTTP errors are received by WebView.
		 */
		@Override
	    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) 
		{
			//super.onReceivedError(view, errorCode, description, failingUrl);
		    Log.e(LOG_TAG, "onReceivedError code: " + errorCode + " on URL " + failingUrl + ": " + description);
		    Toast.makeText(activity, "Your Internet Connection may not be active Or " + description , Toast.LENGTH_LONG).show();
	    }
		
		/**
		 * onReceivedSslError
		 */
		@Override
		public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
			String message = "SSL Certificate error.";
			switch (error.getPrimaryError()) {
				case SslError.SSL_UNTRUSTED:
					message = "The certificate authority is not trusted.";
					break;
				case SslError.SSL_EXPIRED:
					message = "The certificate has expired.";
					break;
				case SslError.SSL_IDMISMATCH:
					message = "The certificate Hostname mismatch.";
					break;
				case SslError.SSL_NOTYETVALID:
					message = "The certificate is not yet valid.";
					break;
				default:
					message = "Unknown certificate error " + error.getPrimaryError();
					break;
			}

			Log.w(LOG_TAG, "onReceivedSslError error message = " + message + " string = " + error);
			/*handler.proceed() ;
			handler.cancel(); */


			if (! this.secondActivity.sslErrorWasAccepted) {
				// Code to ask user how to handle error
				SslAlertDialog dialog = new SslAlertDialog(handler, this.secondActivity, message);
				dialog.show();
			}
			else
			{
				Log.w(LOG_TAG, "onReceivedSslError SSL error already accepted");
				handler.proceed();
			}

		}
		

		
		/**
		 * listCookies
		 * 
		 * @return	string		Return list of cookies with format name=value;name2=value2
		 */
		public String listCookies() 
		{
		    CookieSyncManager.getInstance().sync();	// No more required with API 21
		    CookieManager cookie_manager = CookieManager.getInstance();

		    String cookie_string = cookie_manager.getCookie(savedDolRootUrl);
		    Log.v(LOG_TAG, "cookie_string (path " + savedDolRootUrl + ") = " + cookie_string);

		    return cookie_string;
		}
		
		/**
		 * deleteSessionCookies.
		 * Can be used to clear cookies to have a clean context for debug.
		 */
		public void deleteSessionCookies() 
		{
		    CookieSyncManager.getInstance().sync();	// No more required with API 21
		    CookieManager cookie_manager = CookieManager.getInstance();

		    Log.v(LOG_TAG, "delete session cookies");
		    cookie_manager.removeSessionCookie();
		    CookieSyncManager.getInstance().sync();	// No more required with API 21
	    
		    String cookie_string = cookie_manager.getCookie(savedDolRootUrl);
		    Log.v(LOG_TAG, "cookie_string (path " + savedDolRootUrl + ") = " + cookie_string);
		}		
	}


	/**
	 * WebChromeClientDoliDroid
	 */
	class WebChromeClientDoliDroid extends WebChromeClient
	{
		@Override
		public boolean onJsAlert(WebView view, String url, String message, final android.webkit.JsResult result)  
		{
			Log.d(LOG_TAG, message);
			//Toast.makeText(context, message, 3000).show();
			return true;
		};
		
		/**
		 * This can be called before of after the onPageFinished
		 * 
		 * @return		boolean		True if message is handled by client, false otherwise
		 */
		@Override
		public boolean onConsoleMessage(ConsoleMessage cm) 
		{
			if (cm != null && cm.sourceId() != null && cm.sourceId().contains("jquery.mobile-latest"))		// Test if error to load page thats fails because of jmobile (slow systems)
			{
			    tagToShowInterruptMessage="Error during loading of page url = " + lastLoadUrl + " -- " + cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId();
			    tagToShowInterruptCounter++;
			    Log.e(LOG_TAG, "onConsoleMessage tagToShowInterruptCounter set to "+tagToShowInterruptCounter+" after "+tagToShowInterruptMessage);
			    return true;
			}
			else
			{
				if (cm.messageLevel() == ConsoleMessage.MessageLevel.LOG)		   Log.v(LOG_TAG, "onConsoleMessage "+cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
				else if (cm.messageLevel() == ConsoleMessage.MessageLevel.DEBUG)   Log.d(LOG_TAG, "onConsoleMessage "+cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
				else if (cm.messageLevel() == ConsoleMessage.MessageLevel.WARNING) Log.w(LOG_TAG, "onConsoleMessage "+cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
				else if (cm.messageLevel() == ConsoleMessage.MessageLevel.ERROR)   
				{
				    tagToShowMessage="Javascript error detected on page url = " + lastLoadUrl + " -- " + cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId();
				    tagToShowCounter++;
					Log.e(LOG_TAG, "onConsoleMessage "+cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId());
				    return true;
				}
				else Log.e(LOG_TAG, "onConsoleMessage "+cm.message() + " -- From line " + cm.lineNumber() + " of " + cm.sourceId()); 
			}
			return false;
		}		
		
		/**
		 * Called during loading of page
		 */
		public void onProgressChanged (WebView view, int newProgress) 
		{
			//Log.i(LOG_TAG, "setProgress to "+(newProgress)+" current visibility="+(progress != null?progress.getVisibility():""));
			if (newProgress < 100 && progress.getVisibility() == ProgressBar.GONE)
			{
                progress.setVisibility(ProgressBar.VISIBLE);
            }
            progress.setProgress(newProgress);
            if (newProgress >= 100) 
            {
                progress.setVisibility(ProgressBar.GONE);
            }
        }
        
        
        /**
         * Called when clicked on input select file
         */
        public boolean onShowFileChooser(
                WebView webView, ValueCallback<Uri[]> filePathCallback,
                WebChromeClient.FileChooserParams fileChooserParams) {
            
            Log.d(LOG_TAG, "onShowFileChooser");
            
            if(mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;

            /*Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(LOG_TAG, "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }*/

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("*/*");

            Intent[] intentArray;
            //if(takePictureIntent != null) {
            //    intentArray = new Intent[]{takePictureIntent};
            //} else {
                intentArray = new Intent[0];
            //}

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "File Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, REQUEST_INPUTFILE);

            return true;
        }
        

        // TODO Support camera capture of file
        // See https://stackoverflow.com/questions/29290940/open-camera-for-input-type-file-in-webview-not-opening-android
/*        
        //The undocumented magic method override  
        //Eclipse will swear at you if you try to put @Override here  
        // For Android 3.0+
        public void openFileChooser(ValueCallback<Uri> uploadMsg) 
        {  
            Log.d(LOG_TAG, "openFileChooser a");
            mUploadMessage = uploadMsg;  
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);  
            i.addCategory(Intent.CATEGORY_OPENABLE);  
            i.setType("image/*");  
            SecondActivity.this.startActivityForResult(Intent.createChooser(i,"File Selector"), SecondActivity.REQUEST_INPUTFILE);  
        }

        // For Android 3.0+
        public void openFileChooser(ValueCallback uploadMsg, String acceptType ) 
        {
            Log.d(LOG_TAG, "openFileChooser b");
            mUploadMessage = uploadMsg;
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
        
            SecondActivity.this.startActivityForResult(
                    Intent.createChooser(i, "File Browser"),
                    REQUEST_INPUTFILE);
        }

        //For Android 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture)
        {
            Log.d(LOG_TAG, "openFileChooser c");
            mUploadMessage = uploadMsg;  
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);  
            i.addCategory(Intent.CATEGORY_OPENABLE);  
            i.setType("*REMOVETHIS/*");  
            SecondActivity.this.startActivityForResult( Intent.createChooser( i, "File Selection" ), SecondActivity.REQUEST_INPUTFILE );
        }
    */
    }

    /**
     * onResume
     */
    @Override
    protected void onResume()
    {
        Log.d(LOG_TAG, "onResume start cookie sync");
        CookieSyncManager.getInstance().startSync();    // No more required with API 21
        super.onResume();
    }
    
    /**
     * onPause
     */
    @Override
    protected void onPause()
    {
        Log.d(LOG_TAG, "onPause stop cookie sync");
        CookieSyncManager.getInstance().stopSync(); // No more required with API 21
        super.onPause();
    }
    
    /**
     * onStop
     */
    @Override
    protected void onStop() 
    {
        setResult(RESULT_WEBVIEW);
        super.onStop();
    }
    
    /**
     * onDestroy
     */
    @Override
    protected void onDestroy() 
    {
        // Delete iabHelper for InApp management
        //iabHelper.dispose();
        
        setResult(RESULT_WEBVIEW);
        super.onDestroy();
    }


    /**
     * An instance of this class will be registered as a JavaScript interface
     */
    public class MyJavaScriptInterface
    {
        private static final String LOG_TAG = "MyJavaScriptInterface";
        Context mContext;

        MyJavaScriptInterface(Context c) 
        {
             mContext = c;
        }

        /*
        @JavascriptInterface
        public void functionJavaCalledByJsProcessHTML(String page, String html)
        {
            // Process the html as needed by the app
            // Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
            Log.i(LOG_TAG, "functionJavaCalledByJsProcessHTML page="+page
                    //+" html="+(html == null ? "null" : html.replaceAll("\n", " "))
                    );
            if (html != null && page.equals("menu") && html.contains("<!-- Menu -->"))
            {
                cacheForMenu=html;
                Log.d(LOG_TAG, "Save menu content into cache");
            }
            else
            {
                Log.w(LOG_TAG, "Content does not look to be menu nor quick access page, so we don't cache it");
            }
        }
        */
        
        /*
         * Example: method=post&action=http://192.168.0.1/index.php?dol_hide_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile=1&mainmenu=home&token=cf51a99cad2639a6f2da61753748dc16&loginfunction=loginfunction&tz=0&tz_string=GMT&dst_observed=0&dst_first=&dst_second=&screenwidth=320&screenheight=460&dol_hide_topmenu=1&dol_hide_leftmenu=1&dol_optimize_smallscreen=1&dol_no_mouse_hover=1&dol_use_jmobile=1&username=admin&password=admin
         */
        @JavascriptInterface
        public void functionJavaCalledByJsProcessFormSubmit(String data)
        {
            Log.i(LOG_TAG, "functionJavaCalledByJsProcessFormSubmit data="+data);
            String tmpdata[]=data.split("&");
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            //SharedPreferences sharedPrefs = mContext.getSharedPreferences(FILENAME_INST_PARAM, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPrefs.edit();
            for (String s: tmpdata)
            {
                String keyval[]=s.split("=");
                if (keyval.length >= 2)
                {
                    String key=keyval[0];
                    String val=keyval[1];
                    if ("username".equals(key) || "password".equals(key)) 
                    {
                        tagLastLoginPassToSavedLoginPass=true;
                        Log.d(LOG_TAG,"functionJavaCalledByJsProcessFormSubmit save lastsubmit-"+key+"="+val);
                        editor.putString("lastsubmit-"+key, val);
                    }
                }
            }
            editor.apply();
        }
    }

    /**
     * This is called after another opened activity is finished.
     * We go back here.
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) 
    {
        Log.d(LOG_TAG, "onActivityResult requestCode = "+requestCode+" resultCode = "+resultCode);
        
        if(requestCode != REQUEST_INPUTFILE || mFilePathCallback == null)
        {
            // Not a file upload
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }

        // Handle here if return comes from submit a file
        Uri[] results = null;

        // Check that the response is a good one
        if (resultCode == Activity.RESULT_OK) 
        {
            if(data == null) {
                // If there is not data, then we may have taken a photo
                if(mCameraPhotoPath != null) {
                    results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                }
            } else {
                String dataString = data.getDataString();
                if (dataString != null) {
                    results = new Uri[]{Uri.parse(dataString)};
                }
            }
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }
    
}
