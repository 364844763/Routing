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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.baidu.speech.VoiceRecognitionService;
import com.esri.arcgis.android.samples.routing.R;
import com.esri.core.geometry.Point;
import com.hit.jj.http.OkHttpClientManager;
import com.hit.jj.pathplaning.Buliding;
import com.squareup.okhttp.Request;

import java.util.ArrayList;
import java.util.List;

/**
 * 界面只有输入目的地址，起止地址默认用当前位置坐标
 */
public class SpeakDialogFragment extends DialogFragment implements RecognitionListener{
    private ImageView iv_speech;
    private Button btn_getRoute;
    private EditText et_address;
    private View speechTips;
    private View speechWave;
    private SpeechRecognizer speechRecognizer;
    private Context context;
    private BuildingAdapter mAdapter;
    private Point p;
    SpeakCallback mCallback;
    List<Buliding> mList;
    ListView lv_source;
    public interface SpeakCallback{
        void onDialogRouteClicked(Point p);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        context = getActivity();


        // Removing title from the dialog box
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(context);
        sp.edit().remove("infile").commit(); // infile参数用于控制识别一个PCM音频流（或文件），每次进入程序都将该值清楚，以避免体验时没有使用录音的问题

        View view = inflater.inflate(R.layout.speak_dialog_layout, container);
        initView(view);
        initListener();
        return view;
    }
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (SpeakCallback) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement onDrawerListSelectedListener");
        }

    }

    private void initListener() {
        mList=new ArrayList<Buliding>();
        mAdapter=new BuildingAdapter(getActivity(),mList);
        lv_source.setAdapter(mAdapter);
        lv_source.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                et_address.setText(mList.get(position).getName());
                p = new Point(mList.get(position).getLatitude(), mList.get(position).getLongitude());
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
        btn_getRoute.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Point sp = new Point(119.3249969, 26.0660992);
                mCallback.onDialogRouteClicked(p);
            }
        });
        speechRecognizer.setRecognitionListener(this);
        iv_speech.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        speechTips.setVisibility(View.VISIBLE);
                        speechRecognizer.cancel();
                        Intent intent = new Intent();
                        addSound(intent);
                        intent.putExtra("vad", "touch");
                        speechRecognizer.startListening(intent);
                        return true;
                    case MotionEvent.ACTION_UP:
                        speechRecognizer.stopListening();
                        speechTips.setVisibility(View.GONE);
                        break;
                }
                return false;
            }
        });
    }

    private void initView(View view) {
        iv_speech = (ImageView) view.findViewById(R.id.iv_speech);
        btn_getRoute = (Button) view.findViewById(R.id.bGetRoute);
        et_address = (EditText) view.findViewById(R.id.et_address);
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context,
                new ComponentName(context, VoiceRecognitionService.class));
        lv_source= (ListView) view.findViewById(R.id.lv_destination);
        lv_source.setAdapter(mAdapter);
        speechTips = View.inflate(context, R.layout.popup_speech, null);
        speechWave = speechTips.findViewById(R.id.wave);
        speechTips.setVisibility(View.GONE);
        ((FrameLayout)view).addView(speechTips, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void addSound(Intent intent) {
        intent.putExtra("sound_start", R.raw.bdspeech_recognition_start);
        intent.putExtra("sound_end", R.raw.bdspeech_speech_end);
        intent.putExtra("sound_success", R.raw.bdspeech_recognition_success);
        intent.putExtra("sound_error", R.raw.bdspeech_recognition_error);
        intent.putExtra("sound_cancel", R.raw.bdspeech_recognition_cancel);

    }

    @Override
    public void onDestroy() {
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    @Override
    public void onReadyForSpeech(Bundle params) {

    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onRmsChanged(float rmsdB) {
        final int VTAG = 0xFF00AA01;
        Integer rawHeight = (Integer) speechWave.getTag(VTAG);
        if (rawHeight == null) {
            rawHeight = speechWave.getLayoutParams().height;
            speechWave.setTag(VTAG, rawHeight);
        }

        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) speechWave
                .getLayoutParams();
        params.height = (int) (rawHeight * rmsdB * 0.01);
        params.height = Math.max(params.height, speechWave.getMeasuredWidth());
        speechWave.setLayoutParams(params);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {

    }

    @Override
    public void onEndOfSpeech() {

    }

    @Override
    public void onError(int error) {
        StringBuilder sb = new StringBuilder();
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                sb.append("音频问题");
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                sb.append("没有语音输入");
                break;
            case SpeechRecognizer.ERROR_CLIENT:
                sb.append("其它客户端错误");
                break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                sb.append("权限不足");
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                sb.append("网络问题");
                break;
            case SpeechRecognizer.ERROR_NO_MATCH:
                sb.append("没有匹配的识别结果");
                break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                sb.append("引擎忙");
                break;
            case SpeechRecognizer.ERROR_SERVER:
                sb.append("服务端错误");
                break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                sb.append("连接超时");
                break;
        }
        sb.append(":" + error);
        Toast.makeText(context, "识别失败：" + sb.toString(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResults(Bundle results) {
        ArrayList<String> nbest = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        et_address.setText(nbest.get(0));
        getSearchTips(nbest.get(0));

    }

    @Override
    public void onPartialResults(Bundle partialResults) {

    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        //ERROR_CODE=11
        if (eventType == 11) {
            String reason = params.get("reason").toString();
            Toast.makeText(context, reason, Toast.LENGTH_LONG).show();
        }
    }
    public void getSearchTips(String keyWords){
        String url="http://58.199.250.101:8088/MyPathPlanServer/BuildingFindServer?name=";
        url+=keyWords;
        OkHttpClientManager.getAsyn(url, new OkHttpClientManager.ResultCallback<List<Buliding>>() {
            @Override
            public void onError(Request request, Exception e) {
                Toast.makeText(getActivity(), "网络错误", Toast.LENGTH_LONG).show();
            }

            @Override
            public void onResponse(List<Buliding> bulidings) {
                 {
                     mList = bulidings;

                    mAdapter.setData(bulidings);
                }
            }
        });

    }
}


