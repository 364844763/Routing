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
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.esri.arcgis.android.samples.routing.R;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.SpatialReference;
import com.esri.core.tasks.geocode.Locator;
import com.esri.core.tasks.geocode.LocatorFindParameters;
import com.esri.core.tasks.geocode.LocatorGeocodeResult;
import com.hit.jj.pathplaning.Buliding;

import java.util.ArrayList;
import java.util.List;

/*
 * This fragment displays a dialog box which contains text fields for source and destination addresses.
 * It also contains two icons "My Location" and "Swap Addresses". When user clicks on My Location icon, the focused
 * text field displays the text "My Location". When "Swap Addresses" icon is clicked the addresses in the text boxes are swapped.
 * When the user clicks on the "Route" button, the addresses are geocoded to points which are then used by the RoutingSample activity to
 * display the route.
 * 
 */

public class RoutingDialogFragment extends DialogFragment implements
		 OnClickListener ,View.OnFocusChangeListener{

	EditText et_source;
	EditText et_destination;
	Locator locator;
	static ProgressDialog dialog;
	static Handler handler;
	Button bGetRoute;
	BuildingAdapter mAdapter;
	BuildingAdapter mAdapterS;
	List<Buliding> mList;
	List<Buliding> mListS;
	ListView  et_source_lv;
	ListView et_destination_lv;
	// For storing the result of Geocoding Task
	List<LocatorGeocodeResult> result_origin = null;
	List<LocatorGeocodeResult> result_destination = null;
	List<String> bulidings;
	// Interface to be implemented by the activity
	onGetRoute mCallback;
	Point p1 = null;
	Point p2 = null;
	// To check if the edit text contains "My Location"
	boolean src_isMyLocation = false;
	boolean dest_isMyLocation = false;

	final SpatialReference wm = SpatialReference.create(102100);
	final SpatialReference egs = SpatialReference.create(4326);
	String source;
	String destination;
	int sourcePosition;
	int destinationPosition;
	// Image views for the icons
	ImageView img_sCancel, img_dCancel, img_myLocaion, img_swap;

	// Runnable to dismiss the process dialog
	static public class MyRunnable implements Runnable {
		public void run() {
			dialog.dismiss();
		}
	}

	// Interface
	public interface onGetRoute {
		 void onDialogRouteClicked(Point p1, Point p2);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		handler = new Handler();
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		try {
			mCallback = (onGetRoute) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement onDrawerListSelectedListener");
		}

	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {


		View view = inflater.inflate(R.layout.dialog_layout, container);
		// Adding custom Listener to edit text views
		//et_source.addTextChangedListener(new MyTextWatcher(et_source));


		// Adding Focus Change listener to the edit text views
//		et_source.setOnFocusChangeListener(this);
//		et_destination.setOnFocusChangeListener(this);

		// Setting onClick listener for the icons on the dialog


		// Removing title from the dialog box
		getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
		initView(view);
		initListener();
		return view;
	}

	private void initListener() {
		bGetRoute.setOnClickListener(this);
		img_dCancel.setOnClickListener(this);
		img_sCancel.setOnClickListener(this);
		img_swap.setOnClickListener(this);
		img_myLocaion.setOnClickListener(this);
		et_source.addTextChangedListener(new MyTextWatcher(et_source));
		et_destination.addTextChangedListener(new MyTextWatcher(et_destination));
		et_destination_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				et_destination.setText(mList.get(position).getName());
				p2=new Point(mList.get(position).getLatitude(),mList.get(position).getLongitude());
				//et_destination_lv.setVisibility(View.GONE);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						mAdapter.clear();
					}
				}, 500);

				//et_destination.setFocusable(false);
			}
		});
		et_source_lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				et_source.setText(mListS.get(position).getName());
				p1=new Point(mListS.get(position).getLatitude(),mListS.get(position).getLongitude());
				//et_destination_lv.setVisibility(View.GONE);
				new Handler().postDelayed(new Runnable() {
					@Override
					public void run() {
						mAdapterS.clear();
					}
				}, 500);

				//et_destination.setFocusable(false);
			}
		});
	}

	private void initView(View view) {

		// Set the views from the XML layout
		et_source = (EditText) view.findViewById(R.id.et_source);
		et_destination = (EditText) view.findViewById(R.id.et_destination);
		img_sCancel = (ImageView) view.findViewById(R.id.iv_cancelSource);
		img_dCancel = (ImageView) view.findViewById(R.id.iv_cancelDestination);
		img_swap = (ImageView) view.findViewById(R.id.iv_interchange);
		img_myLocaion = (ImageView) view.findViewById(R.id.iv_myDialogLocation);
		bGetRoute = (Button) view.findViewById(R.id.bGetRoute);
		et_destination_lv= (ListView) view.findViewById(R.id.et_destination_lv);
		mList=new ArrayList<Buliding>();
		mAdapter=new BuildingAdapter(getActivity(),mList);
		et_destination_lv.setAdapter(mAdapter);
		et_source_lv= (ListView) view.findViewById(R.id.et_source_lv);
		mListS=new ArrayList<Buliding>();
		mAdapterS=new BuildingAdapter(getActivity(),mListS);
		et_source_lv.setAdapter(mAdapterS);
	}


	private void geocode(String address1, String address2) {
		try {
			// create Locator parameters from single line address string
			LocatorFindParameters findParams_source = new LocatorFindParameters(
					address1);
			// set the search country to USA
			findParams_source.setSourceCountry("CHINA");
			// limit the results to 2
			findParams_source.setMaxLocations(2);
			// set address spatial reference to match map
			findParams_source.setOutSR(RoutingSample.map.getSpatialReference());
			// execute async task to geocode address

			LocatorFindParameters findParams_dest = new LocatorFindParameters(
					address2);
			findParams_dest.setSourceCountry("CHINA");
			findParams_dest.setMaxLocations(2);
			findParams_dest.setOutSR(RoutingSample.map.getSpatialReference());

			Geocoder gcoder = new Geocoder(findParams_source, findParams_dest);
			gcoder.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private class Geocoder extends AsyncTask<Void, Void, Void> {

		// Location Find Parameters for both source and destination addresses
		LocatorFindParameters lfp_start, lfp_dest;

		// Constructor
		public Geocoder(LocatorFindParameters findParams_start,
				LocatorFindParameters findParams_dest) {
			lfp_start = findParams_start;
			lfp_dest = findParams_dest;
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			// Displaying the Process Dialog
			dialog = ProgressDialog.show(getActivity(), "Routing Sample",
					"Geocoding the addresses ...");

		}

		@Override
		protected Void doInBackground(Void... params) {

			// set the geocode service

			locator = Locator.createOnlineLocator(getResources().getString(
					R.string.geocode_url));
			try {
				// pass address to find method to return point representing
				// address
				if (!src_isMyLocation)
					result_origin = locator.find(lfp_start);
				if (!dest_isMyLocation)
					result_destination = locator.find(lfp_dest);

			} catch (Exception e) {
				e.printStackTrace();
			}

			return null;
		}

		// The result of geocode task is passed as a parameter to map the
		// results

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);

			handler.post(new MyRunnable());
			Point p1 = null;
			Point p2 = null;

			// Projecting the current location to the output spatial ref
			Point currLocation = (Point) GeometryEngine.project(
					RoutingSample.mLocation, egs, wm);

			// Assignign current location to the field with value as
			// "My Location"
			if (src_isMyLocation)
				p1 = currLocation;
			else if (result_origin.size() > 0)
				p1 = result_origin.get(0).getLocation();

			if (dest_isMyLocation)
				p2 = currLocation;
			else if (result_destination.size() > 0)
				p2 = result_destination.get(0).getLocation();

			if (p1 == null) {
				Toast.makeText(getActivity(), "Not a valid source address",
						Toast.LENGTH_LONG).show();
			} else if (p2 == null) {
				Toast.makeText(getActivity(),
						"Not a valid destination address", Toast.LENGTH_LONG)
						.show();
			} else
				mCallback.onDialogRouteClicked(p1, p2);


		}
	}

	@Override
	public void onStop() {
		super.onStop();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

	}

	// Custom Listener for the Edit text views
	private class MyTextWatcher implements TextWatcher {

		private View view;

		private MyTextWatcher(View view) {
			this.view = view;
		}

		public void beforeTextChanged(CharSequence charSequence, int i, int i1,
				int i2) {
		}

		public void onTextChanged(CharSequence charSequence, int i, int i1,
				int i2) {
		}

		public void afterTextChanged(Editable editable) {

			// Displaying the cross icon when the edit text views are not empty
			String text = editable.toString();
			switch (view.getId()) {
			case R.id.et_source:
				if (text.length() > 0) {
					img_sCancel.setVisibility(View.VISIBLE);
					if (text.equals("My Location")){
						dest_isMyLocation=false;
						src_isMyLocation=true;
						break;
					}
					if (text.length() > 1){
						new Handler().post(new Runnable() {
							@Override
							public void run() {
								for (int i = 0; i < 10; i++) {
									Buliding buliding=new Buliding();
									buliding.setName("test"+i);
									buliding.setLatitude(1);
									buliding.setLongitude(2);
									mListS.add(buliding);

								}
								mAdapterS.setData(mListS);
							}
						});
					}

				}
				else{
					img_sCancel.setVisibility(View.INVISIBLE);
					mAdapterS.clear();
				}
				break;
			case R.id.et_destination:
				if (text.length() > 0){
					img_dCancel.setVisibility(View.VISIBLE);
					if (text.equals("My Location")){
						dest_isMyLocation=true;
						src_isMyLocation=false;
						break;
					}
					if (text.length() > 1){
						new Handler().post(new Runnable() {
							@Override
							public void run() {
								for (int i = 0; i < 10; i++) {
									Buliding buliding=new Buliding();
									buliding.setName("test"+i);
									buliding.setLatitude(1);
									buliding.setLongitude(2);
									mList.add(buliding);

								}
								mAdapter.setData(mList);
							}
						});
					}
//					OkHttpClientManager.getAsyn("http://58.199.250.101:8088/MyPathPlanServer/HelloWorld", new OkHttpClientManager.ResultCallback<List<String>>() {
//						@Override
//						public void onError(Request request, Exception e) {
//							bulidings = new ArrayList<String>();
//							for (int i = 0; i < 10; i++) {
//								String buliding = "test" + i;
//								bulidings.add(buliding);
//							}
//
//							new Handler().post(new Runnable() {
//								@Override
//								public void run() {
//									mAdapter.setData(bulidings);
//								}
//							});
////							mAdapter.setData(bulidings);
//						}
//
//						@Override
//						public void onResponse(List<String> bulidings) {
//
//							mAdapter.setData(bulidings);
//						}
//					});
				}
				else {
					img_dCancel.setVisibility(View.INVISIBLE);
					mAdapter.clear();
				}
				break;
			}
		}
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {

		// Checking the focus of the edit text and displaying the cross icon if
		// it is not empty
		switch (v.getId()) {
		case R.id.et_source:
			if (hasFocus && et_source.getText().toString().length() > 0)
				img_sCancel.setVisibility(View.VISIBLE);
			else
				img_sCancel.setVisibility(View.INVISIBLE);
			break;
		case R.id.et_destination:
			if (hasFocus && et_destination.getText().toString().length() > 0)
				img_dCancel.setVisibility(View.VISIBLE);
			else
				img_dCancel.setVisibility(View.INVISIBLE);

			break;
		}
	}

//	// OnClick events for the icons on the dialog box
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.iv_cancelSource:
			// Clearing the text
			et_source.getText().clear();
			mAdapterS.clear();
			p1=null;
			break;
		case R.id.iv_cancelDestination:
			// Clearing the text
			et_destination.getText().clear();
			mAdapter.clear();
			p2=null;
			break;
		case R.id.iv_myDialogLocation:

			// Putting "My Location" in the edit text that is in focus
			if (et_source.hasFocus())
				et_source.setText("My Location");
			else
				et_destination.setText("My Location");
			break;
		case R.id.iv_interchange:

			// Swapping the values
			String temp = et_source.getText().toString();
			et_source.setText(et_destination.getText().toString());
			et_destination.setText(temp);
			break;
			case R.id.bGetRoute:
			{
//				handler.post(new MyRunnable());
//				Point p1 = null;
//				Point p2 = null;

				// Projecting the current location to the output spatial ref
				Point currLocation = (Point) GeometryEngine.project(
						RoutingSample.mLocation, egs, wm);

				// Assignign current location to the field with value as
				// "My Location"
				if (et_source.getText().toString().equals("My Location"))
					p1 = currLocation;


				if (dest_isMyLocation)
					p2 = currLocation;


				if (p1 == null) {
					Toast.makeText(getActivity(), "Not a valid source address",
							Toast.LENGTH_LONG).show();
				} else if (p2 == null) {
					Toast.makeText(getActivity(),
							"Not a valid destination address", Toast.LENGTH_LONG)
							.show();
				} else {
					p1 = new Point(119.2636333,26.05468024);
					p2= new Point(119.2654226,26.05423861);
					mCallback.onDialogRouteClicked(p1, p2);

				}
			}
				break;
			}
		}


	}


