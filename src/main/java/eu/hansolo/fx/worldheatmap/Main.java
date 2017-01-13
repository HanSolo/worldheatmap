package eu.hansolo.fx.worldheatmap;

import eu.hansolo.fx.heatmap.ColorMapping;
import eu.hansolo.fx.heatmap.OpacityDistribution;
import eu.hansolo.fx.world.World;
import eu.hansolo.fx.world.World.Resolution;
import eu.hansolo.fx.world.WorldBuilder;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;


/**
 * User: hansolo
 * Date: 12.01.17
 * Time: 05:54
 */
public class Main extends Application {
    private StackPane     pane;
    private World         worldMap;
    private List<Point2D> cities;

    @Override public void init() {
        try { cities = readCitiesFromFile(); } catch (IOException e) { cities = new ArrayList<>(); }

        worldMap = WorldBuilder.create()
                               .resolution(Resolution.HI_RES)
                               .zoomEnabled(false)
                               .hoverEnabled(false)
                               .selectionEnabled(false)
                               .colorMapping(ColorMapping.BLUE_YELLOW_RED)
                               .fadeColors(true)
                               .eventRadius(3)
                               .heatMapOpacity(0.75)
                               .opacityDistribution(OpacityDistribution.LINEAR)
                               .build();

        pane = new StackPane(worldMap);

        /* Add heatmap events by clicking on the map
        pane.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            double x = event.getX();
            double y = event.getY();
            HeatMap heatMap = worldMap.getHeatMap();
            if (x < heatMap.getEventRadius()) x = heatMap.getEventRadius();
            if (x > pane.getWidth() - heatMap.getEventRadius()) x = pane.getWidth() - heatMap.getEventRadius();
            if (y < worldMap.getHeatMap().getEventRadius()) y = worldMap.getHeatMap().getEventRadius();
            if (y > pane.getHeight() - heatMap.getEventRadius()) y = pane.getHeight() - heatMap.getEventRadius();

            worldMap.getHeatMap().addEvent(x, y);
        });
        */
    }

    @Override public void start(Stage stage) {
        Scene scene = new Scene(pane);

        stage.setTitle("World Cities");
        stage.setScene(scene);
        stage.show();

        worldMap.getHeatMap().addEvents(cities);
    }

    private List<Point2D> readCitiesFromFile() throws IOException {
        List<Point2D>  cities     = new ArrayList<>(8092);
        String         citiesFile = (Main.class.getResource("cities.txt").toExternalForm()).replace("file:", "");
        Stream<String> lines      = Files.lines(Paths.get(citiesFile));
        lines.forEach(line -> {
            String city[] = line.split(",");
            double[] xy = World.latLonToXY(Double.parseDouble(city[1]), Double.parseDouble(city[2]));
            cities.add(new Point2D(xy[0], xy[1]));
        });
        lines.close();
        return cities;
    }

    @Override public void stop() {
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
