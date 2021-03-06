/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.eppleton.physics.editor.scene.widgets;

import de.eppleton.jbox2d.WorldUtilities;
import de.eppleton.physics.editor.scene.WorldEditorScene;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashMap;
import java.util.List;
import org.jbox2d.dynamics.Body;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.action.ResizeProvider.ControlPoint;
import org.netbeans.api.visual.action.ResizeStrategy;
import org.netbeans.api.visual.border.BorderFactory;
import org.netbeans.api.visual.model.ObjectState;
import org.netbeans.api.visual.widget.Widget;
import org.netbeans.api.visual.widget.Widget.Dependency;

/**
 *
 * @author antonepple
 */
public class BodyWidget extends Widget implements Dependency {

    Body body;
    WorldEditorScene scene;
//    private Map<Shape, java.awt.Shape> shapeMap = new HashMap<Shape, java.awt.Shape>();

    public BodyWidget(WorldEditorScene scene, Body body) {
        super(scene);
        this.scene = scene;
        this.body = body;
        //setLayout(LayoutFactory.createOverlayLayout());
        scene.getMainLayer().addChild(this);
        scene.addObject(body, this);
        // initShapes();
        setPreferredLocation(new Point(
                (int) ((body.getPosition().x + scene.getOffsetX()) * scene.getScale()),
                (int) (((body.getPosition().y * -1) + scene.getOffsetX()) * scene.getScale())));

        addActions(scene, body);
    }
    /*
     @Override
     protected Rectangle calculateClientArea() {
     Collection<java.awt.Shape> values = shapeMap.values();
     Rectangle bounds = null;
     for (java.awt.Shape shape : values) {
     if (bounds == null) {
     bounds = shape.getBounds();
     } else {
     bounds.add(shape.getBounds());
     }
     }
     return bounds;
     }
     */

    final void addActions(final WorldEditorScene scene, final Body body) {
        createActions(WorldEditorScene.DISTANCE_JOINT_TOOL).addAction(scene.getDistanceJointAction());
        createActions(WorldEditorScene.REVOLUTE_JOINT_TOOL).addAction(scene.getRevoluteJointAction());

        createActions(WorldEditorScene.SELECT_TOOL).addAction(scene.createSelectAction());

        getActions().addAction(ActionFactory.createResizeAction(new ResizeStrategy() {
            @Override
            public Rectangle boundsSuggested(Widget widget, Rectangle originalBounds, Rectangle suggestedBounds, ControlPoint controlPoint) {
                return suggestedBounds;
            }
        }, scene.getResizeProvider()));
        createActions(WorldEditorScene.SELECT_TOOL).addAction(scene.getMoveAction());
        addDependency(this);
    }

    @Override
    protected void notifyStateChanged(ObjectState previousState, ObjectState state) {
        super.notifyStateChanged(previousState, state);
        //setBorder(state.isSelected() ? BorderFactory.createResizeBorder(6, Color.GRAY, true) : BorderFactory.createEmptyBorder(6));
        setBorder(state.isSelected() ? BorderFactory.createResizeBorder(4) : BorderFactory.createEmptyBorder(4));
//        if (state.isSelected()) {
//            setBackground(Color.BLUE);
//        } else {
//            setBackground(Color.WHITE);
//        }

    }
    int x, y = -1;
    Integer width = new Integer(-1);
    Integer height = new Integer(-1);

    @Override
    public void revalidateDependency() {

        if (getLocation() != null) {
            int newX = getLocation().x;
            int newY = getLocation().y;
            if ((newX != x || newY != y)) {
                body.getPosition().x = WorldUtilities.sceneToWorld(newX, scene.getScale(), scene.getOffsetX(), false);
                body.getPosition().y = WorldUtilities.sceneToWorld(newY, scene.getScale(), scene.getOffsetY(), true);
                x = newX;
                y = newY;
                scene.update();
            }
        }


    }
    Rectangle oldBounds;

    @Override
    protected void paintWidget() {
        super.paintWidget();
        
        Rectangle bounds = getBounds();
        if (oldBounds == null) {
            oldBounds = bounds;
        }
        List<Widget> children = getChildren();
        for (Widget widget : children) { // init contraints if required    
            if (constraintMap.get(widget) == null) {
                Rectangle childBounds = widget.getBounds();
                Insets insets = getBorder().getInsets();
                double cx = (double) ((double) childBounds.x - (double) bounds.x - (double) insets.left) / bounds.width;
                double cy = (double) ((double) childBounds.y - (double) bounds.y - (double) insets.top) / bounds.width;
                double cwidth = (double) childBounds.width / (double) (bounds.width - insets.left - insets.right);
                double cheight = (double) childBounds.height / (double) (bounds.height - insets.top - insets.bottom);
                ShapeLayoutConstraints constraints = new ShapeLayoutConstraints(cx, cy, cwidth, cheight);
                constraintMap.put(widget, constraints);
            }
        }
        if (!bounds.equals(oldBounds)) {
            for (Widget widget : children) {
                ShapeLayoutConstraints constraints = constraintMap.get(widget);
                Insets insets = getBorder().getInsets();
                Rectangle newChildBounds = new Rectangle(
                        bounds.x + (int) (bounds.width * constraints.x) + insets.left,
                        bounds.y + (int) (bounds.height * constraints.y) + insets.top,
                        (int) (bounds.width * constraints.width) - insets.left - insets.right,
                        (int) (bounds.height * constraints.height) - insets.top - insets.bottom);
                widget.setPreferredBounds(newChildBounds);
            }
        }
        oldBounds = bounds;
    }
    /*
     @Override
     protected void paintWidget() {
     super.paintWidget();
     Rectangle bounds = getBounds();
     Rectangle clientArea = calculateClientArea();
     Insets insets = super.getBorder().getInsets();
     double bWidth = bounds.width;
     double cWidth = clientArea.width + insets.left + insets.right;
     double bHeight = bounds.height;
     double cHeight = clientArea.height + insets.bottom + insets.top;
     double scaleX = bWidth / cWidth;
     double scaleY = bHeight / cHeight;
     System.out.println("# scaleX " + scaleX + " scaleY " + scaleY);
     Graphics2D graphics = getGraphics();
     Set<Shape> keySet = shapeMap.keySet();
     for (Shape shape : keySet) {
     java.awt.Shape awtShape = shapeMap.get(shape);
     Rectangle bounds1 = awtShape.getBounds();
     System.out.println("0. " + bounds1);
     int dX = bounds1.x - bounds.x;
     int dY = bounds1.y - bounds.y;
     AffineTransform scaleInstance = AffineTransform.getScaleInstance(scaleX, scaleY);
     java.awt.Shape test2 = scaleInstance.createTransformedShape(awtShape);
     System.out.println("2. " + test2.getBounds());
     Rectangle bounds2 = test2.getBounds();
     int d2X = bounds2.x - bounds.x;
     int d2Y = bounds2.y - bounds.x;
     int tX = dX - d2X;
     int tY = dY - d2Y;
     System.out.print("dX: " + dX + " d2X " + d2X + " tX " + tX);
     AffineTransform translateInstance2 = AffineTransform.getTranslateInstance(tX, tY);
     java.awt.Shape test3 = translateInstance2.createTransformedShape(test2);
     System.out.println("3. " + test3.getBounds());

     graphics.setPaint(Color.green);
     graphics.draw(test3);
     }
     }*/
//    private void initShapes() {
//        if (body.getFixtureList() != null) {
//            Fixture fixture = body.getFixtureList();
//            while (fixture != null) {
//                Shape shape = fixture.getShape();
//                if (!shapeMap.containsKey(shape)) {
//                    if (shape.getType() == ShapeType.POLYGON) {
//                        shapeMap.put(shape, getPolygonShape((PolygonShape) shape));
//                    }
//                }
//                fixture = fixture.getNext();
//            }
//        }
//    }
//    private java.awt.Shape getPolygonShape(PolygonShape shape) {
//        Transform xf = body.getTransform();
//
//        ArrayList<Point> points = new ArrayList<Point>();
//        for (int i = 0; i < shape.getVertexCount(); i++) {
//            Vec2 transformed = new Vec2();
//            Transform.mulToOutUnsafe(xf, shape.m_vertices[i], transformed);
//            transformed.x = transformed.x - body.getPosition().x;
//            transformed.y = transformed.y - body.getPosition().y;
//            Point point = new Point(
//                    (int) ((transformed.x) * scene.getScale()),
//                    (int) ((transformed.y * -1) * scene.getScale()));
//            points.add(point);
//        }
//        int[] xpoints = new int[points.size()];
//        int[] ypoints = new int[points.size()];
//        for (int i = 0; i < points.size(); i++) {
//            xpoints[i] = points.get(i).x;
//            ypoints[i] = points.get(i).y;
//        }
//        Polygon p = new Polygon(xpoints, ypoints, points.size());
//        return p;
//    }
    private HashMap<Widget, ShapeLayoutConstraints> constraintMap = new HashMap<Widget, ShapeLayoutConstraints>();

    private class ShapeLayoutConstraints {

        double x, y, width, height;

        public ShapeLayoutConstraints(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }
    }
}
