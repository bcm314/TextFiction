package de.onyxbits.textfiction.input;

import java.io.File;
import java.io.PrintWriter;

import org.json.JSONArray;

import de.onyxbits.textfiction.FileUtil;
import de.onyxbits.textfiction.R;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.ViewFlipper;
import android.widget.Toast; // ###BCM-Debug###

/**
 * UI interface between the player and the engine's input buffer.
 */
public class InputFragment extends Fragment implements OnClickListener,
		OnEditorActionListener {

	/**
	 * Name of the file (relative to the gamedata dir where to quick commmands
	 * settings are kept.
	 */
	public static final String CMDFILE = "quickcommands.json";

	private EditText cmdLine;
	private ImageButton submit;
	private ImageButton expand;
	private LinearLayout buttonBar;
	private ViewFlipper flipper;

	private ImageButton forwards;
	private ImageButton left;
	private ImageButton right;
	private ImageButton up;
	private ImageButton down;

	// Magic numbers! See: §3.8 of the zmachine standard document.
	public static final char[] C_UP = { 129 };
	public static final char[] C_DOWN = { 130 };
	public static final char[] C_LEFT = { 131 };
	public static final char[] C_RIGHT = { 132 };
	public static final char[] ENTER = { 13 };
	public static final char[] DELETE = { 8 };

	private boolean hasVerb = false;

	private InputProcessor inputProcessor;
	private boolean autoCollapse;

	public InputFragment() {
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		flipper = (ViewFlipper) inflater.inflate(R.layout.fragment_input,
				container, false);
		buttonBar = (LinearLayout) flipper.findViewById(R.id.quickcmdcontainer);
		buttonBar.setTag("");
		cmdLine = (EditText) flipper.findViewById(R.id.userinput);
		submit = (ImageButton) flipper.findViewById(R.id.submit);
		expand = (ImageButton) flipper.findViewById(R.id.expand);
		submit.setOnClickListener(this);
		cmdLine.setOnEditorActionListener(this);
		expand.setOnClickListener(this);

		forwards = (ImageButton) flipper.findViewById(R.id.forwards);
		down = (ImageButton) flipper.findViewById(R.id.cursor_down);
		left = (ImageButton) flipper.findViewById(R.id.cursor_left);
		right = (ImageButton) flipper.findViewById(R.id.cursor_right);
		up = (ImageButton) flipper.findViewById(R.id.cursor_up);

		forwards.setOnClickListener(this);
		down.setOnClickListener(this);
		left.setOnClickListener(this);
		right.setOnClickListener(this);
		up.setOnClickListener(this);

		((KeyboardButton) flipper.findViewById(R.id.keyboard))
				.setInputProcessor(inputProcessor);
		
//		File commands = new File(FileUtil.getDataDir(inputProcessor.getStory()),
//				CMDFILE);
		File commands = new File(FileUtil.getDataDir(inputProcessor.getStory()),
				"quickcommands" + (String)buttonBar.getTag() + ".json");

		Context ctx = getActivity();
//		CommandChanger changer = new CommandChanger(cmdLine, buttonBar, commands);
		CommandChanger changer = new CommandChanger(cmdLine, buttonBar, 
				FileUtil.getDataDir(inputProcessor.getStory()));

		try {
			String buttonDef = getActivity().getString(R.string.defaultcommands);
			JSONArray buttons = new JSONArray(buttonDef);
			if (commands.exists()) {
				buttonDef = FileUtil.getContents(commands);
				buttons = new JSONArray(buttonDef);
			}
			for (int i = 0; i < buttons.length(); i++) {
				ImageButton b = (ImageButton) inflater.inflate(
						R.layout.style_cmdbutton, null).findViewById(R.id.protocmdbutton);
				CmdIcon ico = CmdIcon.fromJSON(buttons.getJSONObject(i));
				b.setTag(ico);
				b.setImageResource(CmdIcon.ICONS[ico.imgid]);
				b.setOnClickListener(this);
				b.setOnLongClickListener(changer);
				b.setContentDescription(ico.cmd);
				buttonBar.addView(b);
			}
		}
		catch (Exception e) {
			Log.w(getClass().getName(), e);
		}
		flipper.setInAnimation(AnimationUtils.loadAnimation(ctx,
				R.animator.slide_in_right));
		flipper.setOutAnimation(AnimationUtils.loadAnimation(ctx,
				R.animator.slide_out_left));

		return flipper;
	}

	/**
	 * Set whether or not the keyboard should collapse when the "DONE" button is
	 * pressed.
	 * 
	 * @param ac
	 *          true to automatically collapse the keyboard on DONE
	 */
	public void setAutoCollapse(boolean ac) {
		autoCollapse = ac;
	}

	/**
	 * Toggle between commandline and keypress input
	 */
	public void toggleInput() {
		flipper.showNext();
	}

	/**
	 * Query the input style
	 * 
	 * @return true if showing the commandline
	 */
	public boolean isPrompt() {
		return (flipper.getCurrentView().getId() == R.id.inputcontainer);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			inputProcessor = (InputProcessor) activity;
		}
		catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnArticleSelectedListener");
		}
	}

	@Override
	public void onClick(View v) {
		if (v == submit) {
			executeCommand();
			return;
		}
		if (v == expand) {
			getActivity().openOptionsMenu();
			return;
		}
		if (v == forwards) {
			inputProcessor.executeCommand(ENTER);
		}
		if (v == up) {
			inputProcessor.executeCommand(C_UP);
		}
		if (v == down) {
			inputProcessor.executeCommand(C_DOWN);
		}
		if (v == left) {
			inputProcessor.executeCommand(C_LEFT);
		}
		if (v == right) {
			inputProcessor.executeCommand(C_RIGHT);
		}

		if (v.getTag() instanceof CmdIcon) {
			CmdIcon ci = (CmdIcon) v.getTag();
			
			/*
			if (ci.atOnce) {
				inputProcessor.executeCommand((ci.cmd + "\n").toCharArray());
			}
			else {
				// Allow the player to select either the verb or an object first. If
				// an object is selected first and a verb second, assume the input to
				// be finished and execute it. The other way around: allow more objects
				// to be added.
				String tmp = "";
				if (hasVerb == false) {
					tmp = cmdLine.getEditableText().toString().trim();
				}
				cmdLine.setText(ci.cmd.trim() + " " + tmp);
				cmdLine.setSelection(cmdLine.getEditableText().toString().length());
				hasVerb = true;
				if (tmp.length() > 0) {
					executeCommand();
				}
			}
			 */
			
			
			
			// ### will redo this part ...

			// short-click on "unconfigured" button will be ignored
			if (ci.imgid < 0)
				return;
			if ((CmdIcon.ICONS[ci.imgid] == R.drawable.ic_action_space) ||
				(CmdIcon.ICONS[ci.imgid] == R.drawable.ic_action_empty))
				return;
			
			String tmp;
			String tmpNew;
			boolean atOnce;
			
			tmpNew=ci.cmd.trim() + " ";
			atOnce=ci.atOnce;
			if (tmpNew!=tmpNew.replace("$", "")) {
				atOnce=true;
				tmpNew=tmpNew.replace("$", "");
			}
			if (tmpNew.charAt(0)=='^') {
				tmp = "";
				tmpNew=tmpNew.substring(1).trim();
			}
			else {
				tmp = cmdLine.getEditableText().toString().trim();
				tmpNew=tmpNew.trim();
			}
			
			if (hasVerb == false)
				tmpNew=tmpNew + " " + tmp;
			else
				tmpNew=tmp.trim() + " " + tmpNew;
			
			tmpNew=tmpNew.trim();
			if (!tmpNew.equals(""))
				tmpNew=tmpNew+" ";
			
			cmdLine.setText(tmpNew);
			cmdLine.setSelection(cmdLine.getEditableText().toString().length());	
			
			if (atOnce)
				tmp="do";
			else if (hasVerb || (ci.cmd.trim().equals("")))
				tmp=""; 
			
			if (!ci.cmd.trim().equals(""))
				hasVerb = true;
			
		
			
			for (int i = 0 ; i < buttonBar.getChildCount() ; i++) {
				if (ci == (buttonBar.getChildAt(i).getTag())) {
					UpdateCmdButtons(i);
					break;
				}
			}				
				
			if (tmp.length() > 0) {
				executeCommand();
			}	
			
		}
	}

	/**
	 * Call after running a command to clear the commandline
	 */
	public void reset() {
		cmdLine.setText("");
		hasVerb = false;
	}

	/**
	 * Append a "word" to the prompt.
	 * 
	 * @param str
	 *          the string to add (does not require whitespaces at the start or
	 *          end).
	 */
	public void appendWord(String str) {
		if (str.length() > 0) {
			String tmp = cmdLine.getText().toString().trim();
			tmp = tmp.trim() + " " + str.trim();
			tmp = tmp.trim();
			if (tmp != "")
				tmp = tmp + " ";
			cmdLine.setText(tmp);
			cmdLine.setSelection(tmp.length());
		}
	}

	/**
	 * Delete the rightmost word on the prompt.
	 */
	public void removeWord() {
		String tmp = cmdLine.getText().toString().trim();
		int idx = tmp.lastIndexOf(' ');
		if (idx > 0) {
			tmp = tmp.substring(0, idx);
			tmp = tmp.trim();
			if (tmp != "")
				tmp = tmp + " ";
			cmdLine.setText(tmp);
			cmdLine.setSelection(tmp.length());
		}
		else {
			reset();
		}
		
		UpdateCmdButtons(-1);
	}

	@Override
	public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
		executeCommand();
		return !autoCollapse;
	}

	private void executeCommand() {

		// Restore top-level-buttons		
		UpdateCmdButtons(-2);	
		
		inputProcessor.executeCommand((cmdLine.getText().toString() + "\n")
				.toCharArray());
	}

	private void UpdateCmdButtons(int buttonNr) {
		String menuPath;
		File commands;
		String buttonDef;
		
		menuPath = (String)buttonBar.getTag();
		if (buttonNr >= 0)
			menuPath = menuPath + "_" + buttonNr;
		else if (buttonNr == -1)
		{
			int idx = menuPath.lastIndexOf('_');
			if (idx > 0) {
				menuPath = menuPath.substring(0, idx-1);
			}
		}
		else
			menuPath = "";

		buttonBar.setTag(menuPath);

//		Toast.makeText(getActivity(),"menuPath=" + menuPath, Toast.LENGTH_LONG).show(); // ###BCM-Debug###
		
		commands = new File(FileUtil.getDataDir(inputProcessor.getStory()),
				"quickcommands" + (String)buttonBar.getTag() + ".json");

		try {
			if (menuPath.equals(""))
				buttonDef = getActivity().getString(R.string.defaultcommands);
			else if (menuPath.equals("_0"))
				buttonDef = getActivity().getString(R.string.defaultcommands_0);
			else if (menuPath.equals("_1"))
				buttonDef = getActivity().getString(R.string.defaultcommands_1);
			else
				buttonDef = getActivity().getString(R.string.emptycommands);
			
			JSONArray buttons = new JSONArray(buttonDef);
			if (commands.exists()) {
				buttonDef = FileUtil.getContents(commands);
				buttons = new JSONArray(buttonDef);
			}
			for (int i = 0; i < buttons.length(); i++) {
				ImageButton b = (ImageButton)buttonBar.getChildAt(i);			
				CmdIcon icoButton = (CmdIcon)b.getTag();

				CmdIcon icoFile = CmdIcon.fromJSON(buttons.getJSONObject(i));

				icoButton.imgid=icoFile.imgid;
				icoButton.cmd=icoFile.cmd;
				icoButton.atOnce=icoFile.atOnce;
				
				if (icoButton.imgid >= 0)
					b.setImageResource(CmdIcon.ICONS[icoButton.imgid]);
				else if (icoButton.imgid == -2)
					b.setImageResource(R.drawable.ic_action_empty);
				else
					b.setImageResource(android.R.color.transparent);
				
				b.setContentDescription(icoButton.cmd);
			}
		}
		catch (Exception e) {
			Log.w(getClass().getName(), e);
		}
		
	}
}
