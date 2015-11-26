/* Copyright 2015 Esri
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hit.jj.mapshow;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.esri.android.map.Callout;
import com.esri.android.map.GraphicsLayer;
import com.esri.android.map.LocationDisplayManager;
import com.esri.android.map.LocationDisplayManager.AutoPanMode;
import com.esri.android.map.MapView;
import com.esri.android.map.ags.ArcGISDynamicMapServiceLayer;
import com.esri.android.map.ags.ArcGISFeatureLayer;
import com.esri.android.map.event.OnLongPressListener;
import com.esri.android.map.event.OnSingleTapListener;
import com.esri.arcgis.android.samples.routing.R;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.map.CallbackListener;
import com.esri.core.map.FeatureSet;
import com.esri.core.map.Graphic;
import com.esri.core.symbol.SimpleLineSymbol;
import com.esri.core.tasks.SpatialRelationship;
import com.esri.core.tasks.ags.query.Query;
import com.esri.core.tasks.na.Route;
import com.esri.core.tasks.na.RouteResult;
import com.esri.core.tasks.na.RouteTask;
import com.hit.jj.http.OkHttpClientManager;
import com.hit.jj.mapshow.fragment.RoutingDialogFragment;
import com.hit.jj.mapshow.fragment.RoutingListFragment;
import com.hit.jj.mapshow.fragment.SpeakDialogFragment;
import com.hit.jj.pathplaning.Path;
import com.squareup.okhttp.Request;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RoutingActivity extends Activity implements
        RoutingListFragment.onDrawerListSelectedListener,
        RoutingDialogFragment.onGetRoute,SpeakDialogFragment.SpeakCallback {
    public static MapView map = null;
    ArcGISDynamicMapServiceLayer tileLayer;
    GraphicsLayer routeLayer, hiddenSegmentsLayer;
    public LocationManager manager;
    // Symbol used to make route segments "invisible"
    SimpleLineSymbol segmentHider = new SimpleLineSymbol(Color.WHITE, 5);
    // Symbol used to highlight route segments
    SimpleLineSymbol segmentShower = new SimpleLineSymbol(Color.RED, 5);
    // Label showing the current direction, time, and length
    TextView directionsLabel;
    // List of the directions for the current route (used for the ListActivity)
    public static ArrayList<String> curDirections = null;
    // Current route, route summary, and gps location
    Route curRoute = null;
    String routeSummary = null;
    public static Point mLocation = null;
    // Global results variable for calculating route on separate thread
    RouteTask mRouteTask = null;
    RouteResult mResults = null;
    // Variable to hold server exception to show to user
    Exception mException = null;
    GraphicsLayer graphicsLayer = null;
    ImageView img_speak;
    ImageView img_cancel;
    ImageView img_currLocation;
    ImageView img_getDirections;
    ImageView img_showDirections;
    public static DrawerLayout mDrawerLayout;
    LocationDisplayManager ldm;
    public static List<Path> mPaths;
    ArcGISFeatureLayer mFeatureLayer;
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

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        init();
        setListener();
    }

    /**
     * 界面初始化
     */
    private void init() {
        manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }
        // Retrieve the map and initial extent from XML layout
        map = (MapView) findViewById(R.id.map);
        // Add tiled layer to MapView
        tileLayer = new ArcGISDynamicMapServiceLayer(
                "http://58.199.250.101:6080/arcgis/rest/services/xmut/map/MapServer");
        map.addLayer(tileLayer);
        mFeatureLayer = new ArcGISFeatureLayer("http://58.199.250.101:6080/arcgis/rest/services/xm/path1/FeatureServer/0", ArcGISFeatureLayer.MODE.SELECTION);
        map.addLayer(mFeatureLayer);
        Callout mapCallout = map.getCallout();
        mapCallout.hide();
        // Add the route graphic layer (shows the full route)
        routeLayer = new GraphicsLayer();
        map.addLayer(routeLayer);
        mFeatureLayer.getFields();
        graphicsLayer = new GraphicsLayer();
        map.addLayer(graphicsLayer);
        MyOnSingleTapListener listener = new MyOnSingleTapListener(this);
        map.setOnSingleTapListener(listener);
        try {
            mRouteTask = RouteTask.createOnlineRouteTask(
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
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        img_cancel = (ImageView) findViewById(R.id.iv_cancel);
        img_currLocation = (ImageView) findViewById(R.id.iv_myLocation);
        img_getDirections = (ImageView) findViewById(R.id.iv_getDirections);
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        img_speak= (ImageView) findViewById(R.id.iv_speak);
        img_showDirections= (ImageView) findViewById(R.id.direction);
        // Get the location display manager and start reading location. Don't
        // auto-pan
        // to center our position
        ldm = map.getLocationDisplayManager();
        ldm.setLocationListener(new MyLocationListener());
        ldm.start();
        ldm.setAutoPanMode(AutoPanMode.LOCATION);

        // Set the directionsLabel with initial instructions.
        directionsLabel = (TextView) findViewById(R.id.directionsLabel);
        directionsLabel.setText(getString(R.string.route_label));
    }

    /**
     * 设置监听
     */
    private void setListener() {

        /**
         * On single clicking the directions label, start a ListActivity to show
         * the list of all directions for this route. Selecting one of those
         * items will return to the map and highlight that segment.
         *
         */
        directionsLabel.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                if (curDirections == null)
                    return;

                mDrawerLayout.openDrawer(Gravity.RIGHT);

                String segment = directionsLabel.getText().toString();

                ListView lv = RoutingListFragment.mDrawerList;
                for (int i = 0; i < lv.getCount() - 1; i++) {
                    String lv_segment = lv.getItemAtPosition(i).toString();
                    if (segment.equals(lv_segment)) {
                        lv.setSelection(i);
                    }
                }
            }

        });
        img_cancel.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {


                clearAll();
            }
        });

        img_currLocation.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.e("mLocation", mLocation.getX() + " " + mLocation.getY());
                Point p = (Point) GeometryEngine.project(mLocation, egs, wm);
                map.zoomToResolution(p, 20.0);

            }
        });

        img_getDirections.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                RoutingDialogFragment frag_dialog = new RoutingDialogFragment();
                ft.add(frag_dialog, "Dialog");
                ft.commit();

            }
        });
        img_speak.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                FragmentManager fm = getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                SpeakDialogFragment frag_dialog = new SpeakDialogFragment();
                ft.add(frag_dialog, "SpeakDialog");
                ft.commit();

            }
        });

        /**
         * On single tapping the map, query for a route segment and highlight
         * the segment and show direction summary in the label if a segment is
         * found.
         */
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
                String label = String.format("%s%n%.1f minutes (%.1f miles)",
                        direction, time, length);
                directionsLabel.setText(label);
                // Zoom to the extent of that segment
                map.setExtent(selected.getGeometry(), 50);
            }

        });

        /**
         * On long pressing the map view, route from our current location to the
         * pressed location.
         *
         */
        map.setOnLongPressListener(new OnLongPressListener() {

            private static final long serialVersionUID = 1L;

            public boolean onLongPress(final float x, final float y) {
                final Point loc = map.toMapPoint(x, y);
                Point  s1= new Point(26.1023006,119.2789993);
                Point sp = new Point(loc.getY(), loc.getX());
//                pathfind(mLocation, sp);//实际的
                pathfind(s1,sp);//测试点
                return true;
            }

        });
    }

    /**
     * If GPS is disabled, app won't be able to route. Hence display a dialoge window to enable the GPS
     */
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please enable your GPS before proceeding")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Updates the UI after a successful rest response has been received.
     */
    void updateUI() {
        dialog.dismiss();
        img_cancel.setVisibility(View.VISIBLE);
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag("Nav Drawer") == null) {
            FragmentTransaction ft = fm.beginTransaction();
            RoutingListFragment frag = new RoutingListFragment();
            ft.add(frag, "Nav Drawer");
            ft.commit();
        } else {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(fm.findFragmentByTag("Nav Drawer"));
            RoutingListFragment frag = new RoutingListFragment();
            ft.add(frag, "Nav Drawer");
            ft.commit();
        }
        //img_showDirections.setVisibility(View.VISIBLE);

    }

    @Override
    public void onDialogRouteClicked(Point p) {
        Point s1 = new Point(26.1023006,119.2789993);
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.remove(fm.findFragmentByTag("SpeakDialog")).commit();
           // pathfind(mLocation, p);//实际导航
            pathfind(s1,p);//测试点
    }

    private class MyLocationListener implements LocationListener {

        public MyLocationListener() {
            super();
        }

        /**
         * If location changes, update our current location. If being found for
         * the first time, zoom to our current position with a resolution of 20
         */
        public void onLocationChanged(Location loc) {
            if (loc == null)
                return;
            boolean zoomToMe = (mLocation == null);
            //Log.e("mLocation","实际"+loc.getLongitude()+" "+loc.getLatitude());
            mLocation = new Point(loc.getLongitude(), loc.getLatitude());
            //测试数据
//			mLocation = new Point(119.2789993,26.1023006);
            if (zoomToMe) {
                Point p = (Point) GeometryEngine.project(mLocation, egs, wm);
                map.zoomToResolution(p, 5);
//
            }
        }

        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), "GPS Disabled",
                    Toast.LENGTH_SHORT).show();
            buildAlertMessageNoGps();
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

    /*
     * When the user selects the segment from the listview, it gets highlighted
     * on the map
     */
    @Override
    public void onSegmentSelected(String id) {

        if (id == null)
            return;
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.remove(fm.findFragmentByTag("Nav Drawer")).commit();
        clearAll();
        // Look for the graphic that corresponds to this direction
        removeRoad(mPaths,id);
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }

    @Override
    public void onDialogRouteClicked(Point p1, Point p2) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.remove(fm.findFragmentByTag("Dialog")).commit();

        pathfind(p1, p2);

    }

    /**
     * 路径搜索
     * @param p1
     * @param p2
     */
    private void pathfind(Point p1, Point p2) {
        clearAll();
        dialog = ProgressDialog.show(RoutingActivity.this, "路径导航",
                "规划路径中...", true);
        List<String> msg;
        String where = "";
        Log.e("jj", "http://58.199.250.101:8088/MyPathPlanServer/PathFindServer?start_x=" + p1.getX() + "&&start_y=" + p1.getY() + "&&end_x=" + p2.getX() + "&&end_y=" + p2.getY());
        try {
            OkHttpClientManager.getAsyn("http://58.199.250.101:8088/MyPathPlanServer/PathFindServer?start_x=" + p1.getX() + "&&start_y=" + p1.getY() + "&&end_x=" + p2.getX() + "&&end_y=" + p2.getY(), new OkHttpClientManager.ResultCallback<List<Path>>() {
                @Override
                public void onError(Request request, Exception e) {
                    Log.e("jj","查询失败");
                }

                @Override
                public void onResponse(final List<Path> paths) {
                    mPaths=paths;
                    String where = "";
                    for (Path path : paths) {
                        Log.e("tag", path.getId());
                        where = where + "luwang_ID=" + "'" + path.getId() + "'" + " or ";
                    }
                    Query mQuery = new Query();
                    mQuery.setOutFields(new String[]{"*"});
                    //  mQuery.setWhere("luwang_DIRECTION='1'");
                    mQuery.setWhere(where.substring(0, where.length() - 4));
                    Log.d("jj", "Select Features Error" + where.substring(0, where.length()));
                    mQuery.setReturnGeometry(true);
                    mQuery.setInSpatialReference(map.getSpatialReference());
                    mQuery.setSpatialRelationship(SpatialRelationship.INTERSECTS);
                    mFeatureLayer.selectFeatures(mQuery, ArcGISFeatureLayer.SELECTION_METHOD.NEW, new CallbackListener<FeatureSet>() {
                        @Override
                        public void onCallback(FeatureSet featureSet) {
                            Log.d("jj", "成功");
                            dialog.dismiss();
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Log.d("jj", "Select Features Error" + mFeatureLayer.getFields()[0]);

                        }

                    });
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            updateUI();
                        }
                    });
                 /*   new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            {
                                String where = "";
                                for (Path path : paths) {
                                    Log.e("tag", path.getId());
                                    where = where + "luwang_ID=" + "'" + path.getId() + "'" + " or ";

                                    Query mQuery = new Query();
                                    mQuery.setOutFields(new String[]{"*"});
                                    //  mQuery.setWhere("luwang_DIRECTION='1'");
                                    mQuery.setWhere(where.substring(0, where.length() - 4));
                                    Log.d("jj", "Select Features Error" + where.substring(0, where.length()));
                                    mQuery.setReturnGeometry(true);
                                    mQuery.setInSpatialReference(map.getSpatialReference());
                                    mQuery.setSpatialRelationship(SpatialRelationship.INTERSECTS);
                                    mFeatureLayer.selectFeatures(mQuery, ArcGISFeatureLayer.SELECTION_METHOD.NEW, new CallbackListener<FeatureSet>() {
                                        @Override
                                        public void onCallback(FeatureSet featureSet) {
                                            Log.d("jj", "成功");
                                        }

                                        @Override
                                        public void onError(Throwable throwable) {
                                            Log.d("jj", "Select Features Error" + mFeatureLayer.getFields()[0]);

                                        }

                                    });}


                            }
                        }
                    }, 3000);*/

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void removeRoad(List<Path> paths,String id){
        boolean is=false;
        String where="";
        for (Path path : paths) {
            if (!is){
                if (path.getId().equals(id)){
                    is=true;
                }
            }else {
                Log.e("tag", path.getId());where = where + "luwang_ID=" + "'" + path.getId() + "'" + " or ";
            }

        }
        Query mQuery = new Query();
        mQuery.setOutFields(new String[]{"*"});
        //  mQuery.setWhere("luwang_DIRECTION='1'");
        mQuery.setWhere(where.substring(0, where.length() - 4));
        Log.d("jj", "Select Features Error" + where.substring(0, where.length()));
        mQuery.setReturnGeometry(true);
        mQuery.setInSpatialReference(map.getSpatialReference());
        mQuery.setSpatialRelationship(SpatialRelationship.INTERSECTS);
        mFeatureLayer.selectFeatures(mQuery, ArcGISFeatureLayer.SELECTION_METHOD.NEW, new CallbackListener<FeatureSet>() {
            @Override
            public void onCallback(FeatureSet featureSet) {
                Log.d("jj", "成功");
                dialog.dismiss();
            }

            @Override
            public void onError(Throwable throwable) {
                Log.d("jj", "Select Features Error" + mFeatureLayer.getFields()[0]);

            }

        });
    }

	/**
	 * Clear the graphics and empty the directions list
	 */
    public void clearAll() {
        mFeatureLayer.removeAll();
        //Removing the graphics from the layer
        routeLayer.removeAll();
        hiddenSegmentsLayer.removeAll();

        curDirections = new ArrayList<String>();
        mResults = null;
        curRoute = null;

        //Setting to default text
        directionsLabel.setText(getString(R.string.route_label));

        //Locking the Drawer
        mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        //Removing the cancel icon
        img_cancel.setVisibility(View.GONE);

        //Removing the RoutingListFragment if present
        FragmentManager fm = getFragmentManager();
        if (fm.findFragmentByTag("Nav Drawer") != null) {
            FragmentTransaction ft = fm.beginTransaction();
            ft.remove(fm.findFragmentByTag("Nav Drawer"));
            ft.commit();
        }

    }

    private class MyOnSingleTapListener implements OnSingleTapListener {

        // Here, we use a single tap to popup the attributes for a report...
        Context _ctx;
        private static final long serialVersionUID = 1L;

        public MyOnSingleTapListener(Context ctx) {
            _ctx = ctx;
        }

        @Override
        public void onSingleTap(float x, float y) {
            Callout mapCallout = map.getCallout();
            mapCallout.hide();
            Point pnt = map.toMapPoint(x, y);

            int[] grs = graphicsLayer.getGraphicIDs(x, y, 20);
            Log.d("Test", "Graphics number is " + grs.length);

            if (grs.length > 0) {
                Graphic g = graphicsLayer.getGraphic(grs[0]);
                Map<String, Object> atts = g.getAttributes();
                String text = "";
                for (int i = 0; i < atts.size(); i++) {
                    text = text + atts.keySet().toArray()[i] + ": " + atts.values().toArray()[i] + "\n";
                }

                TextView tv = new TextView(_ctx);
                tv.setText(text);

                // Here, we populate the Callout with the attribute information
                // from the report.
                mapCallout.setOffset(0, -3);
                mapCallout.setCoordinates(pnt);
                mapCallout.setMaxHeight(350);
                mapCallout.setMaxWidth(900);

                mapCallout.setContent(tv);

                mapCallout.show();
            }

        }
    }

}