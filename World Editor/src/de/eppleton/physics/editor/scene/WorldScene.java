/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.eppleton.physics.editor.scene;

import de.eppleton.jbox2d.Box2DUtilities;
import de.eppleton.jbox2d.WorldUtilities;
import de.eppleton.physics.editor.palette.items.B2DActiveEditorDrop;
import de.eppleton.physics.editor.scene.widgets.ContainerWidget;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.Joint;
import org.netbeans.api.visual.action.AcceptProvider;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.ConnectorState;
import org.netbeans.api.visual.action.MoveProvider;
import org.netbeans.api.visual.action.ResizeProvider;
import org.netbeans.api.visual.action.SelectProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.model.ObjectScene;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.LayerWidget;
import org.netbeans.api.visual.widget.Widget;
import org.openide.explorer.ExplorerManager;
import org.openide.nodes.Node;
import org.openide.text.ActiveEditorDrop;
import org.openide.util.Exceptions;
import org.openide.util.Lookup.Result;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;

/**
 *
 * @author antonepple
 */
public class WorldScene extends ObjectScene implements LookupListener {

    public static final String DELETE_ACTION = "deleteAction";
    private static Logger LOGGER = Logger.getLogger(WorldScene.class.getName());
    private World world;
    private int scale = 300;
    private float offsetX = 0;
    private float offsetY = 0;
    private final Callback callback;
    private Result<Body> lookupResult;
    private ExplorerManager em;
    private final WidgetAction selectAction;
    private ResizeProvider resizeProvider;
    private LayerWidget mainLayer;
    private LayerWidget connectionLayer;
    private LayerWidget interractionLayer = new LayerWidget(this);
    private LayerWidget backgroundLayer = new LayerWidget(this);
    private WidgetAction moveAction = ActionFactory.createMoveAction(null, new MultiMoveProvider());

    public WorldScene(final ExplorerManager em,
            final World world, Callback callback) {
        addChild(backgroundLayer);
        mainLayer = new LayerWidget(this);
        addChild(mainLayer);
        connectionLayer = new LayerWidget(this);
        addChild(connectionLayer);
        addChild(interractionLayer);
        getActions().addAction(ActionFactory.createRectangularSelectAction(this, backgroundLayer));
        this.em = em;
        this.callback = callback;
        this.world = world;
        selectAction = ActionFactory.createSelectAction(new SelectProviderImpl(em));
        world.step(1.0f / 60, 1, 1);
        this.setBackground(Color.BLACK);
        Box2DUtilities.scaleToFit(world, this);
        super.getActions().addAction(ActionFactory.createZoomAction());
        super.getActions().addAction(ActionFactory.createAcceptAction(new AcceptProviderImpl()));
        updateBodies();
        lookupResult = Utilities.actionsGlobalContext().lookupResult(Body.class);
        lookupResult.addLookupListener(this);
        initGrids();
    }

    private void handleTransfer(Point point, B2DActiveEditorDrop transferData) {
        LOGGER.info("handle Transfer: get Bodies");

        HashMap<Integer, Body> addBodies = transferData.addBodies(world);
        LOGGER.info("retrieved Bodies");
        float x = WorldUtilities.sceneToWorld(point.x, scale, offsetX, false);
        float y = WorldUtilities.sceneToWorld(point.x, scale, offsetX, false);
        LOGGER.info("Configuring the Bodies");

        configureBodies(addBodies, x, y);
        /*for (Body newBody : newBodies) {
         newBody.getPosition().x = 
         newBody.getPosition().y = ≈
         } */
        LOGGER.info("Updateing the scene");

        updateBodies();
        LOGGER.info("Ready updateing the scene");
    }

    private void configureBodies(HashMap<Integer, Body> bodies, float x, float y) {
        Collection<Body> values = bodies.values();
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        for (Body body : values) {
            minX = body.getPosition().x < minX ? body.getPosition().x : minX;
            minY = body.getPosition().y < minY ? body.getPosition().y : minY;
        }
        for (Body body : values) {
            body.getPosition().x = body.getPosition().x - minX + x;
            body.getPosition().y = body.getPosition().y - minY + y;
        }
    }

    public void updateBodies() {
        Body nextBody = world.getBodyList();
        while (nextBody != null) {
            if (nextBody.getFixtureList() != null) {
                Fixture fixture = nextBody.getFixtureList();
                while (fixture != null) {
                    Shape shape = nextBody.getFixtureList().getShape();
                    NodeProvider nodeProvider = NodeManager.getNodeProvider(nextBody, shape);
                    Widget widget = super.findWidget(shape);
                    nodeProvider.configureNode(this, widget, nextBody, shape, offsetX, offsetY, scale);//, transform);}
                    fixture = fixture.getNext();
                }
            }
            nextBody = nextBody.getNext();
        }
        if (getView() != null && getView().isShowing()) {
            Joint nextJoint = world.getJointList();
            while (nextJoint != null) {
                JointProvider jointProvider = JointManager.getJointProvider(nextJoint);
                ConnectionWidget widget = (ConnectionWidget) super.findWidget(nextJoint);
                jointProvider.configureWidget(this, widget, nextJoint, offsetX, offsetY, scale);
                nextJoint = nextJoint.getNext();

            }
        }
        LOGGER.info("update ready");
        repaint();
    }

    void fireChange() {
        validate();
        callback.changed();
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public void setOffsetX(float offsetX) {
        this.offsetX = offsetX;
    }

    public void setOffsetY(float offsetY) {
        this.offsetY = offsetY;
    }

    public WidgetAction getSelectAction() {
        return selectAction;
    }

    public ResizeProvider getResizeProvider() {
        return resizeProvider;
    }

    public WidgetAction getMoveAction() {
        return moveAction;
    }

    public LayerWidget getMainLayer() {
        return mainLayer;
    }

    public World getWorld() {
        return world;
    }

    @Override
    public void resultChanged(LookupEvent ev) {
        Collection<? extends Body> allInstances = lookupResult.allInstances();
        HashSet hashSet = new HashSet();
        for (Body body : allInstances) {
            if (getObjects().contains(body)) {
                hashSet.add(body);
            }
        }
        this.setSelectedObjects(hashSet);
        getView().repaint();
    }

    public float getOffsetY() {
        return offsetY;
    }

    public void initGrids() {
        Image sourceImage = Utilities.loadImage("de/eppleton/physics/editor/scene/resources/paper_grid17.png"); // NOI18N
        int width = sourceImage.getWidth(null);
        int height = sourceImage.getHeight(null);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.drawImage(sourceImage, 0, 0, null);
        graphics.dispose();
        TexturePaint PAINT_BACKGROUND = new TexturePaint(image, new Rectangle(0, 0, width, height));
        setBackground(PAINT_BACKGROUND);
        repaint();
        revalidate(false);
        validate();
    }

    void addConnection(ConnectionWidget widget, Joint joint) {
        connectionLayer.addChild(widget);
        addObject(joint, widget);
    }

    private static class SelectProviderImpl implements SelectProvider {

        private final ExplorerManager em;

        public SelectProviderImpl(ExplorerManager em) {
            this.em = em;
        }

        @Override
        public boolean isAimingAllowed(Widget widget, Point localLocation, boolean invertSelection) {
            return false;
        }

        @Override
        public boolean isSelectionAllowed(Widget widget, Point localLocation, boolean invertSelection) {
            return true;
        }

        @Override
        public void select(Widget widget, Point localLocation, boolean invertSelection) {
            try {
                ObjectScene scene = ((ObjectScene) widget.getScene());
                Object object = scene.findObject(widget);

                scene.setFocusedObject(object);
                if (object != null) {
                    if (!invertSelection && scene.getSelectedObjects().contains(object)) {
                        return;
                    }

                    scene.userSelectionSuggested(Collections.singleton(object), invertSelection);
                } else {
                    scene.userSelectionSuggested(Collections.emptySet(), invertSelection);
                }

                if (object instanceof Node) {
                    Set selected = scene.getSelectedObjects();
                    ArrayList<Node> selectedNodes = new ArrayList<Node>();
                    for (Object object1 : selected) {
                        if (object1 instanceof Node) {
                            selectedNodes.add((Node) object1);
                        }
                    }
                    em.setSelectedNodes(selectedNodes.toArray(new Node[0]));
                }
            } catch (PropertyVetoException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    private class AcceptProviderImpl implements AcceptProvider {

        public AcceptProviderImpl() {
        }

        @Override
        public ConnectorState isAcceptable(Widget widget, Point point, Transferable transferable) {
            try {
                if (transferable.isDataFlavorSupported(ActiveEditorDrop.FLAVOR)
                        && (transferable.getTransferData(ActiveEditorDrop.FLAVOR) instanceof B2DActiveEditorDrop)) {
                    return ConnectorState.ACCEPT;
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
            return ConnectorState.REJECT;
        }

        @Override
        public void accept(Widget widget, Point point, Transferable transferable) {
            try {
                if (transferable.isDataFlavorSupported(ActiveEditorDrop.FLAVOR)
                        && (transferable.getTransferData(ActiveEditorDrop.FLAVOR) instanceof B2DActiveEditorDrop)) {
                    B2DActiveEditorDrop transferData = (B2DActiveEditorDrop) transferable.getTransferData(ActiveEditorDrop.FLAVOR);
                    handleTransfer(widget.convertLocalToScene(point), transferData);
                    fireChange();

                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }

        }
    }

    @Override
    public JComponent createView() {
        super.createView();
        addKeyboardActions();
        return getView();
    }

    private void deleteselectedWidgets() {
        HashSet<Joint> jointsToDestroy = new HashSet< Joint>();
        HashSet<Body> bodiesToDestroy = new HashSet<Body>();


        Set<?> selectedObjects = getSelectedObjects();
        for (Object object : selectedObjects) {
            if (object instanceof Joint) {
                jointsToDestroy.add((Joint) object);
            } else if (object instanceof Body) {
                bodiesToDestroy.add((Body) object);
            }
        }
        for (Joint joint : jointsToDestroy) {
            Widget w = findWidget(joint);
            if (w != null) {

                Widget parentWidget = w.getParentWidget();
                if (parentWidget != null) {
                    parentWidget.removeChild(w);
                } else {
                    System.out.println("Could not find a ParentWidget for selected Joint's widget, that smells like a bug!");
                }
                this.removeObject(joint);
                System.out.println("Destroy Joint " + joint);
                world.destroyJoint(joint);
            } else {
                System.out.println("Could not find a Widget for selected Joint, that smells like a bug!");
            }
        }
        for (Body body : bodiesToDestroy) {
            Widget w = findWidget(body);
//            System.out.println("1. destroy Body " + body);
            if (w != null) {
                Widget parentWidget = w.getParentWidget();
                if (parentWidget != null) {
//                    System.out.println("2.remove widget from parent " + w);
                    parentWidget.removeChild(w);
                } else {
                    System.out.println("Could not find a ParentWidget for selected body's widget, that smells like a bug!");
                }
//                System.out.println("3.remove object from scene " + body);

                this.removeObject(body);

                // first we'll remove all the Joints of this body
                Joint joint = null;
                if (body.getJointList() != null) {
                    joint = body.getJointList().joint;
                }
                while (joint != null) {
//                    System.out.println("4. Checking Joint " + joint);
                    Widget findWidget = findWidget(joint);
                    if (findWidget != null && findWidget.getParentWidget() != null) {
//                        System.out.println("5. Removing Joint widget from parent " + joint);

                        findWidget.getParentWidget().removeChild(findWidget);
//                        System.out.println("6. Removing Joint object from scene " + joint);

                        this.removeObject(joint);
//                        System.out.println("7. destroy joint  " + joint);

                        world.destroyJoint(joint);
                    } else {
                        System.out.println("Either widget not found or ParentWidget is null for a joint of this body, that smells like a bug!");
                    }
                    joint = joint.getNext();
//                    System.out.println("7.5 joint next "+joint);
                }
//                System.out.println("8. now destroy the body "+body);

                world.destroyBody(body);
            } else {
                System.out.println("Could not find a Widget for selected Body, that smells like a bug!");
            }
        }


        repaint();
        revalidate(false);
        validate();
        fireChange();
    }

    public void addKeyboardActions() {
        getView().setFocusable(true);
        getActions().addAction(new MouseClickedAction(getView()));

        getView().getInputMap().put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DELETE, 0), "deleteSelectedWidgets");
        getView().getActionMap().put("deleteSelectedWidgets", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                deleteselectedWidgets();
            }
        });

    }

    private class MultiMoveProvider implements MoveProvider {

        private HashMap<Widget, Point> originals = new HashMap<Widget, Point>();
        private Point original;

        public void movementStarted(Widget widget) {
            originals.put(widget, widget.getPreferredLocation());
            for (Object o : getSelectedObjects()) {
                Widget w = findWidget(o);
                if (w != null && (w instanceof ContainerWidget)) {
                    originals.put(w, w.getPreferredLocation());
                }

            }
        }

        public void movementFinished(Widget widget) {
            originals.clear();
            original = null;
        }

        public Point getOriginalLocation(Widget widget) {
            original = widget.getPreferredLocation();
            return original;
        }

        public void setNewLocation(Widget widget, Point location) {
            try {
                int dx = location.x - original.x;
                int dy = location.y - original.y;
                for (Map.Entry<Widget, Point> entry : originals.entrySet()) {
                    Point point = entry.getValue();
                    entry.getKey().setPreferredLocation(new Point(point.x + dx, point.y + dy));
                }
            } catch (NullPointerException nex) {
                System.out.println("original: "+original);
                System.out.println("location "+location);
                
                nex.printStackTrace();
            }
        }
    }
}
