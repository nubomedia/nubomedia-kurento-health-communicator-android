

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.client.methods.HttpGet;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.AuthClientService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.GroupClientService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.MessagingClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.widget.ImageView;

public class ImageDownloader {

	private static final Logger log = LoggerFactory
			.getLogger(ImageDownloader.class.getSimpleName());

	private static final int AVATAR_TYPE = 100;
	private static final int THUMBNAIL_TYPE = 101;

	private static final int HARD_CACHE_CAPACITY = 10;
	private static final int DELAY_BEFORE_PURGE = 30 * 1000; // in milliseconds
	private Context mContext = null;

	// Hard cache, with a fixed maximum capacity and a life duration
	private final HashMap<String, Bitmap> sHardBitmapCache = new LinkedHashMap<String, Bitmap>(
			HARD_CACHE_CAPACITY / 2, 0.75f, true) {
		private static final long serialVersionUID = -7190622541619388252L;

		@Override
		protected boolean removeEldestEntry(Entry<String, Bitmap> eldest) {
			if (size() > HARD_CACHE_CAPACITY) {
				// Entries push-out of hard reference cache are transferred to
				// soft reference cache
				sSoftBitmapCache.put(eldest.getKey(),
						new SoftReference<Bitmap>(eldest.getValue()));
				return true;
			} else {
				return false;
			}
		}
	};

	// Soft cache for bitmap kicked out of hard cache
	private final static ConcurrentHashMap<String, SoftReference<Bitmap>> sSoftBitmapCache = new ConcurrentHashMap<String, SoftReference<Bitmap>>(
			HARD_CACHE_CAPACITY / 2);

	private final Handler purgeHandler = new Handler();

	private final Runnable purger = new Runnable() {
		public void run() {
			clearCache();
		}
	};

	public void downloadMyAvatar(Context context, ImageView imageView,
			String userId, String avatarId) {

		if (isAvatarCero(context, avatarId, imageView, userId,
				R.drawable.ic_contact_picture)) {
			return;
		}
		downloadUserAvatar(context, imageView, userId, avatarId);
	}

	public void downloadUserAvatar(Context context, ImageView imageView,
			String userId, String avatarId) {

		if (isAvatarCero(context, avatarId, imageView, userId,
				R.drawable.ic_contact_picture_small)) {
			return;
		}

		mContext = context;
		resetPurgeTimer();
		Bitmap bitmap = getBitmap(userId, ConstantKeys.AVATAR);

		if (bitmap == null) {
			forceAvatarDownload(null, userId, null, imageView, userId, null);
		} else {
			cancelBitmapDownload(bitmap, avatarId, imageView, userId);
		}
	}

	public void downloadMessageAvatar(Context context, String messageId,
			String avatarId, String timelineId, ImageView imageView,
			String userId) {

		mContext = context;
		resetPurgeTimer();
		Bitmap bitmap = getBitmap(userId, ConstantKeys.AVATAR);

		if (bitmap == null) {
			if (isAvatarCero(context, avatarId, imageView, userId,
					R.drawable.ic_contact_picture_small)) {
				return;
			}

			// In this step, we check if the message is local then we load an
			// avatar from user, and not from the message
			if (messageId.equals(ConstantKeys.STRING_CERO)) {
				downloadUserAvatar(context, imageView, userId, avatarId);
				return;
			}

			forceAvatarDownload(messageId, avatarId, timelineId, imageView,
					userId, null);
		} else {
			cancelBitmapDownload(bitmap, avatarId, imageView, userId);
		}
	}

	public void downloadGroupAvatar(Context context, String groupId,
			String avatarId, ImageView imageView) {

		if (isAvatarCero(context, avatarId, imageView, groupId,
				R.drawable.ic_group)) {
			return;
		}

		mContext = context;
		resetPurgeTimer();
		Bitmap bitmap = getBitmap(groupId, ConstantKeys.AVATAR);

		if (bitmap == null) {
			forceAvatarDownload(null, avatarId, null, imageView, null, groupId);
		} else {
			cancelBitmapDownload(bitmap, avatarId, imageView, groupId);
		}
	}

	public void downloadThumbnail(Context context, Long mediaId,
			Long messageId, ImageView im, String timelineId, String localId,
			Long size) {

		if (localId == null) {
			return;
		}

		mContext = context;
		resetPurgeTimer();
		Bitmap bitmap = getBitmap(localId, ConstantKeys.THUMBNAIL);

		if (bitmap == null) {
			forceThumbnailDownload(mediaId.toString(), messageId.toString(),
					im, timelineId, localId + ConstantKeys.THUMBNAIL, size);
		} else {
			cancelPotentialDownload(im, mediaId.toString(), THUMBNAIL_TYPE);
			im.setBackgroundColor(Color.TRANSPARENT);
			im.setImageBitmap(bitmap);
		}
	}

	public void eraseUserAvatar(String userId) {
		if (userId == null) {
			return;
		}

		eraseBitmap(userId, ConstantKeys.AVATAR);
	}

	private boolean isAvatarCero(Context context, String avatarId,
			ImageView imageView, String itemId, int resource) {
		if (avatarId.equals(ConstantKeys.STRING_CERO)) {
			cancelBitmapDownload(BitmapFactory.decodeResource(
					context.getResources(), resource), avatarId, imageView,
					itemId);
			return true;
		} else {
			return false;
		}
	}

	private void cancelBitmapDownload(Bitmap bitmap, String avatarId,
			ImageView imageView, String itemId) {
		cancelPotentialDownload(imageView, itemId, AVATAR_TYPE);
		imageView.setBackgroundColor(Color.TRANSPARENT);
		imageView.setImageBitmap(bitmap);
	}

	/*
	 * Same as download but the image is always downloaded and the cache is not
	 * used. Kept private at the moment as its interest is not clear. private
	 * void forceDownload(String url, ImageView view) { forceDownload(url, view,
	 * null); }
	 */

	/**
	 * Same as download but the image is always downloaded and the cache is not
	 * used. Kept private at the moment as its interest is not clear.
	 */
	private void forceAvatarDownload(String messageId, String avatarId,
			String timelineId, ImageView imageView, String userId,
			String groupId) {
		if (avatarId == null) {
			imageView.setImageDrawable(null);
			return;
		}

		if (cancelPotentialDownload(imageView, userId, AVATAR_TYPE)) {
			BitmapDownloaderTask task = new BitmapDownloaderTask(imageView);
			DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
			imageView.setImageDrawable(downloadedDrawable);
			task.execute(messageId, avatarId, timelineId, userId, groupId,
					null, null, null);
		}
	}

	private void forceThumbnailDownload(String mediaId, String messageId,
			ImageView im, String timelineId, String localId, Long size) {
		if (mediaId == null) {
			im.setImageDrawable(null);
			return;
		}

		if (cancelPotentialDownload(im, mediaId, THUMBNAIL_TYPE)) {
			BitmapDownloaderTask task = new BitmapDownloaderTask(im);
			DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
			im.setImageDrawable(downloadedDrawable);
			task.execute(messageId, null, timelineId, null, null, mediaId,
					localId, String.valueOf(size));
		}
	}

	/**
	 * Clears the image cache used internally to improve performance. Note that
	 * for memory efficiency reasons, the cache will automatically be cleared
	 * after a certain inactivity delay.
	 */
	public void clearCache() {
		sHardBitmapCache.clear();
		sSoftBitmapCache.clear();
	}

	private void resetPurgeTimer() {
		purgeHandler.removeCallbacks(purger);
		purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
	}

	/**
	 * Returns true if the current download has been canceled or if there was no
	 * download in progress on this image view. Returns false if the download in
	 * progress deals with the same url. The download is not stopped in that
	 * case.
	 */
	private static boolean cancelPotentialDownload(ImageView imageView,
			String itemId, int type) {
		BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
		if (bitmapDownloaderTask != null) {
			String bitmapId;
			if (type == AVATAR_TYPE) {
				bitmapId = bitmapDownloaderTask.userId;
			} else {
				bitmapId = bitmapDownloaderTask.mediaId;
			}

			if ((bitmapId == null) || (!bitmapId.equals(itemId))) {
				bitmapDownloaderTask.cancel(true);
			} else {
				// The same URL is already being downloaded.
				return false;
			}
		}
		return true;
	}

	/**
	 * @param imageView
	 *            Any imageView
	 * @return Retrieve the currently active download task (if any) associated
	 *         with this imageView. null if there is no such task.
	 */
	private static BitmapDownloaderTask getBitmapDownloaderTask(
			ImageView imageView) {
		if (imageView != null) {
			Drawable drawable = imageView.getDrawable();
			if (drawable instanceof DownloadedDrawable) {
				DownloadedDrawable downloadedDrawable = (DownloadedDrawable) drawable;
				return downloadedDrawable.getBitmapDownloaderTask();
			}
		}
		return null;
	}

	/**
	 * @param url
	 *            The URL of the image that will be retrieved from the cache.
	 * @return The cached bitmap or null if it was not found.
	 */
	private Bitmap getBitmap(String contentId, String extensionType) {
		String avatar = contentId + extensionType + ConstantKeys.EXTENSION_JPG;

		// First try the hard reference cache
		synchronized (sHardBitmapCache) {
			final Bitmap bitmap = sHardBitmapCache.get(avatar);
			if (bitmap != null) {
				// Bitmap found in hard cache
				// Move element to first position, so that it is removed last
				sHardBitmapCache.remove(avatar);
				sHardBitmapCache.put(avatar, bitmap);
				return bitmap;
			}
		}

		// Then try the soft reference cache
		SoftReference<Bitmap> bitmapReference = sSoftBitmapCache.get(avatar);
		if (bitmapReference != null) {
			final Bitmap bitmap = bitmapReference.get();
			if (bitmap != null) {
				// Bitmap found in soft cache
				return bitmap;
			} else {
				// Soft reference has been Garbage Collected
				sSoftBitmapCache.remove(avatar);
			}
		}
		// Then try the sd card
		try {
			FileInputStream in = new FileInputStream(FileUtils.getDir() + "/"
					+ avatar);
			Bitmap bitmap = BitmapFactory.decodeStream(in);
			if (bitmap != null) {
				return bitmap;
			}
		} catch (Exception e) {
			//
		}

		return null;
	}

	private void eraseBitmap(String contentId, String extensionType) {
		String avatar = contentId + extensionType + ConstantKeys.EXTENSION_JPG;

		// First remove the hard reference cache
		synchronized (sHardBitmapCache) {
			sHardBitmapCache.remove(avatar);
		}

		// Then remove the soft reference cache
		synchronized (sSoftBitmapCache) {
			sSoftBitmapCache.remove(avatar);
		}

		// Then erase from the sd card
		try {
			File file = new File(FileUtils.getDir() + "/" + avatar);
			file.delete();
		} catch (Exception e) {
			log.error("Error deleting file", e);
		}
	}

	/**
	 * The actual AsyncTask that will asynchronously download the image.
	 */
	class BitmapDownloaderTask extends AsyncTask<String, Void, Bitmap> {
		private static final int IO_BUFFER_SIZE = 1 * 1024;
		private String avatarId;
		private String messageId;
		private String timelineId;
		private String userId;
		private String groupId;

		private String mediaId;
		private String localId;
		private String size;

		private WeakReference<ImageView> imageViewReference;

		public BitmapDownloaderTask(ImageView imageView) {
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		/**
		 * Actual download method.
		 */
		@Override
		protected Bitmap doInBackground(String... params) {
			messageId = params[0];
			avatarId = params[1];
			timelineId = params[2];
			userId = params[3];
			groupId = params[4];

			mediaId = params[5];
			localId = params[6];
			size = params[7];

			Account account = AccountUtils.getAccount(
					mContext.getApplicationContext(), false);
			AccountManager am = (AccountManager) mContext
					.getSystemService(Context.ACCOUNT_SERVICE);

			String authToken = ConstantKeys.STRING_DEFAULT;
			try {
				authToken = am.blockingGetAuthToken(account,
						mContext.getString(R.string.account_type), true);

			} catch (Exception e) {
				return null;
			}

			Bitmap bm = null;
			HttpGet job = null;
			try {
				if (userId != null) {
					bm = getBitmap(userId, ConstantKeys.AVATAR);
				}

				if (bm != null) {
					return bm;
				} else if (mediaId != null) {
					MessagingClientService messagesClient = new MessagingClientService(
							mContext);
					bm = messagesClient.getContent(authToken, mediaId,
							messageId, timelineId, localId, false, true,
							ConstantKeys.LONG_DEFAULT, job);
				} else if (messageId != null) {
					// Take avatar from message
					MessagingClientService messagesClient = new MessagingClientService(
							mContext);
					bm = messagesClient.getContent(authToken, avatarId,
							messageId, timelineId, userId, true, false,
							ConstantKeys.LONG_DEFAULT, job);
				} else if (groupId != null) {
					GroupClientService groupClient = new GroupClientService(
							mContext);
					bm = groupClient.getAvatar(authToken, groupId);
				} else {
					// Take avatar from user
					AuthClientService auth = new AuthClientService(mContext);
					bm = auth.getAvatar(userId);
				}

				if (bm == null) {
					bm = BitmapFactory.decodeResource(mContext.getResources(),
							R.drawable.ic_contact_picture_small);
				}

				return bm;
			} catch (Exception e) {
				log.error("Error getting content", e);
				return null;
			}
		}

		/**
		 * Once the image is downloaded, associates it to the imageView
		 */
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (isCancelled()) {
				bitmap = null;
			}

			// Add bitmap to cache
			if (bitmap != null) {
				synchronized (sHardBitmapCache) {
					sHardBitmapCache.put(avatarId, bitmap);
				}
			}

			if (imageViewReference != null) {
				ImageView imageView = imageViewReference.get();
				BitmapDownloaderTask bitmapDownloaderTask = getBitmapDownloaderTask(imageView);
				// Change bitmap only if this process is still associated with
				// it
				if (this == bitmapDownloaderTask) {
					imageView.setImageBitmap(bitmap);

					if (bitmap != null)
						imageView.setBackgroundColor(Color.TRANSPARENT);
				}
			}

		}

		public void copy(InputStream in, OutputStream out) throws IOException {
			byte[] b = new byte[IO_BUFFER_SIZE];
			int read;
			while ((read = in.read(b)) != -1) {
				out.write(b, 0, read);
			}
		}
	}

	/**
	 * A fake Drawable that will be attached to the imageView while the download
	 * is in progress.
	 * 
	 * <p>
	 * Contains a reference to the actual download task, so that a download task
	 * can be stopped if a new binding is required, and makes sure that only the
	 * last started download process can bind its result, independently of the
	 * download finish order.
	 * </p>
	 */
	static class DownloadedDrawable extends ColorDrawable {
		private final WeakReference<BitmapDownloaderTask> bitmapDownloaderTaskReference;

		public DownloadedDrawable(BitmapDownloaderTask bitmapDownloaderTask) {
			super(Color.TRANSPARENT);
			bitmapDownloaderTaskReference = new WeakReference<BitmapDownloaderTask>(
					bitmapDownloaderTask);
		}

		public BitmapDownloaderTask getBitmapDownloaderTask() {
			return bitmapDownloaderTaskReference.get();
		}
	}

	static class FlushedInputStream extends FilterInputStream {
		public FlushedInputStream(InputStream inputStream) {
			super(inputStream);
		}

		@Override
		public long skip(long n) throws IOException {
			long totalBytesSkipped = ConstantKeys.LONG_DEFAULT;
			while (totalBytesSkipped < n) {
				long bytesSkipped = in.skip(n - totalBytesSkipped);
				if (bytesSkipped == ConstantKeys.LONG_DEFAULT) {
					int b = read();
					if (b < 0) {
						break; // we reached EOF
					} else {
						bytesSkipped = 1; // we read one byte
					}
				}
				totalBytesSkipped += bytesSkipped;
			}
			return totalBytesSkipped;
		}
	}
}