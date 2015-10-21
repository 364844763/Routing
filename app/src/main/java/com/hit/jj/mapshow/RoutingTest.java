/*
package com.esri.arcgis.android.samples.routing;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polyline;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.PictureMarkerSymbol;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.na.NAFeaturesAsFeature;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteDirection;
import com.esri.core.tasks.na.RouteParameters;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.esri.core.tasks.na.StopGraphic;

import java.util.ArrayList;
import java.util.HashMap;

*/
/**
 * Created by Administrator on 2015/9/10.
 *//*

public class RoutingTest extends Activity {
    MapView map = null;
    ArcGISDynamicMapServiceLayer tileLayer;
    GraphicsLayer routeLayer, hiddenSegmentsLayer;
    // Symbol used to make route segments "invisible"
    SimpleLineSymbol segmentHider = new SimpleLineSymbol(Color.WHITE, 5);
    // Symbol used to highlight route segments
    SimpleLineSymbol segmentShower = new SimpleLineSymbol(Color.RED, 5);
    // Label showing the current direction, time, and length
    TextView directionsLabel;
    // List of the directions for the current route (used for the ListActivity)
    ArrayList<String> curDirections = null;
    // Current route, route summary, and gps location
    Route curRoute = null;
    String routeSummary = null;
    LocationDisplayManager ldm;
    Point mLocation = null;
    RouteTask mRouteTask = null;
    // Global results variable for calculating route on separate thread
    RouteResult mResults = null;
    // Variable to hold server exception to show to user
    Exception mException = null;
    // Handler for processing the results
    final Handler mHandler = new Handler();
    final Runnable mUpdateResults = new Runnable() {
        public void run() {
            updateUI();
        }
    };

    // Progress dialog to show when route is being calculated
    ProgressDialog dialog;
    // Spatial references used for projecting points
    final SpatialReference wm = SpatialReference.create(102100);
    final SpatialReference egs = SpatialReference.create(4326);
    // Index of the currently selected route segment (-1 = no selection)
    int selectedSegmentID = -1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Retrieve the map and initial extent from XML layout
        map = (MapView) findViewById(R.id.map);
        // Add tiled layer to MapView�IP���������IP��58.199.250.101

        tileLayer = new ArcGISDynamicMapServiceLayer(
                "http://58.199.250.101:6080/arcgis/rest/services/xmut/map/MapServer");
        map.addLayer(tileLayer);

        // Add the route graphic layer (shows the full route)
        routeLayer = new GraphicsLayer();
        map.addLayer(routeLayer);

        try {
            mRouteTask = RouteTask
                    .createOnlineRouteTask(
                            "http://58.199.250.101:6080/arcgis/rest/services/xmut/path2/NAServer/path",
                            null);
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        // Add the hidden segments layer (for highlighting route segments)
        hiddenSegmentsLayer = new GraphicsLayer();
        map.addLayer(hiddenSegmentsLayer);

        // Make the segmentHider symbol "invisible"
        segmentHider.setAlpha(1);

        // Get the location service and start reading location. Don't auto-pan
        // to center our position
        ldm = map.getLocationDisplayManager();
        ldm.setLocationListener(new MyLocationListener());
        ldm.start();
        ldm.setAutoPanMode(LocationDisplayManager.AutoPanMode.OFF);

        // Set the directionsLabel with initial instructions.
        directionsLabel = (TextView) findViewById(R.id.directionsLabel);
        directionsLabel.setText(getString(R.string.route_label));

        */
/**
         * On single clicking the directions label, start a ListActivity to show
         * the list of all directions for this route. Selecting one of those
         * items will return to the map and highlight that segment.
         *
         *//*

        directionsLabel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (curDirections == null) {
                    Toast.makeText(getApplicationContext(), "û��·����Ϣ", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent i = new Intent(getApplicationContext(),
                        ShowDirections.class);
                i.putStringArrayListExtra("directions", curDirections);
                startActivityForResult(i, 1);
            }

        });

        */
/**
         * On long clicking the directions label, removes the current route and
         * resets all affiliated variables.
         *
         *//*

        directionsLabel.setOnLongClickListener(new View.OnLongClickListener() {

            public boolean onLongClick(View v) {
                Toast.makeText(getApplicationContext(), "1111111", Toast.LENGTH_SHORT).show();
                routeLayer.removeAll();
                hiddenSegmentsLayer.removeAll();
                curRoute = null;
                curDirections = null;
                directionsLabel.setText(getString(R.string.route_label));
                return true;
            }

        });

        */
/**
         * On single tapping the map, query for a route segment and highlight
         * the segment and show direction summary in the label if a segment is
         * found.
         *//*

        map.setOnSingleTapListener(new OnSingleTapListener() {
            private static final long serialVersionUID = 1L;

            public void onSingleTap(float x, float y) {
                // Get all the graphics within 20 pixels the click
                int[] indexes = hiddenSegmentsLayer.getGraphicIDs(x, y, 20);
                // Hide the currently selected segment
                hiddenSegmentsLayer.updateGraphic(selectedSegmentID,
                        segmentHider);

                if (indexes.length < 1) {
                    // If no segments were found but there is currently a route,
                    // zoom to the extent of the full route
                    if (curRoute != null) {
                        map.setExtent(curRoute.getEnvelope(), 250);
                        directionsLabel.setText(routeSummary);
                    }
                    return;
                }
                // Otherwise update our currently selected segment
                selectedSegmentID = indexes[0];
                Graphic selected = hiddenSegmentsLayer
                        .getGraphic(selectedSegmentID);
                // Highlight it on the map
                hiddenSegmentsLayer.updateGraphic(selectedSegmentID,
                        segmentShower);
                String direction = ((String) selected.getAttributeValue("text"));
                double time = ((Double) selected.getAttributeValue("time"))
                        .doubleValue();
                double length = ((Double) selected.getAttributeValue("length"))
                        .doubleValue();
                // Update the label with this direction's information
                String label = String.format(
                        "%s%nTime: %.1f minutes, Length: %.1f miles",
                        direction, time, length);
                directionsLabel.setText(label);
                // Zoom to the extent of that segment
                map.setExtent(selected.getGeometry(), 50);
            }

        });

        */
/**
         * On long pressing the map view, route from our current location to the
         * pressed location.
         *
         *//*

        map.setOnLongPressListener(new OnLongPressListener() {

            private static final long serialVersionUID = 1L;

            public boolean onLongPress(final float x, final float y) {

                // Clear the graphics and empty the directions list
                routeLayer.removeAll();
                hiddenSegmentsLayer.removeAll();
                curDirections = new ArrayList<String>();
                mResults = null;

                // retrieve the user clicked location
                final Point loc = map.toMapPoint(x, y);

                // Show that the route is calculating
                dialog = ProgressDialog.show(RoutingTest.this, "",
                        "Calculating route...", true);
                // Spawn the request off in a new thread to keep UI responsive
                Thread t = new Thread() {
                    @Override
                    public void run() {
                        try {
                            // Start building up routing parameters
                            RouteParameters rp = mRouteTask
                                    .retrieveDefaultRouteTaskParameters();
                            NAFeaturesAsFeature rfaf = new NAFeaturesAsFeature();
                            // Convert point to EGS (decimal degrees)

//							Point p = (Point) GeometryEngine.project(loc, wm,
//									egs);
                            // Create the stop points (start at our location, go
                            // to pressed location)


                            // �������
                            Point s = new Point(119.288822, 26.088625);
//							Point mPoint = (Point) GeometryEngine.project(s, wm,
//									egs);
                            StopGraphic point1 = new StopGraphic(loc);
                            // StopGraphic point1 = new StopGraphic(mLocation);
                            // �յ�����


                            StopGraphic point2 = new StopGraphic(s);

                            rfaf.setFeatures(new Graphic[] { point1, point2 });
                            rfaf.setCompressedRequest(true);
                            rp.setStops(rfaf);
                            // Set the routing service output SR to our map
                            // service's SR
                            rp.setOutSpatialReference(wm);

                            // Create a new routing task pointing to an
                            // NAService (null credentials -> free service)


                            // Solve the route and use the results to update UI
                            // when received
                            mResults = mRouteTask.solve(rp);
                            mHandler.post(mUpdateResults);
                        } catch (Exception e) {
                            System.out.println(e.toString());
                            mException = e;
                            mHandler.post(mUpdateResults);
                        }
                    }
                };
                // Start the operation
                t.start();

                return false;
            }
        });
    }

    */
/**
     * ����UI
     *//*

    void updateUI() {
        dialog.dismiss();
        if (mResults == null) {
            Toast.makeText(RoutingTest.this, mException.toString(),
                    Toast.LENGTH_LONG).show();
            return;
        }
        curRoute = mResults.getRoutes().get(0);
        // Symbols for the route and the destination (blue line, checker flag)
        SimpleLineSymbol routeSymbol = new SimpleLineSymbol(Color.RED, 3);
        PictureMarkerSymbol destinationSymbol = new PictureMarkerSymbol(
                getResources().getDrawable(R.drawable.flag_finish));

        // Add all the route segments with their relevant information to the
        // hiddenSegmentsLayer, and add the direction information to the list
        // of directions
        for (RouteDirection rd : curRoute.getRoutingDirections()) {
            HashMap<String, Object> attribs = new HashMap<String, Object>();
            attribs.put("text", rd.getText());
            attribs.put("time", rd.getMinutes());
            attribs.put("length", rd.getLength());
            curDirections.add(String.format(
                    "%s%nTime: %.1f minutes, Length: %.1f miles", rd.getText(),
                    rd.getMinutes(), rd.getLength()));
            Graphic routeGraphic = new Graphic(rd.getGeometry(), segmentHider,
                    attribs);
            hiddenSegmentsLayer.addGraphic(routeGraphic);
        }
        // Reset the selected segment
        selectedSegmentID = -1;

        // Add the full route graphic and destination graphic to the routeLayer
        Graphic routeGraphic = new Graphic(curRoute.getRouteGraphic().getGeometry(),
                routeSymbol);
        Graphic endGraphic = new Graphic(
                ((Polyline) routeGraphic.getGeometry()).getPoint(((Polyline) routeGraphic
                        .getGeometry()).getPointCount() - 1), destinationSymbol);
        routeLayer.addGraphics(new Graphic[] { routeGraphic, endGraphic });
        // Get the full route summary and set it as our current label
        routeSummary = String.format(
                "%s%nTotal time: %.1f minutes, length: %.1f miles",
                curRoute.getRouteName(), curRoute.getTotalMinutes(),
                curRoute.getTotalMiles());
        directionsLabel.setText(routeSummary);
        // Zoom to the extent of the entire route with a padding
        map.setExtent(curRoute.getEnvelope(), 250);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Response from directions list view
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                String direction = data.getStringExtra("returnedDirection");
                if (direction == null)
                    return;
                // Look for the graphic that corresponds to this direction
                for (int index : hiddenSegmentsLayer.getGraphicIDs()) {
                    Graphic g = hiddenSegmentsLayer.getGraphic(index);
                    if (direction
                            .contains((String) g.getAttributeValue("text"))) {
                        // When found, hide the currently selected, show the new
                        // selection
                        hiddenSegmentsLayer.updateGraphic(selectedSegmentID,
                                segmentHider);
                        hiddenSegmentsLayer.updateGraphic(index, segmentShower);
                        selectedSegmentID = index;
                        // Update label with information for that direction
                        directionsLabel.setText(direction);
                        // Zoom to the extent of that segment
                        map.setExtent(
                                hiddenSegmentsLayer.getGraphic(
                                        selectedSegmentID).getGeometry(), 50);
                        break;
                    }
                }
            }
        }
    }

    private class MyLocationListener implements LocationListener {

        public MyLocationListener() {
            super();
        }

        */
/**
         * If location changes, update our current location. If being found for
         * the first time, zoom to our current position with a resolution of 20
         *//*

        public void onLocationChanged(Location loc) {
            if (loc == null)
                return;
            boolean zoomToMe = (mLocation == null) ? true : false;
            mLocation = new Point(loc.getLongitude(), loc.getLatitude());
            if (zoomToMe) {
                Point p = (Point) GeometryEngine.project(mLocation, egs, wm);
                map.zoomToResolution(p, 20.0);
            }
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "GPS Disabled",
                    Toast.LENGTH_SHORT).show();
        }

        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), "GPS Enabled",
                    Toast.LENGTH_SHORT).show();
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        map.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.unpause();
    }

}

*/
