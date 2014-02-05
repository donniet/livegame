package com.livegameengine.persist;

import javax.jdo.JDOHelper;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Transaction;
import javax.jdo.listener.InstanceLifecycleEvent;
import javax.jdo.listener.InstanceLifecycleListener;
import javax.jdo.listener.LoadLifecycleListener;
import javax.jdo.spi.PersistenceCapable;

import org.springframework.beans.factory.annotation.Autowired;

import com.livegameengine.model.*;
import com.livegameengine.util.Util;

public final class PMF {
    private PersistenceManagerFactory pmfInstance = null;
    
    @Autowired Util util;

    private PMF() {
    	pmfInstance = JDOHelper.getPersistenceManagerFactory("transactions-optional");
    	
    	pmfInstance.addInstanceLifecycleListener(new LoadLifecycleListener() {
			@Override
			public void postLoad(InstanceLifecycleEvent e) {
				if(GameContextAware.class.isAssignableFrom(e.getPersistentInstance().getClass())) {
					GameContextAware aware = (GameContextAware)e.getPersistentInstance();
					
					aware.setUtilityObject(util);
				}
			}
		}, new Class[] {
				ClientMessage.class,
				Game.class,
				GameState.class,
				GameStateData.class,
				GameType.class,
				GameUser.class,
				Player.class,
				Watcher.class
		});
    }

    public PersistenceManagerFactory getFactory() {
        return pmfInstance;
    }
}