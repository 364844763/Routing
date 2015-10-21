
package com.hit.jj.mapshow;

import java.util.ArrayList;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.esri.arcgis.android.samples.routing.R;

public class ShowDirections extends ListActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    
    // Retrieve the list of directions from the intent
    final Intent i = getIntent();
    final ArrayList<String> directions = i.getStringArrayListExtra("directions");
    
    // Sets the list to the list of directions
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, R.layout.list_item, directions);
    setListAdapter(adapter);
    ListView lv = getListView();
    lv.setTextFilterEnabled(true);
    
    // Returns the selected item to the calling activity
    lv.setOnItemClickListener(new OnItemClickListener() {
      public void onItemClick(AdapterView<?> parent, View view, int position,
          long id) {
        i.putExtra("returnedDirection", ((TextView) view).getText());
        setResult(RESULT_OK, i);
        finish();
      }
    });
  }
}

