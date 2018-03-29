package bluetoothchat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.android.bluetoothchat.R;

import common.model.Utility;

/**
 * Created by Kapil on 02-03-2018.
 */

public class OptionFragment extends Fragment {

    Button btnJoinGame;
    Button btnHostGame;
    IOptionListener iOptionListener;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_options_fragment, container, false);
        btnJoinGame = (Button) view.findViewById(R.id.btnJoinGame);
        btnHostGame = (Button) view.findViewById(R.id.btnHostGame);
        iOptionListener = (IOptionListener) ((LaunchActivity) getActivity());
        btnHostGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utility.isHost = true;
                iOptionListener.onClickHost();
            }
        });
        btnJoinGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utility.isHost = false;
                iOptionListener.onClickJoin();
            }
        });

        return view;
    }

    interface IOptionListener {
        void onClickHost();

        void onClickJoin();
    }
}
