package lskSocket;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.ListIterator;
import java.util.Properties;

import tank.*;
import wingman.game.BackgroundObject;
import wingman.game.BigExplosion;
import wingman.game.Bullet;
import wingman.game.PlayerShip;
import wingman.game.Ship;
import wingman.game.SmallExplosion;

import javax.swing.*;

public class SocketClient{
    private static Socket socket;
    private static Writer writer;
    public static TankLevel level;
    public static int mapNum;
    public static int playerId;
    public static int playerName;//�����̬������InputController����
    public static void main(String[] args) throws IOException {
        SocketClient socketClient=new SocketClient();
        //��ʼ��Ϸ����
        JFrame jFrame=new JFrame();
        jFrame.setLayout(null);
        jFrame.setSize(500,500);
        Button button=new Button("Start Game");
        button.setSize(100,50);
        button.setLocation(200,200);
        button.setVisible(true);

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                jFrame.dispose();
                socketClient.StartGame();
            }
        });
        jFrame.add(button);
        jFrame.setLocation(500,300);
        jFrame.setResizable(false);
        jFrame.setVisible(true);
    }

    public void StartGame(){
        TankWorld tankWorld = TankWorld.getInstance();
        SocketClient socketClient = new SocketClient();
        try{
            socket = new Socket("127.0.0.1", 8099);
        }catch (IOException e){
            System.out.println(e.getMessage());
        }
        System.out.println("���ӳɹ�");

        Thread thread = new Thread(new receiveMsgThread(tankWorld, socketClient));
        thread.start();
    }

    static class receiveMsgThread implements Runnable {
        private TankWorld tankWorld;
        private SocketClient socketClient;

        public receiveMsgThread(TankWorld tankWorld, SocketClient socketClient) {
            this.tankWorld = tankWorld;
            this.socketClient = socketClient;
        }

        @Override
        public void run() {
            while (true) {
                receiveMsgHandler(tankWorld, socketClient);
            }

        }
    }

    public static void receiveMsgHandler(TankWorld tankWorld, SocketClient socketClient) {
        String msg = null;
        try {
            msg = socketClient.receiveMsg(SocketClient.socket);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] strings = msg.split("\\+");
        /*����������
         * ��������strings����ĳ���С��2���������֪����ʲôԭ������¼�Ŀ���������
         * ԭ���д�����
         * ����������һ������
         * */
        if (strings.length < 9) {
            return;
        }
        //Э���ʽ�� +��-+playerId+Location+direction+live+health+score+strength
        String FirstString = strings[0];
        String Id = strings[1];
        int x = Integer.parseInt(strings[2]);
        int y = Integer.parseInt(strings[3]);
        int direction = Integer.parseInt(strings[4]);//ͬʱҲ��frame��ֵ
        int live = Integer.parseInt(strings[5]);//ͬʱҲ���ӵ���id
        int health = Integer.parseInt(strings[6]);
        int score = Integer.parseInt(strings[7]);
        int strength = Integer.parseInt(strings[8]);
        int respawnCounter = 0;
        if (strings.length > 9) {
            respawnCounter = Integer.parseInt(strings[9]);
        }

        if (FirstString.equals("randomMap")) {//�����ͼָ��
            //level = new TankLevel("Resources/level" + x + ".txt");
            mapNum=x;
            //ֻ�н��յ���ͼָ����ܿ�ʼ����
            try {
                TankWorld.getInstance().tankWorkStart();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }else if(FirstString.equals("playerId")){
            playerId=x;
            playerName=x+1;
        } else if (FirstString.equals("1")) {//�û�playerλ����Ϣ
            ListIterator<PlayerShip> players = tankWorld.getPlayers();//���ListIterator����
            while (players.hasNext()) {
                Tank player = (Tank) players.next();
                if (player.getName().equals(Id)) {
                    player.setLocation(new Point(x, y));
                    player.setDirection(direction);
                    player.setLive(live);
                    player.setHealth(health);
                    player.setScore(score);
                    player.setStrength(strength);
                }
            }
        } else if (FirstString.equals("11")) {//Aiplayerλ����Ϣ
            ListIterator<PlayerShip> players = tankWorld.getAiPlayers();
            while (players.hasNext()) {
                AiTank player = (AiTank) players.next();
                if (player.getName().equals(Id)) {
                    player.setLocation(new Point(x, y));
                    player.setDirection(direction);
                }
            }
        } else if (FirstString.equals("*")) {//�ӵ�λ����Ϣ
            ArrayList<Bullet> arrayList = tankWorld.getBullet();
            for (int i = 0; i < arrayList.size(); i++) {
                if (arrayList.get(i).BulletID == Integer.parseInt(Id)) {
                    tankWorld.getBullet().get(i).setLocation(new Point(x, y));
                }
            }
        } else if (FirstString.equals("^")) {//�����ӵ�
            ArrayList<PlayerShip> players = tankWorld.getPlayer();
            for (int i = 0; i < players.size(); i++) {
                Tank player = (Tank) players.get(i);
                if (player.getName().equals(Id)) {
                    Bullet tankBullet = new TankBullet(new Point(x, y), new Point(0, 0), 10, player, live);
                    tankWorld.addBullet(tankBullet);
                }
            }

            ArrayList<PlayerShip> aiplayers = tankWorld.getAiPlayer();
            for (int i = 0; i < aiplayers.size(); i++) {
                AiTank aiplayer = (AiTank) aiplayers.get(i);
                if (aiplayer.getName().equals(Id)) {
                    AiTankBullet AiTankBullet = new AiTankBullet(new Point(x, y), new Point(0, 0), 10, aiplayer, live);
                    tankWorld.addBullet(AiTankBullet);
                }
            }
        } else if (FirstString.equals("#")) {//�Ƴ��ӵ�
            /*����ж�һ��ûʲô�ã���Ϊ�ӵ�һ�㶼��С��ը��˳���Ƴ���
             * �����Ҫ���𵽶���ȥ�ӵ�������
             * */
            ArrayList<Bullet> bullets = tankWorld.getBullet();
            for (int i = 0; i < bullets.size(); i++) {
                Bullet bullet = bullets.get(i);
                if (bullet.BulletID == Integer.parseInt(Id)) {//||bullet.BulletID<Integer.parseInt(Id)
                    tankWorld.getBullet().remove(i);
                }
            }
        } else if (FirstString.equals("SmallExplosion")) {//����С��ը
            Point point = new Point(x, y);
            tankWorld.addBackground(new SmallExplosion(point));
            //�Ƴ��ӵ�
            ArrayList<Bullet> bullets = tankWorld.getBullet();
            for (int i = 0; i < bullets.size(); i++) {
                Bullet bullet = bullets.get(i);
                if (bullet.BulletID == Integer.parseInt(Id)) {
                    tankWorld.getBullet().remove(i);
                }

            }
        } else if (FirstString.equals("smallAnimation")) {
            ArrayList<BackgroundObject> backgroundObjects = tankWorld.getBackgroundObject();
            for (int i = 0; i < backgroundObjects.size(); i++) {
                if ((backgroundObjects.get(i) instanceof SmallExplosion) && (backgroundObjects.get(i).getLocationPoint().x == x) && (backgroundObjects.get(i).getLocationPoint().y == y)) {
                    backgroundObjects.get(i).img = SmallExplosion.animation[direction];
                }
            }
        } else if (FirstString.equals("RemoveSmallExplosion")) {
            ArrayList<BackgroundObject> backgroundObjects = tankWorld.getBackgroundObject();
            for (int i = 0; i < backgroundObjects.size(); i++) {//һ��ȷ��С��ը�Ƴ�
                if ((backgroundObjects.get(i) instanceof SmallExplosion) && (backgroundObjects.get(i).getLocationPoint().x == x) && (backgroundObjects.get(i).getLocationPoint().y == y)) {
                    tankWorld.getBackgroundObject().remove(i);
                }
            }
            for(int i=0;i<backgroundObjects.size();i++) {//����ȷ��С��ը�Ƴ�
                if(backgroundObjects.get(i) instanceof SmallExplosion) {
                    tankWorld.getBackgroundObject().remove(i);
                }
            }
        } else if (FirstString.equals("BigExplosion")) {
            Point point = new Point(x, y);
            tankWorld.addBackground(new BigExplosion(point));
        } else if (FirstString.equals("bigAnimation")) {
            ArrayList<BackgroundObject> backgroundObjects = tankWorld.getBackgroundObject();
            for (int i = 0; i < backgroundObjects.size(); i++) {
                if (backgroundObjects.get(i).getLocationPoint().x == x && backgroundObjects.get(i).getLocationPoint().y == y) {
                    backgroundObjects.get(i).img = BigExplosion.animation[direction];
                }
            }
        } else if (FirstString.equals("RemoveBigExplosion")) {
            ArrayList<BackgroundObject> backgroundObjects = tankWorld.getBackgroundObject();
            for (int i = 0; i < backgroundObjects.size(); i++) {
                if (backgroundObjects.get(i).getLocationPoint().x == x & backgroundObjects.get(i).getLocationPoint().y == y) {
                    tankWorld.getBackgroundObject().remove(i);
                }
            }
        } else if (FirstString.equals("BreakableWall")) {
            tankWorld.addBackground(new BreakableWall(x / 32, y / 32));
        } else if (FirstString.equals("RemoveBreakableWall")) {
            ArrayList<BackgroundObject> backgroundObjects = tankWorld.getBackgroundObject();
            for (int i = 0; i < backgroundObjects.size(); i++) {
                if (backgroundObjects.get(i).getLocationPoint().x == x & backgroundObjects.get(i).getLocationPoint().y == y) {
                    tankWorld.getBackgroundObject().remove(i);
                }
            }
        } else if (FirstString.equals("Removepowerup")) {
            ArrayList<Ship> PowerUps = tankWorld.getPowerUp();
            for (int i = 0; i < PowerUps.size(); i++) {
                if (PowerUps.get(i).getLocationPoint().x == x & PowerUps.get(i).getLocationPoint().y == y) {
                    tankWorld.getPowerUp().get(i).show = false;
                }
            }
        } else if (FirstString.equals("gameFinishedAndGameWon")) {
            tankWorld.finishGame();
            tankWorld.setGameWon(true);
        } else if (FirstString.equals("gameFinishedAndNotgameWon")) {
            tankWorld.finishGame();
            tankWorld.setGameWon(false);
        } else if (FirstString.equals("respawnCounter")) {
            ListIterator<PlayerShip> players = tankWorld.getPlayers();//���ListIterator����
            while (players.hasNext()) {
                Tank player = (Tank) players.next();
                if (player.getName().equals(Id)) {
                    player.setDirection(180);
                    player.setLive(live);
                    player.setHealth(health);
                    player.setScore(score);
                    player.setDirection(direction);
                    player.setStrength(strength);
                    player.respawnCounter = respawnCounter;
                }
            }
        } else if (FirstString.equals("AirespawnCounter")) {
            ListIterator<PlayerShip> players = tankWorld.getAiPlayers();//���ListIterator����
            while (players.hasNext()) {
                AiTank player = (AiTank) players.next();
                if (player.getName().equals(Id)) {
                    //player.setLocation(new Point(x, y));
                    player.setDirection(180);
                    player.setLive(live);
                    player.setHealth(health);
                    player.setScore(score);
                    player.setDirection(direction);
                    player.setStrength(strength);
                    player.respawnCounter = respawnCounter;
                }
            }
        } else if (FirstString.equals("tankDisappear")) {
            ListIterator<PlayerShip> players = tankWorld.getPlayers();//���ListIterator����
            while (players.hasNext()) {
                Tank player = (Tank) players.next();
                if (player.getName().equals(Id)) {
                    player.show = false;
                    player.setLive(live);
                    player.setHealth(health);
                    player.setScore(score);
                    player.setDirection(direction);
                    player.setStrength(strength);
                }
            }
        }

    }
    public void sendMsg(String msg, Socket socket) throws IOException {
        if (writer == null) {
            writer = new OutputStreamWriter(socket.getOutputStream(), "UTF-8");
        }
        writer.write(msg);
        writer.write("eof\n");
        writer.flush();// д���Ҫ�ǵ�flush
    }

    /*���شӷ���˽��յ�����Ϣ*/
    public String receiveMsg(Socket socket) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String temp;
        int index;
        //���շ�����������
        while ((temp = br.readLine()) != null) {
            if ((index = temp.indexOf("eof")) != -1) { // ����eofʱ�ͽ�������
                sb.append(temp.substring(0, index));
                break;
            }
            sb.append(temp);
        }
        return sb.toString();

    }


    public static Socket getSocket() {
        return socket;
    }

    public Writer getWriter() {
        return writer;
    }

}

