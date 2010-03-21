package haven;
import java.util.*;
import java.awt.event.KeyEvent;
import java.awt.Color;
import org.relayirc.util.*;
import org.relayirc.core.*;
import org.relayirc.chatengine.*;

public class SlenConsole extends ChatHW implements IRCConnectionListener
{
	public IRCConnection IRC;
	public String user;
	public SlenHud parent;
	private static SlenChat tSCWnd;
	private boolean initialized = false;
	public List<SlenChat> wndList = new ArrayList<SlenChat>();

	static {
    	Widget.addtype("slenlog", new WidgetFactory() {
	    	public Widget create(Coord c, Widget parent, Object[] args) {
	    		String t = (String)args[0];
	    		SlenConsole wnd = new SlenConsole((SlenHud)parent);
	    		wnd.out.append(t);
			    return(wnd);
	    	}
    	});
    }

    public SlenConsole(SlenHud parent)
    {
    	super(parent, "Console", false);
    	this.parent = parent;
    	if(CustomConfig.ircDefNick == null || CustomConfig.ircDefNick.equals(""))
    	{
    		CustomConfig.ircDefNick = ui.sess.username;
    		CustomConfig.ircAltNick = ui.sess.username + "|C";
    	}
    	user = CustomConfig.ircDefNick;
    	if(!CustomConfig.ircServerAddress.equals("") && CustomConfig.isIRCOn)
    	{
	    	IRC = new IRCConnection(CustomConfig.ircServerAddress, 6667,
	    							CustomConfig.ircDefNick, CustomConfig.ircAltNick,
	    							CustomConfig.ircDefNick, CustomConfig.ircDefNick);
	    	IRC.setIRCConnectionListener(this);
	    	IRC.open();
    	}
    	Debug.setDebug(false);
    	ui.sess.IRC = IRC;
    	initialized = true;
    	out.append("Press ` to bring down the console.");
    	out.append("Press CTRL + O to open the options menu.");
    }
	public void handleInput(String input, ChatHW src)
	{
		if(input == null || input.trim().equals(""))	return;
		if(IRC != null)
			if(IRC.getState() != IRC.CONNECTED){
	    		IRC.open(IRC);
	    		IRC.setIRCConnectionListener(this);
	   		}
	    if(input.charAt(0) == '/')
	    {
	    	String[] cArgs = input.split(" ");
	    	String cmd = cArgs.length >= 1 ? cArgs[0].toUpperCase() : "/INVALIDCOMMAND";

	    	//	JOIN -	Join the specified channels
	    	if(cmd.equals("/JOIN") && IRC != null)
	    	{
	    		if(cArgs.length >= 2) {
	    			List<Listbox.Option> tChannels = new ArrayList<Listbox.Option>();

	    			//	Split the appropriate list into channels & their passwords using the Listbox.Options class
		   			for(int i = 1; i < cArgs.length; i++)
		   			{
		   				if(cArgs[i].charAt(0) == '#') {
		   					if(i+1 < cArgs.length && cArgs[i+1].charAt(0) != '#') {
		   						tChannels.add(new Listbox.Option(cArgs[i], cArgs[i+1]));
		   						i++;
		   						continue;
		   					}
		   					tChannels.add(new Listbox.Option(cArgs[i], ""));
		   				}
		   			}

		   			//	Create all the channels specified if they dont'already exist
		   			for(Listbox.Option tChanInfo : tChannels)
		   			{
		   				tSCWnd = findWindow(tChanInfo.name);
		    			if(tSCWnd != null)	return;
		    			IRC.writeln("JOIN " + tChanInfo.name + " " + tChanInfo.disp);
		    			tSCWnd = new SlenChat(this, tChanInfo.name, tChanInfo.disp);
		    			if(!wndList.contains(tSCWnd))
		    				wndList.add(tSCWnd);
		   			}
	    		} else {
    				src.out.append("FORMAT: /JOIN <#CHANNEL1> [PASSWORD1] [#CHANNEL2 [PASSWORD2]...]");
	    		}
	    		return;
	    		//	INVITE - Invite an user to the specified channel
	    	} else if(cmd.equals("/INVITE") && IRC != null)
	    	{
	    		if(cArgs.length >= 3)
	    		{
	    			IRC.writeln("INVITE " + cArgs[1] + " " + cArgs[2]);
	    		} else {
	    			src.out.append("FORMAT: /INVITE <NICKNAME> <#CHANNEL>");
	    		}
	    		return;
	    		//	ACCEPT - Accept an invitation to join a channel
	    	} else if(cmd.equals("/ACCEPT") && IRC != null)
	    	{
	    		if(cArgs.length >= 2)
	    		{
	    			handleInput("/JOIN " + cArgs[1], src);
	    		} else {
	    			src.out.append("FORMAT: /ACCEPT <#CHANNEL>");
	    		}
	    		return;
	    		//	NICK - Change your nick in the current server
	    	} else if(cmd.equals("/NICK") && IRC != null)
	    	{
	    		if(cArgs.length >= 2) {
		   			IRC.writeln("NICK " + cArgs[1]);
		   			user = cArgs[1];
	    		} else {
	    			src.out.append("FORMAT: /NICK <NEWNICK>");
	    		}
	    		return;
	    		//	SERVER - Connect to the specified server
	    	} else if(cmd.equals("/SERVER"))
	    	{
	    		if(cArgs.length >= 2){
		   			if(IRC != null)	IRC.close();
		   			IRC = new IRCConnection(cArgs[1], 6667, CustomConfig.ircDefNick, CustomConfig.ircAltNick,
		   													CustomConfig.ircDefNick, CustomConfig.ircDefNick);
	    			IRC.setIRCConnectionListener(this);
	    			IRC.open();
	    			CustomConfig.isIRCOn = true;
	    		} else {
	    			src.out.append("FORMAT: /SERVER <SERVERADDRESS>");
	    		}
    			return;
    			//	TOPIC - Set the topic for the current channel
	    	} else if(cmd.equals("/TOPIC")
	    		&& src.getClass().getName().equalsIgnoreCase(tSCWnd.getClass().getName())
	    		&& IRC != null)
	    	{
	    		if(cArgs.length >=2){
		   			String fullTopic = "";
		   			for(int i = 1; i < cArgs.length; i++)
		   				fullTopic += cArgs[i] + " ";
		   			IRC.writeln("TOPIC " + ((SlenChat)src).getChannel() + " :" + fullTopic);
	    		} else{
    				src.out.append("FORMAT: /TOPIC <NEWTOPIC>");
	    		}
	    		return;
	    		//	NOTICE - Send a message to the specified user/channel without creating a new window
	    	} else if(cmd.equals("/NOTICE") && IRC != null)
	    	{
	    		if(cArgs.length >= 3) {
	    			String msg = "";
	    			for(int i = 2; i < cArgs.length; i++)
		   				msg += cArgs[i] + " ";
		   			IRC.writeln("NOTICE " + cArgs[1] + " :" + msg);
		   			src.out.append(user + ": " + msg, Color.DARK_GRAY);
	    		} else {
	    			src.out.append("FORMAT: /NOTICE <USER> <MESSAGE>");
	    		}
	    		return;
	    		//	MSG PM TELL - Send a private message to the specified user in a new window
	    	} else if((cmd.equals("/MSG") || cmd.equals("/PM") || cmd.equals("/TELL")) && IRC != null)
	    	{
	    		if(cArgs.length >= 3) {
	    			String msg = "";
	    			for(int i = 2; i < cArgs.length; i++)
		   				msg += cArgs[i] + " ";
		   			IRC.writeln("PRIVMSG " + cArgs[1].trim() + " :" + msg);
		   			tSCWnd = findWindow(cArgs[1].trim());
		   			if(tSCWnd == null) {
		   				tSCWnd = new SlenChat(this, cArgs[1].trim(), null, false);
		   				wndList.add(tSCWnd);
		   			}
		   			tSCWnd.out.append(user + ": " + msg);
	    		} else {
	    			src.out.append("FORMAT: /MSG <USER> <MESSAGE>");
	    		}
	    		return;
	    	} else if((cmd.equals("/ME") || cmd.equals("/E") || cmd.equals("/EMOTE")) && IRC != null)
	    	{
	    		if(cArgs.length >= 2 ) {
	    			String msg = "";
	    			for(int i = 1; i < cArgs.length; i++)
		   				msg += cArgs[i] + " ";
		   			IRC.writeln("PRIVMSG " + ((SlenChat)src).getChannel() + " :\001ACTION " + msg + "\001");
		   			src.out.append("*" + user + " " + msg + "*", Color.MAGENTA.darker());
	    		} else {
	    			src.out.append("FORMAT: <[/E][/EMOTE][/ME]> <ACTION>");
	    		}
	    		return;
	    	} else if(cmd.equals("/COMMANDS"))
	    	{
	    		src.out.append("Placeholder", Color.WHITE);
	    	} else
	    	{
	    		src.out.append("Command not recognized.\n see /COMMANDS for a list of all available commands");
	    		return;
	    	}
	    }
	    if(tSCWnd == null)	return;
	    if(src instanceof SlenChat)
	    {
	    	IRC.writeln("PRIVMSG " + ((SlenChat)src).getChannel() + " :" + input);
	    	src.out.append(user + ": " + input, Color.RED.darker());
	    	return;
	    }
	}

    public void onAction( String user, String chan, String txt )
	{
		tSCWnd = findWindow(chan);
		if(tSCWnd != null)	tSCWnd.out.append("*" + user + " " + txt + "*", Color.MAGENTA.darker());
	}
	public void onBan( String banned, String chan, String banner)
	{
		tSCWnd = findWindow(chan);
		tSCWnd.out.append(banned + " was banned by " + banner, Color.GREEN.darker());
		if(tSCWnd.userList != null)	tSCWnd.userList.rmvUser(banned);
	}
	public void onClientInfo(String orgnick){}
	public void onClientSource(String orgnick){}
	public void onClientVersion(String orgnick){}
	public void onConnect()
	{
		out.append("Successfully connected to: "+ IRC.getServer());
		for(Listbox.Option channel : CustomConfig.ircChannelList)
		{
			handleInput("/JOIN " + channel.name + " " + channel.disp, this);
		}
		for(SlenChat tSCWnd : wndList)
		{
			if(tSCWnd.getChannel().charAt(0) == '#')
			{
				IRC.writeln("/JOIN " + tSCWnd.getChannel() + " " + tSCWnd.getPassword());
			}
		}
	}
	public void onDisconnect()
	{
		out.append("Disconnected", Color.GREEN.darker());
		if(wndList == null)	return;
		for(SlenChat tSCWnd : wndList)
		{
			tSCWnd.out.append("Disconnected from server");
		}
	}
	public void onIsOn( String[] usersOn ){}
	public void onInvite(String orgin,String orgnick,String invitee,String chan)
	{
		orgnick = parseNick(orgnick);
		invitee = parseNick(invitee);
		tSCWnd = findWindow(chan);
		if(tSCWnd != null)
		{
			tSCWnd.out.append(orgin + ": " + orgnick + " invited " + invitee + " into the channel.");
			return;
		}
		for(SlenChat tSCWnd : wndList)
		{
			if(tSCWnd.visible)
			{
				tSCWnd.out.append("You have been invited into the " + chan + " channel by "
						+ orgnick + ".  Type /ACCEPT "+ chan + " to accept the invitation.");
			}
		}
		out.append("You have been invited into the " + chan + " channel by "
						+ orgnick + ".  Type /ACCEPT "+ chan + " to accept the invitation.");
	}
	public void onJoin( String user, String nick, String chan, boolean create )
	{
		nick = parseNick(nick);
		tSCWnd = findWindow(chan);
		if(tSCWnd == null)	return;
		if(tSCWnd.userList != null)
		{
			tSCWnd.userList.addUser(user.trim(), nick.trim());
			tSCWnd.out.append(nick + " has joined the channel", Color.GREEN.darker());
		}
	}
	public void onJoins(String users, String chan)
	{
	}
	public void onKick( String kicked, String chan, String kicker, String txt )
	{
		tSCWnd = findWindow(chan);
		if(tSCWnd == null)	return;
		tSCWnd.out.append(kicked + " has been kicked from the channel by "+ kicker + ". Reason: " + txt
			, Color.GREEN.darker());
		//	Removes the user from the userlist
		if(tSCWnd.userList != null)
		{
			tSCWnd.userList.rmvUser(kicked);
		}
	}
	public void onMessage(String message)
		{
			out.append(message,Color.GREEN.darker());
		}
	public void onPrivateMessage(String orgnick, String chan, String txt)
		{
			orgnick = parseNick(orgnick);
			//	Searches for existing windows and appends to the appropriate screen
			tSCWnd = findWindow(chan);		//	Checks for a channel
			if(tSCWnd == null){				//	Might be an existing PM screen
				tSCWnd = findWindow(orgnick);
			}

			//	Window exists and text is added
			if(tSCWnd != null){
				tSCWnd.out.append(orgnick + ": " + txt);

				//	Changes the button color if the window isn't visible
				int urgency = tSCWnd.urgent;
				urgency += urgency >= 0 && urgency < 3 ? 1 : 0;
				tSCWnd.makeurgent(urgency);
				return;
			}

			//	Window doesn't exist because its a PM with no channel, so a new one is created
			tSCWnd = findWindow(orgnick);
			if(tSCWnd == null)
				tSCWnd = new SlenChat(this, orgnick, null, false);
			wndList.add(tSCWnd);
	   		tSCWnd.out.append(orgnick + ": " + txt);
		}
	public void onNick( String user, String oldnick, String newnick )
	{
		oldnick = parseNick(oldnick);
		newnick = parseNick(newnick);
		for(SlenChat tSCWnd : wndList)
		{
			//	Changes the links in the appropriate windows so all private messages are still
			//	directed to the correct window
			if(oldnick.equalsIgnoreCase(tSCWnd.getChannel()))
			{
				tSCWnd.setChannel(newnick);
				tSCWnd.out.append(oldnick + " is now known as " + newnick, Color.GREEN.darker());
			}
			//	Changes the nick on the appropriate userlist
			if(tSCWnd.userList != null)
			{
				tSCWnd.userList.changeNick(oldnick, newnick);
			}
		}
	}
	public void onNotice(String text)
	{
		out.append(text);
	}
	public void onPart( String user, String nick, String chan )
	{
		nick = parseNick(nick);
		tSCWnd = findWindow(chan);
		if(tSCWnd == null)	return;
		tSCWnd.out.append(nick + " has left " + chan, Color.GREEN.darker());
		if(tSCWnd.userList != null)	tSCWnd.userList.rmvUser(user);
	}
	public void onOp( String oper, String chan, String oped )
	{
		tSCWnd = findWindow(chan);
		if(tSCWnd != null)
		{
			tSCWnd.out.append(oper + " " + oped);
		}
	}
	public void onParsingError(String message)
	{
		out.append(message, Color.DARK_GRAY);
	}
	public void onPing(String params)
	{
		IRC.writeln("PONG " + params);
	}
	public void onStatus(String msg)
	{
		this.out.append(msg, Color.GRAY);
	}
	public void onTopic(String chanName, String newTopic)
	{
		tSCWnd = findWindow(chanName);
		if(tSCWnd==null)	return;
		tSCWnd.out.append("Topic changed to: " + newTopic, Color.GREEN.darker());
	}
	public void onVersionNotice(String orgnick, String origin, String version){}
	public void onQuit(String user, String nick, String txt )
	{
		nick = parseNick(nick);
		out.append(nick + " quit. Reason: " + txt, Color.GREEN.darker());

		//	Checks all windows for this nick and removes it when found
		for(SlenChat SC : wndList)
		{
			if(SC.userList != null)
			{
				if(SC.userList.containsUser(nick))
				{
					SC.out.append(nick + " has left the channel. Reason: " + txt, Color.GREEN.darker());
					SC.userList.rmvUser(nick);
				}
			}
		}
	}

	public void onReplyVersion(String version){}
	public void onReplyListUserChannels(int channelCount){}
	public void onReplyListStart(){}
	public void onReplyList(String channel, int userCount, String topic){}
	public void onReplyListEnd(){}
	public void onReplyListUserClient(String msg){}
	public void onReplyWhoIsUser(String nick, String user,
								String name, String host)
	{
		nick = parseNick(nick);
		for(SlenChat tSCWnd : wndList)
		{
			if(tSCWnd.userList != null)
				if(tSCWnd.userList.containsNick(nick))
					tSCWnd.userList.changeUser("", user);
		}
	}
	public void onReplyWhoIsServer(String nick, String server, String info){}
	public void onReplyWhoIsOperator(String info){}
	public void onReplyWhoIsIdle(String nick, int idle, Date signon){}
	public void onReplyEndOfWhoIs(String nick){}
	public void onReplyWhoIsChannels(String nick, String channels){}
	public void onReplyMOTDStart()
	{
		out.append("Receiving MOTD: ", Color.CYAN.darker());
	}
	public void onReplyMOTD(String msg)
	{
		out.append(msg);
	}
	public void onReplyMOTDEnd()
	{
		out.append("MOTD Received.", Color.CYAN.darker());
		out.append("Press ` to bring down the console.");
		out.append("Press CTRL + O to open the options menu.");
	}
	public void onReplyNameReply(String channel, String users)
	{
		tSCWnd = findWindow(channel);
		if(tSCWnd == null)	return;
		//	Splits up the users string into an array of strings containing the user names
		//	then adds them to the temporary list
		final String tNick[] = users.split(" ");
		new Thread(){
			public void run()
			{
				for(int i = 0; i < tNick.length; i++)
				{
					tNick[i] = parseNick(tNick[i]);
					tSCWnd.userList.addUser("", tNick[i]);
					IRC.writeln("WHOIS " + tNick[i]);
				}
			}
		}.start();
	}
	public void onReplyTopic(String channel, String topic)
	{
		tSCWnd = findWindow(channel);
		if(tSCWnd == null)	return;
		tSCWnd.out.append(topic, Color.GREEN.darker());
	}
	public void onErrorNoMOTD()
	{
		out.append("No MOTD received");
	}
	public void onErrorNeedMoreParams()
	{
		out.append("Error: Command needs more parameters");
	}
	public void onErrorNoNicknameGiven()
	{
		out.append("Error: No nickname provided, please provide a nickname.");
	}
	public void onErrorNickNameInUse(String badNick)
	{
		out.append("Nick: \"" + badNick + "\" already in use");
		if(!badNick.equals(CustomConfig.ircAltNick)){
			user = CustomConfig.ircAltNick;
			IRC.sendNick(CustomConfig.ircAltNick);
		}
	}
	public void onErrorNickCollision(String badNick)
	{
		out.append("Nick collission: " + badNick);
	}
	public void onErrorErroneusNickname(String badNick)
	{
		out.append("Erroneous Nickname: \"" + badNick + "\".  Please pick a different one");
	}
	public void onErrorAlreadyRegistered()
	{
		out.append("Already registered");
	}
	public void onErrorUnknown(String message)
	{
	//	out.append("ERROR: " + message);
	}
	public void onErrorUnsupported(String message)
	{
	//	out.append("ERROR: " + message);
	}

	public void uimsg(String name, Object... args) {
		if(name == "log") {
		    out.append((String)args[0]);
		} else {
		    super.uimsg(name, args);
		}
    }

	public void wdgmsg(Widget sender, String msg, Object... args) {
		if(sender == in)
		{
			handleInput((String)args[0], this);
			in.settext("");
			return;
		}
		super.wdgmsg(sender, msg, args);
	}
	//	Attempts to find a window of the same title; returns null if no windows match.
	public SlenChat findWindow(String wndTitle)
	{
		if(wndList == null)	return null;
		//	Searches for existing windows
		for(SlenChat tSCWnd : wndList)
		{
			//	There are no windows in the list
			if(tSCWnd == null)	break;

			//	Found a matching window
			if(wndTitle.equalsIgnoreCase(tSCWnd.title))
			{
				return tSCWnd;
			}
		}
		//	No existing windows match
		return null;
	}

	public static String parseNick(String nickname)
	{
		return nickname.replace('~',' ').replace('@',' ').replace('%', ' ').replace('+', ' ').trim();
	}
	public void destroy()
	{
		IRC.close();
		for(SlenChat tSCWnd : wndList)
		{
			parent.remwnd(tSCWnd);
		}
		wndList.clear();
	}
}