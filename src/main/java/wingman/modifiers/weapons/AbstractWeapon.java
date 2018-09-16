package wingman.modifiers.weapons;

import wingman.GameWorld;
import wingman.WingmanWorld;
import wingman.game.Bullet;
import wingman.game.PlayerShip;
import wingman.game.Ship;
import wingman.modifiers.AbstractGameModifier;

import java.util.Observer;

/*Weapons are fired by motion controllers on behalf of players or ships
 * They observe motions and are observed by the Game World
 */
public abstract class AbstractWeapon extends AbstractGameModifier {
    public int reload = 5;
    protected Bullet[] bullets;
    protected int direction;
    boolean friendly;
    int lastFired = 0, reloadTime;

    public AbstractWeapon() {
        this(WingmanWorld.getInstance());
    }

    public AbstractWeapon(Observer world) {
        super();
        this.addObserver(world);
    }

    public void fireWeapon(Ship theShip) {
        if (theShip instanceof PlayerShip) {
            direction = 1;
        } else {
            direction = -1;
        }
    }

    /* read is called by Observers when they are notified of a change */
    public void read(Object theObject) {
        GameWorld world = (GameWorld) theObject;
        world.addBullet(bullets);
    }

    public void remove() {
        this.deleteObserver(WingmanWorld.getInstance());
    }

	public Bullet[] getBullets() {
		return bullets;
	}

	public void setBullets(Bullet[] bullets) {
		this.bullets = bullets;
	}

	
    
    
}
