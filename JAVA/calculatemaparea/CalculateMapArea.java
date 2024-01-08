package com.bosonshiggs.calculatemaparea;

import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.runtime.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.GeometryUtil;

import android.app.Activity;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.util.Log;

@DesignerComponent(version = 1,
    description = "Extension to draw and calculate areas on the Map",
    category = ComponentCategory.EXTENSION,
    nonVisible = true,
    iconName = "aiwebres/icon.png")
@SimpleObject(external = true)
public class CalculateMapArea extends AndroidNonvisibleComponent {

	private Map mapComponent;
    private Polygon polygon;
    private LineString lineString;
    private List<GeoPoint> lineStringPoints = new ArrayList<>();
    private List<GeoPoint> polygonPoints = new ArrayList<>();
    private List<Marker> currentMarkers = new ArrayList<>();
    private List<Polygon> currentPolygons = new ArrayList<>();
    
    private boolean isLineString = false;
    private boolean isPolygon = false;
    
    private Activity activity;
    
    private boolean flagLogName = false;
    
    public CalculateMapArea(ComponentContainer container) {
        super(container.$form());
        this.activity = container.$context();
    }
    
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Use a Boolean variable to define a LineString on the map.")
    public void SetLineMapDraw(boolean isLineString) {
    	this.isLineString = !this.isPolygon && isLineString;
    }
    
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Use a Boolean variable to define a polygon on the map.")
    public void SetPolygonMapDraw(boolean isPolygon) {
    	this.isPolygon = !this.isLineString && isPolygon;
    }
    
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get a boolean variable that defines a LineString on the map.")
    public boolean GetLineMapDraw() {
    	return this.isLineString;
    }
    
    @SimpleProperty(category = PropertyCategory.BEHAVIOR, description = "Get a boolean variable that defines a polygon on the map.")
    public boolean SetPolygonMapDraw() {
    	return this.isPolygon;
    }

    @SimpleFunction(description = "Set the Map component to be used.")
    public void SetMapComponent(Map map) {
        this.mapComponent = map;
        this.polygon = new Polygon(map);
        this.lineString = new LineString(map);
    }
    
    @SimpleFunction(description = "Add a point to the polygon and draw it on the map.")
    public void AddPoint(double latitude, double longitude) {
        try {
            GeoPoint point = new GeoPoint(latitude, longitude);
            
            if (isPolygon) polygonPoints.add(point);

            if (isLineString) lineStringPoints.add(point);

        } catch (Exception e) {
            if(flagLogName) Log.e("AddPoint", "Error adding point to polygon", e);
            e.printStackTrace();
            ReportError("Error adding point to polygon!");
        }
    }
    
    @SimpleFunction(description = "Draw the LineString on the map.")
    public void DrawLineString() {
    	if (lineStringPoints.size() > 1) {           
            YailList points = GeometryUtil.pointsListToYailList(lineStringPoints);
            lineString.Points(points);
            mapComponent.addFeature(lineString);
            
        } else {
            if(flagLogName) Log.e("DrawLineString", "Not enough points to form a line string.");
            ReportError("Not enough points to form a line string.");
        }
    }
    
    @SimpleFunction(description = "Set the style of the line.")
    public void SetLineStyle(int color, int strokeWidth) {
    	lineString.StrokeColor(color);
        lineString.StrokeWidth(strokeWidth);
        mapComponent.getController().updateFeatureStroke(lineString);
    }
    
    @SimpleFunction(description = "Set the style of the polygon.")
    public void SetPolygonStyle(int fillColor, int strokeColor, int strokeWidth) {
        for (Polygon polygon : currentPolygons) {
            if (polygon != null) {
                polygon.FillColor(fillColor); // Define a cor de preenchimento do polígono
                polygon.StrokeColor(strokeColor); // Define a cor da borda do polígono
                polygon.StrokeWidth(strokeWidth); // Define a largura da borda do polígono
                mapComponent.getController().updateFeatureFill(polygon); // Atualiza o preenchimento no mapa
                mapComponent.getController().updateFeatureStroke(polygon); // Atualiza a borda no mapa
            }
        }
    }


    
    @SimpleFunction(description = "Draw the polygon on the map using markers for each point.")
    public void DrawMarker() {
        try {
            List<GeoPoint> orderedPoints = new ArrayList<>();

            if (isPolygon && !polygonPoints.isEmpty()) {
                orderedPoints = reorderPolygonPoints(polygonPoints);
            } else if (isLineString && !lineStringPoints.isEmpty()) {
                orderedPoints = reorderPolygonPoints(lineStringPoints);
            }

            for (GeoPoint point : orderedPoints) {
                Marker marker = new Marker(mapComponent);
                marker.SetLocation(point.getLatitude(), point.getLongitude());
                mapComponent.addFeature(marker);
                currentMarkers.add(marker);
            }
        } catch (Exception e) {
            if(flagLogName) Log.e("DrawMarker", "Error drawing marker", e);
            e.printStackTrace();
            ReportError("Error drawing marker.");
        }
    }

    
    @SimpleFunction(description = "Draw the polygon on the map using markers for each point.")
    public void DrawPolygon() {
        try {

            if (polygonPoints.size() > 2) {
                List<GeoPoint> orderedPoints = reorderPolygonPoints(polygonPoints);
                Polygon polygon = new Polygon(mapComponent);
                polygon.Initialize();

                YailList pointsYailList = GeometryUtil.pointsListToYailList(orderedPoints);
                if(flagLogName) Log.d("DrawPolygon", "Points: " + pointsYailList.toString());

                polygon.Points(pointsYailList);
                mapComponent.addFeature(polygon);
                currentPolygons.add(polygon);

            }
        } catch (Exception e) {
            if(flagLogName) Log.e("DrawPolygon", "Error drawing polygon", e);
            e.printStackTrace();
            ReportError("Error drawing polygon.");
        }
    }
    
    @SimpleFunction(description = "Returns a YailList of ordered polygon points.")
    public YailList GetOrderedPointsYailList() {
        try {
        	GeoPoint centroid = null;
            List<GeoPoint> points = new ArrayList<>();

            if (isPolygon) {
                centroid = calculateCentroid(polygonPoints);
                points = polygonPoints;
            } else if (isLineString) {
                centroid = calculateCentroid(lineStringPoints);
                points = lineStringPoints;
            }
        	
            if (centroid == null) {
                // Tratar o caso em que o centróide não está definido
                return YailList.makeEmptyList();
            }
        	
            // Calcular o centróide
        	if (isPolygon) {
        		centroid = calculateCentroid(polygonPoints);
        		points = polygonPoints;
        	} else if (isLineString) {
        		centroid = calculateCentroid(polygonPoints);
        		points = lineStringPoints;
        	}
        	
        	final GeoPoint centroidFinal = centroid; 
            // Ordenar os pontos em relação ao centróide
            Collections.sort(points, new Comparator<GeoPoint>() {
                @Override
                public int compare(GeoPoint p1, GeoPoint p2) {
                    double angle1 = Math.atan2(p1.getLatitude() - centroidFinal.getLatitude(), p1.getLongitude() - centroidFinal.getLongitude());
                    double angle2 = Math.atan2(p2.getLatitude() - centroidFinal.getLatitude(), p2.getLongitude() - centroidFinal.getLongitude());
                    return Double.compare(angle1, angle2);
                }
            });

            // Criar a YailList a partir dos pontos ordenados
            List<YailList> yailPointsList = new ArrayList<>();
            for (GeoPoint geoPoint : points) {
                yailPointsList.add(YailList.makeList(new Double[]{geoPoint.getLatitude(), geoPoint.getLongitude()}));
            }

            return YailList.makeList(yailPointsList);
        } catch (Exception e) {
            if(flagLogName) Log.e("GetOrderedPointsYailList", "Error ordering points", e);
            e.printStackTrace();
            ReportError("Error ordering points.");
            return YailList.makeEmptyList();
        }
    }
    
    @SimpleFunction(description = "Calculates the area of the polygon drawn on the map.")
    public double CalculateArea() {
    	
    	GeoPoint centroid = null;
        List<GeoPoint> points = new ArrayList<>();

        if (isPolygon) {
        	if (polygonPoints.size() < 3) return 0.0;
            centroid = calculateCentroid(polygonPoints);
            points = polygonPoints;
        } else if (isLineString) {
        	if (lineStringPoints.size() < 2) return 0.0;
            centroid = calculateCentroid(lineStringPoints);
            points = lineStringPoints;
        } else {
        	return 0.0;
        }
    	
        if (centroid == null) {
            // Tratar o caso em que o centróide não está definido
            return 0.0;
        }        

        double totalArea = 0.0;

        // Escolher um ponto central para formar triângulos com os pontos do polígono
        Marker centroidMarker = new Marker(mapComponent);
        centroidMarker.SetLocation(centroid.getLatitude(), centroid.getLongitude());

        // Calcular a área somando a área dos triângulos formados
        for (int i = 0; i < points.size(); i++) {
            GeoPoint point1 = points.get(i);
            GeoPoint point2 = points.get((i + 1) % points.size());

            // Calcular as distâncias entre os pontos e o centróide
            double dist1 = centroidMarker.DistanceToPoint(point1.getLatitude(), point1.getLongitude());
            double dist2 = centroidMarker.DistanceToPoint(point2.getLatitude(), point2.getLongitude());
            double dist3 = distanceBetween(point1, point2); // Substituir por uma função que calcule a distância entre dois GeoPoint

            // Calcular o semi-perímetro do triângulo
            double s = (dist1 + dist2 + dist3) / 2;

            // Calcular a área do triângulo usando a fórmula de Heron
            totalArea += Math.sqrt(s * (s - dist1) * (s - dist2) * (s - dist3));
        }

        return totalArea; // Área em metros quadrados
    }
    
    @SimpleFunction(description = "Calculates the distance between two points considering the Earth's curvature.")
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Raio médio da Terra em metros

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distância em metros
    }
    
    @SimpleFunction(description = "Calculates the total distance of the added lines.")
    public double CalculateTotalLineDistance() {
        if (lineStringPoints.size() < 2) return 0.0;

        double totalDistance = 0.0;
        for (int i = 0; i < lineStringPoints.size() - 1; i++) {
            totalDistance += calculateDistance(
                lineStringPoints.get(i).getLatitude(), lineStringPoints.get(i).getLongitude(),
                lineStringPoints.get(i + 1).getLatitude(), lineStringPoints.get(i + 1).getLongitude()
            );
        }

        return totalDistance;
    }
    
    @SimpleFunction(description = "Reset the area points and remove specific features from the map.")
    public void ResetArea(final YailList featuresToRemove) {
        // Executar na thread de UI
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Iterar pela YailList e processar cada recurso
                for (int i = 0; i < featuresToRemove.size(); i++) {
                    Object feature = featuresToRemove.getObject(i);

                    if (feature instanceof Polygon) {
                        mapComponent.removeFeature((Polygon) feature);
                    } else if (feature instanceof Marker) {
                        mapComponent.removeFeature((Marker) feature);
                    } else if (feature instanceof LineString) {
                        mapComponent.removeFeature((LineString) feature);
                    }
                }

                // Limpar listas de pontos
                polygonPoints.clear();
                lineStringPoints.clear();
                currentPolygons.clear();
                currentMarkers.clear();

                // Reinicializar LineString
                lineString = new LineString(mapComponent);

                // Forçar o mapa a se atualizar
                mapComponent.getView().invalidate();
            }
        });
    }
    
    /*
     * EVENTS
     */
    
    @SimpleEvent(description = "Report an error with a custom message")
    public void ReportError(String errorMessage) {
        EventDispatcher.dispatchEvent(this, "ReportError", errorMessage);
    }
    
    /*
     * PRIVATE METHODS
     */
    private double distanceBetween(GeoPoint point1, GeoPoint point2) {
        final int R = 6371000; // Raio da Terra em metros
        double lat1 = Math.toRadians(point1.getLatitude());
        double lon1 = Math.toRadians(point1.getLongitude());
        double lat2 = Math.toRadians(point2.getLatitude());
        double lon2 = Math.toRadians(point2.getLongitude());

        double deltaLat = lat2 - lat1;
        double deltaLon = lon2 - lon1;

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c; // Distância em metros
    }
    
    private GeoPoint calculateCentroid(List<GeoPoint> points) {
        double latitude = 0;
        double longitude = 0;
        int n = points.size();

        for (GeoPoint point : points) {
            latitude += point.getLatitude();
            longitude += point.getLongitude();
        }

        return new GeoPoint(latitude / n, longitude / n);
    }

    private List<GeoPoint> reorderPolygonPoints(List<GeoPoint> points) {
        final GeoPoint centroid = calculateCentroid(points);

        Collections.sort(points, new Comparator<GeoPoint>() {
            @Override
            public int compare(GeoPoint p1, GeoPoint p2) {
                double angle1 = Math.atan2(p1.getLatitude() - centroid.getLatitude(), p1.getLongitude() - centroid.getLongitude());
                double angle2 = Math.atan2(p2.getLatitude() - centroid.getLatitude(), p2.getLongitude() - centroid.getLongitude());

                // Ordenar em sentido horário
                return Double.compare(angle1, angle2);
            }
        });

        return points;  // Retorna a lista reordenada
    }

}
