/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package bluetoothchat;

import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bluetoothchat.R;
import com.example.android.common.logger.Log;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.StringReader;
import java.util.List;

import common.model.QuestionModel;
import common.model.Utility;


/**
 * This fragment controls Bluetooth to communicate with other devices.
 */
public class BluetoothChatFragment extends Fragment {

    private static final String TAG = "BluetoothChatFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;


    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendQOne, mSendQTwo;
    private Button mbtnOp1, mbtnOp2, mbtnOp3, mbtnOp4;
    private Button mBtnJoinGame, btnStartGame,btnBack;
    private LinearLayout llOption, llQbuttons;
    private TextView tvQuestion;

    QuestionModel crntQuestion;
    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothChatService mChatService = null;
    private Button mBtnHostGame;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            activity.finish();
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mChatService == null) {
            setupChat();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mChatService != null) {
            mChatService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
                // Start the Bluetooth chat services
                mChatService.start();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_bluetooth_chat, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendQOne = (Button) view.findViewById(R.id.button_send);
        mSendQTwo = (Button) view.findViewById(R.id.buttonQuestionTwo);
        mbtnOp1 = (Button) view.findViewById(R.id.btnOp1);
        mbtnOp2 = (Button) view.findViewById(R.id.btnOp2);
        mbtnOp3 = (Button) view.findViewById(R.id.btnOp3);
        mbtnOp4 = (Button) view.findViewById(R.id.btnOp4);
        mbtnOp4 = (Button) view.findViewById(R.id.btnOp4);
        btnStartGame = (Button) view.findViewById(R.id.btnStartGame);
        mBtnJoinGame = (Button) view.findViewById(R.id.btnJoinGame);
        mBtnHostGame = (Button) view.findViewById(R.id.btnHostGame);
        llOption = (LinearLayout) view.findViewById(R.id.llOptions);
        llQbuttons = (LinearLayout) view.findViewById(R.id.llQbuttons);
        tvQuestion = (TextView) view.findViewById(R.id.tvQuestion);
        btnBack = (Button) view.findViewById(R.id.btnBack);
        llQbuttons.setVisibility(View.GONE);

        mbtnOp4.setOnClickListener(new optionClickListener());
        mbtnOp3.setOnClickListener(new optionClickListener());
        mbtnOp2.setOnClickListener(new optionClickListener());
        mbtnOp1.setOnClickListener(new optionClickListener());

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                llOption.setVisibility(View.GONE);
                llQbuttons.setVisibility(View.GONE);
                btnStartGame.setVisibility(View.VISIBLE);
                mBtnJoinGame.setVisibility(View.GONE);
                mBtnHostGame.setVisibility(View.GONE);
                Utility.isHost=false;
                tvQuestion.setVisibility(View.GONE);
                mChatService.stop();
            }
        });

    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBtnJoinGame.setVisibility(View.VISIBLE);
                mBtnHostGame.setVisibility(View.VISIBLE);
                btnStartGame.setVisibility(View.GONE);
            }
        });
        // Initialize the send button with a listener that for click events
        mSendQOne.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString();


                    sendMessage("{\"CorrectAns\": \"Option 1\",\"optionA\": \"Option 1\",\"optionB\": \"Option 2\",\"optionC\": \"Option 3\",\"optionD\": \"Option 4\",\"isHost\": true,\"Question\": \"What is answer of this question, options are: \"}");

                }
            }
        });
        mSendQTwo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                Toast.makeText(getContext(), "Ready", Toast.LENGTH_LONG).show();

//                sendMessage("{\"optionA\": \"Option One\",\"optionB\": \"Option two\",\"optionC\": \"Option Three\",\"optionD\": \"Option Four\",\"isHost\": true,\"Question\": \"What is answer of this Second question, options are: \"}");


            }
        });
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(getActivity(), mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        mBtnJoinGame.setVisibility(View.GONE);
        mBtnHostGame.setVisibility(View.GONE);
        mBtnHostGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                llOption.setVisibility(View.GONE);
                llQbuttons.setVisibility(View.VISIBLE);
                mSendQOne.setText("Start Game!");
                mBtnJoinGame.setVisibility(View.GONE);
                mBtnHostGame.setVisibility(View.GONE);
                Utility.isHost = true;
            }
        });
        mBtnJoinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBtnJoinGame.setVisibility(View.GONE);
                mBtnHostGame.setVisibility(View.GONE);
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                Utility.isHost = false;
            }
        });
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        try {
            FragmentActivity activity = getActivity();
            if (null == activity) {
                return;
            }
            final ActionBar actionBar = activity.getActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(resId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        try {
            FragmentActivity activity = getActivity();
            if (null == activity) {
                return;
            }
            final ActionBar actionBar = activity.getActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(subTitle);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            try {
                                setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                            System.out.println("STATE_LISTEN");
                        case BluetoothChatService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //check here
                    Gson gson = new Gson();
                    JsonReader reader = new JsonReader(new StringReader(readMessage.toString().trim()));
                    reader.setLenient(true);
                    QuestionModel questionModel = gson.fromJson(reader, QuestionModel.class);
                    crntQuestion = questionModel;
                    if (questionModel != null) {
                        if (questionModel.getIsHost()) {
                            System.out.println("" + questionModel.getQuestion());
                            if (!Utility.isHost)
                                updateQuestion();
                        }
                    }
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                        if (!Utility.isHost) {
//
//                            llQbuttons.setVisibility(View.VISIBLE);
//                            mSendQOne.setVisibility(View.GONE);
//                            mSendQTwo.setVisibility(View.VISIBLE);
//                            mBtnJoinGame.setVisibility(View.GONE);
//                            mBtnHostGame.setVisibility(View.GONE);
//                            mSendQTwo.setText("Make Ready");
//                            return;
                            llQbuttons.setVisibility(View.VISIBLE);
                            mBtnJoinGame.setVisibility(View.GONE);
                            mBtnHostGame.setVisibility(View.GONE);
                            llOption.setVisibility(View.GONE);
                            mSendQOne.setVisibility(View.GONE);
                            mSendQTwo.setText("Make Ready");
                            mSendQTwo.setVisibility(View.VISIBLE);
                            Toast.makeText(getContext(), "You joined game successfully", Toast.LENGTH_LONG).show();
                        } else {
//                            if (llQbuttons.getVisibility() != View.GONE) {
//                                llQbuttons.setVisibility(View.GONE);
//                                mBtnJoinGame.setVisibility(View.GONE);
//                                mBtnHostGame.setVisibility(View.GONE);
//                                llOption.setVisibility(View.VISIBLE);
//                                Toast.makeText(activity, "Game started"
//                                        + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
//                            }
                            llQbuttons.setVisibility(View.VISIBLE);

                            mBtnJoinGame.setVisibility(View.GONE);
                            mBtnHostGame.setVisibility(View.GONE);
                            Toast.makeText(activity, "Game started"
                                    + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                        }

                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    private void updateQuestion() {
        if (llOption.getVisibility() == View.GONE) {
            llQbuttons.setVisibility(View.GONE);
            llOption.setVisibility(View.VISIBLE);
        }
        mSendQOne.setVisibility(View.GONE);
        mSendQTwo.setVisibility(View.GONE);
        llOption.setVisibility(View.VISIBLE);
        tvQuestion.setVisibility(View.VISIBLE);

        tvQuestion.setText(crntQuestion.getQuestion());
        mbtnOp1.setText(crntQuestion.getOptionA());
        mbtnOp2.setText(crntQuestion.getOptionB());
        mbtnOp3.setText(crntQuestion.getOptionC());
        mbtnOp4.setText(crntQuestion.getOptionD());

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, 0);
                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, 0);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, int secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mChatService.connect(device, true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
//        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    private class optionClickListener implements View.OnClickListener {
        @Override
        public void onClick(View view) {
            if (view != null) {
                String s = ((Button) view).getText().toString();
                if (crntQuestion.getCorrectAns().equalsIgnoreCase(s)) {
                    Toast.makeText(getContext(), "Option is correct", Toast.LENGTH_LONG).show();
                } else
                    Toast.makeText(getContext(), "Option is incorrect", Toast.LENGTH_LONG).show();
            }
        }
    }
}
