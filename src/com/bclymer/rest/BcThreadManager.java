package com.bclymer.rest;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import android.os.Handler;

public class BcThreadManager {

	private static final int CORE_POOL_SIZE = 3;
	private static final int MAXIMUM_POOL_SIZE = 128;
	private static final int KEEP_ALIVE = 1;
	private static final AtomicInteger threadId = new AtomicInteger();

	private static final ThreadFactory mThreadFactory = new ThreadFactory() {
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r);
			t.setName("ThreadManager-" + threadId.getAndIncrement());
			return t;
		}
	};
	private static final BlockingQueue<Runnable> mPoolWorkQueue = new LinkedBlockingQueue<Runnable>();
	private static final Executor mExecutor = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, mPoolWorkQueue, mThreadFactory);
	private static Handler handler;

	/**
	 * Must be called on the main thread (just call it in Application:onCreate please).
	 */
	public static void init() {
		handler = new Handler();
	}

	public static void runOnUi(Runnable runnable) {
		handler.post(runnable);
	}

	public static void runInBackground(Runnable runnable) {
		mExecutor.execute(runnable);
	}

}