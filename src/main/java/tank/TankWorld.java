package tank;

import wingman.GameClock;
import wingman.GameSounds;
import wingman.GameWorld;
import wingman.game.*;
import wingman.modifiers.AbstractGameModifier;
import wingman.modifiers.motions.MotionController;
import wingman.modifiers.weapons.AbstractWeapon;
import wingman.modifiers.weapons.PulseWeapon;
import wingman.modifiers.weapons.SimpleWeapon;
import wingman.ui.GameMenu;
import wingman.ui.InfoBar;
import wingman.ui.InterfaceObject;

import javax.swing.*;

import lskSocket.SocketClient;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class TankWorld extends GameWorld {

    public static final GameSounds sound = new GameSounds();//游戏声音
    public static final GameClock clock = new GameClock();//游戏时钟
    // GameWorld is a singleton class!
    private static final TankWorld game = new TankWorld();
    public static HashMap<String, Image> sprites = GameWorld.sprites;
    public static HashMap<String, MotionController> motions = new HashMap<String, MotionController>();
    public TankLevel level;//map
    GameMenu menu;//菜单
    int score = 0, life = 4;//初始化分数为0，生命为4条
    Random generator = new Random();//随机数
    int sizeX, sizeY;
    public Point mapSize;
    // is player still playing, did they win, and should we exit
    private  boolean gameOver, gameWon, gameFinished;
    ImageObserver observer;
    public int playerViewX,playerViewY;
    private Thread thread;
    private BufferedImage bimg, player1view, player2view;
    /*Some ArrayLists to keep track of game things*/
    private ArrayList<Bullet> bullets;
    //private CopyOnWriteArrayList<Bullet> bullets;
    private ArrayList<PlayerShip> players;
    private ArrayList<InterfaceObject> ui;
    private ArrayList<Ship> powerups;
    private static Socket socket;
    private static SocketClient socketClient;
    private ArrayList<PlayerShip> aiplayers;
    
    // constructors makes sure the game is focusable, then
    // initializes a bunch of ArrayLists
    private TankWorld() {
        this.setFocusable(true);
        background = new ArrayList<BackgroundObject>();
        bullets = new ArrayList<Bullet>();
        //bullets=new CopyOnWriteArrayList<>();
        players = new ArrayList<PlayerShip>();
        ui = new ArrayList<InterfaceObject>();
        powerups = new ArrayList<Ship>();
        aiplayers=new ArrayList<>();
    }

    /* This returns a reference to the currently running game*/
    public static TankWorld getInstance() {
        return game;
    }
    
   
	public void tankWorkStart() throws IOException {
        final TankWorld game = TankWorld.getInstance();
        JFrame f = new JFrame("TankWars Game");
        f.addWindowListener(new WindowAdapter() {
            public void windowGainedFocus(WindowEvent e) {
                game.requestFocusInWindow();
            }
        });
        f.getContentPane().add("Center", game);
        f.pack();//使此窗口的大小适合其子组件的首选大小和布局
        
        //图形界面的总体的大小
        f.setSize(new Dimension(900, 600));
        
        game.setDimensions(800, 600);
        game.init();
        
        f.setVisible(true);
        f.setResizable(false);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        GameWorld.sound.playmp3("Resources/AlliedForces.mp3");
        game.start();
    }


    // this is the main function where game stuff happens!
    // each frame is also drawn here
    public void drawFrame(int w, int h, Graphics2D g2) {
        ListIterator<?> iterator = getBackgroundObjects();
        ArrayList<BackgroundObject> backgroundObjects2=getBackgroundObject();
        for(int i=0;i<backgroundObjects2.size();i++) {
        	BackgroundObject obj = backgroundObjects2.get(i);
        	obj.draw(g2, this);
        }
        if (!gameFinished) {
        	//对子弹进行相应的处理
            ArrayList<Bullet> bullets=this.getBullet();
            for(int i=0;i<bullets.size();i++){
                Bullet bullet=bullets.get(i);
                bullet.draw(g2,this);
            }
            // update players and draw
            iterator = getPlayers();
            while (iterator.hasNext()) {
                PlayerShip player = (PlayerShip) iterator.next();
                //bullets = this.getBullets();
                ListIterator<Ship> powerups = this.powerups.listIterator();
                while (powerups.hasNext()) {
                    Ship powerup = powerups.next();
                    powerup.draw(g2, this);                
                }
            }
            PlayerShip p1 = players.get(0);
            PlayerShip p2 = players.get(1);         
            p1.draw(g2, this);
            p2.draw(g2, this);

            //aiplayer
			PlayerShip p3 = aiplayers.get(0);
			p3.draw(g2, this);
			
			PlayerShip p4=aiplayers.get(1);
			p4.draw(g2, this);
        } else {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Calibri", Font.PLAIN, 24));
            if (!gameWon) {
                g2.drawImage(sprites.get("gameover"), w / 3 - 50, h / 2, null);
            } else {
                g2.drawImage(sprites.get("youwon"), sizeX / 3, 100, null);
            }
            g2.drawString("Score", playerViewX+getSize().width/2, playerViewY+50);
            int i = 2;
            for (PlayerShip player : players) {
                g2.drawString(player.getName() + ": " + Integer.toString(player.getScore()), playerViewX+getSize().width/2,playerViewY+50*i);
                i++;
            }
        }

    }
    
    
    /* paint each frame */
    public void paint(Graphics g) {
    	//System.out.println("paint run");
        if (players.size() != 0)
            clock.tick();
        Dimension windowSize = getSize();
        Graphics2D g2 = createGraphics2D(mapSize.x, mapSize.y);
        drawFrame(mapSize.x, mapSize.y, g2);
        g2.dispose();//处理此图形上下文并释放其正在使用的任何系统资源

       /* int p1x = this.players.get(0).getX() - windowSize.width / 4 > 0 ? this.players.get(0).getX() - windowSize.width / 4 : 0;
        int p1y = this.players.get(0).getY() - windowSize.height / 2 > 0 ? this.players.get(0).getY() - windowSize.height / 2 : 0;*/
        int p1x = this.players.get(SocketClient.playerId).getX() - windowSize.width / 4 > 0 ? this.players.get(SocketClient.playerId).getX() - windowSize.width / 4 : 0;
        int p1y = this.players.get(SocketClient.playerId).getY() - windowSize.height / 2 > 0 ? this.players.get(SocketClient.playerId).getY() - windowSize.height / 2 : 0;
        playerViewX=p1x;
        playerViewY=p1y;

        if (p1x > mapSize.x - windowSize.width) {
            p1x = mapSize.x - windowSize.width;
        }
        if (p1y > mapSize.y - windowSize.height) {
            p1y = mapSize.y - windowSize.height;
        }
        player1view = bimg.getSubimage(p1x, p1y, windowSize.width, windowSize.height);
        g.drawImage(player1view, 0, 0, this);
        g.drawRect(windowSize.width / 2 - 76, 399, 151, 151);
        g.drawImage(bimg, windowSize.width / 2 - 75, 400, 150, 150, observer);
        // interface stuff
        ListIterator<InterfaceObject> objects = ui.listIterator();
        int offset = 0;
        while (objects.hasNext()) {
            InterfaceObject object = objects.next();
            object.draw(g, offset, windowSize.height);
            offset += 500;
        }
    }

    /* start the game thread*/
    public void start() {
        thread = new Thread(this);
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    /* run the game */
    public void run() {

        Thread me = Thread.currentThread();
      
        while (thread == me) {
            this.requestFocusInWindow();
            repaint();

            try {
            	//每隔23ms刷新一次
                thread.sleep(23); // pause a little to slow things down
            } catch (InterruptedException e) {
                break;
            }

        }
    }
	
    /*Game Initialization */
    public void init() {
        setBackground(Color.white);
        //setBackground(Color.RED);
        loadSprites();
        gameOver = false;
        observer = this;
        level=new TankLevel("Resources/level"+SocketClient.mapNum+".txt");
        level.addObserver(this);//TankWorld观察TankLevel的变化
        clock.addObserver(level);//TankLevel观察GameClock的变化
        //小地图
        mapSize = new Point(level.w * 32, level.h * 32);//在坐标空间中 (x,y)位置构建和初始化一个点。  
        GameWorld.setSpeed(new Point(0, 0));
        addBackground(new Background(mapSize.x, mapSize.y, GameWorld.getSpeed(), sprites.get("background")));
        level.load();
    }

    /*Functions for loading image resources*/
    protected void loadSprites() {
        sprites.put("background", getSprite("Resources/Background.png"));

        sprites.put("wall", getSprite("Resources/Blue_wall1.png"));
        sprites.put("wall2", getSprite("Resources/Blue_wall2.png"));

        sprites.put("bullet", getSprite("Resources/bullet.png"));
        sprites.put("powerup", getSprite("Resources/powerup.png"));

        sprites.put("explosion1_1", getSprite("Resources/explosion1_1.png"));
        sprites.put("explosion1_2", getSprite("Resources/explosion1_2.png"));
        sprites.put("explosion1_3", getSprite("Resources/explosion1_3.png"));
        sprites.put("explosion1_4", getSprite("Resources/explosion1_4.png"));
        sprites.put("explosion1_5", getSprite("Resources/explosion1_5.png"));
        sprites.put("explosion1_6", getSprite("Resources/explosion1_6.png"));
        sprites.put("explosion2_1", getSprite("Resources/explosion2_1.png"));
        sprites.put("explosion2_2", getSprite("Resources/explosion2_2.png"));
        sprites.put("explosion2_3", getSprite("Resources/explosion2_3.png"));
        sprites.put("explosion2_4", getSprite("Resources/explosion2_4.png"));
        sprites.put("explosion2_5", getSprite("Resources/explosion2_5.png"));
        sprites.put("explosion2_6", getSprite("Resources/explosion2_6.png"));
        sprites.put("explosion2_7", getSprite("Resources/explosion2_7.png"));

        sprites.put("player1", getSprite("Resources/Tank_blue_basic_strip60.png"));
        sprites.put("player2", getSprite("Resources/Tank_blue_basic_strip60.png"));
        
        sprites.put("aiplayer1", getSprite("Resources/Tank_blue_basic_strip60.png"));
		sprites.put("aiplayer2", getSprite("Resources/Tank_blue_basic_strip60.png"));
    }

    public Image getSprite(String name) {
        URL url = TankWorld.class.getResource(name);
        Image img = java.awt.Toolkit.getDefaultToolkit().getImage(url);
        try {
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(img, 0);
            tracker.waitForID(0);
        } catch (Exception e) {
        }
        return img;
    }

    /********************************
     * 	These functions GET things	*
     * 		from the game world		*
     ********************************/

    public int getFrameNumber() {
        return clock.getFrame();
    }

    public int getTime() {
        return clock.getTime();
    }

    public void removeClockObserver(Observer theObject) {
        clock.deleteObserver(theObject);
    }

    public ListIterator<BackgroundObject> getBackgroundObjects() {
        return background.listIterator();
    }
    
    

    public ListIterator<PlayerShip> getPlayers() {
        return players.listIterator();
    }

    public ListIterator<PlayerShip> getAiPlayers() {
        return aiplayers.listIterator();
    }
    
    public ArrayList<PlayerShip> getAiPlayer() {
        return aiplayers;
    }
    
    public ListIterator<Bullet> getBullets() {
        return bullets.listIterator();
    }

    public int countPlayers() {
        return players.size();
    }

    public void setDimensions(int w, int h) {
        this.sizeX = w;
        this.sizeY = h;
    }

    /********************************
     * 	These functions ADD things	*
     * 		to the game world		*
     ********************************/

    public void addBullet(Bullet... newObjects) {
        for (Bullet bullet : newObjects) {
            bullets.add(bullet);
        }
    }

    public void addPlayer(PlayerShip... newObjects) {
        for (PlayerShip player : newObjects) {
            players.add(player);
            ui.add(new InfoBar(player, Integer.toString(players.size())));
        }
    }
    
    public void addAiPlayer(PlayerShip... newObjects) {
		for (PlayerShip player : newObjects) {
			aiplayers.add(player);
		}
	}

    // add background items (islands)
    public void addBackground(BackgroundObject... newObjects) {
        for (BackgroundObject object : newObjects) {
            background.add(object);
        }
    }

    
    // add power ups to the game world
    public void addPowerUp(Ship powerup) {
        powerups.add(powerup);
    }
    
    public ArrayList<Ship> getPowerUp(){
    	return this.powerups;
    }
    

    public void addRandomPowerUp() {
        // rapid fire weapon or pulse weapon
        if (generator.nextInt(10) % 2 == 0)
            powerups.add(new PowerUp(generator.nextInt(sizeX), 1, new SimpleWeapon(5)));
        else {
            powerups.add(new PowerUp(generator.nextInt(sizeX), 1, new PulseWeapon()));
        }
    }

    public void addClockObserver(Observer theObject) {
        clock.addObserver(theObject);
    }


    public Graphics2D createGraphics2D(int w, int h) {
        Graphics2D g2 = null;
        if (bimg == null || bimg.getWidth() != w || bimg.getHeight() != h) {
            bimg = (BufferedImage) createImage(w, h);//创建一个用于双缓冲的屏幕外可绘图像。 参数  width - 指定的宽度 height - 指定的高度 
        }
        g2 = bimg.createGraphics();//创建一个 Graphics2D ，可以用来绘制这个 BufferedImage 
        g2.setBackground(getBackground());//设置Graphics2D上下文的背景颜色
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);//为渲染算法设置单个首选项的值
        g2.clearRect(0, 0, w, h);//通过填充当前图形表面的背景颜色来清除指定的矩形。
        return g2;
    }

   

    /* End the game, and signal either a win or loss */
    public void endGame(boolean win) {
        this.gameOver = true;
        this.gameWon = win;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    // signal that we can stop entering the game loop
    public void finishGame() {
        gameFinished = true;
    }

    /*I use the 'read' function to have observables act on their observers.
     */
    @Override
    public void update(Observable o, Object arg) {
        AbstractGameModifier modifier = (AbstractGameModifier) o;
        modifier.read(this);
    }

	
    /*public void setBullets(ArrayList<Bullet> bullets) {
		this.bullets = bullets;
	}*/

    public ArrayList<Bullet> getBullet() {
        return bullets;
    }
    
   /* public CopyOnWriteArrayList<Bullet> getBullet(){
		return bullets;
	}*/
    
    public ArrayList<BackgroundObject> getBackgroundObject(){
    	return this.background;
    }

	public boolean isGameWon() {
		return gameWon;
	}

	public void setGameWon(boolean gameWon) {
		this.gameWon = gameWon;
	}

	public boolean isGameFinished() {
		return gameFinished;
	}

	public void setGameFinished(boolean gameFinished) {
		this.gameFinished = gameFinished;
	}

	public void setGameOver(boolean gameOver) {
		this.gameOver = gameOver;
	}

	public ArrayList<PlayerShip> getPlayer() {
		return players;
	}
    
    
}