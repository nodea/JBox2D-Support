/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.eppleton.physics.editor.nodes;

import de.eppleton.physics.editor.scene.WorldEditorScene;
import de.eppleton.physics.editor.scene.WorldScene;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.List;
import org.jbox2d.dynamics.Body;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;

/**
 *
 * @author antonepple
 */
public class FakeChildFactory extends ChildFactory<Body> implements PropertyChangeListener {

    WorldEditorScene scene;
    List<Body> keys = Collections.EMPTY_LIST;

    public FakeChildFactory(WorldEditorScene scene) {
        this.scene = scene;
    }

    public void setKeys(List<Body> currentKeys) {
        keys = currentKeys;
        refresh(true);
    }

    @Override
    protected boolean createKeys(List<Body> toPopulate) {
        toPopulate.addAll(keys);
        return true;
    }

    @Override
    protected Node createNodeForKey(Body key) {
        BodyNode bodyNode = new BodyNode(key);
        bodyNode.addPropertyChangeListener(this);
        return bodyNode;
    }

    
    @Override
    public void propertyChange(PropertyChangeEvent pce) {
       // TODO replace with something new
        //scene.updateBody();
    }
}
