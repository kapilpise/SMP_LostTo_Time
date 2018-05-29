package bluetoothchat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.android.bluetoothchat.R;

/**
 * Created by Kapil on 03-03-2018.
 */

public class HostFragment extends Fragment {

    Button btnStartGame,btnKickUser;
    TextView tvName, tvJoinName;
    LinearLayout llJoin;
    IHostStartGame iHostStartGame;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_host, container, false);
        iHostStartGame= (IHostStartGame) getActivity();
        btnStartGame = (Button) view.findViewById(R.id.btnStartGame);
        btnKickUser = (Button) view.findViewById(R.id.btnKickUser);
        tvName = (TextView) view.findViewById(R.id.tvName);
        tvJoinName = (TextView) view.findViewById(R.id.tvJoinName);
        llJoin = (LinearLayout) view.findViewById(R.id.llJoin);
        Bundle bundle = getArguments();
        tvName.setText(bundle.getString("Name"));
        btnStartGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iHostStartGame.startGame();
            }
        });
        btnKickUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                iHostStartGame.kickUser();
            }
        });

        return view;
    }

    public void setJoinName(String name) {
        tvJoinName.setText(name);
        llJoin.setVisibility(View.VISIBLE);
    }

    interface IHostStartGame{
        void startGame();
        void kickUser();
    }
}
