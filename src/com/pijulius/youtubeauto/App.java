package com.pijulius.youtubeauto;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Application;
import android.os.Handler;

public class App extends Application {
	static ExecutorService executor = Executors.newFixedThreadPool(4);
	static Handler handler = new Handler();
}