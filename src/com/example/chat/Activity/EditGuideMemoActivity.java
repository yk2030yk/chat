package com.example.chat.Activity;

import com.example.chat.Loading;
import com.example.chat.R;
import com.example.chat.R.id;
import com.example.chat.R.layout;
import com.example.chat.backgroundTask.PostServerTask;
import com.example.chat.common.IntentKey;
import com.example.chat.common.URLManager;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EditGuideMemoActivity extends Activity {
	private String guideId = "";
	private Context context;
	private Loading loading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_edit_guide_memo);
		getActionBar().hide();

		context = this;

		final EditText et = (EditText) findViewById(R.id.editText1);
		Button btn = (Button) findViewById(R.id.button1);

		guideId = getIntent().getStringExtra(IntentKey.GUIDE_ID);
		String memo = getIntent().getStringExtra("Memo");
		et.setText(memo);

		loading = new Loading(this, "メモの保存中です...");

		btn.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String memo = et.getText().toString();
				if (!memo.equals("")) {
					loading.show();

					PostServerTask task = new PostServerTask(URLManager.EDIT_GUIDE_MEMO_URL) {

						@Override
						protected void onPostExecute(Boolean result) {
							super.onPostExecute(result);
							if (this.taskResult) {
								finish();
							} else {
								Toast.makeText(context, "保存に失敗しました", Toast.LENGTH_SHORT).show();
							}
							loading.hide();
						}

					};
					task.setPostData("GuideId", guideId);
					task.setPostData("Memo", memo);
					task.execute();
				}
			}
		});
	}

}
