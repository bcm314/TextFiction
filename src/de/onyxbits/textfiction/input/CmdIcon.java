package de.onyxbits.textfiction.input;

import org.json.JSONException;
import org.json.JSONObject;

import de.onyxbits.textfiction.R;

/**
 * Container class that stores the info for quick access command buttons.
 * 
 * @author patrick
 * 
 */
public class CmdIcon {


	/**
	 * List of all icon resource ids, the user is allowed to assign to a button
	 */
	public static final int[] ICONS = {
			R.drawable.ic_action_user0,
			R.drawable.ic_action_user1,
			R.drawable.ic_action_user2,
			R.drawable.ic_action_user3,
			R.drawable.ic_action_user4,
			R.drawable.ic_action_user5,
			R.drawable.ic_action_user6,
			R.drawable.ic_action_user7,
			R.drawable.ic_action_user8,
			R.drawable.ic_action_user9,
			R.drawable.ic_action_user10,
			R.drawable.ic_action_user11,
			R.drawable.ic_action_user12,
			R.drawable.ic_action_user13,
			R.drawable.ic_action_user14,
			R.drawable.ic_action_user15,
			R.drawable.ic_action_user16,
			R.drawable.ic_action_user17,
			R.drawable.ic_action_user18,
			R.drawable.ic_action_user19,
			R.drawable.ic_action_user20,
			R.drawable.ic_action_user21,
			R.drawable.ic_action_user22,
			R.drawable.ic_action_user23,
			R.drawable.ic_action_user24,
			R.drawable.ic_action_user25,
			R.drawable.ic_action_user26,
			R.drawable.ic_action_user27,
			R.drawable.ic_action_user28,
			R.drawable.ic_action_user29,
			R.drawable.ic_action_user30,
			R.drawable.ic_action_user31,
			R.drawable.ic_action_up,
			R.drawable.ic_action_down,
			R.drawable.ic_action_left,
			R.drawable.ic_action_right,
			R.drawable.ic_action_upleft,
			R.drawable.ic_action_upright,
			R.drawable.ic_action_downleft,
			R.drawable.ic_action_downright,
			R.drawable.ic_action_up2,
			R.drawable.ic_action_down2,
			R.drawable.ic_action_go,
			R.drawable.ic_action_space,			
			R.drawable.ic_action_arrow_sync_outline,
			R.drawable.ic_action_book,
			R.drawable.ic_action_camera_outline,
			R.drawable.ic_action_document_text,
			R.drawable.ic_action_home_outline,
			R.drawable.ic_action_info_outline,
			R.drawable.ic_action_input_checked_outline,
			R.drawable.ic_action_mail,
			R.drawable.ic_action_microphone_outline,
			R.drawable.ic_action_phone_outline,
			R.drawable.ic_action_plane_outline,
			R.drawable.ic_action_plug,
			R.drawable.ic_action_scissors_outline,
			R.drawable.ic_action_social_dribbble,
			R.drawable.ic_action_stopwatch,
			R.drawable.ic_action_support,
			R.drawable.ic_action_thermometer,
			R.drawable.ic_action_warning_outline,
			R.drawable.ic_action_waves_outline,
			R.drawable.ic_action_weather_snow,
			R.drawable.ic_action_weather_sunny,
			R.drawable.ic_action_zoom_in_outline,
			R.drawable.ic_action_zoom_out_outline,			
	};


	/**
	 * Alias (string-ID) of all icon resources which will be used in settings.
	 * So there is no problem in changing the sorting.
	 */
	public static String getIconName(int iconID)
	{
		switch (iconID)
		{
		case R.drawable.ic_action_user0: 				return "EYE";
		case R.drawable.ic_action_user1: 				return "LENS";
		case R.drawable.ic_action_user2: 				return "NEWS";
		case R.drawable.ic_action_user3: 				return "THUMBSUP";
		case R.drawable.ic_action_user4: 				return "THUMBSDOWN";
		case R.drawable.ic_action_user5: 				return "UNLOCK";
		case R.drawable.ic_action_user6: 				return "LOCK";
		case R.drawable.ic_action_user7: 				return "BRIEFCASE";
		case R.drawable.ic_action_user8: 				return "UPLOAD";
		case R.drawable.ic_action_user9: 				return "DOWNLOAD";
		case R.drawable.ic_action_user10:				return "SHOPPING";
		case R.drawable.ic_action_user11:				return "WATCH";
		case R.drawable.ic_action_user12:				return "COG";
		case R.drawable.ic_action_user13:				return "MESSAGES";
		case R.drawable.ic_action_user14:				return "MINIMIZE";
		case R.drawable.ic_action_user15:				return "MAXIMIZE";
		case R.drawable.ic_action_user16:				return "SPANNER";
		case R.drawable.ic_action_user17:				return "POI";
		case R.drawable.ic_action_user18:				return "PUZZLE";
		case R.drawable.ic_action_user19:				return "COFFEE";
		case R.drawable.ic_action_user20:				return "HEART";
		case R.drawable.ic_action_user21:				return "LIGHTBULB";
		case R.drawable.ic_action_user22:				return "GIFT";
		case R.drawable.ic_action_user23:				return "EJECT";
		case R.drawable.ic_action_user24:				return "TRASH";
		case R.drawable.ic_action_user25:				return "FLASH";
		case R.drawable.ic_action_user26:				return "KEY";
		case R.drawable.ic_action_user27:				return "USER";
		case R.drawable.ic_action_user28:				return "DIRECTIONS";
		case R.drawable.ic_action_user29:				return "PEN";
		case R.drawable.ic_action_user30:				return "NOTES";
		case R.drawable.ic_action_user31:				return "BELL";
		case R.drawable.ic_action_up:					return "UP";
		case R.drawable.ic_action_down:					return "DOWN";
		case R.drawable.ic_action_left:					return "LEFT";
		case R.drawable.ic_action_right:				return "RIGHT";
		case R.drawable.ic_action_upleft:				return "UPLEFT";
		case R.drawable.ic_action_upright:				return "UPRIGHT";
		case R.drawable.ic_action_downleft:				return "DOWNLEFT";
		case R.drawable.ic_action_downright:			return "DOWNRIGHT";
		case R.drawable.ic_action_up2:					return "HIGHER";
		case R.drawable.ic_action_down2:				return "LOWER";
		case R.drawable.ic_action_go:					return "GO";
		case R.drawable.ic_action_arrow_sync_outline:	return "SYNC";
		case R.drawable.ic_action_book:					return "BOOK";
		case R.drawable.ic_action_camera_outline:		return "CAMERA";
		case R.drawable.ic_action_document_text:		return "DOCUMENT";
		case R.drawable.ic_action_home_outline:			return "HOME";
		case R.drawable.ic_action_info_outline:			return "INFO";
		case R.drawable.ic_action_input_checked_outline:return "INPUTCHECKED";
		case R.drawable.ic_action_mail:					return "MAIL";
		case R.drawable.ic_action_microphone_outline:	return "MICRO";
		case R.drawable.ic_action_phone_outline:		return "PHONE";
		case R.drawable.ic_action_plane_outline:		return "PLANE";
		case R.drawable.ic_action_plug:					return "PLUG";
		case R.drawable.ic_action_scissors_outline:		return "SCISSORS";
		case R.drawable.ic_action_social_dribbble:		return "BALL";
		case R.drawable.ic_action_stopwatch:			return "STOPWATCH";
		case R.drawable.ic_action_support:				return "SUPPORT";
		case R.drawable.ic_action_thermometer:			return "THERMO";
		case R.drawable.ic_action_warning_outline:		return "WARN";
		case R.drawable.ic_action_waves_outline:		return "WAVES";
		case R.drawable.ic_action_weather_snow:			return "SNOW";
		case R.drawable.ic_action_weather_sunny:		return "SUN";
		case R.drawable.ic_action_zoom_in_outline:		return "ZOOMIN";
		case R.drawable.ic_action_zoom_out_outline:		return "ZOOMOUT";		
		case R.drawable.ic_action_space:				return "DOT";			
		case R.drawable.ic_action_empty:				return "EMPTY";		
		}
		
		return "";
	}
	
	/**
	 * the (user) command (string) associated with the button.
	 */
	public String cmd;

	/**
	 * Whether or not the command should be executed immediately on the button
	 * press.
	 */
	public boolean atOnce;

	/**
	 * ID number of the drawable (for saving in the preferences storage)
	 */
	public int imgid;

	public CmdIcon(int imgid, String cmd, boolean atOnce) {
		this.imgid = imgid;
		this.cmd = cmd;
		this.atOnce = atOnce;
	}

	public static JSONObject toJSON(CmdIcon ico) {
		JSONObject ret = new JSONObject();
		try {
			String cmd;
			String imgString;
			
			imgString=getIconName(ICONS[ico.imgid]);
			if (imgString.equals(""))
				ret.put("imgid", ico.imgid);
			else
				ret.put("imgid", imgString);

			cmd=ico.cmd;
			if ((ico.atOnce) && (cmd==cmd.replace("$", "")))
				cmd=cmd + "$";
			ret.put("cmd", cmd);
			//BCM ret.put("atonce", ico.atOnce);
		}
		catch (JSONException e) {
			// Can't really see this happening
			throw new RuntimeException(e);
		}
		return ret;
	}

	public static CmdIcon fromJSON(JSONObject in) {
		int imgid;		
		String imgString;
		
		imgString= in.optString("imgid", "");
		for (imgid=0 ; imgid < ICONS.length ; imgid++)
			if (imgString.equals(getIconName(ICONS[imgid])))
				break;
		
		if (imgid > ICONS.length - 1)
			imgid = in.optInt("imgid", 0);

		if (imgid > ICONS.length - 1) {
			imgid = 0;
		}
		String cmd = in.optString("cmd", "???");
		boolean atOnce = in.optBoolean("atonce", false);
		return new CmdIcon(imgid, cmd, atOnce);
	}

}
