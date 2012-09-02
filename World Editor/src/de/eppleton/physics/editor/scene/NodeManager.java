 /*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.eppleton.physics.editor.scene;

import de.eppleton.jbox2d.WorldUtilities;
import de.eppleton.physics.editor.scene.widgets.CircleWidget;
import de.eppleton.physics.editor.scene.widgets.ContainerWidget;
import de.eppleton.physics.editor.scene.widgets.PolygonWidget;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Transform;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyType;
import org.netbeans.api.visual.action.ActionFactory;
import org.netbeans.api.visual.widget.ConnectionWidget;
import org.netbeans.api.visual.widget.Widget;

/**
 * NodeManager gives you a NodeProvider, which will create and configure JavaFX
 * Nodes for you. Add your own Providers for custom rendering.
 *
 * @author eppleton
 */
public class NodeManager {

    private static ArrayList<CircleProvider> circleProviders = new ArrayList<CircleProvider>();
    private static ArrayList<PolygonProvider> polygonProviders = new ArrayList<PolygonProvider>();
    private static PolygonProvider DEFAULT_POLYGON_PROVIDER;
    private static CircleProvider DEFAULT_CIRCLE_PROVIDER;
    private static Color STATIC_BODY_COLOR = new Color(0.5f, 0.9f, 0.3f);
    private static Color ACTIVE_BODY_COLOR = new Color(0.5f, 0.5f, 0.3f);
    private static Color KINEMATIC_BODY_COLOR = new Color(0.5f, 0.5f, 0.9f);
    private static Color AWAKE_BODY_COLOR = new Color(0.9f, 0.7f, 0.7f);
    private static Color SLEEPING_BODY_COLOR = new Color(0.5f, 0.5f, 0.5f);

    public static void addCircleProvider(CircleProvider provider) {
        circleProviders.add(provider);
    }

    public static void removeProvider(CircleProvider provider) {
        circleProviders.remove(provider);
    }

    public static void addPolygonProvider(PolygonProvider provider) {
        polygonProviders.add(provider);
    }

    public static void removeProvider(PolygonProvider provider) {
        polygonProviders.remove(provider);
    }

    public static NodeProvider getNodeProvider(Body body, Shape shape) {

        if (shape instanceof CircleShape) {
            for (NodeProvider provider : circleProviders) {
                if (provider.providesNodeFor(body, shape)) {
                    return provider;
                }
            }
            if (DEFAULT_CIRCLE_PROVIDER == null) {
                DEFAULT_CIRCLE_PROVIDER = new DefaultCircleProvider();
            }
            return DEFAULT_CIRCLE_PROVIDER;
        } else if (shape instanceof PolygonShape) {
            for (NodeProvider provider : polygonProviders) {
                if (provider.providesNodeFor(body, shape)) {
                    return provider;
                }
            }
            if (DEFAULT_POLYGON_PROVIDER == null) {
                DEFAULT_POLYGON_PROVIDER = new DefaultPolygonProvider();
            }
            return DEFAULT_POLYGON_PROVIDER;
        }
        return null;
    }

    static Widget getContainer(WorldScene scene, Body body, final float offset_x,
            final float offset_y,
            final int scale) {
        Widget containerWidget = scene.findWidget(body);
        if (containerWidget == null) {
            containerWidget = new ContainerWidget(scene);
            scene.getMainLayer().addChild(containerWidget);
            scene.addObject(body, containerWidget);
            addActions(scene, containerWidget, body, offset_x, offset_y, scale);
        }
        return containerWidget;
    }

    static void addActions(final WorldScene scene, final Widget containerWidget, final Body body, final float offset_x,
            final float offset_y,
            final int scale) {
        containerWidget.getActions().addAction(ActionFactory.createResizeAction(null, scene.getResizeProvider()));
        containerWidget.getActions().addAction(scene.getMoveAction());
        containerWidget.getActions().addAction(scene.getSelectAction());
        containerWidget.addDependency(
                new Widget.Dependency() {
                    int x, y, width, height;

                    @Override
                    public void revalidateDependency() {

                        if (containerWidget.getLocation() != null) {
                            int newX = containerWidget.getLocation().x;
                            int newY = containerWidget.getLocation().y;
                            if ((newX != x || newY != y)) {
//                                    System.out.println("#x " + x + "->" + newX);
//                                    System.out.println("#y " + y + "->" + newY);
                                //  System.out.println("old "+payload.getPosition());
                                body.getPosition().x = WorldUtilities.sceneToWorld(newX, scale, offset_x, false);
                                body.getPosition().y = WorldUtilities.sceneToWorld(newY, scale, offset_y, true);
                                // System.out.println("new "+payload.getPosition());
                                x = newX;
                                y = newY;
                                scene.fireChange();
                            }
                        }
                        Rectangle bounds = containerWidget.getBounds();
                        if (bounds != null) {
                            int newHeight = bounds.height;
                            int newWidth = bounds.width;
                        }


                    }
                });
    }

    

    public static class DefaultPolygonProvider implements PolygonProvider<PolygonWidget> {

        @Override
        public PolygonWidget configureNode(WorldScene scene, PolygonWidget polygon, Body body, PolygonShape shape, float offset_x, float offset_Y, int scale) { //, Transform[] transform) {
            Widget containerWidget = getContainer(scene, body, offset_x, offset_Y, scale);
            Transform xf = body.getTransform();
            if (polygon == null) {
                ArrayList<Point> points = new ArrayList<Point>();
                for (int i = 0; i < shape.getVertexCount(); i++) {
                    Vec2 transformed = new Vec2();
                    Transform.mulToOutUnsafe(xf, shape.m_vertices[i], transformed);
                    transformed.x = transformed.x - body.getPosition().x;
                    transformed.y = transformed.y - body.getPosition().y;
                    Point point = new Point(
                            (int) ((transformed.x) * scale),
                            (int) ((transformed.y * -1) * scale));
                    points.add(point);
                }

                polygon = new PolygonWidget(scene, points);
                scene.addObject(shape, polygon);
                // polygon.setPreferredLocation(new Point(0, 0));
                containerWidget.addChild(polygon);
            } else {
                for (int i = 0; i < shape.getVertexCount(); i++) {
                    Vec2 transformed = new Vec2();
                    Transform.mulToOutUnsafe(xf, shape.m_vertices[i], transformed);

                    transformed.x = transformed.x - body.getPosition().x;
                    transformed.y = transformed.y - body.getPosition().y;
                    //Vec2 transformed = vec2;
                    polygon.getPoints().get(i).x = (int) ((transformed.x) * scale);
                    polygon.getPoints().get(i).y = (int) ((transformed.y * -1) * scale);
                }
            }
            if (body.isActive() == false) {
                polygon.setForeground(ACTIVE_BODY_COLOR);
            } else if (body.getType() == BodyType.STATIC) {
                polygon.setForeground(STATIC_BODY_COLOR);
            } else if (body.getType() == BodyType.KINEMATIC) {
                polygon.setForeground(KINEMATIC_BODY_COLOR);
            } else if (body.isAwake() == false) {
                polygon.setForeground(SLEEPING_BODY_COLOR);
            } else {
                polygon.setForeground(AWAKE_BODY_COLOR);
            }
            polygon.setPreferredLocation(new Point(0, 0));
            containerWidget.setPreferredLocation(new Point(
                    (int) ((body.getPosition().x + offset_x) * scale),
                    (int) (((body.getPosition().y * -1) + offset_Y) * scale)));
            return polygon;
        }

        @Override
        public boolean providesNodeFor(Body body, PolygonShape shape) {
            // dummy, because this will never be asked for
            return shape instanceof PolygonShape;
        }

        public int normalize(int[] values) {
            int min = Integer.MAX_VALUE;
            for (int i : values) {
                if (i < min) {
                    min = i;
                }
            }
            for (int i = 0; i < values.length; i++) {
                values[i] = values[i] + min;

            }
            return min;
        }
    }

    public static class DefaultCircleProvider implements CircleProvider<CircleWidget> {

        @Override
        public CircleWidget configureNode(WorldScene scene, CircleWidget circle, Body body, CircleShape shape, float offset_x, float offset_Y, int scale) { //, Transform[] transform) {
            Widget containerWidget = getContainer(scene, body, offset_x, offset_Y, scale);

            if (circle == null) {
                circle = new CircleWidget(scene, (int) (shape.m_radius * scale));
                containerWidget.addChild(circle);
                scene.addObject(shape, circle);
            }
            Transform xf = body.getTransform();
            Vec2 center = new Vec2();
            Transform.mulToOutUnsafe(xf, shape.m_p, center);
            containerWidget.setPreferredLocation(new Point((int) ((center.x + offset_x) * scale),
                    (int) (((center.y * -1) + offset_Y) * scale)));
            if (body.isActive() == false) {
                circle.setForeground(ACTIVE_BODY_COLOR);
            } else if (body.getType() == BodyType.STATIC) {
                circle.setForeground(STATIC_BODY_COLOR);
            } else if (body.getType() == BodyType.KINEMATIC) {
                circle.setForeground(KINEMATIC_BODY_COLOR);
            } else if (body.isAwake() == false) {
                circle.setForeground(SLEEPING_BODY_COLOR);
            } else {
                circle.setForeground(AWAKE_BODY_COLOR);
            }


            scene.validate();
            return circle;
        }

        @Override
        public boolean providesNodeFor(Body body, CircleShape shape) {
            // dummy, because this will never be asked for
            return shape instanceof CircleShape;
        }
    }

    private void dump() {
    }
}
