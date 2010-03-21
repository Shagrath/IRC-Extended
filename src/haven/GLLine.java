package haven;

import java.util.regex.*;
import java.util.List;
import java.util.LinkedList;
import java.awt.image.BufferedImage;
import java.awt.Font;
import java.awt.Color;
import java.awt.Graphics;

public class GLLine{
		public int y, width;
		private static Pattern pat = Pattern.compile("((http://)(\\S+))|((www+\\.)(\\S+))|(\\S+\\.(co|net|com|org|edu|ru)(\\S+)?+)", Pattern.CASE_INSENSITIVE);
		private List<GLCharacter> characters;
		public Text.Foundry fnd = new Text.Foundry(new Font("SansSerif", Font.PLAIN, 9), Color.BLACK);
		public BufferedImage img;
		public GLLine(String line, Color col, int y){
			this.y = y;
			characters = new LinkedList();
    		GLCharacter tGLChar;
	    	String[] words = line.trim().split(" ");
	    	int nextCharLoc = 3;
	    	Coord strSize = fnd.strsize(line);
	    	fnd.defcol = col;
	    	img = new BufferedImage(strSize.x, strSize.y, BufferedImage.TYPE_INT_ARGB);
	    	Matcher match = null;
	    	Graphics g = img.getGraphics();
	    	for(int i = 0; i < words.length; i++){
	    		match = pat.matcher(words[i]);
	    		boolean isLink = match.find();

		    	for(int j = 0; j < words[i].length(); j++)
		    	{
		    		tGLChar = new GLCharacter(words[i].charAt(j),nextCharLoc ,fnd,false);
		    		if(isLink) tGLChar.setLink(words[i]);
		    		synchronized(characters)
		    		{
		    			characters.add(tGLChar);
		    			g.drawImage(tGLChar.img(), tGLChar.x,0, null);
		    		}
		    		nextCharLoc += tGLChar.size().x;
		    		width += nextCharLoc;
		    	}
		    	if(i != words.length)
		    	{
		    		tGLChar = new GLCharacter(' ',nextCharLoc ,fnd,false);
		    		synchronized(characters)
		    		{
		    			characters.add(tGLChar);
		    			g.drawImage(tGLChar.img(), tGLChar.x,0, null);
		    		}
		    		nextCharLoc += tGLChar.size().x;
		    		width += nextCharLoc;
		    	}
		    } 	
    	}
	}