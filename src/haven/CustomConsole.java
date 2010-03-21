package haven;
import java.awt.Color;
import java.awt.event.KeyEvent;
class CustomConsole extends Window {
	public static Textlog out;
	public TextEntry in;
	public static String log = "IRC-Extended Client Console - Type HELP for a list of commands";
	public static String newText = "";
	public static boolean logChanged = false;
	public static void log(String text)
	{
		newText += "\n" + text;
		logChanged = true;
	}
	public void draw(GOut g)
	{
		if(logChanged){
			append(newText);
			newText = "";
			logChanged = false;
		}
		super.draw(g);
	}
	public CustomConsole(CustomConsole oldConsole, Widget newParent)
	{
		super(oldConsole.c, oldConsole.sz, newParent, oldConsole.cap.text);
		out = oldConsole.out;
		in = oldConsole.in;
		out.parent = this;
		in.parent = this;
		setfocus(in);
	}
	public CustomConsole(Coord c, Coord sz, Widget parent, String title)
	{
		super(c, sz, parent, title, false);
		ui.bind(this, CustomConfig.wdgtID++);
		out = new Textlog(Coord.z, sz.add(0,-20), this);
		in = new TextEntry(new Coord(0, 200), new Coord(sz.x, 20), this, ""){
			public boolean type(char c, KeyEvent ev)
			{
				if(c == '`' && !(ev.isAltDown() || ev.isControlDown() || ev.isShiftDown())){
					ev.consume();
					parent.toggle();
					return true;
				}
				return super.type(c, ev);
			}
		};
		in.canactivate = true;

		if(log != null){
			final String[] lines = log.trim().split("\n");
			Thread consoleThread = new Thread(HackThread.tg(), "Console starter thread"){
				public void run(){
					for(int i = 0; i < lines.length; i++)
						out.append(lines[i]);
				}
			};
			consoleThread.setPriority(Thread.MIN_PRIORITY);
			consoleThread.start();
		}
		setfocus(in);
	}
	public void append(String text, Color color){
		out.append(text, color);
		log += "\n" + text;
	}
	public void append(String text){
		append(text, Color.BLACK);
	}
	public void wdgmsg(Widget sender, String msg, Object... args)
	{
		if(sender == in)
		{
			if (args[0] != null || ((String)args[0]).length() > 0){
		   		String cmdText = ((String)args[0]).trim().toUpperCase();
		   		String cmd = cmdText.contains(" ") ? cmdText.substring(0, cmdText.indexOf(" ")).trim() : cmdText;
		   		cmdText = cmdText.contains(" ") ? cmdText.substring(cmdText.indexOf(" ")).trim() : "";
		   		append("Command: " + cmd + "\nArguments: " + cmdText, Color.BLUE.darker());
		   		String[] cmdArgs = cmdText.split(" ");
		   		in.settext("");
		   		if(cmd.equals("NIGHTVISION")){
		   			if(!cmdArgs[0].trim().equals("")){
		   				if(cmdArgs[0].equals("ON") || cmdArgs[0].equals("TRUE"))
		   					CustomConfig.hasNightVision = true;
		   				if(cmdArgs[0].equals("OFF") || cmdArgs[0].equals("FALSE"))
		   					CustomConfig.hasNightVision = false;
		   			} else{
		   				append("NIGHTVISION - " + (CustomConfig.hasNightVision ? "ON" : "OFF"));
		   			}
		   		}else if(cmd.equals("IRC")){
		   			if(!cmdArgs[0].trim().equals("")){
		   				if(cmdArgs[0].equals("ON") || cmdArgs[0].equals("TRUE"))
		   					CustomConfig.isIRCOn = true;
		   				if(cmdArgs[0].equals("OFF") || cmdArgs[0].equals("FALSE"))
		   					CustomConfig.isIRCOn = false;
		   			} else{
		   				append("IRC - " + (CustomConfig.isIRCOn ? "ON" : "OFF"));
		   			}
		   		}if(cmd.equals("SCREENSIZE") || cmd.equals("WINDOWSIZE")){
		   			if(!cmdArgs[0].trim().equals("") && cmdArgs.length >= 2){
		   				try{
		   					int x = Integer.parseInt(cmdArgs[0]);
		   					int y = Integer.parseInt(cmdArgs[1]);
		   					if(x >= 800 && y >= 600){
		   						CustomConfig.setWindowSize(x, y);
		   					}
		   					CustomConfig.saveSettings();
		   					append("Client must be restarted for new settings to take effect.", Color.RED.darker());
		   				}catch(NumberFormatException e){
		   					append("Dimensions must be numbers");
		   				}
		   			}else {
		   				append("SCREENSIZE = " + CustomConfig.windowSize.toString());
		   			}
		   		}else if(cmd.equals("SOUND")){
		   			int vol = 0;
		   			if(!cmdArgs[0].trim().equals(""))
		   			{
		   				try{
		   					if(cmdArgs[0].equals("ON") || cmdArgs[0].equals("TRUE")){
		   						CustomConfig.isSoundOn = true;
		   					}else if(cmdArgs[0].equals("OFF") || cmdArgs[0].equals("FALSE")){
		   						CustomConfig.isSoundOn = false;
		   					}else if((vol = Integer.parseInt(cmdArgs[0])) >= 0 && vol <= 100){
		   						CustomConfig.sfxVol = vol;
		   					}else throw new NumberFormatException("vol = " + vol);
		   				}catch(NumberFormatException e){
		   					append("Volume must be an integer between 0-100");
		   				}
		   			}else {
		   				append("SOUND = " + (CustomConfig.isSoundOn ? "ON  " : "OFF ")
		   						+ "VOLUME = " + CustomConfig.sfxVol);
		   			}
		   		}else if(cmd.equals("MUSIC")){
		   			int vol = 0;
		   			if(!cmdArgs[0].trim().equals(""))
		   			{
		   				try{
		   					if(cmdArgs[0].equals("ON") || cmdArgs[0].equals("TRUE")){
		   						CustomConfig.isMusicOn = true;
		   					}else if(cmdArgs[0].equals("OFF") || cmdArgs[0].equals("FALSE")){
		   						CustomConfig.isMusicOn = false;
		   					}else if((vol = Integer.parseInt(cmdArgs[0])) >= 0 && vol <= 100){
		   						CustomConfig.musicVol = vol;
		   					}else throw new NumberFormatException("vol = " + vol);
		   				}catch(NumberFormatException e){
		   					append("Volume must be an integer between 0-100");
		   				}
		   			}else {
		   				append("MUSIC = " + (CustomConfig.isMusicOn ? "ON  " : "OFF ")
		   						+ "VOLUME = " + CustomConfig.musicVol);
		   			}
		   		}else if(cmd.equals("SAVE")){
		   			CustomConfig.saveSettings();
		   		}else if(cmd.equals("FORCESAVE")){
		   			CustomConfig.isSaveable = true;
		   			CustomConfig.saveSettings();
		   		}else if(cmd.equals("DEBUG")){
		   			if(!cmdArgs[0].trim().equals(""))
		   			{
		   				if(cmdArgs[0].equals("IRC")){
		   					if(cmdArgs.length >= 2){
		   						if(cmdArgs[1].equals("ON") || cmdArgs[1].equals("TRUE")){
		   							CustomConfig.logIRC = true;
		   						}else if(cmdArgs[1].equals("OFF") || cmdArgs[1].equals("FALSE")){
		   							CustomConfig.logIRC = false;
		   						}
		   					}else{
		   						append("DEBUG LOGS",Color.BLUE.darker());
		   						append("IRC - " + (CustomConfig.logIRC ? "ON" : "OFF"), Color.GREEN.darker());
		   					}
		   				}else if(cmdArgs[0].equals("SRVMSG")){
		   					if(cmdArgs.length >= 2){
		   						if(cmdArgs[1].equals("ON") || cmdArgs[1].equals("TRUE")){
		   							CustomConfig.logServerMessages = true;
		   						}else if(cmdArgs[1].equals("OFF") || cmdArgs[1].equals("FALSE")){
		   							CustomConfig.logServerMessages = false;
		   						}
		   					}else{
		   						append("DEBUG LOGS",Color.BLUE.darker());
		   						append("SRVMSG - " + (CustomConfig.logServerMessages ? "ON" : "OFF"), Color.GREEN.darker());
		   					}
		   				}if(cmdArgs[0].equals("LOAD")){
		   					if(cmdArgs.length >= 2){
		   						if(cmdArgs[1].equals("ON") || cmdArgs[1].equals("TRUE")){
		   							CustomConfig.logLoad = true;
		   						}else if(cmdArgs[1].equals("OFF") || cmdArgs[1].equals("FALSE")){
		   							CustomConfig.logLoad = false;
		   						}
		   					}else{
		   						append("DEBUG LOGS",Color.BLUE.darker());
		   						append("LOAD - " + (CustomConfig.logLoad ? "ON" : "OFF"), Color.GREEN.darker());
		   					}
		   				}
		   			}else {
		   				append("DEBUG LOGS", Color.BLUE.darker());
		   				append("IRC - " + (CustomConfig.logIRC ? "ON" : "OFF"), Color.GREEN.darker());
		   				append("LOAD - " + (CustomConfig.logLoad ? "ON" : "OFF"), Color.GREEN.darker());
		   				append("SRVMSG - " + (CustomConfig.logServerMessages ? "ON" : "OFF"), Color.GREEN.darker());
		   			}
		   		}else if(cmd.equals("HELP")){
		   			append("You can check the current status of each variable by "
		   						+ "typing the command without arguments.", Color.RED.darker());
		   			append("NIGHTVISION TRUE | FALSE | ON | OFF - Turns nightvision on or off");
		   			append("SCREENSIZE #### #### - Sets the screensize to the specified size.");
		   			append("SOUND TRUE | FALSE | ON | OFF | 0-100 - Turns the sound effects on/off, or sets "
		   					+ "the volume to the specified level");
		   			append("MUSIC TRUE | FALSE | ON | OFF | 0-100 - Turns the music on/off, or sets "
		   					+ "the volume to the specified level");
		   			append("SAVE - Saves the current settings if they are saveable.");
		   			append("FORCESAVE - Saves the current settings whether or not they are "
		   					+ "saveable (Might cause errors).");
		   			append("DEBUG IRC | LOAD   ON | OFF - Enables/disables debug text being dumped into the console "
		   					+ "for the specified system.");
		   			append("HELP - Shows this text.");
		   		}else{
		   			append("Command not recognized.  Type /help to see a list of commands.");
		   		}
	   		}
	   	} else {
	   		super.wdgmsg(sender, msg, args);
		}
	}
	public boolean toggle()
	{
		if(super.toggle())
			setfocus(in);
		return visible;
	}
}
