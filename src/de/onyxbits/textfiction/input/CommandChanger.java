package de.onyxbits.textfiction.input;

import java.io.File;
import java.io.PrintWriter;

import org.json.JSONArray;

import de.onyxbits.textfiction.R;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Allows the user to redefine the command buttons in the inputfragment. Just
 * bind an object of this class as an OnLongClickListener to the button in
 * question and make sure the tag of the button is an index into CmdIcon.ICONS.
 * 
 * @author patrick
 * 
 */
class CommandChanger implements OnItemClickListener, OnLongClickListener {

	private CmdIcon cmdIcon;
	// BCM private CheckBox atOnce;
	private String text;
	private AlertDialog dialog;
	private TextView cmdLine;
	private ImageView target;
	private LinearLayout buttonBar;
	private File storyDir;

	public CommandChanger(TextView cmdLine, LinearLayout buttonBar, File storyDir) {
		this.cmdLine = cmdLine;
		this.buttonBar = buttonBar;
		this.storyDir=storyDir;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		// BCM cmdIcon.atOnce = atOnce.isChecked();
		cmdIcon.cmd = text;
		cmdIcon.imgid = ((Integer)view.getTag()).intValue();
		target.setImageResource(CmdIcon.ICONS[position]);
		dialog.dismiss();
		try {
			JSONArray array = new JSONArray();
			for (int i = 0; i < buttonBar.getChildCount(); i++) {
				array.put(CmdIcon.toJSON((CmdIcon)buttonBar.getChildAt(i).getTag()));
			}
			PrintWriter pw = new PrintWriter(new File(storyDir, "quickcommands" + (String)buttonBar.getTag() + ".json"));
			
			pw.write(array.toString(2));
			pw.close();
		}
		catch (Exception e) {
			Log.w(getClass().getName(), e);
		}
	}

	@Override
	public boolean onLongClick(View v) {

		Context ctx = v.getContext();
		text = cmdLine.getText().toString();

		if (text.length() > 0) {
			text = cmdLine.getEditableText().toString();
			LayoutInflater li = (LayoutInflater) ctx
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			cmdIcon = (CmdIcon) v.getTag();
			View layout = li.inflate(R.layout.quickcmdsettings, null);
			GridView gridView = (GridView) layout.findViewById(R.id.iconselect);
			// BCM atOnce = (CheckBox) layout.findViewById(R.id.executeatonce);
			gridView.setAdapter(new IconAdapter(ctx));
			TextView txt = (TextView) layout.findViewById(R.id.replacementcmd);
			txt.setText("'" + text.trim() + "'");
			gridView.setOnItemClickListener(this);
			// BCM atOnce.setChecked(cmdIcon.atOnce);
			target = (ImageView) v;
			AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
			dialog = builder.setTitle(R.string.title_change_commmand).setView(layout)
					.create();
			dialog.show();
		}
		else {
			Toast.makeText(ctx, ctx.getString(R.string.msg_no_cmd),
					Toast.LENGTH_SHORT).show();
		}
		return true;
	}

}
