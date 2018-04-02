package bluetoothchat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bluetoothchat.R;

import common.model.Utility;

/**
 * Created by Kapil on 02-03-2018.
 */

public class MarkReadyFragment extends Fragment {

    Button btnMarkReady;
    TextView tvName;
    ISendMarkReadyMessage iSendMarkReadyMessage;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_markready_fragment, container, false);
        iSendMarkReadyMessage = (ISendMarkReadyMessage) getActivity();
        btnMarkReady = (Button) view.findViewById(R.id.btnMarkReadyGame);
        tvName = (TextView) view.findViewById(R.id.tvName);
        Bundle bundle = getArguments();
        tvName.setText(bundle.getString("Name"));
        btnMarkReady.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnMarkReady.getText().toString().equalsIgnoreCase("Leave Lobby")) {
                    try {
                        getActivity().onBackPressed();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return;
                }
                Utility.MarkReady = true;
                Toast.makeText(getActivity(), "Marked Ready", Toast.LENGTH_LONG).show();
                btnMarkReady.setText("Leave Lobby");
                iSendMarkReadyMessage.sendReadyMessage();
            }
        });

        return view;
    }

    interface ISendMarkReadyMessage {
        void sendReadyMessage();
    }
}
