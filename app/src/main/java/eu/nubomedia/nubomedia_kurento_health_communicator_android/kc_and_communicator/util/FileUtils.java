// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.util;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.json.JSONException;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.R;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.DataBasesAccess;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.database.MessageObject;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.CommandStoreService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.services.MessagingClientService;
import eu.nubomedia.nubomedia_kurento_health_communicator_android.kc_and_communicator.ui.activity.Preferences;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.Toast;

import com.kurento.agenda.datamodel.pojo.Command;
import com.kurento.agenda.services.pojo.MessageSend;

public class FileUtils {

	private static final Logger log = LoggerFactory
			.getLogger(FileUtils.class.getSimpleName());

	public static void deleteTemp(String fileTmp) {
		if (fileTmp == null) {
			log.warn("Invalid file: {}", fileTmp);
			return;
		}

		File file = new File(fileTmp);
		file.delete();
	}

	public static String entityToString(HttpEntity entity) throws IOException {
		InputStream is = entity.getContent();
		BufferedReader bufferedReader = new BufferedReader(
				new InputStreamReader(is));
		StringBuilder str = new StringBuilder();

		String line = null;
		try {
			while ((line = bufferedReader.readLine()) != null) {
				str.append(line + "\n");
			}
		} finally {
			try {
				is.close();
			} catch (IOException ignore) {
			}
		}
		return str.toString();
	}

	public static Bitmap entityToBitmap(Context context, HttpEntity entity,
			final String contentId, boolean withProgressBar, Long contentSize) {
		try {
			File file = new File(getDir(), contentId);
			file.createNewFile();
			InputStream is = entity.getContent();

			/*
			 * Read bytes to the Buffer until there is nothing more to read(-1)
			 * and write on the fly in the file.
			 */
			final FileOutputStream fos = new FileOutputStream(file);
			final int BUFFER_SIZE = 23 * 1024;
			BufferedInputStream bis = new BufferedInputStream(is, BUFFER_SIZE);
			byte[] baf = new byte[BUFFER_SIZE];
			int actual = 0;
			long total = 0;
			int totalpercent = 0;
			int auxtotal = 0;

			while (actual != -1) {
				fos.write(baf, 0, actual);
				actual = bis.read(baf, 0, BUFFER_SIZE);

				if (withProgressBar) {
					try {
						total = total + actual;
						auxtotal = (int) (((total * 100) / contentSize)) / 10;
						if ((totalpercent != auxtotal) && (totalpercent != 10)) {
							totalpercent = auxtotal;
							Intent intent = new Intent();
							intent.setAction(ConstantKeys.BROADCAST_DIALOG_PROGRESSBAR);
							intent.putExtra(ConstantKeys.TOTAL,
									totalpercent * 10);
							intent.putExtra(
									ConstantKeys.LOCALID,
									contentId
											.replace(
													ConstantKeys.EXTENSION_3GP,
													ConstantKeys.STRING_DEFAULT)
											.replace(
													ConstantKeys.EXTENSION_JPG,
													ConstantKeys.STRING_DEFAULT));
							context.sendBroadcast(intent);
						}
					} catch (Exception e) {
						log.error(
								"Error tryng to send broadcast to progressbar",
								e);
					}
				}
			}

			fos.close();

			if (contentId.contains(ConstantKeys.EXTENSION_JPG)) {
				return decodeSampledBitmapFromPath(file.getAbsolutePath(), 200,
						200);
			} else {
				return ThumbnailUtils.createVideoThumbnail(
						file.getAbsolutePath(),
						MediaStore.Images.Thumbnails.MINI_KIND);
			}

		} catch (Exception e) {
			new File(getDir(), contentId).delete();
			return null;
		}
	}

	public static void FreeUpSpace(Context ctx, Long fileSize) {
		File dir = new File(FileUtils.getDir());
		File[] files = dir.listFiles();
		long actualSize = Long.valueOf(0);
		for (int i = 0; i < files.length; i++) {
			if (!files[i].getName().contains("_")) {
				actualSize = actualSize + files[i].length();
			}
		}

		long futureSize = actualSize + fileSize;
		long totalSize = Long.valueOf(Preferences.getMediaTotalSize(ctx)) * 1024 * 1024;
		if (futureSize < totalSize) {
			return;
		}

		log.debug("We need more space, let's delete some files");
		orderFiles(files);
		for (int i = 0; i < files.length; i++) {
			if (!files[i].getName().contains("_")) {
				futureSize = futureSize - (files[i].length());
				files[i].delete();
				if (futureSize < totalSize) {
					return;
				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static void orderFiles(File[] files) {
		Arrays.sort(files, new Comparator() {
			public int compare(Object o1, Object o2) {

				if (((File) o1).lastModified() < ((File) o2).lastModified()) {
					return -1;
				} else if (((File) o1).lastModified() > ((File) o2)
						.lastModified()) {
					return 1;
				} else {
					return 0;
				}
			}

		});
	}

	public static void DownloadFromUrl(final String media,
			final String messageId, final Context ctx,
			final ImageView container, final Object object,
			final String timelineId, final String localId, final Long fileSize) {

		new AsyncTask<Void, Void, Boolean>() {
			private boolean retry = true;
			private Bitmap imageDownloaded;
			private BroadcastReceiver mDownloadCancelReceiver;
			private HttpGet job;
			private AccountManager am;
			private Account account;
			private String authToken;

			@Override
			protected void onPreExecute() {
				IntentFilter downloadFilter = new IntentFilter(
						ConstantKeys.BROADCAST_CANCEL_PROCESS);
				mDownloadCancelReceiver = new BroadcastReceiver() {
					@Override
					public void onReceive(Context context, Intent intent) {
						String localIdToClose = (String) intent.getExtras()
								.get(ConstantKeys.LOCALID);
						if (localId.equals(localIdToClose)) {
							try {
								job.abort();
							} catch (Exception e) {
								log.debug("The process was canceled");
							}
							cancel(false);
						}
					}
				};

				// registering our receiver
				ctx.getApplicationContext().registerReceiver(
						mDownloadCancelReceiver, downloadFilter);
			}

			@Override
			protected void onCancelled() {
				File file1 = new File(FileUtils.getDir(), localId
						+ ConstantKeys.EXTENSION_JPG);
				File file2 = new File(FileUtils.getDir(), localId
						+ ConstantKeys.EXTENSION_3GP);

				if (file1.exists()) {
					file1.delete();
				}
				if (file2.exists()) {
					file2.delete();
				}

				file1 = null;
				file2 = null;
				System.gc();
				try {
					ctx.getApplicationContext().unregisterReceiver(
							mDownloadCancelReceiver);
				} catch (Exception e) {
					log.debug("Receriver unregister from another code");
				}

				for (int i = 0; i < AppUtils.getlistOfDownload().size(); i++) {
					if (AppUtils.getlistOfDownload().get(i).equals(localId)) {
						AppUtils.getlistOfDownload().remove(i);
					}
				}

				DataBasesAccess.getInstance(ctx.getApplicationContext())
						.MessagesDataBaseWriteTotal(localId, 100);

				Intent intent = new Intent();
				intent.setAction(ConstantKeys.BROADCAST_DIALOG_DOWNLOAD_FINISH);
				intent.putExtra(ConstantKeys.LOCALID, localId);
				ctx.sendBroadcast(intent);

				if (object != null) {
					((ProgressDialog) object).dismiss();
				}
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				try {
					File file1 = new File(FileUtils.getDir(), localId
							+ ConstantKeys.EXTENSION_JPG);
					File file2 = new File(FileUtils.getDir(), localId
							+ ConstantKeys.EXTENSION_3GP);
					// firt we are goint to search the local files
					if ((!file1.exists()) && (!file2.exists())) {
						account = AccountUtils.getAccount(
								ctx.getApplicationContext(), false);
						am = (AccountManager) ctx
								.getSystemService(Context.ACCOUNT_SERVICE);
						authToken = ConstantKeys.STRING_DEFAULT;
						authToken = am.blockingGetAuthToken(account,
								ctx.getString(R.string.account_type), true);

						MessagingClientService messageService = new MessagingClientService(
								ctx.getApplicationContext());

						URL urlObj = new URL(
								Preferences.getServerProtocol(ctx),
								Preferences.getServerAddress(ctx),
								Preferences.getServerPort(ctx),
								ctx.getString(R.string.url_get_content));

						String url = ConstantKeys.STRING_DEFAULT;
						url = Uri.parse(urlObj.toString()).buildUpon().build()
								.toString()
								+ timelineId
								+ "/"
								+ messageId
								+ "/"
								+ "content";

						job = new HttpGet(url);
						// first, get free space
						FreeUpSpace(ctx, fileSize);
						messageService.getContent(authToken, media, messageId,
								timelineId, localId, false, false, fileSize,
								job);
					}

					if (file1.exists()) {
						imageDownloaded = decodeSampledBitmapFromPath(
								file1.getAbsolutePath(), 200, 200);
					} else if (file2.exists()) {
						imageDownloaded = ThumbnailUtils.createVideoThumbnail(
								file2.getAbsolutePath(),
								MediaStore.Images.Thumbnails.MINI_KIND);
					}

					if (imageDownloaded == null) {
						return false;
					}
					return true;
				} catch (Exception e) {
					deleteFiles();
					return false;
				}
			}

			@Override
			protected void onPostExecute(Boolean result) {
				// We have the media
				try {
					ctx.getApplicationContext().unregisterReceiver(
							mDownloadCancelReceiver);
				} catch (Exception e) {
					log.debug("Receiver was closed on cancel");
				}

				if (!(localId.contains(ConstantKeys.AVATAR))) {
					for (int i = 0; i < AppUtils.getlistOfDownload().size(); i++) {
						if (AppUtils.getlistOfDownload().get(i).equals(localId)) {
							AppUtils.getlistOfDownload().remove(i);
						}
					}

					DataBasesAccess.getInstance(ctx.getApplicationContext())
							.MessagesDataBaseWriteTotal(localId, 100);

					Intent intent = new Intent();
					intent.setAction(ConstantKeys.BROADCAST_DIALOG_DOWNLOAD_FINISH);
					intent.putExtra(ConstantKeys.LOCALID, localId);
					ctx.sendBroadcast(intent);
				}

				if (object != null) {
					((ProgressDialog) object).dismiss();
				}

				// Now the only container could be the avatar in edit screen
				if (container != null) {
					if (imageDownloaded != null) {
						container.setImageBitmap(imageDownloaded);
					} else {
						deleteFiles();
						imageDownloaded = decodeSampledBitmapFromResource(
								ctx.getResources(),
								R.drawable.ic_error_loading, 200, 200);
						container.setImageBitmap(imageDownloaded);
						Toast.makeText(
								ctx.getApplicationContext(),
								ctx.getApplicationContext().getText(
										R.string.donwload_fail),
								Toast.LENGTH_SHORT).show();
					}
				} else {
					showMedia(localId, ctx, (ProgressDialog) object);
				}

			}

			private void deleteFiles() {
				File file1 = new File(FileUtils.getDir(), localId
						+ ConstantKeys.EXTENSION_JPG);
				File file2 = new File(FileUtils.getDir(), localId
						+ ConstantKeys.EXTENSION_3GP);
				if (file1.exists()) {
					file1.delete();
				}
				if (file2.exists()) {
					file2.delete();
				}
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private static void showMedia(final String localId, final Context ctx,
			final ProgressDialog dialog) {
		new AsyncTask<Void, Void, Boolean>() {

			private boolean noMedia = false;

			@Override
			protected Boolean doInBackground(Void... params) {
				File file1 = new File(FileUtils.getDir(), localId
						+ ConstantKeys.EXTENSION_JPG);
				File file2 = new File(FileUtils.getDir(), localId
						+ ConstantKeys.EXTENSION_3GP);

				Intent mediaPlayer = new Intent(Intent.ACTION_VIEW);

				if (file1.exists()) {
					mediaPlayer.setDataAndType(Uri.fromFile(file1), "image/*");
				} else if (file2.exists()) {
					mediaPlayer.setDataAndType(Uri.fromFile(file2), "video/*");

				} else {
					noMedia = true;
					return false;
				}
				ctx.startActivity(mediaPlayer);
				return true;
			}

			@Override
			protected void onPostExecute(Boolean result) {
				if (dialog != null) {
					dialog.dismiss();
				}
				if (noMedia) {
					Toast.makeText(
							ctx.getApplicationContext(),
							ctx.getApplicationContext().getText(
									R.string.no_media), Toast.LENGTH_SHORT)
							.show();
					noMedia = false;
				}
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

	}

	public static String sendMessageToServer(final Context ctx,
			final String path, final Long toId, final String toType,
			final Long timelineId, boolean retry, Long localId,
			String messageBody, Object messageApp) {

		Account account = AccountUtils.getAccount(ctx.getApplicationContext(),
				true);
		AccountManager am = (AccountManager) ctx
				.getSystemService(Context.ACCOUNT_SERVICE);

		MessageSend m = new MessageSend();
		m.setBody(messageBody);
		m.setFrom(Long.valueOf(am.getUserData(account, JsonKeys.ID_STORED)));
		m.setTo(toId);
		m.setLocalId(localId);
		if (messageApp != null)
			m.setApp(messageApp);

		CommandStoreService cs = new CommandStoreService(ctx.getApplicationContext());
		try {
			boolean result = false;
			if (toType.equals(JsonKeys.GROUP)) {
				result = cs.createCommand(JsonParser.MessageSendToJson(m),
						Command.METHOD_SEND_MESSAGE_TO_GROUP, path);
			} else if (toType.equals(JsonKeys.USER)) {
				result = cs.createCommand(JsonParser.MessageSendToJson(m),
						Command.METHOD_SEND_MESSAGE_TO_USER, path);
			}

			if (result) {
				return ConstantKeys.SENDING_OK;
			} else {
				return ConstantKeys.SENDING_OFFLINE;
			}
		} catch (JSONException e) {
			log.error("Cannot create command", e);
			return ConstantKeys.SENDING_FAIL;
		}
	}

	public static String getRealPathFromURI(Uri contentUri, Context ctx) {
		String[] proj = { MediaStore.Video.Media.DATA };
		Cursor cursor = ((Activity) ctx).managedQuery(contentUri, proj, null,
				null, null);
		if (cursor != null) {
			int columnIndex = cursor
					.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
			cursor.moveToFirst();
			String ret = cursor.getString(columnIndex);

			return ret;
		} else {
			return contentUri.getPath();
		}
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res,
			int resId, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	public static Bitmap decodeSampledBitmapFromPath(String path, int reqWidth,
			int reqHeight) {
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(path, options);
		options.inSampleSize = calculateInSampleSize(options, reqWidth,
				reqHeight);
		options.inJustDecodeBounds = false;

		Bitmap b = BitmapFactory.decodeFile(path, options);

		ExifInterface exif;
		try {
			exif = new ExifInterface(path);
		} catch (IOException e) {
			return null;
		}
		int orientation = exif
				.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
		Matrix matrix = new Matrix();
		if (orientation == 6)
			matrix.postRotate(90);
		else if (orientation == 3)
			matrix.postRotate(180);
		else if (orientation == 8)
			matrix.postRotate(270);

		return Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(),
				matrix, true);
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth) {
			final int heightRatio = Math.round((float) height
					/ (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	public static void takePicture(Context ctx, Uri mFileCaptureUri) {
		if (!isIntentAvailable(ctx, MediaStore.ACTION_IMAGE_CAPTURE)) {
			log.warn("Cannot take picture (Intent is not available)");
			return;
		}

		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, mFileCaptureUri);
		intent.putExtra("return-data", true);

		try {
			((Activity) ctx).startActivityForResult(intent,
					AppUtils.ACTION_TAKE_PICTURE);
		} catch (ActivityNotFoundException e) {
			log.warn("Cannot take picture", e);
		}
	}

	public static void recordVideo(Context ctx, Uri mFileCaptureUri) {
		if (!isIntentAvailable(ctx, MediaStore.ACTION_VIDEO_CAPTURE)) {
			log.warn("Cannot record video (Intent is not available)");
			return;
		}

		Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
		takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mFileCaptureUri);

		int durationLimit = ctx.getResources().getInteger(
				R.integer.video_recording_duration_limit);
		if (durationLimit > 0) {
			takeVideoIntent.putExtra("android.intent.extra.durationLimit",
					durationLimit);
		}

		try {
			((Activity) ctx).startActivityForResult(takeVideoIntent,
					AppUtils.ACTION_RECORD_VIDEO);
		} catch (ActivityNotFoundException e) {
			log.warn("Cannot record video", e);
		}
	}

	public static void copyFile(File src, File dst) throws IOException {
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dst).getChannel();

		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}

	public static void createThumnail(File file) {
		String name = file.getName();
		String filePath = file.getAbsolutePath();
		Bitmap bm;

		if ((file.toString().contains(ConstantKeys.EXTENSION_JPG))
				|| (file.toString().contains(ConstantKeys.EXTENSION_PNG))
				|| (file.toString().contains(ConstantKeys.EXTENSION_JPEG))) {
			bm = decodeSampledBitmapFromPath(filePath, 250, 250);
			name = name.replace(ConstantKeys.EXTENSION_JPG,
					ConstantKeys.STRING_DEFAULT);

		} else {
			bm = ThumbnailUtils.createVideoThumbnail(filePath,
					MediaStore.Images.Thumbnails.MINI_KIND);
			name = name.replace(ConstantKeys.EXTENSION_3GP,
					ConstantKeys.STRING_DEFAULT);

		}

		if (bm == null) {
			log.error("Error creating thumbnail");
			return;
		}

		File f = new File(getDir(), name + ConstantKeys.THUMBNAIL
				+ ConstantKeys.EXTENSION_JPG);
		try {
			f.createNewFile();
		} catch (IOException e1) {
			log.error("Error creating file for thumbnail", e1);
			return;
		}
		// Convert bitmap to byte array
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bm.compress(CompressFormat.PNG, 0, bos);
		byte[] bitmapdata = bos.toByteArray();
		// write the bytes in file
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(f);
			fos.write(bitmapdata);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			log.debug("Error creagint thumnail", e);
		}

		bm = null;
		System.gc();
	}

	public static String getDir() {
		File dir = new File(Environment.getExternalStorageDirectory()
				.toString() + "/.kurento");
		if (!dir.exists()) {
			dir.mkdir();
		}
		return dir.getAbsolutePath();
	}

	private static void showError(Context ctx) {
		Toast.makeText(ctx.getApplicationContext(),
				ctx.getApplicationContext().getText(R.string.auth_fail),
				Toast.LENGTH_SHORT).show();
	}

	private static boolean isIntentAvailable(Context context, String action) {
		final PackageManager packageManager = context.getPackageManager();
		final Intent intent = new Intent(action);
		List<ResolveInfo> list = packageManager.queryIntentActivities(intent,
				PackageManager.MATCH_DEFAULT_ONLY);
		return list.size() > 0;
	}

	public static void deleteMediaFromTimeline(Context ctx, Long timeLine) {

		ArrayList<MessageObject> list = DataBasesAccess.getInstance(
				ctx.getApplicationContext()).MessagesDataBaseReadSelected(
				timeLine, null);

		for (int i = 0; i < list.size(); i++) {
			File toDeleteJpg = new File(FileUtils.getDir() + "/"
					+ list.get(i).getLocalId() + ConstantKeys.EXTENSION_JPG);
			File toDelete3gp = new File(FileUtils.getDir() + "/"
					+ list.get(i).getLocalId() + ConstantKeys.EXTENSION_3GP);
			if (toDeleteJpg.exists()) {
				toDeleteJpg.delete();
				toDeleteJpg = null;
			}

			if (toDelete3gp.exists()) {
				toDelete3gp.delete();
				toDelete3gp = null;
			}

			DataBasesAccess.getInstance(ctx.getApplicationContext())
					.MessagesDataBaseDelete(list.get(i).getLocalId());
		}
	}
}
