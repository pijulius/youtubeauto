package com.pijulius.youtubeauto;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.MediaPlayer.Event;
import org.videolan.libvlc.MediaPlayer.EventListener;
import org.videolan.libvlc.util.AndroidUtil;

import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.gms.car.input.InputManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewGroup.MarginLayoutParams;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

class BaseActivity {
	public Context context;
	public Window window;
	public CarUiController carUiController;
	public InputManager carInputManager;

	public View toolbar;
	public View toolbarSearch;

	public AutoEditText searchFor;
	public ImageButton backButton;
	public ImageButton homeButton;
	public ImageButton refreshButton;
	public ImageButton searchButton;
	public ImageButton fullScreenButton;
	public ImageButton filesButton;
	public ImageButton settingsButton;

	public ProgressBar progressBar;
	public FrameLayout webFrame;
	public VideoWebView webView;
	public FrameLayout filesFrame;
	public VideoFilesView filesView;
	public FrameLayout videoFrame;
	public SurfaceView videoSurface;

	public View videoController;
	public ImageButton previousButton;
	public ImageButton playButton;
	public ImageButton nextButton;
	public SeekBar seekBar;
	public boolean seeking;

	public LibVLC libVLC;
	public IVLCVout vout;
	public File playingFile;
	public float playingPos;
	public float playingInitialPos;
	public MediaPlayer mediaPlayer;
	public MediaController mediaController;
	public AudioManager audioManager;
	public OnAudioFocusChangeListener audioFocusListener;

	public SharedPreferences settings;

	public Runnable toolbarHideRunnable = new Runnable() {
		@Override
		public void run() {
			hideToolbar();
		}
	};

	public void onCreate() {
		window.setContentView(R.layout.app);

		View view = window.findViewById(android.R.id.content);
		View actionbar = window.findViewById(context.getResources().getIdentifier("action_bar_container", "id", "android"));

		audioManager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);

		if (actionbar != null)
			actionbar.setVisibility(View.GONE);

		searchFor = view.findViewById(R.id.search_for);
		searchFor.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
					clickToolbar(homeButton);
					webView.loadUrl(settings.getString("search_url",
							context.getResources().getString(R.string.search_url)) + searchFor.getText());

					return true;
				}

				return false;
			}
		});

		searchFor.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE) {
					clickToolbar(homeButton);
					webView.loadUrl(settings.getString("search_url",
							context.getResources().getString(R.string.search_url)) + v.getText());
				}
				return false;
			}
		});

		toolbar = view.findViewById(R.id.toolbar);
		toolbarSearch = view.findViewById(R.id.toolbar_search);
		progressBar = view.findViewById(R.id.progress_bar);
		webView = view.findViewById(R.id.web_view);
		webFrame = view.findViewById(R.id.web_frame);
		filesFrame = view.findViewById(R.id.files_frame);
		filesView = view.findViewById(R.id.files_view);
		videoFrame = view.findViewById(R.id.video_frame);
		videoSurface = view.findViewById(R.id.video_surface);
		videoController = view.findViewById(R.id.video_controller);

		filesView.setBaseActivity(this);

		previousButton = view.findViewById(R.id.previous_button);
		previousButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mediaPlayer == null)
					return;

				File previous = filesView.getPreviousVideo(playingFile);
				if (previous != null)
					playVideo(previous);
				else
					clickToolbar(backButton);
			}
		});

		playButton = view.findViewById(R.id.play_button);
		playButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mediaPlayer == null)
					return;

				if (mediaPlayer.isPlaying()) {
					mediaPlayer.pause();
					playButton.setImageResource(R.drawable.ic_play);
				} else {
					mediaPlayer.play();
					playButton.setImageResource(R.drawable.ic_pause);
				}
			}
		});

		nextButton = view.findViewById(R.id.next_button);
		nextButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if (mediaPlayer == null)
					return;

				File next = filesView.getNextVideo(playingFile);
				if (next != null)
					playVideo(next);
				else
					clickToolbar(backButton);
			}
		});

		seekBar = view.findViewById(R.id.seek_bar);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				if (mediaPlayer == null)
					return;

				mediaPlayer.setPosition(seekBar.getProgress()/100f);
				seeking = false;
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				seeking = true;
			}

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
			}
		});

		settings = PreferenceManager.getDefaultSharedPreferences(context);

		backButton = view.findViewById(R.id.back_button);
		backButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickToolbar(backButton);
			}
		});

		homeButton = view.findViewById(R.id.home_button);
		homeButton.setActivated(true);
		homeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickToolbar(homeButton);
			}
		});

		refreshButton = view.findViewById(R.id.refresh_button);
		refreshButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickToolbar(refreshButton);
			}
		});

		searchButton = view.findViewById(R.id.search_button);
		searchButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickToolbar(searchButton);
			}
		});

		fullScreenButton = view.findViewById(R.id.fullscreen_button);
		fullScreenButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickToolbar(fullScreenButton);
			}
		});

		filesButton = view.findViewById(R.id.files_button);
		filesButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clickToolbar(filesButton);
			}
		});

		settingsButton = view.findViewById(R.id.settings_button);
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(context, SettingsActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				context.startActivity(intent);
			}
		});

		filesView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				File file = (File) filesView.list.get(position);
				if (file == null)
					return;

				if (file.isDirectory()) {
					if (filesView.go(file.toString())) {
						SharedPreferences.Editor editor = settings.edit();
						editor.putString("last_dir", file.toString());
						editor.apply();
					}

					return;
				}

				if (filesView.isVideo(file))
					playVideo(file);
			}
		});

		videoFrame.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (fullScreenButton.isActivated() && event.getAction() == MotionEvent.ACTION_DOWN) {
					toggleToolbarAnimation();
				}

				return false;
			}
		});

		if (carUiController != null)
			settingsButton.setVisibility(View.GONE);

		webView.setInitialScale((int)((context.getResources().getDisplayMetrics().density*Float.valueOf(settings.getString("website_scaling", "1")))*100));
		webView.setWebViewClient(new CustomWebViewClient());

		ViewGroup fullScreenVideoView = view.findViewById(R.id.full_screen_view);
		VideoEnabledWebChromeClient videoEnabledWebChromeClient = new VideoEnabledWebChromeClient(webFrame,
				fullScreenVideoView, new ProgressBar(context), webView);

		videoEnabledWebChromeClient.setOnToggledFullscreen(new VideoEnabledWebChromeClient.ToggledFullscreenCallback() {
			@Override
			public void toggledFullscreen(boolean fullscreen) {
				fullScreenButton.setActivated(fullscreen);
				fullScreenButton.setImageResource((fullscreen?R.drawable.ic_fullscreen_exit:R.drawable.ic_fullscreen));

				if (fullscreen) {
					hideToolbar();
				} else {
					showToolbar();
				}
			}
		});

		videoEnabledWebChromeClient.setVideoTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					toggleToolbarAnimation();
				}
				return false;
			}
		});

		webView.setWebChromeClient(videoEnabledWebChromeClient);
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setAllowFileAccess(true);
		webView.getSettings().setDomStorageEnabled(true);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			webView.getSettings().setAllowUniversalAccessFromFileURLs(true);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			webView.getSettings().setAllowFileAccessFromFileURLs(true);
		}
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
			webView.getSettings().setMediaPlaybackRequiresUserGesture(false);
		}
		webView.getSettings().setAllowContentAccess(true);
		if (BuildConfig.DEBUG) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
				WebView.setWebContentsDebuggingEnabled(true);
			}
		}

		String url = settings.getString("home_url", context.getResources().getString(R.string.home_url));

		if (!settings.getString("last_url", "").isEmpty())
			url = settings.getString("last_url", "");

		webView.loadUrl(url);
		webView.requestFocus();
	}

	public void onStart() {
		if (playingFile != null) {
			playVideo(playingFile);
		}
	}

	public void onResume() {
		if (videoFrame.getVisibility() == View.VISIBLE) {
			mediaPlayer.play();
			playButton.setImageResource(R.drawable.ic_pause);
		}
	}

	public void onPause() {
		if (videoFrame.getVisibility() == View.VISIBLE) {
			mediaPlayer.pause();
			playButton.setImageResource(R.drawable.ic_play);
		}
	}

	public void onStop() {
		showToolbar();
		releasePlayer();

		if (webView.isVideoFullscreen()) {
			webView.exitFullScreen();
		}

		if (fullScreenButton.isActivated()) {
			fullScreenButton.setImageResource(R.drawable.ic_fullscreen);
			fullScreenButton.setActivated(false);
		}

		if (videoFrame.getVisibility() == View.VISIBLE) {
			videoFrame.setVisibility(View.GONE);
			filesFrame.setVisibility(View.VISIBLE);
		}
	}

	@SuppressWarnings("deprecation")
	public void createPlayer() {
		if (libVLC != null)
			return;

		DisplayMetrics displayMetrics = new DisplayMetrics();
		window.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		displayMetrics.heightPixels += 40;

		final ArrayList<String> args = new ArrayList<>();
		args.add("--vout=android-display");

		libVLC = new LibVLC(videoFrame.getContext(), args);
		mediaPlayer = new MediaPlayer(libVLC);

		audioFocusListener = new OnAudioFocusChangeListener() {
			@Override
			public void onAudioFocusChange(int focusChange) {
				switch (focusChange) {
					case AudioManager.AUDIOFOCUS_LOSS:
						if (mediaPlayer.isPlaying())
							mediaPlayer.pause();
						break;
					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
						mediaPlayer.setVolume(36);
						break;
					case AudioManager.AUDIOFOCUS_GAIN:
					case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
					case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
						mediaPlayer.setVolume(100);
						break;
				}
			}
		};

		audioManager.requestAudioFocus(audioFocusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);

		mediaPlayer.setEventListener(new EventListener() {
			@Override
			public void onEvent(Event arg0) {
				if (!seeking) {
					playingPos = mediaPlayer.getPosition();
					seekBar.setProgress((int) (playingPos*100));
				}

				if (arg0.type == Event.Playing && playingInitialPos > 0) {
					mediaPlayer.setPosition(playingInitialPos);
					playingInitialPos = 0;
				}

				if (arg0.type == Event.EndReached)
					nextButton.performClick();
			}
		});

		vout = mediaPlayer.getVLCVout();
		vout.setVideoSurface(videoSurface.getHolder().getSurface(), videoSurface.getHolder());
		vout.attachViews(new IVLCVout.OnNewVideoLayoutListener() {
			@Override
			public void onNewVideoLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
				if (width > 0) {
					LayoutParams params = videoSurface.getLayoutParams();
					float wScale = displayMetrics.widthPixels/(float)width;
					float hScale = displayMetrics.heightPixels/(float)height;

					if (width > height && width*hScale > displayMetrics.widthPixels) {
						params.width = (int)(width*hScale);
						params.height = (int)(height*hScale);
						((MarginLayoutParams)params).setMargins((displayMetrics.widthPixels-params.width)/2, 0, 0, 0);

					} else {
						params.width = (int)(width*wScale);
						params.height = (int)(height*wScale);
						((MarginLayoutParams)params).setMargins(0, (displayMetrics.heightPixels-params.height)/2, 0, 0);
					}

					videoSurface.setLayoutParams(params);
				}
			}
		});
	}

	@SuppressWarnings("deprecation")
	public void releasePlayer() {
		if (libVLC == null)
			return;

		playingInitialPos = playingPos;
		audioManager.abandonAudioFocus(audioFocusListener);

		mediaPlayer.stop();
		vout.detachViews();
		mediaPlayer.release();
		libVLC.release();

		libVLC = null;
		mediaPlayer = null;
		vout = null;
		audioFocusListener = null;
	}

	public void playVideo(File file) {
		videoFrame.setVisibility(View.VISIBLE);
		filesFrame.setVisibility(View.GONE);

		createPlayer();

		mediaPlayer.play(AndroidUtil.FileToUri(file));
		playButton.setImageResource(R.drawable.ic_pause);

		playingFile = file;
	}

	public void showAndHideToolbarAnimation() {
		toolbar.removeCallbacks(toolbarHideRunnable);
		showToolbar();
		toolbar.postDelayed(toolbarHideRunnable, 3000);
	}

	public void toggleToolbarAnimation() {
		if (toolbar.getTranslationY() == 0) {
			hideToolbar();
		} else {
			showAndHideToolbarAnimation();
		}
	}

	public void showToolbar() {
		toolbar.setVisibility(View.VISIBLE);
		toolbar.removeCallbacks(toolbarHideRunnable);
		toolbar.clearAnimation();

		if (toolbar.getTranslationY() < 0) {
			toolbar.animate().setDuration(200).translationY(0);
		}

		if (videoFrame.getVisibility() == View.VISIBLE) {
			videoController.setVisibility(View.VISIBLE);
			videoController.clearAnimation();

			if (videoController.getTranslationY() > 0) {
				videoController.animate().setDuration(200).translationY(0);
			}
		}
	}

	public void hideToolbar() {
		if (seeking) {
			showAndHideToolbarAnimation();
			return;
		}

		toolbar.clearAnimation();
		if (toolbar.getTranslationY() == 0) {
			toolbar.animate().setDuration(200).translationY(-toolbar.getMeasuredHeight());
		}

		if (videoFrame.getVisibility() == View.VISIBLE) {
			videoController.clearAnimation();
			if (videoController.getTranslationY() == 0) {
				videoController.animate().setDuration(200).translationY(videoController.getMeasuredHeight());
			}
		}
	}

	public void clickToolbar(ImageButton button) {
		if (toolbarSearch.getVisibility() == View.VISIBLE) {
			toolbarSearch.setVisibility(View.GONE);
			searchFor.clearFocus();

			if (carInputManager != null)
				carInputManager.stopInput();

			searchButton.setActivated(false);

			if (button == searchButton)
				return;
		}

		if (fullScreenButton.isActivated()) {
			if (webView.isVideoFullscreen()) {
				webView.exitFullScreen();
			}

			if (videoFrame.getVisibility() == View.VISIBLE) {
				showToolbar();

				DisplayMetrics displayMetrics = new DisplayMetrics();
				window.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
			}

			fullScreenButton.setImageResource(R.drawable.ic_fullscreen);
			fullScreenButton.setActivated(false);

			if (button == fullScreenButton)
				return;
		}

		if (button == backButton) {
			if (filesFrame.getVisibility() == View.VISIBLE) {
				if (filesView.goUp()) {
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("last_dir", filesView.curDir.toString());
					editor.apply();
				}
			}

			if (videoFrame.getVisibility() == View.VISIBLE) {
				mediaPlayer.stop();
				playingFile = null;

				videoFrame.setVisibility(View.GONE);
				filesFrame.setVisibility(View.VISIBLE);
			}

			if (webFrame.getVisibility() == View.VISIBLE) {
				if (webView.canGoBack()) {
					webView.goBack();
				}
			}

			return;
		}

		if (button == homeButton) {
			webFrame.setVisibility(View.VISIBLE);

			if (filesFrame.getVisibility() == View.VISIBLE) {
				filesFrame.setVisibility(View.GONE);
				filesButton.setActivated(false);
			}

			if (videoFrame.getVisibility() == View.VISIBLE) {
				mediaPlayer.stop();
				playingFile = null;

				videoFrame.setVisibility(View.GONE);
				filesButton.setActivated(false);
			}

			if (homeButton.isActivated())
				webView.loadUrl(settings.getString("home_url", context.getResources().getString(R.string.home_url)));

			homeButton.setActivated(true);
			return;
		}

		if (button == refreshButton) {
			if (webFrame.getVisibility() == View.VISIBLE) {
				webView.reload();
			}

			if (filesFrame.getVisibility() == View.VISIBLE) {
				filesView.go(filesView.curDir.toString(), true);
			}

			if (videoFrame.getVisibility() == View.VISIBLE) {
				mediaPlayer.setPosition(0);
			}

			return;
		}

		if (button == searchButton) {
			toolbarSearch.setVisibility(View.VISIBLE);
			searchFor.requestFocus();

			if (carInputManager != null)
				carInputManager.startInput(searchFor);

			searchButton.setActivated(true);
			return;
		}

		if (button == fullScreenButton) {
			if (webFrame.getVisibility() == View.VISIBLE) {
				webView.requestFullScreen();
			}

			if (videoFrame.getVisibility() == View.VISIBLE) {
				DisplayMetrics displayMetrics = new DisplayMetrics();
				window.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

				hideToolbar();
			}

			fullScreenButton.setImageResource(R.drawable.ic_fullscreen_exit);
			fullScreenButton.setActivated(true);
			return;
		}

		if (button == filesButton) {
			if (webFrame.getVisibility() == View.VISIBLE) {
				webFrame.setVisibility(View.GONE);
				homeButton.setActivated(false);
			}

			if (filesFrame.getVisibility() == View.GONE) {
				filesFrame.setVisibility(View.VISIBLE);

				if (filesView.curDir == null)
					filesView.go(settings.getString("last_dir", null));
			}

			if (videoFrame.getVisibility() == View.VISIBLE) {
				mediaPlayer.stop();
				playingFile = null;

				videoFrame.setVisibility(View.GONE);
				filesFrame.setVisibility(View.VISIBLE);

			} else if (filesButton.isActivated()) {
				if (filesView.goUp()) {
					SharedPreferences.Editor editor = settings.edit();
					editor.putString("last_dir", filesView.curDir.toString());
					editor.apply();
				}
			}

			filesButton.setActivated(true);
			return;
		}
	}

	public class CustomWebViewClient extends WebViewClient {
		@Override
		public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
			return super.shouldInterceptRequest(view, request);

		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
			return super.shouldOverrideUrlLoading(view, request);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView view, final String url) {
			// editText.setText(url);
			if (url.startsWith("file:///") && !url.endsWith("/")) {
				view.post(new Runnable() {
					@Override
					public void run() {
						/*
						 * try { showNativePlayer(URLDecoder.decode(url, "UTF-8")); } catch
						 * (UnsupportedEncodingException e) { e.printStackTrace(); }
						 */
					}
				});
				return true;
			} else if (url.startsWith("intent://")) {
				try {
					Context context = view.getContext();
					Intent intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);

					if (intent != null) {
						view.stopLoading();

						PackageManager packageManager = context.getPackageManager();
						ResolveInfo info = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
						if (info != null) {
							context.startActivity(intent);
						} else {
							String fallbackUrl = intent.getStringExtra("browser_fallback_url");
							view.loadUrl(fallbackUrl);

						}

						return true;
					}
				} catch (URISyntaxException e) {
					// Log.e(TAG, "Can't resolve intent://", e);
				}
			}

			return false;
		}

		@Override
		public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
			super.doUpdateVisitedHistory(view, url, isReload);

			SharedPreferences.Editor editor = settings.edit();
			editor.putString("last_url", url);
			editor.apply();
		}

		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			super.onPageStarted(view, url, favicon);
			progressBar.setVisibility(View.VISIBLE);
		}

		@Override
		public void onPageFinished(WebView view, String url) {
			progressBar.setVisibility(View.GONE);
			super.onPageFinished(view, url);

			/*
			 * String injection = ("javascript:" + "var css = '" + "';\n" +
			 * "var head = document.getElementsByTagName('head')[0];\n" +
			 * "var style = document.createElement('style');style.type = 'text/css';\n" +
			 * "style.appendChild(document.createTextNode(css));\n"+
			 * "head.appendChild(style);"); webView.loadUrl(injection);
			 */
		}
	}
}