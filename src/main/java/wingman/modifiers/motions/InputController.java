package wingman.modifiers.motions;

import wingman.WingmanWorld;
import wingman.game.PlayerShip;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.Socket;
import java.util.Observable;

import lskSocket.SocketClient;
import tank.TankWorld;

public class InputController extends MotionController implements KeyListener {
    Field field;
    Method action;
    int moveState;
    int[] keys;
    boolean player;
    private PlayerShip tankPlayer;

    public InputController(PlayerShip player) {
        this(player,
                new int[]{KeyEvent.VK_LEFT, KeyEvent.VK_UP, KeyEvent.VK_RIGHT, KeyEvent.VK_DOWN, KeyEvent.VK_SPACE});
        moveState = 0;
        this.player = true;
    }

    public InputController(PlayerShip player, int[] keys) {
        this(player, keys, WingmanWorld.getInstance());

    }

    public InputController(PlayerShip player, int[] keys, Component world) {
        this.addObserver(player);
        this.action = null;
        this.field = null;
        this.keys = keys;
        this.tankPlayer=player;
        world.addKeyListener(this);
    }

    public void signalKeyPress(KeyEvent e) {

    }

    private void setMove(String direction) {
        try {
            field = PlayerShip.class.getDeclaredField(direction);
            moveState = 1;
            this.setChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
        notifyObservers();
    }

    public void read(Object theObject) {
        PlayerShip player = (PlayerShip) theObject;

        try {
            field.setInt(player, moveState);
        } catch (Exception e) {
            // e.printStackTrace();
            try {
                action.invoke(player);
            } catch (Exception e2) {
            }
        }
    }

    private void setFire() {
        field = null;
        try {
            action = PlayerShip.class.getMethod("startFiring");
            this.setChanged();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        notifyObservers();
    }

    private void unsetMove(String direction) {
        try {
            field = PlayerShip.class.getDeclaredField(direction);
            moveState = 0;
            this.setChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
        notifyObservers();
    }

    private void unsetFire() {
        field = null;
        try {
            action = PlayerShip.class.getMethod("stopFiring");
            this.setChanged();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        notifyObservers();
    }


    public void clearChanged() {
        super.clearChanged();
    }

    @Override
    public void update(Observable o, Object arg) {

    }

    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();
        Socket socket = SocketClient.getSocket();
        SocketClient socketClient = new SocketClient();
        // left
        if (code == keys[0]) {
            try {
                socketClient.sendMsg("1"+this.tankPlayer.getName()+"left", socket);
                // this.setMove("left");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        // up
        else if (code == keys[1]) {
            try {
                socketClient.sendMsg("1"+this.tankPlayer.getName()+"up", socket);
                // this.setMove("up");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        // right
        else if (code == keys[2]) {
            try {
                socketClient.sendMsg("1"+this.tankPlayer.getName()+"right", socket);
                // this.setMove("right");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        // down
        else if (code == keys[3]) {
            try {
                socketClient.sendMsg("1"+this.tankPlayer.getName()+"down", socket);
                // this.setMove("down");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        // fire
        else if (code == keys[4]) {
            try {
                socketClient.sendMsg("*"+this.tankPlayer.getName()+"isFiring", socket);
                // this.setFire();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        setChanged();
        this.notifyObservers();
    }

    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();
        SocketClient socketClient = new SocketClient();
        Socket socket = socketClient.getSocket();
        if (code == keys[0]) {
            try {
                socketClient.sendMsg("0"+this.tankPlayer.getName()+"left", socket);
                // this.unsetMove("left");
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else if (code == keys[1]) {
            try {
                socketClient.sendMsg("0"+this.tankPlayer.getName()+"up", socket);
                // this.unsetMove("up");
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else if (code == keys[2]) {
            try {
                socketClient.sendMsg("0"+this.tankPlayer.getName()+"right", socket);
                // this.unsetMove("right");
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        } else if (code == keys[3]) {
            try {
                socketClient.sendMsg("0"+this.tankPlayer.getName()+"down", socket);
                // this.unsetMove("down");
            } catch (IOException e1) {
                e1.printStackTrace();
            }

        } else if (code == keys[4]) {
            try {
                socketClient.sendMsg("%"+this.tankPlayer.getName()+"isFiring", socket);
                // this.unsetFire();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        setChanged();
        this.notifyObservers();
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}