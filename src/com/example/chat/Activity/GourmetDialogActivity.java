package com.example.chat.Activity;

import java.util.ArrayList;

import com.example.chat.R;
import com.example.chat.R.animator;
import com.example.chat.R.drawable;
import com.example.chat.R.id;
import com.example.chat.R.layout;
import com.example.chat.api.LocaTouchApiURLCreator;
import com.example.chat.backgroundTask.PostServerTask;
import com.example.chat.bean.Gourmet;
import com.example.chat.common.IntentKey;
import com.example.chat.common.URLManager;
import com.example.chat.common.XmlReader;
import com.example.chat.image.ImageFileHelper;
import com.example.chat.image.ImageLoadTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class GourmetDialogActivity extends Activity {
	private String gourmetId = "-1";
	public Context context;
	private LayoutInflater layoutInflater;

	private TextView titleText;
	private TextView expText;
	private LinearLayout detailLayout;
	private TextView okButton;
	private Button closeButton;
	private Button mapButton;
	private ImageView image;
	private ProgressBar progressBar;

	private int paddingX;
	private int paddingY;

	ImageFileHelper imageFileHelper;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detail_dialog);
		context = this;
		layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		imageFileHelper = new ImageFileHelper(this);

		titleText = (TextView) findViewById(R.id.textView_title);
		expText = (TextView) findViewById(R.id.explanation);
		detailLayout = (LinearLayout) findViewById(R.id.layout_detail);

		okButton = (TextView) findViewById(R.id.textView_ok);
		closeButton = (Button) findViewById(R.id.btn_cancel);
		mapButton = (Button) findViewById(R.id.button_map);

		image = (ImageView) findViewById(R.id.imageView_spot);
		progressBar = (ProgressBar) findViewById(R.id.progressbar1);

		// 画面のpaddingを設定
		WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();
		Point size = new Point();
		disp.getSize(size);
		paddingX = size.x / 20;
		paddingY = size.y / 20;
		LinearLayout wholeLayout = (LinearLayout) findViewById(R.id.linearlayout_whole);
		wholeLayout.setPadding(paddingX, 2 * paddingY, paddingX, 2 * paddingY);

		gourmetId = getIntent().getExtras().getString(IntentKey.GOURMET_ID);

		titleText.setText("読み込み中...");
		checkFavorite();

		LocaTouchApiURLCreator c = new LocaTouchApiURLCreator();
		c.setId(gourmetId);
		String url = c.createUrl();

		PostServerTask psTask = new PostServerTask(url) {

			@Override
			protected void onPostExecute(Boolean result) {
				XmlReader xr = new XmlReader(httpData);
				ArrayList<Gourmet> list = xr.getGourmetLocaTouch();

				if (list.size() > 0) {
					final Gourmet gourmet = list.get(0);
					titleText.setText(gourmet.name);
					expText.setText("");

					setFavoriteEvent(gourmet.name, gourmet.lat, gourmet.lng);

					addItem("カテゴリ", gourmet.category);
					addItem("平均評価", gourmet.rate);
					addItem("住所", gourmet.address);
					addItem("アクセス", gourmet.access);
					addItem("電話番号", gourmet.tel);
					String openTime = "";
					openTime += "　平日　　:" + gourmet.weekday + "\n";
					openTime += "　土曜日　:" + gourmet.saturday + "\n";
					openTime += "日曜・祝日:" + gourmet.holiday + "\n";
					addItem("営業時間", openTime);
					addItem("定休日", gourmet.close);
					addItem("ランチの予算", gourmet.dinner);
					addItem("ディナーの予算", gourmet.lunch);
					addItem("画像", gourmet.imageURL);

					addItemWebPage("webページ", gourmet.url);

					String fileName = Gourmet.GOURMET_IMAGE_FILE_NAME + gourmet.gourmetId;
					int iconSize = ImageLoadTask.getPxFromDp(context, 50);
					Bitmap bitmap = imageFileHelper.loadBitmap(fileName, iconSize, iconSize);
					if (bitmap != null) {
						progressBar.setVisibility(View.GONE);
						image.setImageBitmap(bitmap);
					} else {
						loadImage(gourmet.imageURL);
					}

					mapButton.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent i = new Intent(GourmetDialogActivity.this, MapActivity.class);
							i.putExtra("GOURMET_DATA", gourmet);
							startActivity(i);
						}
					});
				} else {
					titleText.setText("読み込みの失敗");
				}
			}

		};
		psTask.setPostData("GourmetId", gourmetId);
		psTask.execute();

		closeButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}

		});

	}

	@Override
	public void finish() {
		super.finish();
		overridePendingTransition(0, R.animator.disappearance);
	}

	void setFavoriteEvent(String n, double lt, double lg) {
		final String name = n;
		final double lat = lt;
		final double lng = lg;

		okButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				okButton.setEnabled(false);
				PostServerTask pst = new PostServerTask(URLManager.ADD_FAVORITE_URL_GOURMET) {

					@Override
					protected void onPostExecute(Boolean result) {
						if (taskResult) {
							Toast.makeText(context, "お気に入りが完了しました", Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(context, "お気に入りが失敗しました", Toast.LENGTH_SHORT).show();
						}
						checkFavorite();
					}

				};
				pst.setPostData("GourmetId", gourmetId);
				pst.setPostData("Name", name);
				pst.setPostData("Lat", lat);
				pst.setPostData("Lng", lng);
				pst.setPostData("group_id", 1);
				pst.execute();
			}

		});
	}

	private void checkFavorite() {
		okButton.setEnabled(false);
		PostServerTask psTask = new PostServerTask(URLManager.CHECK_EXIST_FAVORITE_GOURMET_URL) {

			@Override
			protected void onPostExecute(Boolean result) {
				XmlReader xr = new XmlReader(httpData);
				boolean res = false;
				String label = "";
				String color = "#f0f0f0";
				if (xr.getResult()) {
					res = false;
					label = "★";
					color = "#ffd700";
				} else {
					res = true;
					label = "★";
				}
				okButton.setEnabled(res);
				okButton.setText(label);
				okButton.setTextColor(Color.parseColor(color));
			}

		};
		psTask.setPostData("GourmetId", gourmetId);
		psTask.execute();
	}

	private void addItem(String title, String value) {
		View view = layoutInflater.inflate(R.layout.item_detail, null);
		TextView tView = (TextView) view.findViewById(R.id.item_detail_title);
		TextView valueView = (TextView) view.findViewById(R.id.item_detail_value);
		tView.setAutoLinkMask(Linkify.WEB_URLS);
		tView.setText(title);
		valueView.setText(value);
		detailLayout.addView(view);
	}

	private void addItemWebPage(String title, String value) {
		View view = layoutInflater.inflate(R.layout.item_detail, null);
		TextView tView = (TextView) view.findViewById(R.id.item_detail_title);
		TextView valueView = (TextView) view.findViewById(R.id.item_detail_value);
		tView.setText(title);
		// linkMaskはsetTextの前に設定する
		valueView.setAutoLinkMask(Linkify.WEB_URLS);
		valueView.setText(value);
		detailLayout.addView(view);
	}

	private void loadImage(String url) {
		(new ImageLoadTask(url, image, this) {
			@Override
			protected void onPostExecute(Boolean result) {
				if (bitmap != null) {
					image.setImageBitmap(bitmap);
				} else {
					image.setImageResource(R.drawable.loadfailure);
				}
				progressBar.setVisibility(View.GONE);
				// int s = getPxFromDp(context, 200);
				// image.setLayoutParams(new
				// LinearLayout.LayoutParams(s, s));

			}
		}).execute();
	}
}
