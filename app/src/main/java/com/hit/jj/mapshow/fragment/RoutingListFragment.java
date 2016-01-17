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

package com.hit.jj.mapshow.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import com.hit.jj.pathplaning.Path;

import com.esri.arcgis.android.samples.routing.R;
import com.hit.jj.mapshow.RoutingActivity;
import com.hit.jj.mapshow.adapter.PathAdapter;


/*
 * This fragment populates the Navigation Drawer with
 * a customized listview and also provides the interface 
 * for communicating with the activity.
 */
public class RoutingListFragment extends Fragment implements
		ListView.OnItemClickListener, TextToSpeech.OnInitListener {

	private Context context;
	public static ListView mDrawerList;
	onDrawerListSelectedListener mCallback;
	private TextToSpeech tts;
	private boolean isSoundOn = true;
	private RoutingActivity routingActivity;

	public RoutingListFragment(){

	}
	public RoutingListFragment(RoutingActivity routingActivity){
		this.routingActivity=routingActivity;
	}

	// Container Activity must implement this interface
	public interface onDrawerListSelectedListener {
		public void onSegmentSelected(String id);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.context=getActivity();
	}

	@Override
	public void onAttach(Activity activity) {

		super.onAttach(activity);

		try {
			mCallback = (onDrawerListSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement onDrawerListSelectedListener");
		}

		setHasOptionsMenu(true);

	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		tts = new TextToSpeech(getActivity(), this);

		PathAdapter adapter = new PathAdapter( RoutingActivity.mPaths
				,getActivity());
		mDrawerList = (ListView) getActivity().findViewById(R.id.right_drawer);

		mDrawerList.setAdapter(adapter);
		mDrawerList.setOnItemClickListener(this);

		Switch sound_toggle = (Switch) getActivity().findViewById(R.id.switch1);
		sound_toggle.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
										 boolean isChecked) {
				isSoundOn = isChecked;
			}
		});

	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		TextView segment = (TextView) view.findViewById(R.id.segment);
		//RoutingSample.mDrawerLayout.closeDrawers();
//		if (isSoundOn)
//			speakOut(segment.getText().toString());
		if (isSoundOn) {
			Path path = (Path) parent.getItemAtPosition(position);
			switch (path.getNextDirection()) {
				case 1:
					routingActivity.speak("前方左转");
					break;
				case 0:
					routingActivity.speak("直行");
					break;
				case 2:
					routingActivity.speak("前方右转");
					break;
			}
		}

		mCallback.onSegmentSelected(segment.getText().toString());
	}

//	private void speakOut(String text) {
//		tts.speak(text, TextToSpeech.QUEUE_FLUSH, null);
//	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.action_layout, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.direction:
			if (RoutingActivity.mDrawerLayout.isDrawerOpen(Gravity.RIGHT)) {
                RoutingActivity.mDrawerLayout.closeDrawers();
			} else {
                RoutingActivity.mDrawerLayout.openDrawer(Gravity.RIGHT);
			}
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onInit(int status) {

	}

	@Override
	public void onStop() {
		super.onStop();
//		tts.shutdown();

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

}
