package haven;

import java.util.*;
import java.util.regex.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.datatransfer.*;

public class ExtTextlog extends Widget implements ClipboardOwner{

	private List<GLCharacter> characterMap;
	private BufferedImage visibleCharacters = new BufferedImage(sz.x,sz.y,BufferedImage.TYPE_INT_ARGB);
	private BufferedImage drawnCharacters = new BufferedImage(sz.x,sz.y*20,BufferedImage.TYPE_INT_ARGB);
	private Coord nextCharLoc = new Coord(3,0);
	private int lineHeight = 0;
	private Scrollbar scrollBar;
	private boolean scrollLocked;
	private boolean dragging = false;
	private boolean ctrlPressed = false;
	private Rectangle selectedArea = new Rectangle(0,0,0,0);
	private String selectedText ="";
	static Color transparent = new Color(Color.WHITE.getRed(), Color.WHITE.getGreen(), Color.WHITE.getBlue(), Color.TRANSLUCENT);
	static Color alphaBlue = new Color(Color.BLUE.darker().getRed(),Color.BLUE.darker().getGreen(),Color.BLUE.darker().getBlue(),100);
    static Tex texpap = Resource.loadtex("gfx/hud/texpap");
    private Text.Foundry fnd = new Text.Foundry(new Font("SansSerif", Font.PLAIN, 9), Color.BLACK);

	static {
	Widget.addtype("log", new WidgetFactory() {
		public Widget create(Coord c, Widget parent, Object[] args) {
		    return(new ExtTextlog(c, (Coord)args[0], parent));
		}
	    });
    }

    public void draw(GOut g) {
	Coord dc = new Coord();
	for(dc.y = 0; dc.y < sz.y; dc.y += texpap.sz().y) {
	    for(dc.x = 0; dc.x < sz.x; dc.x += texpap.sz().x) {
		g.image(texpap, dc);
	    }
	}
	if(lineHeight > 0 && scrollBar != null)
		g.image(visibleCharacters,Coord.z);
	g.chcolor(alphaBlue);
	if(selectedArea != null)
		synchronized (selectedArea)
		{
			g.frect(new Coord(selectedArea.x,selectedArea.y),new Coord(selectedArea.width,selectedArea.height));
		}
	g.chcolor();
	super.draw(g);
    }

    public ExtTextlog(Coord c, Coord sz, Widget parent) {
	super(c, sz, parent);
	characterMap = new LinkedList<GLCharacter>();
	setcanfocus(true);
	scrollBar = new Scrollbar(Coord.z.add(sz.x,5), sz.y-10, this,0,0);
    }

    public synchronized void append(String line, Color col) {
    	if(line == null || line == "") line = ".";
    	if(line.contains("\n"))
    	{
    		String[] lines = line.trim().split("\n");
    		for(int i = 0; i < lines.length; i++)
    		{
    			append(lines[i], col);
    		}
    		return;
    	}
    	GLCharacter tGLChar;
    	String[] words = line.trim().split(" ");
    	nextCharLoc.x = 3;
    	nextCharLoc.y += lineHeight;

    	Graphics g = drawnCharacters.getGraphics();
    	if(drawnCharacters.getHeight() < nextCharLoc.y + lineHeight)
    	{
    		BufferedImage tDrawnCharacters = drawnCharacters;
    		drawnCharacters = new BufferedImage(drawnCharacters.getWidth(),
    											drawnCharacters.getHeight(),
    											BufferedImage.TYPE_INT_ARGB);
    		g = drawnCharacters.getGraphics();
    		tDrawnCharacters = tDrawnCharacters.getSubimage(0,lineHeight,tDrawnCharacters.getWidth(),tDrawnCharacters.getHeight()-lineHeight);
    		g.drawImage(tDrawnCharacters,0,0, null);
    		nextCharLoc.y -= lineHeight;
    		scrollLocked = true;
    	}

    	fnd.defcol = col;
    	if(nextCharLoc.y+lineHeight > sz.y && !scrollLocked)
    	{
    		scrollBar.max++;
    	}
    	if(scrollBar.val+1 == scrollBar.max)
		{
		    scrollBar.val = scrollBar.max;
		}

    	for(int i = 0; i < words.length; i++){
    		if(nextCharLoc.x + fnd.strsize(words[i]).x >= sz.x-5)
    		{
    			tGLChar = new GLCharacter(' ', nextCharLoc, fnd, false);
    			synchronized(characterMap)
	    		{
	    			characterMap.add(tGLChar);
	    			g.drawImage(tGLChar.img(), tGLChar.location.x,tGLChar.location.y*20, null);
	    		}
    			nextCharLoc.x = 3;
    			nextCharLoc.y += lineHeight;
    			if(nextCharLoc.y+lineHeight > sz.y && !scrollLocked)
		    	{
		    		scrollBar.max++;
		    	}
    			if(scrollBar.val+1 == scrollBar.max)
		    	{
		    		scrollBar.val = scrollBar.max;
		    	}
	    	}
	    	Pattern pat = Pattern.compile("((http://)(\\S+))|((www.)(\\S+))|(\\S+\\.(co|net|com|org|edu)(\\S+)?+)", Pattern.CASE_INSENSITIVE);
	    	Matcher match = pat.matcher(words[i]);
	    	boolean isLink = match.find();

	    	for(int j = 0; j < words[i].length(); j++)
	    	{
	    		tGLChar = new GLCharacter(words[i].charAt(j),nextCharLoc ,fnd,false);
	    		if(isLink) tGLChar.setLink(words[i]);
	    		synchronized(characterMap)
	    		{
	    			characterMap.add(tGLChar);
	    			g.drawImage(tGLChar.img(), tGLChar.location.x,tGLChar.location.y, null);
	    		}
	    		nextCharLoc.x += tGLChar.size().x;
	    		if(lineHeight == 0){
	    			lineHeight = tGLChar.size().y;
	    		}
	    	}
	    	if(i != words.length)
	    	{
	    		tGLChar = new GLCharacter(' ',nextCharLoc ,fnd,false);
	    		synchronized(characterMap)
	    		{
	    			characterMap.add(tGLChar);
	    			g.drawImage(tGLChar.img(), tGLChar.location.x,tGLChar.location.y, null);
	    		}
	    		nextCharLoc.x += tGLChar.size().x;
	    	}
    	}
    	updateDrawData();
    }

    public synchronized void append(String line) {
		append(line, Color.BLACK);
    }

    public void uimsg(String msg, Object... args) {
		if(msg == "apnd") {
	    	append((String)args[0]);
		}
    }
    public void updateDrawData()
    {
    	BufferedImage background = ((TexI)texpap).back;
    	Graphics g = visibleCharacters.createGraphics();
    	g.clearRect(0,0,sz.x,sz.y);
    	g.drawImage(background,0,0,sz.x,sz.y, null);
    	if(lineHeight > 0 && scrollBar != null)
    		g.drawImage(drawnCharacters, 0, 0-(lineHeight*(scrollBar.val)), null);
   }

    public boolean mousewheel(Coord c, int amount) {
   		if(scrollBar.mousewheel(c,amount))
   		{
   			updateDrawData();
   			return true;
   		}
   		return super.mousewheel(c, amount);
    }

    public boolean mousedown(Coord c, int button) {
    	ui.grabmouse(this);
    	if(c.x >= sz.x-5 && c.x <= sz.x && scrollBar.mousedown(c,button))
    	{
    		updateDrawData();
    		return true;
    	}
    	if(button == 1 && c.x >= 0 && c.x < sz.x-5 && c.y >=0 && c.y < sz.y)
    	{
    		parent.setfocus(this);
    		dragging = true;
    		synchronized (selectedArea)
    		{
    			selectedArea.setBounds(c.x,c.y,0,0);
    		}
    		return true;
    	}
	return(super.mousedown(c, button));
    }

    public void mousemove(Coord c) {
    	if(c.x >= sz.x-5 && c.x <= sz.x)
    	{
    		scrollBar.mousemove(c);
    		updateDrawData();
    		return;
    	}
    	if(dragging && c.x >= 0 && c.x < sz.x-5 && c.y >0 && c.y < sz.y){
    		synchronized (selectedArea)
    		{
    			selectedArea.setSize(c.x-selectedArea.getLocation().x, c.y-selectedArea.getLocation().y);
    		}
    		return;
    	}
    }

    public boolean mouseup(Coord c, int button) {
    	ui.grabmouse(null);
    	if(c.x >= sz.x-5 && c.x <= sz.x && scrollBar.mouseup(c, button))
    	{
    		updateDrawData();
    		return true;
    	}
    	if(button == 1 && c.x >= 0 && c.x < sz.x-5 && dragging)
    	{
    		dragging = false;
    		if(selectedArea.width == 0 && selectedArea.height == 0)
    		{
    			return checkLink();
    		}
	    	if(selectedArea.width < 0)
		   	{
		   		selectedArea.x += selectedArea.width;
		   		selectedArea.width = Math.abs(selectedArea.width);
		   	}
		   	if(selectedArea.height < 0)
		   	{
		   		selectedArea.y += selectedArea.height;
		   		selectedArea.height = Math.abs(selectedArea.height);
		   	}
	    	showSelected();
	    	synchronized (selectedArea)
	    	{
	    		selectedArea.setBounds(0,0,0,0);
	    	}
	    	return true;
    	}
		return(super.mouseup(c, button));
    }
    public void lostfocus()
    {
    	selectedArea.setBounds(0,0,0,0);
    	showSelected();
    }
    public boolean checkLink()
    {
    	Rectangle charArea;

    	selectedArea.width = 2;
    	selectedArea.height = 2;
    	synchronized(characterMap)
    	{
	    	for(GLCharacter tGLChar : characterMap)
	    	{
	    		charArea = new Rectangle(tGLChar.location.x, tGLChar.location.y-(lineHeight*scrollBar.val),
	    							 tGLChar.size().x, tGLChar.size().y);
	    		if(selectedArea.intersects(charArea) && tGLChar.isLink())
	    		{
	    			selectedArea.setBounds(0,0,0,0);
	    			return tGLChar.activate();
	    		}
	    	}
    	}
    	selectedArea.setBounds(0,0,0,0);
    	return false;
    }
    public void showSelected()
    {
    	Rectangle charArea;
    	Graphics g = drawnCharacters.getGraphics();
    	selectedText = "";

    	synchronized(characterMap)
    	{
	    	for(GLCharacter tGLChar : characterMap)
	    	{
	    		charArea = new Rectangle(tGLChar.location.x, tGLChar.location.y-(lineHeight*scrollBar.val),
	    							 tGLChar.size().x, tGLChar.size().y);
	    		if(!selectedArea.intersects(charArea) && tGLChar.isSelected())
	    		{
	    			tGLChar.setSelected(false);
	    			g.drawImage(tGLChar.img(), tGLChar.location.x, tGLChar.location.y, null);
	    		}
	    		if(selectedArea.intersects(charArea) && !tGLChar.isSelected())
	    		{
	    			tGLChar.setSelected(true);
	    			g.drawImage(tGLChar.img(), tGLChar.location.x, tGLChar.location.y, null);
	    			selectedText += tGLChar.letter();
	    		}
	    	}
    	}
    	updateDrawData();
    }
	public boolean type(char ch, KeyEvent ev)
	{
		ch+=96;
		if(( ch == 'c' || ch == 'C') && ev.isControlDown())
		{
			setClipboardContents();
			return true;
		}
		ch-=96;
		return super.type(ch,ev);
	}
	/**
	 * Method lostOwnership
	 *
	 *
	 * @param clipboard
	 * @param contents
	 *
	 */
	public void lostOwnership(Clipboard clipboard, Transferable contents) {
		// TODO: Add your code here
	}
	public void setClipboardContents()
	{
		if(selectedText.trim().equals(""))	return;
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(selectedText), this);
	}
}
