package com.example.chat.Fragment;

import java.util.ArrayList;
import java.util.HashMap;

import com.example.chat.R;
import com.example.chat.Activity.FavoriteSelectActivity;
import com.example.chat.Activity.GourmetDialogActivity;
import com.example.chat.R.id;
import com.example.chat.R.layout;
import com.example.chat.api.LocaTouchApiURLCreator;
import com.example.chat.backgroundTask.PostServerTask;
import com.example.chat.bean.Gourmet;
import com.example.chat.common.IntentKey;
import com.example.chat.common.URLManager;
import com.example.chat.common.XmlReader;
import com.example.chat.googlemap.Point;
import com.example.chat.image.ImageFileHelper;
import com.example.chat.image.ImageLoadTask;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class FavoriteSelectGourmetFragment extends Fragment {
	private Context context;
	private ArrayList<Point> gourmets = new ArrayList<Point>();
	private static MyCustomAdapter adapter;
	private View frgLayout;
	private ListView listView;
	private final String SAVE_KEY = "GROUMETS";
	private Vibrator vibrator;
	public ImageFileHelper imageFileHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		frgLayout = inflater.inflate(R.layout.fragment_favorite, container, false);
		return frgLayout;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		context = getActivity().getApplicationContext();
		vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		imageFileHelper = new ImageFileHelper(context);

		if (savedInstanceState != null) {
			gourmets = (ArrayList<Point>) savedInstanceState.getSerializable(SAVE_KEY);
		}
	}

	public void onStart() {
		super.onStart();
		listView = (ListView) frgLayout.findViewById(R.id.listView1);
		adapter = new MyCustomAdapter(context, gourmets);
		listView.setAdapter(adapter);

		if (gourmets.isEmpty()) {
			PostServerTask task = new PostServerTask(URLManager.GET_FAVORITE_GOURMET_URL) {

				@Override
				protected void onPostExecute(Boolean result) {
					super.onPostExecute(result);
					XmlReader xr = new XmlReader(this.httpData);
					ArrayList<HashMap<String, String>> list = xr.getFavoriteGourmetId();
					for (int i = 0; i < list.size(); i++) {
						HashMap<String, String> g = list.get(i);
						Gourmet gourmet = new Gourmet();
						gourmet.gourmetId = g.get("GourmetId");
						gourmet.name = g.get("Name");
						gourmet.lat = Double.parseDouble(g.get("Lat"));
						gourmet.lng = Double.parseDouble(g.get("Lng"));
						adapter.add(new Point(gourmet));
					}
				}
			};
			task.execute();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(SAVE_KEY, gourmets);
		Log.d("saveInstavceState", "セーブされたよ");
	}

	private class MyCustomAdapter extends ArrayAdapter<Point> {
		public Context mycontext;
		private LayoutInflater layoutInflater;
		private ArrayList<Point> arrayList;
		private int viewResId = R.layout.item_select_favorite;
		private int iconSize;

		public MyCustomAdapter(Context context, ArrayList<Point> list) {
			super(context, 0, list);
			mycontext = context;
			layoutInflater = (LayoutInflater) mycontext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			arrayList = list;
			iconSize = ImageLoadTask.getPxFromDp(context, 48);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = convertView;
			if (view == null) {
				view = layoutInflater.inflate(viewResId, null);
			}

			final Point itemData = arrayList.get(position);
			final TextView nameText = (TextView) view.findViewById(R.id.item_favorite_content);
			final TextView addButton = (TextView) view.findViewById(R.id.item_favorite_del);
			final ImageView image = (ImageView) view.findViewById(R.id.item_favorite_image);
			nameText.setText(itemData.name);

			String fileName = Gourmet.GOURMET_IMAGE_FILE_NAME + itemData.id;
			Bitmap bitmap = imageFileHelper.loadBitmap(fileName, iconSize, iconSize);
			if (bitmap != null) {
				image.setImageBitmap(bitmap);
			} else {
				loadImage(itemData.id, fileName);
			}

			final FavoriteSelectActivity activity = (FavoriteSelectActivity) getActivity();
			if (activity.routePoints.contains(itemData)) {
				int rank = activity.routePoints.indexOf(itemData) + 1;
				addButton.setText("" + rank);
			} else {
				addButton.setText("");
			}

			addButton.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					vibrator.vibrate(10);
					if (activity.routePoints.contains(itemData)) {
						activity.routePoints.remove(itemData);
					} else {
						activity.routePoints.add(itemData);
					}
					adapter.notifyDataSetChanged();
				}
			});

			nameText.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent i = new Intent(context, GourmetDialogActivity.class);
					i.putExtra(IntentKey.GOURMET_ID, itemData.id);
					startActivity(i);
				}
			});

			return view;
		}

		private void loadImage(String id, String f) {
			final String fileName = f;

			LocaTouchApiURLCreator c = new LocaTouchApiURLCreator();
			c.setId(id);
			String url = c.createUrl();

			PostServerTask task = new PostServerTask(url) {

				@Override
				protected void onPostExecute(Boolean result) {
					super.onPostExecute(result);
					XmlReader xr = new XmlReader(this.httpData);
					ArrayList<Gourmet> list = xr.getGourmetLocaTouch();
					if (!list.isEmpty()) {
						Gourmet g = list.get(0);
						ImageLoadTask load = new ImageLoadTask(g.imageURL, context) {
							@Override
							protected void onPostExecute(Boolean result) {
								super.onPostExecute(result);
								saveLocalFile(fileName);
								adapter.notifyDataSetChanged();
							}
						};
						load.execute();
					}
				}
			};
			task.execute();
		}
	}
}
