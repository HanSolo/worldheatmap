/*
 * Copyright (c) 2016 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.fx.world;

import eu.hansolo.fx.heatmap.ColorMapping;
import eu.hansolo.fx.heatmap.HeatMap;
import eu.hansolo.fx.heatmap.HeatMapBuilder;
import eu.hansolo.fx.heatmap.HeatMapEvent;
import eu.hansolo.fx.heatmap.OpacityDistribution;
import javafx.application.Platform;
import javafx.beans.DefaultProperty;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.event.WeakEventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.Shape;
import javafx.scene.text.Font;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign.MaterialDesign;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static javafx.scene.input.MouseEvent.MOUSE_ENTERED;
import static javafx.scene.input.MouseEvent.MOUSE_EXITED;
import static javafx.scene.input.MouseEvent.MOUSE_PRESSED;
import static javafx.scene.input.MouseEvent.MOUSE_RELEASED;


/**
 * Created by hansolo on 22.11.16.
 */
@DefaultProperty("children")
public class World extends Region {
    public enum Resolution { HI_RES, LO_RES };
    private static final StyleablePropertyFactory<World> FACTORY          = new StyleablePropertyFactory<>(Region.getClassCssMetaData());
    private static final String                          HIRES_PROPERTIES = "eu/hansolo/fx/world/hires.properties";
    private static final String                          LORES_PROPERTIES = "eu/hansolo/fx/world/lores.properties";
    private static final double                          PREFERRED_WIDTH  = 1009;
    private static final double                          PREFERRED_HEIGHT = 665;
    private static final double                          MINIMUM_WIDTH    = 100;
    private static final double                          MINIMUM_HEIGHT    = 66;
    private static final double                          MAXIMUM_WIDTH    = 2018;
    private static final double                          MAXIMUM_HEIGHT   = 1330;
    private static       double                          MAP_OFFSET_X     = -PREFERRED_WIDTH * 0.0285;
    private static       double                          MAP_OFFSET_Y     = PREFERRED_HEIGHT * 0.195;
    private static final double                          ASPECT_RATIO     = PREFERRED_HEIGHT / PREFERRED_WIDTH;
    private static final CssMetaData<World, Color>       BACKGROUND_COLOR = FACTORY.createColorCssMetaData("-background-color", s -> s.backgroundColor, Color.web("#3f3f4f"), false);
    private        final StyleableProperty<Color>        backgroundColor;
    private static final CssMetaData<World, Color>       FILL_COLOR = FACTORY.createColorCssMetaData("-fill-color", s -> s.fillColor, Color.web("#d9d9dc"), false);
    private        final StyleableProperty<Color>        fillColor;
    private static final CssMetaData<World, Color>       STROKE_COLOR = FACTORY.createColorCssMetaData("-stroke-color", s -> s.strokeColor, Color.BLACK, false);
    private        final StyleableProperty<Color>        strokeColor;
    private static final CssMetaData<World, Color>       HOVER_COLOR = FACTORY.createColorCssMetaData("-hover-color", s -> s.hoverColor, Color.web("#456acf"), false);
    private        final StyleableProperty<Color>        hoverColor;
    private static final CssMetaData<World, Color>       PRESSED_COLOR = FACTORY.createColorCssMetaData("-pressed-color", s -> s.pressedColor, Color.web("#789dff"), false);
    private        final StyleableProperty<Color>        pressedColor;
    private static final CssMetaData<World, Color>       SELECTED_COLOR = FACTORY.createColorCssMetaData("-selected-color", s-> s.selectedColor, Color.web("#9dff78"), false);
    private        final StyleableProperty<Color>        selectedColor;
    private static final CssMetaData<World, Color>       LOCATION_COLOR = FACTORY.createColorCssMetaData("-location-color", s -> s.locationColor, Color.web("#ff0000"), false);
    private        final StyleableProperty<Color>        locationColor;
    private              BooleanProperty                 hoverEnabled;
    private              BooleanProperty                 selectionEnabled;
    private              ObjectProperty<Country>         selectedCountry;
    private              BooleanProperty                 zoomEnabled;
    private              DoubleProperty                  scaleFactor;
    private              Properties                      resolutionProperties;
    private              Country                         formerSelectedCountry;
    private              double                          zoomSceneX;
    private              double                          zoomSceneY;
    private              double                          width;
    private              double                          height;
    private              Ikon                            locationIconCode;
    private              Pane                            pane;
    private              Group                           group;
    private              HeatMap                         heatMap;
    private              Map<String, List<CountryPath>>  countryPaths;
    private              ObservableMap<Location, Shape>  locations;
    private              ColorMapping                    colorMapping;
    private              double                          eventRadius;
    private              boolean                         fadeColors;
    private              OpacityDistribution             opacityDistribution;
    private              double                          heatMapOpacity;
    private              BooleanProperty                 heatMapVisible;
    // internal event handlers
    protected            EventHandler<MouseEvent>        _mouseEnterHandler;
    protected            EventHandler<MouseEvent>        _mousePressHandler;
    protected            EventHandler<MouseEvent>        _mouseReleaseHandler;
    protected            EventHandler<MouseEvent>        _mouseExitHandler;
    private              EventHandler<ScrollEvent>       _scrollEventHandler;
    // exposed event handlers
    private              EventHandler<MouseEvent>        mouseEnterHandler;
    private              EventHandler<MouseEvent>        mousePressHandler;
    private              EventHandler<MouseEvent>        mouseReleaseHandler;
    private              EventHandler<MouseEvent>        mouseExitHandler;


    // ******************** Constructors **************************************
    public World() {
        this(Resolution.HI_RES, ColorMapping.INFRARED_3, 5, false, OpacityDistribution.EXPONENTIAL, 0.75);
    }
    public World(final Resolution RESOLUTION) {
        this(RESOLUTION, ColorMapping.INFRARED_3, 5, false, OpacityDistribution.EXPONENTIAL, 0.75);
    }
    public World(final Resolution RESOLUTION, final ColorMapping COLOR_MAPPING, final double EVENT_RADIUS, final boolean FADE_COLORS, final OpacityDistribution OPACITY_DISTRIBUTION, final double HEAT_MAP_OPACITY) {
        resolutionProperties = readProperties(Resolution.HI_RES == RESOLUTION ? World.HIRES_PROPERTIES : World.LORES_PROPERTIES);
        backgroundColor      = new StyleableObjectProperty<Color>(BACKGROUND_COLOR.getInitialValue(World.this)) {
            @Override protected void invalidated() { setBackground(new Background(new BackgroundFill(get(), CornerRadii.EMPTY, Insets.EMPTY))); }
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "backgroundColor"; }
            @Override public CssMetaData<? extends Styleable, Color> getCssMetaData() { return BACKGROUND_COLOR; }
        };
        fillColor            = new StyleableObjectProperty<Color>(FILL_COLOR.getInitialValue(World.this)) {
            @Override protected void invalidated() { setFillAndStroke(); }
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "fillColor"; }
            @Override public CssMetaData<? extends Styleable, Color> getCssMetaData() { return FILL_COLOR; }
        };
        strokeColor          = new StyleableObjectProperty<Color>(STROKE_COLOR.getInitialValue(World.this)) {
            @Override protected void invalidated() { setFillAndStroke(); }
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "strokeColor"; }
            @Override public CssMetaData<? extends Styleable, Color> getCssMetaData() { return STROKE_COLOR; }
        };
        hoverColor           = new StyleableObjectProperty<Color>(HOVER_COLOR.getInitialValue(World.this)) {
            @Override protected void invalidated() { }
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "hoverColor"; }
            @Override public CssMetaData<? extends Styleable, Color> getCssMetaData() { return HOVER_COLOR; }
        };
        pressedColor         = new StyleableObjectProperty<Color>(PRESSED_COLOR.getInitialValue(this)) {
            @Override protected void invalidated() {}
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "pressedColor"; }
            @Override public CssMetaData<? extends Styleable, Color> getCssMetaData() { return PRESSED_COLOR; }
        };
        selectedColor        = new StyleableObjectProperty<Color>(SELECTED_COLOR.getInitialValue(this)) {
            @Override protected void invalidated() {}
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "selectedColor"; }
            @Override public CssMetaData<? extends Styleable, Color> getCssMetaData() { return SELECTED_COLOR; }
        };
        locationColor        = new StyleableObjectProperty<Color>(LOCATION_COLOR.getInitialValue(this)) {
            @Override protected void invalidated() {
                locations.forEach((location, shape) -> shape.setFill(null == location.getColor() ? get() : location.getColor()));
            }
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "locationColor"; }
            @Override public CssMetaData<? extends Styleable, Color> getCssMetaData() { return LOCATION_COLOR; }
        };
        hoverEnabled         = new BooleanPropertyBase(true) {
            @Override protected void invalidated() {}
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "hoverEnabled"; }
        };
        selectionEnabled     = new BooleanPropertyBase(false) {
            @Override protected void invalidated() {}
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "selectionEnabled"; }
        };
        selectedCountry      = new ObjectPropertyBase<Country>() {
            @Override protected void invalidated() {}
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "selectedCountry"; }
        };
        zoomEnabled          = new BooleanPropertyBase(false) {
            @Override protected void invalidated() {
                if (null == getScene()) return;
                if (get()) {
                    getScene().addEventFilter(ScrollEvent.ANY, _scrollEventHandler);
                } else {
                    getScene().removeEventFilter(ScrollEvent.ANY, _scrollEventHandler);
                }
            }
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "zoomEnabled"; }
        };
        scaleFactor          = new DoublePropertyBase(1.0) {
            @Override protected void invalidated() {
                if (isZoomEnabled()) {
                    setScaleX(get());
                    setScaleY(get());
                }
            }
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "scaleFactor"; }
        };
        countryPaths         = createCountryPaths();
        locations            = FXCollections.observableHashMap();
        colorMapping         = COLOR_MAPPING;
        eventRadius          = EVENT_RADIUS;
        fadeColors           = FADE_COLORS;
        opacityDistribution  = OPACITY_DISTRIBUTION;
        heatMapOpacity       = HEAT_MAP_OPACITY;
        heatMapVisible       = new BooleanPropertyBase(true) {
            @Override protected void invalidated() {
                heatMap.setVisible(get());
                heatMap.setManaged(get());
            }
            @Override public Object getBean() { return World.this; }
            @Override public String getName() { return "heatMapVisible"; }
        };

        locationIconCode     = MaterialDesign.MDI_CHECKBOX_BLANK_CIRCLE;
        pane                 = new Pane();
        group                = new Group();

        _mouseEnterHandler   = evt -> handleMouseEvent(evt, mouseEnterHandler);
        _mousePressHandler   = evt -> handleMouseEvent(evt, mousePressHandler);
        _mouseReleaseHandler = evt -> handleMouseEvent(evt, mouseReleaseHandler);
        _mouseExitHandler    = evt -> handleMouseEvent(evt, mouseExitHandler);
        _scrollEventHandler  = evt -> {
            if (group.getTranslateX() != 0 || group.getTranslateY() != 0) { resetZoom(); }
            double delta    = 1.2;
            double scale    = getScaleFactor();
            double oldScale = scale;
            scale           = evt.getDeltaY() < 0 ? scale / delta : scale * delta;
            scale           = clamp( 1, 10, scale);
            double factor   = (scale / oldScale) - 1;
            if (Double.compare(1, getScaleFactor()) == 0) {
                zoomSceneX = evt.getSceneX();
                zoomSceneY = evt.getSceneY();
                resetZoom();
            }
            double deltaX = (zoomSceneX - (getBoundsInParent().getWidth() / 2 + getBoundsInParent().getMinX()));
            double deltaY = (zoomSceneY - (getBoundsInParent().getHeight() / 2 + getBoundsInParent().getMinY()));
            setScaleFactor(scale);
            setPivot(deltaX * factor, deltaY * factor);

            evt.consume();
        };

        initGraphics();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void initGraphics() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 ||
            Double.compare(getWidth(), 0.0) <= 0 || Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        getStyleClass().add("world");

        Color fill   = getFillColor();
        Color stroke = getStrokeColor();

        countryPaths.forEach((name, pathList) -> {
            Country country = Country.valueOf(name);
            pathList.forEach(path -> {
                path.setFill(null == country.getColor() ? fill : country.getColor());
                path.setStroke(stroke);
                path.setStrokeWidth(0.2);
                path.setOnMouseEntered(new WeakEventHandler<>(_mouseEnterHandler));
                path.setOnMousePressed(new WeakEventHandler<>(_mousePressHandler));
                path.setOnMouseReleased(new WeakEventHandler<>(_mouseReleaseHandler));
                path.setOnMouseExited(new WeakEventHandler<>(_mouseExitHandler));
            });
            pane.getChildren().addAll(pathList);
        });

        group.getChildren().add(pane);

        heatMap  = HeatMapBuilder.create()
                                 .prefSize(1009, 665)
                                 .colorMapping(colorMapping)
                                 .eventRadius(eventRadius)
                                 .fadeColors(fadeColors)
                                 .opacityDistribution(opacityDistribution)
                                 .heatMapOpacity(heatMapOpacity)
                                 .build();

        getChildren().setAll(group, heatMap);

        setBackground(new Background(new BackgroundFill(getBackgroundColor(), CornerRadii.EMPTY, Insets.EMPTY)));
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
        sceneProperty().addListener(o -> {
            if (!locations.isEmpty()) { addShapesToScene(locations.values()); }
            if (isZoomEnabled()) { getScene().addEventFilter( ScrollEvent.ANY, new WeakEventHandler<>(_scrollEventHandler)); }

            locations.addListener((MapChangeListener<Location, Shape>) CHANGE -> {
                if (CHANGE.wasAdded()) {
                    addShapesToScene(CHANGE.getValueAdded());
                } else if(CHANGE.wasRemoved()) {
                    Platform.runLater(() -> pane.getChildren().remove(CHANGE.getValueRemoved()));
                }
            });
        });
    }


    // ******************** Methods *******************************************
    @Override protected double computeMinWidth(final double HEIGHT)  { return MINIMUM_WIDTH; }
    @Override protected double computeMinHeight(final double WIDTH)  { return MINIMUM_HEIGHT; }
    @Override protected double computePrefWidth(final double HEIGHT) { return super.computePrefWidth(HEIGHT); }
    @Override protected double computePrefHeight(final double WIDTH) { return super.computePrefHeight(WIDTH); }
    @Override protected double computeMaxWidth(final double HEIGHT)  { return MAXIMUM_WIDTH; }
    @Override protected double computeMaxHeight(final double WIDTH)  { return MAXIMUM_HEIGHT; }

    @Override public ObservableList<Node> getChildren() { return super.getChildren(); }

    public Map<String, List<CountryPath>> getCountryPaths() { return countryPaths; }

    public void setMouseEnterHandler(final EventHandler<MouseEvent> HANDLER) { mouseEnterHandler = HANDLER; }
    public void setMousePressHandler(final EventHandler<MouseEvent> HANDLER) { mousePressHandler = HANDLER; }
    public void setMouseReleaseHandler(final EventHandler<MouseEvent> HANDLER) { mouseReleaseHandler = HANDLER;  }
    public void setMouseExitHandler(final EventHandler<MouseEvent> HANDLER) { mouseExitHandler = HANDLER; }

    public Color getBackgroundColor() { return backgroundColor.getValue(); }
    public void setBackgroundColor(final Color COLOR) { backgroundColor.setValue(COLOR); }
    public ObjectProperty<Color> backgroundColorProperty() { return (ObjectProperty<Color>) backgroundColor; }

    public Color getFillColor() { return fillColor.getValue(); }
    public void setFillColor(final Color COLOR) { fillColor.setValue(COLOR); }
    public ObjectProperty<Color> fillColorProperty() { return (ObjectProperty<Color>) fillColor; }

    public Color getStrokeColor() { return strokeColor.getValue(); }
    public void setStrokeColor(final Color COLOR) { strokeColor.setValue(COLOR); }
    public ObjectProperty<Color> strokeColorProperty() { return (ObjectProperty<Color>) strokeColor; }

    public Color getHoverColor() { return hoverColor.getValue(); }
    public void setHoverColor(final Color COLOR) { hoverColor.setValue(COLOR); }
    public ObjectProperty<Color> hoverColorProperty() { return (ObjectProperty<Color>) hoverColor; }

    public Color getPressedColor() { return pressedColor.getValue(); }
    public void setPressedColor(final Color COLOR) { pressedColor.setValue(COLOR); }
    public ObjectProperty<Color> pressedColorProperty() { return (ObjectProperty<Color>) pressedColor; }

    public Color getSelectedColor() { return selectedColor.getValue(); }
    public void setSelectedColor(final Color COLOR) { selectedColor.setValue(COLOR); }
    public ObjectProperty<Color> selectedColorProperty() { return (ObjectProperty<Color>) selectedColor; }

    public Color getLocationColor() { return locationColor.getValue(); }
    public void setLocationColor(final Color COLOR) { locationColor.setValue(COLOR); }
    public ObjectProperty<Color> locationColorProperty() { return (ObjectProperty<Color>) locationColor; }

    public boolean isHoverEnabled() { return hoverEnabled.get(); }
    public void setHoverEnabled(final boolean ENABLED) { hoverEnabled.set(ENABLED); }
    public BooleanProperty hoverEnabledProperty() { return hoverEnabled; }

    public boolean isSelectionEnabled() { return selectionEnabled.get(); }
    public void setSelectionEnabled(final boolean ENABLED) { selectionEnabled.set(ENABLED); }
    public BooleanProperty selectionEnabledProperty() { return selectionEnabled; }

    public Country getSelectedCountry() { return selectedCountry.get(); }
    public void setSelectedCountry(final Country COUNTRY) { selectedCountry.set(COUNTRY); }
    public ObjectProperty<Country> selectedCountryProperty() { return selectedCountry; }

    public boolean isZoomEnabled() { return zoomEnabled.get(); }
    public void setZoomEnabled(final boolean ENABLED) { zoomEnabled.set(ENABLED); }
    public BooleanProperty zoomEnabledProperty() { return zoomEnabled; }

    public double getScaleFactor() { return scaleFactor.get(); }
    public void setScaleFactor(final double FACTOR) { scaleFactor.set(FACTOR); }
    public DoubleProperty scaleFactorProperty() { return scaleFactor; }

    public boolean isHeatMapVisible() { return heatMapVisible.get(); }
    public void setHeatMapVisible(final boolean VISIBLE) { heatMapVisible.set(VISIBLE); }
    public BooleanProperty heatMapVisibleProperty() { return heatMapVisible; }

    public void resetZoom() {
        setScaleFactor(1.0);
        setTranslateX(0);
        setTranslateY(0);
        group.setTranslateX(0);
        group.setTranslateY(0);
    }

    public Ikon getLocationIconCode() { return locationIconCode; }
    public void setLocationIconCode(final Ikon ICON_CODE) { locationIconCode = ICON_CODE; }

    public void addLocation(final Location LOCATION) {
        double x = (LOCATION.getLongitude() + 180) * (PREFERRED_WIDTH / 360) + MAP_OFFSET_X;
        double y = (PREFERRED_HEIGHT / 2) - (PREFERRED_WIDTH * (Math.log(Math.tan((Math.PI / 4) + (Math.toRadians(LOCATION.getLatitude()) / 2)))) / (2 * Math.PI)) + MAP_OFFSET_Y;

        FontIcon locationIcon = new FontIcon(null == LOCATION.getIconCode() ? locationIconCode : LOCATION.getIconCode());
        locationIcon.setIconSize(LOCATION.getIconSize());
        locationIcon.setTextOrigin(VPos.CENTER);
        locationIcon.setIconColor(null == LOCATION.getColor() ? getLocationColor() : LOCATION.getColor());
        locationIcon.setX(x - LOCATION.getIconSize() * 0.5);
        locationIcon.setY(y);

        StringBuilder tooltipBuilder = new StringBuilder();
        if (!LOCATION.getName().isEmpty()) tooltipBuilder.append(LOCATION.getName());
        if (!LOCATION.getInfo().isEmpty()) tooltipBuilder.append("\n").append(LOCATION.getInfo());
        String tooltipText = tooltipBuilder.toString();
        if (!tooltipText.isEmpty()) {
            Tooltip tooltip = new Tooltip(tooltipText);
            tooltip.setFont(Font.font(10));
            Tooltip.install(locationIcon, tooltip);
        }

        if (null != LOCATION.getMouseEnterHandler()) locationIcon.setOnMouseEntered(new WeakEventHandler<>(LOCATION.getMouseEnterHandler()));
        if (null != LOCATION.getMousePressHandler()) locationIcon.setOnMousePressed(new WeakEventHandler<>(LOCATION.getMousePressHandler()));
        if (null != LOCATION.getMouseReleaseHandler()) locationIcon.setOnMouseReleased(new WeakEventHandler<>(LOCATION.getMouseReleaseHandler()));
        if (null != LOCATION.getMouseExitHandler()) locationIcon.setOnMouseExited(new WeakEventHandler<>(LOCATION.getMouseExitHandler()));

        locations.put(LOCATION, locationIcon);
    }
    public void removeLocation(final Location LOCATION) {
        locations.remove(LOCATION);
    }

    public void addLocations(final Location... LOCATIONS) {
        for (Location location : LOCATIONS) { addLocation(location); }
    }
    public void clearLocations() { locations.clear(); }

    public void showLocations(final boolean SHOW) {
        for (Shape shape : locations.values()) {
            shape.setManaged(SHOW);
            shape.setVisible(SHOW);
        }
    }

    public void zoomToCountry(final Country COUNTRY) {
        if (!isZoomEnabled()) return;
        if (null != getSelectedCountry()) {
            setCountryFillAndStroke(getSelectedCountry(), getFillColor(), getStrokeColor());
        }
        zoomToArea(getBounds(COUNTRY));
    }

    public void zoomToRegion(final CRegion REGION) {
        if (!isZoomEnabled()) return;
        if (null != getSelectedCountry()) {
            setCountryFillAndStroke(getSelectedCountry(), getFillColor(), getStrokeColor());
        }
        zoomToArea(getBounds(REGION.getCountries()));
    }

    public HeatMap getHeatMap() { return heatMap; }

    public void addEvents(final Point2D... EVENTS) { heatMap.addEvents(EVENTS); }

    /**
     * Add a list of events and update the heatmap after all events
     * have been added
     * @param EVENTS
     */
    public void addEvents(final List<Point2D> EVENTS) { heatMap.addEvents(EVENTS); }

    /**
     * Visualizes an event with the given radius and opacity gradient
     * @param X
     * @param Y
     * @param OFFSET_X
     * @param OFFSET_Y
     * @param RADIUS
     * @param OPACITY_GRADIENT
     */
    public void addEvent(final double X, final double Y, final double OFFSET_X, final double OFFSET_Y, final double RADIUS, final OpacityDistribution OPACITY_GRADIENT) { heatMap.addEvent(X, Y, OFFSET_X, OFFSET_Y, RADIUS, OPACITY_GRADIENT); }

    /**
     * Visualizes an event with a given image at the given position and with
     * the given offset. So one could use different weighted images for different
     * kinds of events (e.g. important events more opaque as unimportant events)
     * @param X
     * @param Y
     * @param EVENT_IMAGE
     * @param OFFSET_X
     * @param OFFSET_Y
     */
    public void addEvent(final double X, final double Y, final Image EVENT_IMAGE, final double OFFSET_X, final double OFFSET_Y) { heatMap.addEvent(X, Y, EVENT_IMAGE, OFFSET_X, OFFSET_Y); }

    /**
     * If you don't need to weight events you could use this method which
     * will create events that always use the global weight
     * @param X
     * @param Y
     */
    public void addEvent(final double X, final double Y) { heatMap.addEvent(X, Y); }

    /**
     * Calling this method will lead to a clean new heat map without any data
     */
    public void clearHeatMap() { heatMap.clearHeatMap(); }

    /**
     * Returns the used color mapping with the gradient that is used
     * to visualize the data
     * @return
     */
    public ColorMapping getColorMapping() { return heatMap.getColorMapping(); }

    /**
     * The ColorMapping enum contains some examples for color mappings
     * that might be useful to visualize data and here you could set
     * the one you like most. Setting another color mapping will recreate
     * the heat map automatically.
     * @param COLOR_MAPPING
     */
    public void setColorMapping(final ColorMapping COLOR_MAPPING) { heatMap.setColorMapping(COLOR_MAPPING); }

    /**
     * Returns true if the heat map is used to visualize frequencies (default)
     * @return true if the heat map is used to visualize frequencies
     */
    public boolean isFadeColors() { return heatMap.isFadeColors(); }

    /**
     * If true each event will be visualized by a radial gradient
     * with the colors from the given color mapping and decreasing
     * opacity from the inside to the outside. If you set it to false
     * the color opacity won't fade out but will be opaque. This might
     * be handy if you would like to visualize the density instead of
     * the frequency
     * @param FADE_COLORS
     */
    public void setFadeColors(final boolean FADE_COLORS) { heatMap.setFadeColors(FADE_COLORS); }

    /**
     * Returns the radius of the circle that is used to visualize an
     * event.
     * @return the radius of the circle that is used to visualize an event
     */
    public double getEventRadius() { return heatMap.getEventRadius(); }

    /**
     * Each event will be visualized by a circle filled with a radial
     * gradient with decreasing opacity from the inside to the outside.
     * If you have lot's of events it makes sense to set the event radius
     * to a smaller value. The default value is 15.5
     * @param RADIUS
     */
    public void setEventRadius(final double RADIUS) { heatMap.setEventRadius(RADIUS); }

    public double getHeatMapOpacity() { return heatMap.getOpacity(); }

    public void setHeatMapOpacity(final double OPACITY) { heatMap.setOpacity(clamp(0.0, 1.0, OPACITY)); }

    /**
     * Returns the opacity distribution that will be used to visualize
     * the events in the monochrome map. If you have lot's of events
     * it makes sense to reduce the radius and the set the opacity
     * distribution to exponential.
     * @return the opacity distribution of events in the monochrome map
     */
    public OpacityDistribution getOpacityDistribution() { return heatMap.getOpacityDistribution(); }

    /**
     * Changing the opacity distribution will affect the smoothing of
     * the heat map. If you choose a linear opacity distribution you will
     * see bigger colored dots for each event than using the exponential
     * opacity distribution (at the same event radius).
     * @param OPACITY_DISTRIBUTION
     */
    public void setOpacityDistribution(final OpacityDistribution OPACITY_DISTRIBUTION) { heatMap.setOpacityDistribution(OPACITY_DISTRIBUTION); }

    public static double[] latLonToXY(final double LATITUDE, final double LONGITUDE) {
        double x = (LONGITUDE + 180) * (PREFERRED_WIDTH / 360) + MAP_OFFSET_X;
        double y = (PREFERRED_HEIGHT / 2) - (PREFERRED_WIDTH * (Math.log(Math.tan((Math.PI / 4) + (Math.toRadians(LATITUDE) / 2)))) / (2 * Math.PI)) + MAP_OFFSET_Y;
        return new double[]{ x, y };
    }

    private double[] getBounds(final Country... COUNTRIES) { return getBounds(Arrays.asList(COUNTRIES)); }
    private double[] getBounds(final List<Country> COUNTRIES) {
        double upperLeftX  = PREFERRED_WIDTH;
        double upperLeftY  = PREFERRED_HEIGHT;
        double lowerRightX = 0;
        double lowerRightY = 0;
        for (Country country : COUNTRIES) {
            List<CountryPath> paths = countryPaths.get(country.getName());
            for (int i = 0; i < paths.size(); i++) {
                CountryPath path   = paths.get(i);
                Bounds      bounds = path.getLayoutBounds();
                upperLeftX  = Math.min(bounds.getMinX(), upperLeftX);
                upperLeftY  = Math.min(bounds.getMinY(), upperLeftY);
                lowerRightX = Math.max(bounds.getMaxX(), lowerRightX);
                lowerRightY = Math.max(bounds.getMaxY(), lowerRightY);
            }
        }
        return new double[]{ upperLeftX, upperLeftY, lowerRightX, lowerRightY };
    }

    private void zoomToArea(final double[] BOUNDS) {
        group.setTranslateX(0);
        group.setTranslateY(0);
        double      areaWidth   = BOUNDS[2] - BOUNDS[0];
        double      areaHeight  = BOUNDS[3] - BOUNDS[1];
        double      areaCenterX = BOUNDS[0] + areaWidth * 0.5;
        double      areaCenterY = BOUNDS[1] + areaHeight * 0.5;
        Orientation orientation = areaWidth < areaHeight ? Orientation.VERTICAL : Orientation.HORIZONTAL;
        double sf = 1.0;
        switch(orientation) {
            case VERTICAL  : sf = clamp(1.0, 10.0, 1 / (areaHeight / height)); break;
            case HORIZONTAL: sf = clamp(1.0, 10.0, 1 / (areaWidth / width)); break;
        }

        /*
        Rectangle bounds = new Rectangle(BOUNDS[0], BOUNDS[1], areaWidth, areaHeight);
        bounds.setFill(Color.TRANSPARENT);
        bounds.setStroke(Color.RED);
        bounds.setStrokeWidth(0.5);
        bounds.setMouseTransparent(true);
        group.getChildren().add(bounds);
        */

        setScaleFactor(sf);
        group.setTranslateX(width * 0.5 - (areaCenterX));
        group.setTranslateY(height * 0.5 - (areaCenterY));
    }

    private void setPivot(final double X, final double Y) {
        setTranslateX(getTranslateX() - X);
        setTranslateY(getTranslateY() - Y);
    }

    private void handleMouseEvent(final MouseEvent EVENT, final EventHandler<MouseEvent> HANDLER) {
        final CountryPath       COUNTRY_PATH = (CountryPath) EVENT.getSource();
        final String            COUNTRY_NAME = COUNTRY_PATH.getName();
        final Country           COUNTRY      = Country.valueOf(COUNTRY_NAME);
        final List<CountryPath> PATHS        = countryPaths.get(COUNTRY_NAME);

        final EventType TYPE = EVENT.getEventType();
        if (MOUSE_ENTERED == TYPE) {
            if (isHoverEnabled()) {
                Color color = isSelectionEnabled() && COUNTRY.equals(getSelectedCountry()) ? getSelectedColor() : getHoverColor();
                for (SVGPath path : PATHS) { path.setFill(color); }
            }
        } else if (MOUSE_PRESSED == TYPE) {
            if (isSelectionEnabled()) {
                Color color;
                if (null == getSelectedCountry()) {
                    setSelectedCountry(COUNTRY);
                    color = getSelectedColor();
                } else {
                    color = null == getSelectedCountry().getColor() ? getFillColor() : getSelectedCountry().getColor();
                }
                for (SVGPath path : countryPaths.get(getSelectedCountry().getName())) { path.setFill(color); }
            } else {
                if (isHoverEnabled()) {
                    for (SVGPath path : PATHS) { path.setFill(getPressedColor()); }
                }
            }
        } else if (MOUSE_RELEASED == TYPE) {
            Color color;
            if (isSelectionEnabled()) {
                if (formerSelectedCountry == COUNTRY) {
                    setSelectedCountry(null);
                    color = null == COUNTRY.getColor() ? getFillColor() : COUNTRY.getColor();
                } else {
                    setSelectedCountry(COUNTRY);
                    color = getSelectedColor();
                }
                formerSelectedCountry = getSelectedCountry();
            } else {
                color = getHoverColor();
            }
            if (isHoverEnabled()) {
                for (SVGPath path : PATHS) { path.setFill(color); }
            }
        } else if (MOUSE_EXITED == TYPE) {
            if (isHoverEnabled()) {
                Color color = isSelectionEnabled() && COUNTRY.equals(getSelectedCountry()) ? getSelectedColor() : getFillColor();
                for (SVGPath path : PATHS) {
                    path.setFill(null == COUNTRY.getColor() || COUNTRY == getSelectedCountry() ? color : COUNTRY.getColor());
                }
            }
        }

        if (null != HANDLER) HANDLER.handle(EVENT);
    }

    private void setFillAndStroke() {
        countryPaths.keySet().forEach(name -> {
            Country country = Country.valueOf(name);
            setCountryFillAndStroke(country, null == country.getColor() ? getFillColor() : country.getColor(), getStrokeColor());
        });
    }
    private void setCountryFillAndStroke(final Country COUNTRY, final Color FILL, final Color STROKE) {
        List<CountryPath> paths = countryPaths.get(COUNTRY.getName());
        for (CountryPath path : paths) {
            path.setFill(FILL);
            path.setStroke(STROKE);
        }
    }

    private void addShapesToScene(final Shape... SHAPES) {
        addShapesToScene(Arrays.asList(SHAPES));
    }
    private void addShapesToScene(final Collection<Shape> SHAPES) {
        if (null == getScene()) return;
        Platform.runLater(() -> pane.getChildren().addAll(SHAPES));
    }

    private double clamp(final double MIN, final double MAX, final double VALUE) {
        if (VALUE < MIN) return MIN;
        if (VALUE > MAX) return MAX;
        return VALUE;
    }

    private Properties readProperties(final String FILE_NAME) {
        final ClassLoader LOADER     = Thread.currentThread().getContextClassLoader();
        final Properties  PROPERTIES = new Properties();
        try(InputStream resourceStream = LOADER.getResourceAsStream(FILE_NAME)) {
            PROPERTIES.load(resourceStream);
        } catch (IOException exception) {
            System.out.println(exception);
        }
        return PROPERTIES;
    }

    private Map<String, List<CountryPath>> createCountryPaths() {
        Map<String, List<CountryPath>> countryPaths = new HashMap<>();
        resolutionProperties.forEach((key, value) -> {
            String            name     = key.toString();
            List<CountryPath> pathList = new ArrayList<>();
            for (String path : value.toString().split(";")) { pathList.add(new CountryPath(name, path)); }
            countryPaths.put(name, pathList);
        });
        return countryPaths;
    }


    // ******************** Style related *************************************
    @Override public String getUserAgentStylesheet() {
        return World.class.getResource("world.css").toExternalForm();
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() { return FACTORY.getCssMetaData(); }

    @Override public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() { return FACTORY.getCssMetaData(); }


    // ******************** Resizing ******************************************
    private void resize() {
        width  = getWidth() - getInsets().getLeft() - getInsets().getRight();
        height = getHeight() - getInsets().getTop() - getInsets().getBottom();

        if (ASPECT_RATIO * width > height) {
            width = 1 / (ASPECT_RATIO / height);
        } else if (1 / (ASPECT_RATIO / height) > width) {
            height = ASPECT_RATIO * width;
        }

        if (width > 0 && height > 0) {
            if (isZoomEnabled()) resetZoom();

            pane.setCache(true);
            pane.setCacheHint(CacheHint.SCALE);

            pane.setScaleX(width / PREFERRED_WIDTH);
            pane.setScaleY(height / PREFERRED_HEIGHT);

            group.resize(width, height);
            group.relocate((getWidth() - width) * 0.5, (getHeight() - height) * 0.5);

            heatMap.setSize(width, height);
            heatMap.relocate(((getWidth() - getInsets().getLeft() - getInsets().getRight()) - width) * 0.5, ((getHeight() - getInsets().getTop() - getInsets().getBottom()) - height) * 0.5);

            pane.setCache(false);
        }
    }
}
