package de.onyxbits.textfiction;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.Iterator;

import org.json.JSONArray;

import de.onyxbits.textfiction.input.CompassFragment;
import de.onyxbits.textfiction.input.InputFragment;
import de.onyxbits.textfiction.input.InputProcessor;
import de.onyxbits.textfiction.input.WordExtractor;
import de.onyxbits.textfiction.zengine.GrueException;
import de.onyxbits.textfiction.zengine.StyleRegion;
import de.onyxbits.textfiction.zengine.ZMachine;
import de.onyxbits.textfiction.zengine.ZState;
import de.onyxbits.textfiction.zengine.ZStatus;
import de.onyxbits.textfiction.zengine.ZWindow;

import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Build;
import android.preference.PreferenceManager;

/**
 * The activity where actual gameplay takes place.
 * 
 * @author patrick
 * 
 */
public class GameActivity extends FragmentActivity implements DialogInterface.OnClickListener,
		OnInitListener, OnSharedPreferenceChangeListener, InputProcessor {

	/**
	 * Name of the file we keep our highlights in
	 */
	public static final String HIGHLIGHTFILE = "highlights.json";

	/**
	 * This activity must be started through an intent and be passed the filename
	 * of the game via this extra.
	 */
	public static final String LOADFILE = "loadfile";

	/**
	 * How many items to keep in the messagebuffer at most. Note: this should be
	 * an odd number so the log starts with a narrator entry.
	 */
	public static final int MAXMESSAGES = 81;

	private static final int PENDING_NONE = 0;
	private static final int PENDING_RESTART = 1;
	private static final int PENDING_RESTORE = 2;
	private static final int PENDING_SAVE = 3;

	/**
	 * Displays the message log
	 */
	private ListView storyBoard;

	/**
	 * Adapter for the story list
	 */
	private StoryAdapter messages;

	/**
	 * The "upper window" of the z-machine containing the status part
	 */
	private TextView statusWindow;

	/**
	 * Holds stuff that needs to survive config changes (e.g. screen rotation).
	 */
	private RetainerFragment retainerFragment;

	/**
	 * The input prompt
	 */
	private InputFragment inputFragment;

	/**
	 * On screen compass
	 */
	private CompassFragment compassFragment;

	/**
	 * Contains story- and status screen.
	 */
	private ViewFlipper windowFlipper;

	/**
	 * For entering a filename to save the current game as.
	 */
	private EditText saveName;

	/**
	 * The game playing in this activity
	 */
	private File storyFile;

	/**
	 * State variable for when we are showing a "confirm" dialog.
	 */
	private int pendingAction = PENDING_NONE;

	/**
	 * Words we are highligting in the story
	 */
	private String[] highlighted;

	/**
	 * Language of the story
	 */
	private String storyLanguage;

	class storyLanguageClass
	{
	    public int count; 
	    public int en;  
	    public int de; 
	    public int es;  
	};
	storyLanguageClass storyLanguageData;
	
	private SharedPreferences prefs;
	private TextToSpeech speaker;
	private boolean ttsReady;
	private WordExtractor wordExtractor;

	private ProgressBar loading;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// by default first guess, story language is English
		storyLanguage="en";
		storyLanguageData = new storyLanguageClass ();
		storyLanguageData.count = 0;
		storyLanguageData.en = 0;
		storyLanguageData.de = 0;
		storyLanguageData.es = 0;

		prefs = PreferenceManager.getDefaultSharedPreferences(this);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);

		// Set the custom theme
		try {
			Field field = R.style.class.getField(prefs.getString("theme", ""));
			setTheme(field.getInt(null));
		}
		catch (Exception e) {
			Log.w(getClass().getName(), e);
		}

		prefs.registerOnSharedPreferenceChangeListener(this);

		super.onCreate(savedInstanceState);
		LayoutInflater infl = getLayoutInflater();
		requestWindowFeature(Window.FEATURE_PROGRESS);
		storyFile = new File(getIntent().getStringExtra(LOADFILE));

		View content = infl.inflate(R.layout.activity_game, null);
		setContentView(content);

		// Check if this is a genuine start or if we are restarting because the
		// device got rotated.
		FragmentManager fm = getSupportFragmentManager();

		inputFragment = (InputFragment) fm.findFragmentById(R.id.fragment_input);
		compassFragment = (CompassFragment) fm.findFragmentById(R.id.fragment_compass);
		retainerFragment = (RetainerFragment) fm.findFragmentByTag("retainer");
		if (retainerFragment == null) {
			// First start
			retainerFragment = new RetainerFragment();
			fm.beginTransaction().add(retainerFragment, "retainer").commit();
		}
		else {
			// Likely a restart because of the screen being rotated. This may have
			// happened while loading, so don't figure if we don't have an engine.
			if (retainerFragment.engine != null) {
				figurePromptStyle();
				figureMenuState();
			}
		}

		// Load the highlight file
		try {
			File file = new File(FileUtil.getDataDir(storyFile), HIGHLIGHTFILE);
			JSONArray js = new JSONArray(FileUtil.getContents(file));
			for (int i = 0; i < js.length(); i++) {
				retainerFragment.highlighted.add(js.getString(i));
			}
		}
		catch (Exception e) {
			// No big deal. Probably the first time this game runs -> use defaults
			String[] ini = getResources().getStringArray(R.array.initial_highlights);
			for (String i : ini) {
				retainerFragment.highlighted.add(i);
			}
		}
		highlighted = retainerFragment.highlighted.toArray(new String[0]);

		storyBoard = (ListView) content.findViewById(R.id.storyboard);
		// storyBoard.setVerticalFadingEdgeEnabled(true);
		wordExtractor = new WordExtractor(this);
		wordExtractor.setInputFragment(inputFragment);
		wordExtractor.setInputProcessor(this);
		messages = new StoryAdapter(this, 0, retainerFragment.messageBuffer, wordExtractor);

		storyBoard.setAdapter(messages);

		windowFlipper = (ViewFlipper) content.findViewById(R.id.window_flipper);
		statusWindow = (TextView) content.findViewById(R.id.status);
		loading = (ProgressBar) findViewById(R.id.gameloading);
		statusWindow.setText(retainerFragment.upperWindow);

		speaker = new TextToSpeech(this, this);
		onSharedPreferenceChanged(prefs, "");
		dimSoftButtonsIfPossible();
	}

	@SuppressLint("NewApi")
	private void dimSoftButtonsIfPossible() {
		int currentapiVersion = android.os.Build.VERSION.SDK_INT;
		if (currentapiVersion >= android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		dimSoftButtonsIfPossible();
	}

	@Override
	public void onPause() {
		if (ttsReady && speaker.isSpeaking()) {
			speaker.stop();
		}
		super.onPause();
	}

	@Override
	public void onDestroy() {
		prefs.unregisterOnSharedPreferenceChangeListener(this);
		if (ttsReady) {
			speaker.shutdown();
		}

		if (retainerFragment == null || retainerFragment.engine == null) {
			if (retainerFragment.postMortem != null) {
				// Let's not go into details here. The user won't understand them
				// anyways.
				Toast.makeText(this, R.string.msg_corrupt_game_file, Toast.LENGTH_SHORT).show();
			}
			super.onDestroy();
			return;
		}

		if (retainerFragment.postMortem != null) {
			// Let's not go into details here. The user won't understand them anyways.
			Toast.makeText(this, R.string.msg_corrupt_game_file, Toast.LENGTH_SHORT).show();
			super.onDestroy();
			return;
		}

		if (retainerFragment.engine.getRunState() == ZMachine.STATE_WAIT_CMD) {
			ZState state = new ZState(retainerFragment.engine);
			File f = new File(FileUtil.getSaveGameDir(storyFile), getString(R.string.autosavename));
			state.disk_save(f.getPath(), retainerFragment.engine.pc);
		}
		else {
			Toast.makeText(this, R.string.mg_not_at_a_commandprompt, Toast.LENGTH_LONG).show();
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.game, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		boolean rest = !(retainerFragment == null || retainerFragment.engine == null
				|| retainerFragment.engine.getRunState() == ZMachine.STATE_RUNNING || retainerFragment.engine
				.getRunState() == ZMachine.STATE_INIT);
		menu.findItem(R.id.mi_save).setEnabled(rest && inputFragment.isPrompt());
		menu.findItem(R.id.mi_restore).setEnabled(rest && inputFragment.isPrompt());
		menu.findItem(R.id.mi_restart).setEnabled(rest);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.mi_flip_view: {
				flipView(windowFlipper.getCurrentView() != storyBoard);
				return true;
			}
			case R.id.mi_save: {
				pendingAction = PENDING_SAVE;
				saveName = new EditText(this);
				saveName.setSingleLine(true);
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.title_save_game).setPositiveButton(android.R.string.ok, this)
						.setView(saveName).show();
				return true;
			}
			case R.id.mi_restore: {
				String[] sg = FileUtil.listSaveName(storyFile);
				if (sg.length > 0) {
					pendingAction = PENDING_RESTORE;
					AlertDialog.Builder builder = new AlertDialog.Builder(this);
					builder.setTitle(R.string.title_restore_game).setItems(sg, this).show();
				}
				else {
					Toast.makeText(this, R.string.msg_no_savegames, Toast.LENGTH_SHORT).show();
				}
				return true;
			}

			case R.id.mi_clear_log: {
				retainerFragment.messageBuffer.clear();
				messages.notifyDataSetChanged();
				return true;
			}
			case R.id.mi_WriteStoryLanguageFiles: {
				WriteStoryLangueSettings(storyLanguage);
				return true;
			}
			case R.id.mi_help: {
				MainActivity.openUri(this, Uri.parse(getString(R.string.url_help)));
				return true;
			}
			case R.id.mi_restart: {
				pendingAction = PENDING_RESTART;
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.title_please_confirm).setMessage(R.string.msg_really_restart)
						.setPositiveButton(android.R.string.yes, this)
						.setNegativeButton(android.R.string.no, this).show();
				return true;
			}

			case android.R.id.home:
				// This ID represents the Home or Up button. In the case of this
				// activity, the Up button is shown. Use NavUtils to allow users
				// to navigate up one level in the application structure. For
				// more details, see the Navigation pattern on Android Design:
				//
				// http://developer.android.com/design/patterns/navigation.html#up-vs-back
				//
				NavUtils.navigateUpFromSameTask(this);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void executeCommand(char[] inputBuffer) {
		ZMachine engine = retainerFragment.engine;
		if (engine != null && engine.getRunState() != ZMachine.STATE_RUNNING) {
			retainerFragment.engine.fillInputBuffer(inputBuffer);
			if (retainerFragment.engine.getRunState() != ZMachine.STATE_WAIT_CHAR) {
				String tmp = new String(inputBuffer).replaceAll("\n", "").trim();
				SpannableString ss = new SpannableString(tmp);
				retainerFragment.messageBuffer.add(new StoryItem(ss, StoryItem.MYSELF));
			}
			try {
				retainerFragment.engine.run();
				publishResult();
				if (retainerFragment.engine.saveCalled || retainerFragment.engine.restoreCalled) {
					// This is a really ugly hack to let the user know that the
					// save/restore commands
					// don't work
					Toast.makeText(this, R.string.err_sr_deprecated, Toast.LENGTH_LONG).show();
					retainerFragment.engine.saveCalled = false;
					retainerFragment.engine.restoreCalled = false;
				}
			}
			catch (GrueException e) {
				retainerFragment.postMortem = e;
				Log.w(getClass().getName(), e);
				finish();
			}
		}
	}

	/**
	 * Callback: publish results after the engine has run
	 */
	public void publishResult() {

		ZWindow upper = retainerFragment.engine.window[1];
		ZWindow lower = retainerFragment.engine.window[0];
		ZStatus status = retainerFragment.engine.status_line;
		String tmp = "";
		boolean showLower = false;

		// Evaluate game status
		if (status != null) {
			// Z3 game -> copy the status bar object into the upper window.
			retainerFragment.engine.update_status_line();
			retainerFragment.upperWindow = status.toString();
			statusWindow.setText(retainerFragment.upperWindow);
		}
		else {
			if (upper.maxCursor > 0) {
				// The normal, "status bar" upper window.
				tmp = upper.stringyfy(upper.startWindow, upper.maxCursor);
			}
			else {
				tmp = "";
			}
			
			String oldText = statusWindow.getText().toString();
			
			statusWindow.setText(tmp);
			retainerFragment.upperWindow = tmp;

			if (!tmp.equals(""))
				if (!tmp.equals(oldText)) {
					if ((tmp.indexOf('\n') == -1) && (tmp.indexOf('\r') == -1))
						tmp=tmp.trim(); // single line
					Toast toast = Toast.makeText(this, tmp, Toast.LENGTH_LONG);
				    toast.setGravity(Gravity.TOP, 0, 0);
				    toast.show();    					
				}
		
		}
		upper.retrieved();

		// Evaluate story progress
		if (lower.cursor > 0) {
			showLower = true;
			tmp = new String(lower.frameBuffer, 0, lower.noPrompt());
			
			// because we use bubbles, newline at beginning and end are not needed
			while ((" "+tmp).charAt(tmp.length()) == '\n') {
				tmp = tmp.substring(0,tmp.length()-1); 		
			}
			int CuttedLeft;
			CuttedLeft=0;
			while ((tmp+" ").charAt(0) == '\n')	{
				tmp = tmp.substring(1); 		
				CuttedLeft++;
			}

			AutoDetectLangue(tmp);
			
			if (ttsReady && prefs.getBoolean("narrator", false)) {
				speaker.speak(tmp, TextToSpeech.QUEUE_FLUSH, null);
			}
			SpannableString stmp = new SpannableString(tmp);
			StyleRegion reg = lower.regions;
			if (reg != null) {
				while (reg != null) {
					reg.start=reg.start-CuttedLeft;
					reg.end=reg.end-CuttedLeft;
					
					if (reg.next == null) {
						// The printer does not "close" the last style since it doesn't know
						// when the last character is printed.
						reg.end = tmp.length() - 1;
					}
					// Did the game style the prompt (which we cut away)?
					//??? reg.end = Math.min(reg.end, tmp.length() - 1);
					reg.end = Math.min(reg.end, tmp.length());
					
					switch (reg.style) {
						case ZWindow.BOLD: {
							stmp.setSpan(new StyleSpan(Typeface.BOLD), reg.start, reg.end, 0);
							break;
						}
						case ZWindow.ITALIC: {
							stmp.setSpan(new StyleSpan(Typeface.ITALIC), reg.start, reg.end, 0);
							break;
						}
						case ZWindow.FIXED: {
							stmp.setSpan(new TypefaceSpan("monospace"), reg.start, reg.end, 0);
							break;
						}
					}
					reg = reg.next;
				}
			}
			highlight(stmp, highlighted);
			try {
				retainerFragment.messageBuffer.add(new StoryItem(stmp, StoryItem.NARRATOR));
			}
			catch (IndexOutOfBoundsException e) {
				// This is a workaround! Some games manage to mess up the spans. It is
				// better to loose the styles than to crash the game.
				retainerFragment.messageBuffer.add(new StoryItem(new SpannableString(tmp),
						StoryItem.NARRATOR));
			}
		}
		lower.retrieved();

		// Throw out old story items.
		while (retainerFragment.messageBuffer.size() > MAXMESSAGES) {
			retainerFragment.messageBuffer.remove(0);
		}
		messages.notifyDataSetChanged();

		// Scroll the storyboard to the latest item.
		if (prefs.getBoolean("smoothscrolling", true)) {
			// NOTE:smoothScroll() does not work properly if the theme defines
			// dividerheight > 0!
			storyBoard.smoothScrollToPosition(retainerFragment.messageBuffer.size() - 1);
		}
		else {
			storyBoard.setSelection(retainerFragment.messageBuffer.size() - 1);
		}

		inputFragment.reset();

		// Kinda dirty: assume that the lower window is the important one. If
		// anything got added to it, ensure that it is visible. Otherwise assume
		// that we are dealing with something like a menu and switch the display to
		// display the upperwindow
		flipView(showLower);
		figurePromptStyle();
		figureMenuState();
	}

	/**
	 * Show the correct prompt.
	 */
	private void figurePromptStyle() {
		if (retainerFragment.engine.getRunState() == ZMachine.STATE_WAIT_CHAR
				&& inputFragment.isPrompt()) {
			inputFragment.toggleInput();
		}
		if (retainerFragment.engine.getRunState() == ZMachine.STATE_WAIT_CMD
				&& !inputFragment.isPrompt()) {
			inputFragment.toggleInput();
		}
	}

	/**
	 * Enable/Disable menu items
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void figureMenuState() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			invalidateOptionsMenu();
		}
	}

	/**
	 * Make either the storyboard or the statusscreen visible
	 * 
	 * @param showstory
	 *          true to swtich to the story view, false to swtich to the status
	 *          screen. nothing happens if the desired view is already showing.
	 */
	private void flipView(boolean showstory) {
		View now = windowFlipper.getCurrentView();

		if (showstory) {
			if (now != storyBoard) {
				windowFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.animator.slide_in_right));
				windowFlipper
						.setOutAnimation(AnimationUtils.loadAnimation(this, R.animator.slide_out_left));
				windowFlipper.showPrevious();
			}
		}
		else {
			if (now == storyBoard) {
				windowFlipper.setInAnimation(AnimationUtils.loadAnimation(this,
						android.R.anim.slide_in_left));
				windowFlipper.setOutAnimation(AnimationUtils.loadAnimation(this,
						android.R.anim.slide_out_right));
				windowFlipper.showPrevious();
			}
		}
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		switch (pendingAction) {
			case PENDING_RESTART: {
				if (which == DialogInterface.BUTTON_POSITIVE) {
					retainerFragment.messageBuffer.clear();
					try {
						retainerFragment.engine.restart();
						retainerFragment.engine.run();
					}
					catch (GrueException e) {
						// This should never happen
						retainerFragment.postMortem = e;
						finish();
					}
					publishResult();
				}
				break;
			}
			case PENDING_SAVE: {
				String name = saveName.getEditableText().toString();
				name = name.replace('/', '_');
				if (name.length() > 0) {
					ZState state = new ZState(retainerFragment.engine);
					File f = new File(FileUtil.getSaveGameDir(storyFile), name);
					state.disk_save(f.getPath(), retainerFragment.engine.pc);
					Toast.makeText(this, R.string.msg_game_saved, Toast.LENGTH_SHORT).show();
				}
			}

			case PENDING_RESTORE: {
				if (which > -1) {
					File file = FileUtil.listSaveGames(storyFile)[which];
					ZState state = new ZState(retainerFragment.engine);
					if (state.restore_from_disk(file.getPath())) {
						statusWindow.setText(""); // Wrong, but the best we can do.
						retainerFragment.messageBuffer.clear();
						messages.notifyDataSetChanged();
						retainerFragment.engine.restore(state);
						figurePromptStyle();
						figureMenuState();
						Toast.makeText(this, R.string.msg_game_restored, Toast.LENGTH_SHORT).show();
					}
					else {
						Toast.makeText(this, R.string.msg_restore_failed, Toast.LENGTH_SHORT).show();
					}
				}
			}
		}
		pendingAction = PENDING_NONE;
	}

	@Override
	public void onInit(int status) {
		ttsReady = (status == TextToSpeech.SUCCESS);
		if (ttsReady) {
			// Was the game faster to load?
			if (retainerFragment != null && retainerFragment.messageBuffer.size() > 0
					&& prefs.getBoolean("narrator", false)) {
				speaker.speak(
						retainerFragment.messageBuffer.get(retainerFragment.messageBuffer.size() - 1).message
								.toString(), TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

		String font = prefs.getString("font", "");
		if (font.equals("default")) {
			messages.setTypeface(Typeface.DEFAULT);
		}
		if (font.equals("sans")) {
			messages.setTypeface(Typeface.SANS_SERIF);
		}
		if (font.equals("serif")) {
			messages.setTypeface(Typeface.SERIF);
		}
		if (font.equals("monospace")) {
			messages.setTypeface(Typeface.MONOSPACE);
		}
		if (font.equals("comicsans")) {
			Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/LDFComicSans.ttf");
			messages.setTypeface(tf);
		}
		if (font.equals("ziggyzoe")) {
			Typeface tf = Typeface.createFromAsset(getAssets(), "fonts/ziggyzoe.ttf");
			messages.setTypeface(tf);
		}

		String fontSize = prefs.getString("fontsize", "");
		TextView tmp = new TextView(this);
		if (fontSize.equals("small")) {
			tmp.setTextAppearance(this, android.R.style.TextAppearance_Small);
			messages.setTextSize(tmp.getTextSize());
		}
		if (fontSize.equals("medium")) {
			tmp.setTextAppearance(this, android.R.style.TextAppearance_Medium);
			messages.setTextSize(tmp.getTextSize());
		}
		if (fontSize.equals("large")) {
			tmp.setTextAppearance(this, android.R.style.TextAppearance_Large);
			messages.setTextSize(tmp.getTextSize());
		}

		inputFragment.setAutoCollapse(prefs.getBoolean("autocollapse", false));

		wordExtractor.setKeyclick(prefs.getBoolean("keyclick", false));
		compassFragment.setKeyclick(prefs.getBoolean("keyclick", false));

	}

	@Override
	public void toggleTextHighlight(String str) {
		int tmp;
		String txt = str.toLowerCase();
		if (retainerFragment.highlighted.contains(txt)) {
			retainerFragment.highlighted.remove(txt);
			tmp = R.string.msg_unmarked;
		}
		else {
			retainerFragment.highlighted.add(txt);
			tmp = R.string.msg_marked;
		}
		Toast.makeText(this, getResources().getString(tmp, txt), Toast.LENGTH_SHORT).show();
		highlighted = retainerFragment.highlighted.toArray(new String[0]);
		Iterator<StoryItem> it = retainerFragment.messageBuffer.listIterator();
		while (it.hasNext()) {
			highlight(it.next().message, highlighted);
		}
		messages.notifyDataSetChanged();
		try {
			JSONArray array = new JSONArray(retainerFragment.highlighted);
			File f = new File(FileUtil.getDataDir(storyFile), HIGHLIGHTFILE);
			PrintStream ps = new PrintStream(f);
			ps.write(array.toString(2).getBytes());
			ps.close();
		}
		catch (Exception e) {
			Log.w(getClass().getName(), e);
		}
	}

	@Override
	public void utterText(CharSequence txt) {
		if (ttsReady) {
			if (speaker.isSpeaking() && txt == null) {
				speaker.stop();
			}
			if (txt != null) {
				speaker.speak(txt.toString(), TextToSpeech.QUEUE_FLUSH, null);
			}
		}
	}

	/**
	 * Add underlines to a text blob. Any existing underlines are removed. before
	 * new ones are added.
	 * 
	 * @param span
	 *          the blob to modify
	 * @param words
	 *          the words to underline (all lowercase!)
	 */
	private static void highlight(SpannableString span, String... words) {
		UnderlineSpan old[] = span.getSpans(0, span.length(), UnderlineSpan.class);
		for (UnderlineSpan del : old) {
			span.removeSpan(del);
		}
		char spanChars[] = span.toString().toLowerCase().toCharArray();
		for (String word : words) {
			char[] wc = word.toCharArray();
			int last = spanChars.length - wc.length + 1;
			for (int i = 0; i < last; i++) {
				// First check if there is a word-sized gap at spanchars[i] as we don't
				// want to highlight words that are actually just substrings (e.g.
				// "east" in "lEASTwise").
				if ((i > 0 && Character.isLetterOrDigit(spanChars[i - 1]))
						|| (i + wc.length != spanChars.length && Character.isLetterOrDigit(spanChars[i
								+ wc.length]))) {
					continue;
				}
				int a = i;
				int b = 0;
				while (b < wc.length) {
					if (spanChars[a] != wc[b]) {
						b = 0;
						break;
					}
					a++;
					b++;
				}
				if (b == wc.length) {
					span.setSpan(new UnderlineSpan(), i, a, 0);
					i = a;
				}
			}
		}
	}

	@Override
	public File getStory() {
		return storyFile;
	}

	/**
	 * Show/hide the spinner indicating that we are currently loading a game
	 * 
	 * @param b
	 *          true to show the spinner.
	 */
	public void setLoadingVisibility(boolean b) {
		try {
			loading.setIndeterminate(b);
			if (b) {
				loading.setVisibility(View.VISIBLE);
			}
			else {
				loading.setVisibility(View.GONE);
			}
		}
		catch (Exception e) {
			// TODO: Getting here is a bug! I haven't figured out how to trigger it
			// yet,
			// the User message on Google Play for the stack trace reads
			// "crash on resume".
			Log.w("TextFiction", e);
		}
	}

	@Override
	public void onOptionsMenuClosed(Menu m) {
		dimSoftButtonsIfPossible();
	}

	// http://stackoverflow.com/questions/25831634/android-openoptionsmenu-does-nothing-in-kitkat
	@Override
	public void openOptionsMenu() {
		super.openOptionsMenu();
		Configuration config = getResources().getConfiguration();
		if ((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) > Configuration.SCREENLAYOUT_SIZE_LARGE) {
			int originalScreenLayout = config.screenLayout;
			config.screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE;
			super.openOptionsMenu();
			config.screenLayout = originalScreenLayout;
		} else {
			super.openOptionsMenu();
		}
	}
	
	private void WriteSetting(int resId, String FileName) {
		File StoryFile;
		
		StoryFile=FileUtil.getDataDir(storyFile);

		try {
			String buttonDef = getString(resId);		
			JSONArray array = new JSONArray(buttonDef);
			PrintWriter pw = new PrintWriter(new File(StoryFile, FileName));
			pw.write(array.toString(2));
			pw.close();
		}
		catch (Exception e) {
			Log.w(getClass().getName(), e);
		}
	}

	private void SetDefaultCommands(int c, int c0, int c1, int c2, int c10) {
		WriteSetting(c,   "quickcommands.json");	
		WriteSetting(c0,  "quickcommands_0.json");	
		WriteSetting(c1,  "quickcommands_1.json");	
		WriteSetting(c2,  "quickcommands_2.json");	
		WriteSetting(c10, "quickcommands_10.json");	
	}
	
	public void WriteStoryLangueSettings(String lang) {
		String txt;
		int langID;
		int highlightID;
		
		if (lang.equals("en")) {
			SetDefaultCommands (R.string.defaultcommands,
								R.string.defaultcommands_0,		
								R.string.defaultcommands_1,
								R.string.defaultcommands_2,
								R.string.defaultcommands_10);
			highlightID=R.array.initial_highlights;
			langID=R.string.storylang_en;
		} else if (lang.equals("de")) { 
			SetDefaultCommands (R.string.defaultcommands_de,
								R.string.defaultcommands_de_0,		
								R.string.defaultcommands_de_1,
								R.string.defaultcommands_de_2,
								R.string.defaultcommands_de_10);		
			highlightID=R.array.initial_highlights_de;
			langID=R.string.storylang_de;
		} else if (lang.equals("es")) { 
			SetDefaultCommands (R.string.defaultcommands_es,
								R.string.emptycommands,		
								R.string.emptycommands,
								R.string.emptycommands,
								R.string.emptycommands);		
			highlightID=R.array.initial_highlights_es;
			langID=R.string.storylang_es;
		} else if (lang.equals("..")) { 
			SetDefaultCommands (R.string.emptycommands,
								R.string.emptycommands,		
								R.string.emptycommands,
								R.string.emptycommands,
								R.string.emptycommands);		
			highlightID=R.array.initial_highlights;
			langID=R.string.storylang_en;
		}
		else
			return;
		
		inputFragment.UpdateCmdButtons(-2);

		txt = getString(R.string.storylang_written, getString(langID));
		Toast.makeText(this, txt, Toast.LENGTH_LONG).show();
		
		retainerFragment.highlighted.clear();
		String[] ini = getResources().getStringArray(highlightID);
		for (String i : ini) {
			retainerFragment.highlighted.add(i);
		}
		highlighted = retainerFragment.highlighted.toArray(new String[0]);
		
		try {
			JSONArray array = new JSONArray(retainerFragment.highlighted);
			File f = new File(FileUtil.getDataDir(storyFile), HIGHLIGHTFILE);
			PrintStream ps = new PrintStream(f);
			ps.write(array.toString(2).getBytes());
			ps.close();
		}
		catch (Exception e) {
			Log.w(getClass().getName(), e);
		}
	}	

	private int CountWords(String words, String txt)  {
		int count=0;
		String word;
		
		int pos;

		while (!words.equals(""))
		{
			pos = (words+",").indexOf(",", 0);
			word = " " + words.substring(0, pos) + " ";
			words = (words+" ").substring(pos+1).trim();
		
			pos = txt.indexOf(word, 0);
			while (pos != -1) {
				count++;
				pos = txt.indexOf(word, pos+1);
			}
		}
		
		return count;
	}
	
	private void AutoDetectLangue(String txt)  {
		int maxCount;
		String tmp;
		
		if (storyLanguageData.count > 4)
			return; // enough detected
		
		storyLanguageData.count++;
	
		txt = " " + txt.toLowerCase() + " ";
		txt = txt.replace(".", " ");
		txt = txt.replace(",", " ");
		txt = txt.replace("\"", " ");
		txt = txt.replace("!", " ");
		txt = txt.replace("?", " ");
		txt = txt.replace("(", " ");
		txt = txt.replace(")", " ");
		txt = txt.replace("/", " ");
		txt = txt.replace("-", " ");
		txt = txt.replace("\r", " ");
		txt = txt.replace("\n", " ");
		
		// check for words, mostly used in only one of the following languages:
		storyLanguageData.en += CountWords("the,a,i,you", txt);
		storyLanguageData.de += CountWords("der,die,das,ein,eine,einer,ist", txt);
		storyLanguageData.es += CountWords("la,los,las,una,el", txt);
		// ... (other languages)

		storyLanguage="en";
		maxCount=storyLanguageData.en;
		
		if (storyLanguageData.de > maxCount) {
			storyLanguage="de";
			maxCount=storyLanguageData.de;
		}
		if (storyLanguageData.es > maxCount) {
			storyLanguage="es";
			maxCount=storyLanguageData.es;
		}
		// ... (other languages)
		
		
		// Only if we are sure about the language (and no other settings are already there)
		// we'll write default settings
		
		// When there are a lot of languages, it will be a lot of more work.
		// Because it will happen (more often) that the same words are in different languages.
		
		if (maxCount > 5) {
			
			File StoryDir;
			StoryDir=new File (FileUtil.getDataDir(storyFile), "");
			File[] files = StoryDir.listFiles();

			if (files != null) {
				if (files.length == 0) {
					// 	Empty directory, so we write default language settings
					WriteStoryLangueSettings(storyLanguage);
				}
			}
		}
		
		// ### BCM - Debug ###
		/*
		SpannableString ss;
		
		tmp = storyLanguage  
			    + "  c=" + storyLanguageData.count 
				+ "  en=" + storyLanguageData.en 
				+ "  de=" + storyLanguageData.de 
				+ "  es=" + storyLanguageData.es;
		
		//Toast.makeText(this, tmp, Toast.LENGTH_LONG).show();
		
		ss = new SpannableString(tmp);retainerFragment.messageBuffer.add(new StoryItem(ss, StoryItem.MYSELF));

		// ss = new SpannableString(txt + "\n" + tmp);retainerFragment.messageBuffer.add(new StoryItem(ss, StoryItem.MYSELF));
		*/
	}
}
