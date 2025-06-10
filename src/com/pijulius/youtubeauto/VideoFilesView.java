package com.pijulius.youtubeauto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.videolan.libvlc.util.AndroidUtil;
import org.videolan.libvlc.util.Extensions;
import org.videolan.libvlc.util.VLCUtil;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class VideoFilesView extends GridView {
	public Context context;
	public File curDir;
	public File rootDir = Environment.getExternalStorageDirectory();

	public List<File> list;
	public Map<String, Integer> positions;
	public FilesAdapter filesAdapter;
	public BaseActivity baseActivity;
	public List<File> thumbnailGenerated;

	public VideoFilesView(Context context) {
		this(context, null);
	}

	public VideoFilesView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public VideoFilesView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;

		list = new ArrayList<File>();
		thumbnailGenerated = new ArrayList<File>();
		positions = new HashMap<String, Integer>();
		filesAdapter = new FilesAdapter(getContext(), list);

		setAdapter(filesAdapter);
	}

	public void setBaseActivity(BaseActivity baseActivity) {
		this.baseActivity = baseActivity;
	}

	public boolean go(String dir) {
		return go(dir, false);
	}

	public boolean go(String dir, boolean reload) {
		if (curDir != null && !reload && curDir.toString().equals(dir)) {
			smoothScrollToPositionFromTop(0, 0, 0);
			return false;
		}

		if (dir == null || dir.isEmpty())
			dir = rootDir.toString();

		File newDir = new File(dir);
		File[] files = newDir.listFiles();

		if (files == null) {
			smoothScrollToPositionFromTop(0, 0, 0);
			return false;
		}

		if (curDir != null)
			positions.put(curDir.toString(), getFirstVisiblePosition());
		else
			positions.put(rootDir.toString(), getFirstVisiblePosition());

		list.clear();

		for (int i = 0; i < files.length; i++) {
			if (!files[i].isHidden() && (files[i].isDirectory() || isVideo(files[i])))
				list.add(files[i]);
		}

		filesAdapter.sort();
		filesAdapter.notifyDataSetChanged();

		if ((curDir != null && curDir.toString().length() > newDir.toString().length()) ||
			(curDir == null && rootDir.toString().length() > newDir.toString().length()))
		{
			Integer oldPos = positions.get(newDir.toString());

			if (oldPos != null)
				smoothScrollToPositionFromTop(oldPos, 0, 0);

		} else {
			smoothScrollToPositionFromTop(0, 0, 0);
		}

		curDir = newDir;
		return true;
	}

	public boolean goUp() {
		if (curDir != null) {
			File parent = curDir.getParentFile();

			if (parent != null)
				return go(parent.toString());

			return go(null);
		}

		return go(null);
	}

	public boolean goHome() {
		return go(null);
	}

	public boolean isVideo(File file) {
		if (file == null)
			return false;

		String fileName = file.getName();
		int i = fileName.lastIndexOf('.');

		if (i <= 0)
			return false;

		return Extensions.VIDEO.contains(fileName.substring(i));
	}

	public File getPreviousVideo(File file) {
		Integer postion = list.indexOf(file);

		if (postion == -1)
			return null;

		for(int i = postion-1; i >= 0; i--) {
			File f = list.get(i);

			if (isVideo(f))
				return f;
		}

		return null;
	}

	public File getNextVideo(File file) {
		Integer postion = list.indexOf(file);

		if (postion == -1)
			return null;

		for(int i = postion+1; i < list.size(); i++) {
			File f = list.get(i);

			if (isVideo(f))
				return f;
		}

		return null;
	}

	public String hashString(String s) {
		try {
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			StringBuilder hexString = new StringBuilder();
			for (int i = 0; i < messageDigest.length; i++) {
				hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
		}

		return "";
	}

	@SuppressWarnings("deprecation")
	public Bitmap getThumbnail(final File file, boolean cacheOnly) {
		if (file == null) {
			return null;
		}

		File dir = context.getCacheDir();
		File f = new File(dir, hashString(file.toString()) + ".jpg");

		if (!f.exists()) {
			if (cacheOnly || thumbnailGenerated.contains(file))
				return null;

			thumbnailGenerated.add(file);
			generateThumbnail(file);
		}

		if (f.length() <= 0) {
			f.delete();
			return null;
		}

		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = false;
		options.inPreferredConfig = Config.RGB_565;
		options.inDither = true;

		Bitmap bitmap = BitmapFactory.decodeFile(f.toString(), options);

		if (bitmap == null) {
			f.delete();
			return null;
		}

		f.setLastModified(System.currentTimeMillis());
		return bitmap;
	}

	public void generateThumbnail(File file) {
		baseActivity.createPlayer();

		if (baseActivity.libVLC == null)
			return;

		App.executor.execute(new Runnable() {
			@Override
			public void run() {
				int width = 1024;
				int height = 768;

				Bitmap thumbnail = Bitmap.createBitmap(width, height, Config.ARGB_8888);
				byte[] b = VLCUtil.getThumbnail(baseActivity.libVLC, AndroidUtil.FileToUri(file), width, height);

				if (b == null) {
					thumbnail = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
				} else {
					thumbnail.copyPixelsFromBuffer(ByteBuffer.wrap(b));
				}

				File dir = context.getCacheDir();
				File f = new File(dir, hashString(file.toString()) + ".jpg");

				OutputStream os;
				try {
					os = new FileOutputStream(f);
					thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, os);
					os.close();

					App.handler.post(new Runnable() {
						@Override
						public void run() {
							filesAdapter.notifyDataSetChanged();
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	class FilesAdapter extends BaseAdapter {
		LayoutInflater inflater;
		public List<File> list;

		FilesAdapter(Context context, List<File> list) {
			this.list = list;
			inflater = LayoutInflater.from(context);
		}

		public void sort() {
			Collections.sort(list, new Comparator<File>() {
				@Override
				public int compare(File x, File y) {
					if (x.isDirectory() && !y.isDirectory())
						return -1;

					return x.getName().compareTo(y.getName());
				}
			});
		}

		@Override
		public int getCount() {
			return list.size();
		}

		@Override
		public File getItem(int position) {
			return list.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = inflater.inflate(R.layout.file, null);

			if (position > list.size()-1) {
				convertView.setVisibility(View.GONE);
				return convertView;
			}

			convertView.setVisibility(View.VISIBLE);
			TextView fileName = (TextView)convertView.findViewById(R.id.file_name);
			ImageView fileIcon = (ImageView)convertView.findViewById(R.id.file_icon);
			File file = list.get(position);

			convertView.setTag(file);
			fileName.setText(file.getName());

			if (file.isDirectory()) {
				fileIcon.setBackground(getResources().getDrawable(R.drawable.ic_folder, null));
				fileIcon.setImageBitmap(null);

			} else if (isVideo(file)) {
				Bitmap thumb = getThumbnail(file, false);

				if (thumb != null) {
					fileIcon.setBackground(null);
					fileIcon.setImageBitmap(thumb);

				} else {
					fileIcon.setBackground(getResources().getDrawable(R.drawable.ic_video_file, null));
					fileIcon.setImageBitmap(null);
				}

			} else {
				fileIcon.setBackground(getResources().getDrawable(R.drawable.ic_file, null));
				fileIcon.setImageBitmap(null);
			}

			return convertView;
		}
	}
}
