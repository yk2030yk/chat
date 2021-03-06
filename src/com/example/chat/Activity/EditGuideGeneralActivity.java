package com.example.chat.Activity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;

import com.example.chat.R;
import com.example.chat.R.id;
import com.example.chat.R.layout;
import com.example.chat.api.JalanApiURLCreator;
import com.example.chat.backgroundTask.PostServerTask;
import com.example.chat.bean.Hotel;
import com.example.chat.common.IntentKey;
import com.example.chat.common.URLManager;
import com.example.chat.common.XmlReader;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Toast;

public class EditGuideGeneralActivity extends Activity {
	private final int REQUEST_CODE = 111;

	private String guideId = "";
	private String title = "";
	private String hotelId = "";
	private String hotelName = "";

	public Context context;
	private EditText titleEditText;
	private Button sDateButton;
	private Button eDateButton;
	private Button hotelButton;
	private Button createButton;

	private DatePickerDialog.OnDateSetListener sDateListener;
	private DatePickerDialog.OnDateSetListener eDateListener;
	private Calendar sCalendar;
	private Calendar eCalendar;

	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日(E)", Locale.JAPAN);
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.JAPAN);
	private ArrayList<String> errors = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create_guide);
		getActionBar().hide();

		context = this;
		guideId = getIntent().getExtras().getString(IntentKey.GUIDE_ID);

		titleEditText = (EditText) findViewById(R.id.editText_title);
		sDateButton = (Button) findViewById(R.id.button_date_start);
		eDateButton = (Button) findViewById(R.id.button_date_end);
		hotelButton = (Button) findViewById(R.id.button_select_hotel);
		createButton = (Button) findViewById(R.id.button_create);

		sCalendar = Calendar.getInstance();
		eCalendar = Calendar.getInstance();
		sDateButton.setText(sdf.format(sCalendar.getTime()));
		eDateButton.setText(sdf.format(eCalendar.getTime()));

		sDateListener = new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int month, int day) {
				sCalendar.set(year, month, day);
				sDateButton.setText(sdf.format(sCalendar.getTime()));
			}
		};

		eDateListener = new DatePickerDialog.OnDateSetListener() {
			@Override
			public void onDateSet(DatePicker view, int year, int month, int day) {
				eCalendar.set(year, month, day);
				eDateButton.setText(sdf.format(eCalendar.getTime()));
			}
		};

		sDateButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				DatePickerDialog datePickerDialog = new DatePickerDialog(context, sDateListener, sCalendar.get(Calendar.YEAR), sCalendar.get(Calendar.MONTH), sCalendar.get(Calendar.DAY_OF_MONTH));
				datePickerDialog.show();
			}
		});

		eDateButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				DatePickerDialog datePickerDialog = new DatePickerDialog(context, eDateListener, eCalendar.get(Calendar.YEAR), eCalendar.get(Calendar.MONTH), eCalendar.get(Calendar.DAY_OF_MONTH));
				datePickerDialog.show();
			}
		});

		hotelButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent i = new Intent(EditGuideGeneralActivity.this, SelectGuideHotelActivity.class);
				startActivityForResult(i, REQUEST_CODE);
			}
		});

		createButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (!validation()) {
					String groupId = "1";
					title = titleEditText.getText().toString();

					Calendar calendar = Calendar.getInstance();
					String endDay = dateFormat.format(eCalendar.getTime());
					calendar.set(sCalendar.get(Calendar.YEAR), sCalendar.get(Calendar.MONTH), sCalendar.get(Calendar.DAY_OF_MONTH));

					ArrayList<String> days = new ArrayList<String>();
					String day = dateFormat.format(calendar.getTime());
					days.add(day);
					while (!day.equals(endDay)) {
						calendar.add(Calendar.DAY_OF_MONTH, 1);
						day = dateFormat.format(calendar.getTime());
						days.add(day);
					}

					PostServerTask task = new PostServerTask(URLManager.EDIT_GUIDE_URL) {

						@Override
						protected void onPostExecute(Boolean result) {
							super.onPostExecute(result);
							if (this.taskResult) {
								Toast.makeText(context, "登録に成功しました", Toast.LENGTH_SHORT).show();
								setResult(RESULT_OK);
								finish();
							}
						}

					};
					task.setPostData("GroupId", groupId);
					task.setPostData("GuideId", guideId);
					task.setPostData("HotelId", hotelId);
					task.setPostData("Title", title);
					task.setPostData("StayCount", days.size());
					for (int i = 0; i < days.size(); i++) {
						task.setPostData("Days[]", days.get(i));
					}
					task.execute();
				} else {
					showErrors();
				}
			}
		});

		PostServerTask task = new PostServerTask(URLManager.GET_GUIDE_VALUE_URL) {

			@Override
			protected void onPostExecute(Boolean result) {
				super.onPostExecute(result);
				XmlReader xr = new XmlReader(this.httpData);
				if (xr.getGuideId().size() > 0) {
					HashMap<String, String> map = xr.getGuideId().get(0);
					titleEditText.setText(map.get("GuideName"));
					hotelId = map.get("HotelId");

					String sDate[] = map.get("StartDay").split("-");
					sCalendar.set(Integer.parseInt(sDate[0]), Integer.parseInt(sDate[1]) - 1, Integer.parseInt(sDate[2]));
					sDateButton.setText(sdf.format(sCalendar.getTime()));

					String eDate[] = map.get("EndDay").split("-");
					eCalendar.set(Integer.parseInt(eDate[0]), Integer.parseInt(eDate[1]) - 1, Integer.parseInt(eDate[2]));
					eDateButton.setText(sdf.format(eCalendar.getTime()));

					JalanApiURLCreator j = new JalanApiURLCreator();
					j.setHotelID(hotelId);
					PostServerTask task = new PostServerTask(j.createHotelURL()) {

						@Override
						protected void onPostExecute(Boolean result) {
							super.onPostExecute(result);
							XmlReader xr = new XmlReader(this.httpData);
							if (xr.getHotelData().size() > 0) {
								Hotel h = xr.getHotelData().get(0);
								hotelButton.setText(h.name);
							}
						}
					};
					task.execute();
				}

			}

		};
		task.setPostData("GuideId", guideId);
		task.execute();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
		case REQUEST_CODE:
			if (resultCode == RESULT_OK) {
				hotelId = data.getExtras().getString(IntentKey.HOTEL_ID);
				hotelName = data.getExtras().getString(IntentKey.HOTEL_NAME);
				hotelButton.setText(hotelName + hotelId);
			}
			break;

		default:
			break;
		}
	}

	private boolean checkDateValidation(Calendar sCal, Calendar eCal) {
		if (sCal.before(eCal) || sCal.equals(eCal)) {
			return true;
		} else {
			return false;
		}
	}

	private boolean validation() {
		boolean result = false;
		errors.clear();

		String title = titleEditText.getText().toString();
		if (title == null || title.equals("")) {
			errors.add("・タイトルを入力してください");
			result = true;
		}

		if (hotelId == null || hotelId.equals("")) {
			errors.add("・宿を選択してください");
			result = true;
		}

		// if (!checkDateValidation(nowCalendar, sCalendar)) {
		// errors.add("・今日より前の日付は出発日に設定できません");
		// result = true;
		// }

		if (!checkDateValidation(sCalendar, eCalendar)) {
			errors.add("・帰宅日は出発日よりあとにしてください");
			result = true;
		}

		return result;
	}

	private void showErrors() {
		String myError = "";
		for (int i = 0; i < errors.size(); i++) {
			myError += errors.get(i) + "\n";
		}
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle("入力エラー");
		alertDialogBuilder.setMessage(myError);
		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}
}
